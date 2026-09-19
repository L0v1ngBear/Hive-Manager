package my.hive.domain.aftersales.service;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import jakarta.annotation.Resource;
import my.hive.domain.aftersales.mapper.*;
import my.hive.domain.aftersales.model.dto.*;
import my.hive.domain.aftersales.model.entity.*;
import my.hive.shared.context.TenantPermissionContext;
import my.hive.shared.exception.BusinessException;
import my.hive.shared.permission.PermissionCatalogV3;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

@Service
public class AfterSalesTreatmentService {
    @Resource private AfterSalesTicketMapper ticketMapper;
    @Resource private AfterSalesTreatmentMapper treatmentMapper;
    @Resource private AfterSalesPartMapper partMapper;
    @Resource private AfterSalesPartStockRecordMapper stockRecordMapper;
    @Resource private AfterSalesService afterSalesService;

    public List<AfterSalesTreatment> list(Long ticketId) {
        requireTicket(ticketId, false);
        return records(ticketId).stream().map(this::hydrate).toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public AfterSalesTreatment create(Long ticketId, AfterSalesTreatmentSaveRequest request) {
        AfterSalesTicket ticket = requireTicket(ticketId, true);
        requireOperator(ticket);
        List<AfterSalesTreatment> records = records(ticketId);
        String hash = hash(JSON.toJSONString(request));
        for (AfterSalesTreatment record : records) {
            if (Objects.equals(request.getRequestKey(), record.getRequestKey())) {
                if (!hash.equals(record.getRequestHash())) throw new BusinessException("重复请求标识对应内容不同，请重新打开新增表单");
                return hydrate(record);
            }
        }
        if (!Set.of("processing", "closed").contains(ticket.getStatus())) {
            throw new BusinessException("请先完成原工单研判、审批及出库，再追加处理记录");
        }
        if (Integer.valueOf(1).equals(ticket.getApprovalRequired()) && !"approved".equals(ticket.getApprovalStatus())) {
            throw new BusinessException("工单尚未审核通过，不能追加处理");
        }
        if (ticket.getDiagnosis() == null || ticket.getDiagnosis().isBlank()) throw new BusinessException("请先完成故障研判");
        if (ticket.getAssigneeUserId() == null) throw new BusinessException("请先指派处理人");
        if (records.size() >= 100) throw new BusinessException("单个工单最多保留100次追加处理");
        var record = new AfterSalesTreatment();
        record.setTenantCode(ticket.getTenantCode()); record.setTicketId(ticketId); record.setTicketNo(ticket.getTicketNo());
        record.setSequenceNo(records.size() + 1); record.setRequestKey(request.getRequestKey()); record.setRequestHash(hash);
        record.setOriginalOpeningDate(ticket.getOpeningDate());
        record.setCreatorUserId(TenantPermissionContext.getUserId()); record.setCreatorName(afterSalesService.currentOperatorName());
        record.setVersion(0); record.setCreateTime(LocalDateTime.now()); record.setUpdateTime(record.getCreateTime());
        record.setStatus("waiting_outbound");
        applyDetails(record, request);
        if (record.getParts().isEmpty()) record.setStatus("processing");
        treatmentMapper.insert(record);
        // Explicit continuation reopens the case but leaves all original treatment and follow-up fields intact.
        ticket.setStatus("processing");
        ticketMapper.updateById(ticket);
        return hydrate(record);
    }

    @Transactional(rollbackFor = Exception.class)
    public AfterSalesTreatment update(Long ticketId, Long id, AfterSalesTreatmentSaveRequest request) {
        AfterSalesTicket ticket = requireTicket(ticketId, true);
        requireOperator(ticket);
        if (!"processing".equals(ticket.getStatus())) throw new BusinessException("主工单不在处理中，请刷新后重试");
        AfterSalesTreatment record = requireRecord(ticketId, id, request.getVersion());
        AfterSalesTreatmentPolicy.requireEditable(record.getStatus());
        applyDetails(record, request);
        if ("waiting_outbound".equals(record.getStatus()) && record.getParts().isEmpty()) record.setStatus("processing");
        persist(record);
        return hydrate(record);
    }

    private void applyDetails(AfterSalesTreatment record, AfterSalesTreatmentSaveRequest request) {
        boolean motor = Set.of("motor_replacement", "parts_and_motor").contains(request.getTreatmentType());
        if (motor && (request.getNewMotorModel() == null || request.getNewMotorModel().isBlank()
                || request.getMotorQuantity() == null || request.getMotorQuantity() <= 0)) {
            throw new BusinessException("更换电机请填写新电机型号和数量");
        }
        boolean requiresParts = Set.of("resend_parts", "parts_and_motor").contains(request.getTreatmentType());
        if (requiresParts && (request.getParts() == null || request.getParts().isEmpty())) throw new BusinessException("补发配件请填写配件明细");
        List<AfterSalesTicketPart> parts;
        if ("processing".equals(record.getStatus())) {
            // Inventory is already final for this record. Compare submitted lines before accepting logistics edits.
            var previous = JSON.parseObject(record.getDetailsJson(), AfterSalesTreatmentSaveRequest.class);
            if (!Objects.equals(JSON.toJSONString(previous.getParts()), JSON.toJSONString(request.getParts()))
                    || !Objects.equals(previous.getTreatmentType(), request.getTreatmentType())) {
                throw new BusinessException("已进入处理的配件和处理类型不可修改，请新增处理记录");
            }
            parts = JSON.parseArray(record.getPartsJson(), AfterSalesTicketPart.class);
        } else {
            parts = normalizeParts(request.getParts());
        }
        afterSalesService.normalizeRepairImages(request.getRepairImages());
        record.setTreatmentType(request.getTreatmentType());
        record.setDetailsJson(JSON.toJSONString(request));
        record.setPartsJson(JSON.toJSONString(parts));
        record.setParts(parts);
    }

    @Transactional(rollbackFor = Exception.class)
    public AfterSalesTreatment action(Long ticketId, Long id, String action, AfterSalesTreatmentActionRequest request) {
        AfterSalesTicket ticket = requireTicket(ticketId, true);
        if (!"processing".equals(ticket.getStatus())) throw new BusinessException("主工单不在处理中，请刷新后重试");
        AfterSalesTreatment record = requireRecord(ticketId, id, request.getVersion());
        if ("outbound".equals(action)) {
            if (!TenantPermissionContext.hasPermission(PermissionCatalogV3.CODE_AFTER_SALES_PART_OUTBOUND)) throw new BusinessException(403, "没有售后配件出库权限");
            if (!"waiting_outbound".equals(record.getStatus())) throw new BusinessException("本次配件已出库或不在待出库状态");
            outbound(record);
            record.setStatus("processing");
        } else {
            requireOperator(ticket);
            if ("complete".equals(action)) {
                if (!"processing".equals(record.getStatus())) throw new BusinessException("只有处理中的记录可以完成");
                if (request.getResolution() == null || request.getResolution().isBlank()) throw new BusinessException("请填写本次处理结果");
                record.setResolution(request.getResolution().trim());
                record.setCompletedTime(LocalDateTime.now()); record.setStatus("waiting_follow_up");
            } else if ("follow-up".equals(action)) {
                if (!"waiting_follow_up".equals(record.getStatus())) throw new BusinessException("本次处理尚未完成或已回访，不能覆盖历史回访");
                var follow = request.getFollowUp();
                if (follow == null || follow.getContent() == null || follow.getContent().isBlank() || follow.getResolved() == null
                        || follow.getSatisfaction() == null || !Set.of("满意", "一般", "不满意").contains(follow.getSatisfaction())) throw new BusinessException("请填写完整回访结果");
                afterSalesService.normalizeRepairImages(follow.getFollowUpImages());
                follow.setFollowUpTime(LocalDateTime.now());
                record.setFollowUpTime(follow.getFollowUpTime()); record.setFollowUpOperatorName(afterSalesService.currentOperatorName());
                record.setFollowUpJson(JSON.toJSONString(follow));
                record.setStatus(Boolean.TRUE.equals(follow.getResolved()) ? "resolved" : "unresolved");
            } else throw new BusinessException("不支持的处理操作");
        }
        persist(record);
        return hydrate(record);
    }

    private void outbound(AfterSalesTreatment record) {
        if (record.getParts() == null || record.getParts().isEmpty()) throw new BusinessException("本次处理没有待出库配件");
        // Stable part ordering limits deadlock risk when two cases use the same stock.
        for (AfterSalesTicketPart line : record.getParts().stream().sorted(Comparator.comparing(AfterSalesTicketPart::getPartId)).toList()) {
            var part = partMapper.selectOne(new LambdaQueryWrapper<AfterSalesPart>()
                    .eq(AfterSalesPart::getTenantCode, record.getTenantCode()).eq(AfterSalesPart::getId, line.getPartId()).eq(AfterSalesPart::getStatus, 1));
            if (part == null) throw new BusinessException("配件不存在或已停用");
            int before = part.getStockQty() == null ? 0 : part.getStockQty();
            int quantity = line.getQuantity();
            int updated = partMapper.update(null, new LambdaUpdateWrapper<AfterSalesPart>()
                    .eq(AfterSalesPart::getTenantCode, record.getTenantCode()).eq(AfterSalesPart::getId, part.getId())
                    .eq(AfterSalesPart::getVersion, part.getVersion()).ge(AfterSalesPart::getStockQty, quantity)
                    .set(AfterSalesPart::getStockQty, before - quantity).set(AfterSalesPart::getVersion, part.getVersion() + 1));
            if (updated != 1) throw new BusinessException("配件库存不足或已被其他操作更新，请刷新后重试");
            var stock = new AfterSalesPartStockRecord();
            stock.setTenantCode(record.getTenantCode()); stock.setTicketId(record.getTicketId()); stock.setTreatmentId(record.getId());
            stock.setPartId(part.getId()); stock.setOperateType("out"); stock.setQuantity(quantity); stock.setBeforeQty(before); stock.setAfterQty(before - quantity);
            stock.setOperatorUserId(TenantPermissionContext.getUserId()); stock.setOperatorName(afterSalesService.currentOperatorName());
            stock.setRemark("第" + record.getSequenceNo() + "次追加处理"); stockRecordMapper.insert(stock);
            line.setLineStatus("outbound");
        }
        record.setPartsJson(JSON.toJSONString(record.getParts()));
    }

    public my.hive.shared.dto.PageResult<AfterSalesTreatment> myTasks(Integer pageNum, Integer pageSize) {
        if (TenantPermissionContext.getUserId() == null || TenantPermissionContext.getTenantCode() == null) throw new BusinessException(403, "请先登录");
        var query = new LambdaQueryWrapper<AfterSalesTreatment>()
                .eq(AfterSalesTreatment::getTenantCode, TenantPermissionContext.getTenantCode())
                .in(AfterSalesTreatment::getStatus, Set.of("waiting_outbound", "processing", "waiting_follow_up", "unresolved"))
                .apply("(status <> 'unresolved' OR NOT EXISTS (SELECT 1 FROM after_sales_treatment newer WHERE newer.ticket_id = after_sales_treatment.ticket_id AND newer.tenant_code = after_sales_treatment.tenant_code AND (newer.status IN ('waiting_outbound','processing','waiting_follow_up') OR newer.update_time > after_sales_treatment.update_time OR (newer.update_time = after_sales_treatment.update_time AND newer.id > after_sales_treatment.id))))")
                .apply("EXISTS (SELECT 1 FROM after_sales_ticket t WHERE t.id = after_sales_treatment.ticket_id AND t.tenant_code = {0} AND t.assignee_user_id = {1})",
                        TenantPermissionContext.getTenantCode(), TenantPermissionContext.getUserId())
                .orderByAsc(AfterSalesTreatment::getCreateTime).orderByAsc(AfterSalesTreatment::getId);
        var page = treatmentMapper.selectPage(new com.baomidou.mybatisplus.extension.plugins.pagination.Page<AfterSalesTreatment>(
                pageNum == null ? 1 : Math.max(1, pageNum), pageSize == null ? 10 : Math.max(1, Math.min(100, pageSize))), query);
        var result = new my.hive.shared.dto.PageResult<AfterSalesTreatment>();
        result.setData(page.getRecords().stream().map(this::hydrate).toList()); result.setTotal(page.getTotal());
        result.setCurrent(page.getCurrent()); result.setSize(page.getSize()); result.setPages(page.getPages());
        return result;
    }

    private List<AfterSalesTicketPart> normalizeParts(List<AfterSalesTicketSaveRequest.AfterSalesTicketPartItem> items) {
        var lines = new ArrayList<AfterSalesTicketPart>();
        var seen = new HashSet<Long>();
        for (var item : items == null ? List.<AfterSalesTicketSaveRequest.AfterSalesTicketPartItem>of() : items) {
            if (item == null || item.getPartId() == null || !seen.add(item.getPartId())
                    || item.getQuantity() == null || item.getQuantity() <= 0) throw new BusinessException("配件明细不能为空、重复或数量小于1");
            if (item.getPartLocation() != null && !Set.of("一车间", "二车间", "三车间", "其他").contains(item.getPartLocation())) throw new BusinessException("配件地点不合法");
            AfterSalesPart part = partMapper.selectOne(new LambdaQueryWrapper<AfterSalesPart>()
                    .eq(AfterSalesPart::getTenantCode, TenantPermissionContext.getTenantCode()).eq(AfterSalesPart::getId, item.getPartId()).eq(AfterSalesPart::getStatus, 1));
            if (part == null) throw new BusinessException("配件不存在或已停用");
            var line = new AfterSalesTicketPart();
            line.setPartId(part.getId()); line.setPartCode(part.getPartCode()); line.setPartName(part.getPartName());
            line.setModelSpec(part.getModelSpec()); line.setUnit(part.getUnit()); line.setQuantity(item.getQuantity());
            line.setPartLocation(item.getPartLocation()); line.setRemark(item.getRemark()); line.setLineStatus("pending");
            lines.add(line);
        }
        return lines;
    }

    private AfterSalesTicket requireTicket(Long id, boolean lock) {
        if (id == null || id <= 0 || TenantPermissionContext.getTenantCode() == null) throw new BusinessException("售后工单不能为空");
        var ticket = ticketMapper.selectOne(new LambdaQueryWrapper<AfterSalesTicket>()
                .eq(AfterSalesTicket::getTenantCode, TenantPermissionContext.getTenantCode()).eq(AfterSalesTicket::getId, id)
                .last(lock ? "LIMIT 1 FOR UPDATE" : "LIMIT 1"));
        if (ticket == null) throw new BusinessException("售后工单不存在或不属于当前组织");
        return ticket;
    }

    private void requireOperator(AfterSalesTicket ticket) {
        if (!TenantPermissionContext.hasPermission(PermissionCatalogV3.CODE_AFTER_SALES_UPDATE)
                && !(TenantPermissionContext.hasPermission(PermissionCatalogV3.CODE_AFTER_SALES_PROCESS)
                && Objects.equals(ticket.getAssigneeUserId(), TenantPermissionContext.getUserId())
                && TenantPermissionContext.getUserId() != null)) throw new BusinessException(403, "仅工单负责人或具有编辑权限的人员可以处理");
    }

    private List<AfterSalesTreatment> records(Long ticketId) {
        return treatmentMapper.selectList(new LambdaQueryWrapper<AfterSalesTreatment>()
                .eq(AfterSalesTreatment::getTenantCode, TenantPermissionContext.getTenantCode())
                .eq(AfterSalesTreatment::getTicketId, ticketId).orderByAsc(AfterSalesTreatment::getSequenceNo));
    }

    private AfterSalesTreatment requireRecord(Long ticketId, Long id, Integer version) {
        var record = treatmentMapper.selectOne(new LambdaQueryWrapper<AfterSalesTreatment>()
                .eq(AfterSalesTreatment::getTenantCode, TenantPermissionContext.getTenantCode())
                .eq(AfterSalesTreatment::getTicketId, ticketId).eq(AfterSalesTreatment::getId, id));
        if (record == null) throw new BusinessException("处理记录不存在或不属于当前工单");
        if (version == null || !version.equals(record.getVersion())) throw new BusinessException("处理记录已更新，请刷新后重试");
        return hydrate(record);
    }

    private AfterSalesTreatment hydrate(AfterSalesTreatment record) {
        record.setDetails(JSON.parseObject(record.getDetailsJson(), AfterSalesTreatmentSaveRequest.class));
        record.setParts(JSON.parseArray(record.getPartsJson(), AfterSalesTicketPart.class));
        record.setFollowUp(JSON.parseObject(record.getFollowUpJson(), AfterSalesTicketFollowUpRequest.class));
        return record;
    }

    private void persist(AfterSalesTreatment record) {
        record.setVersion(record.getVersion() + 1); record.setUpdateTime(LocalDateTime.now());
        treatmentMapper.updateById(record);
    }

    private String hash(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch (java.security.NoSuchAlgorithmException exception) { throw new IllegalStateException(exception); }
    }
}
