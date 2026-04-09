package my.management.controller;

import jakarta.annotation.Resource;
import my.management.common.dto.Result;
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

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/document")
@Validated
public class DocumentController {

    @Resource
    private DocumentService documentService;

    @GetMapping("/list/{parentId}")
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
    public Result<Void> createFolder(@RequestBody DocumentAddRequest request) {
        documentService.addFolder(request);
        return Result.success(null);
    }

    @PutMapping("/rename")
    public Result<Void> renameDocument(@RequestParam Long documentId, @RequestParam String newName) {
        documentService.renameDocument(documentId, newName);
        return Result.success(null);
    }

    @PutMapping("/move")
    public Result<Void> moveDocument(@RequestParam Long documentId, @RequestParam Long newParentId) {
        documentService.moveDocument(documentId, newParentId);
        return Result.success(null);
    }

    @GetMapping("/breadcrumbs")
    public Result<List<DocumentVO>> breadcrumbs(@RequestParam Long documentId) {
        return Result.success(documentService.getBreadcrumbs(documentId));
    }
}
