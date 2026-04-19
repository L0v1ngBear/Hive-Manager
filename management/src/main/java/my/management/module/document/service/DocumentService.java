package my.management.module.document.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import my.hive.common.context.TenantPermissionContext;
import my.hive.common.exception.BusinessException;
import my.management.module.document.DocumentTypeEnum;
import my.management.module.document.mapper.DocumentMapper;
import my.management.module.document.model.dto.DocumentAddRequest;
import my.management.module.document.model.entity.Document;
import my.management.module.document.model.vo.DocumentVO;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
/**
 * DocumentService 属于管理端后端单据模块，实现核心业务编排与规则逻辑。
 */
@Slf4j
@Service
public class DocumentService {

    @Resource
    private DocumentMapper documentMapper;

    public List<Document> selectDocumentByParentId(Long parentId) {
        LambdaQueryWrapper<Document> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Document::getParentId, parentId);
        queryWrapper.eq(Document::getTenantCode, TenantPermissionContext.getTenantCode());
        queryWrapper.orderByAsc(Document::getType);
        queryWrapper.orderByAsc(Document::getCreateTime);
        return documentMapper.selectList(queryWrapper);
    }

    @Transactional(rollbackFor = Exception.class)
    public void addFolder(DocumentAddRequest request) {
        LambdaQueryWrapper<Document> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Document::getParentId, request.getParentId());
        queryWrapper.eq(Document::getName, request.getName());
        queryWrapper.eq(Document::getTenantCode, TenantPermissionContext.getTenantCode());
        if (documentMapper.selectOne(queryWrapper) != null) {
            throw new BusinessException("folder already exists");
        }
        insertFolder(request);
    }

    @Transactional(rollbackFor = Exception.class)
    protected void insertFolder(DocumentAddRequest request) {
        Document document = new Document();
        document.setName(request.getName());
        document.setParentId(request.getParentId());
        document.setType(DocumentTypeEnum.FOLDER.getType());
        document.setTenantCode(TenantPermissionContext.getTenantCode());
        document.setCreatorId(TenantPermissionContext.getUserId());
        documentMapper.insert(document);
    }

    public void renameDocument(Long documentId, String newName) {
        Document document = requireDocument(documentId);
        LambdaQueryWrapper<Document> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Document::getParentId, document.getParentId());
        queryWrapper.eq(Document::getName, newName);
        queryWrapper.eq(Document::getTenantCode, TenantPermissionContext.getTenantCode());
        Document oldDocument = documentMapper.selectOne(queryWrapper);
        if (oldDocument != null && !oldDocument.getId().equals(documentId)) {
            throw new BusinessException("document name already exists");
        }
        document.setName(newName);
        documentMapper.updateById(document);
    }

    public void moveDocument(Long documentId, Long targetParentId) {
        if (documentId.equals(targetParentId)) {
            throw new BusinessException("target location is invalid");
        }

        Document currentDoc = requireDocument(documentId);

        if (targetParentId != null && targetParentId != 0L) {
            Document targetDoc = requireDocument(targetParentId);
            if (!DocumentTypeEnum.FOLDER.getType().equals(targetDoc.getType())) {
                throw new BusinessException("target location is not a folder");
            }
        }

        if (DocumentTypeEnum.FOLDER.getType().equals(currentDoc.getType()) && targetParentId != null && targetParentId != 0L) {
            Long checkId = targetParentId;
            int maxDepth = 20;
            int depth = 0;
            while (checkId != null && checkId != 0L && depth < maxDepth) {
                if (checkId.equals(documentId)) {
                    throw new BusinessException("cannot move a folder into its child folder");
                }
                Document checkDoc = requireDocument(checkId);
                checkId = checkDoc.getParentId();
                depth++;
            }
        }

        currentDoc.setParentId(targetParentId);
        documentMapper.updateById(currentDoc);
    }

    public List<DocumentVO> getBreadcrumbs(Long documentId) {
        if (documentId == null || documentId <= 0) {
            return Collections.emptyList();
        }

        List<DocumentVO> breadcrumbs = new ArrayList<>();
        Long currentId = documentId;
        int maxDepth = 20;
        int depth = 0;

        while (currentId != null && currentId > 0 && depth < maxDepth) {
            Document document = documentMapper.selectById(currentId);
            if (document == null) {
                break;
            }
            DocumentVO vo = new DocumentVO();
            BeanUtils.copyProperties(document, vo);
            breadcrumbs.add(vo);
            currentId = document.getParentId();
            depth++;
        }

        if (depth >= maxDepth) {
            log.warn("document breadcrumbs reached max depth, documentId={}", documentId);
        }

        Collections.reverse(breadcrumbs);
        return breadcrumbs;
    }

    private Document requireDocument(Long documentId) {
        Document document = documentMapper.selectById(documentId);
        if (document == null || !TenantPermissionContext.getTenantCode().equals(document.getTenantCode())) {
            throw new BusinessException("document not found");
        }
        return document;
    }
}
