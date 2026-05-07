package my.management.controller;

import jakarta.annotation.Resource;
import my.hive.common.annotation.RequirePermission;
import my.hive.common.dto.Result;
import my.management.common.tenant.RequireTenantFeature;
import my.management.module.document.model.dto.DocumentAddRequest;
import my.management.module.document.model.entity.Document;
import my.management.module.document.model.vo.DocumentVO;
import my.management.module.document.service.DocumentService;
import org.springframework.beans.BeanUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;
/**
 * DocumentController 是管理端后端请求入口控制类，负责接收请求并调用对应服务。
 */
    // 文件内容存储已预留扩展点，后续接入 OSS 时保持当前接口契约不变。
@org.springframework.stereotype.Controller
@org.springframework.web.bind.annotation.ResponseBody
@RestController
@RequestMapping("/document")
@RequireTenantFeature("module.document")
@Validated
public class DocumentController {

    @Resource
    private DocumentService documentService;

    @GetMapping("/list/{parentId}")
    @RequirePermission(value = "document:list", message = "您没有权限查看文档列表")
    public Result<List<DocumentVO>> list(@PathVariable Long parentId) {
        List<Document> documentList = documentService.selectDocumentByParentId(parentId);
        List<DocumentVO> documentVOList = documentList.stream().map(doc -> {
            DocumentVO documentVO = new DocumentVO();
            BeanUtils.copyProperties(doc, documentVO);
            return documentVO;
        }).collect(Collectors.toList());
        return Result.success(documentVOList);
    }

    @PostMapping("/folder/create")
    @RequirePermission(value = "document:folder:create", message = "您没有权限创建文件夹")
    public Result<Void> createFolder(@RequestBody DocumentAddRequest request) {
        documentService.addFolder(request);
        return Result.success(null);
    }

    @PostMapping("/file/upload")
    @RequirePermission(value = "document:file:upload", message = "您没有权限上传文件")
    public Result<DocumentVO> uploadFile(@RequestParam("file") MultipartFile file,
                                         @RequestParam(value = "parentId", required = false, defaultValue = "0") Long parentId) {
        return Result.success(documentService.uploadFile(file, parentId));
    }

    @PutMapping("/rename")
    @RequirePermission(value = "document:rename", message = "您没有权限重命名文档")
    public Result<Void> renameDocument(@RequestParam Long documentId, @RequestParam String newName) {
        documentService.renameDocument(documentId, newName);
        return Result.success(null);
    }

    @PutMapping("/move")
    @RequirePermission(value = "document:move", message = "您没有权限移动文档")
    public Result<Void> moveDocument(@RequestParam Long documentId, @RequestParam Long newParentId) {
        documentService.moveDocument(documentId, newParentId);
        return Result.success(null);
    }

    @GetMapping("/breadcrumbs")
    @RequirePermission(value = "document:breadcrumbs", message = "您没有权限查看文档面包屑")
    public Result<List<DocumentVO>> breadcrumbs(@RequestParam Long documentId) {
        return Result.success(documentService.getBreadcrumbs(documentId));
    }
}
