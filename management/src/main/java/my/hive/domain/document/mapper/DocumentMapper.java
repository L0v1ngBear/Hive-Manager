package my.hive.domain.document.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import my.hive.domain.document.model.entity.Document;
import org.apache.ibatis.annotations.Select;

public interface DocumentMapper extends BaseMapper<Document> {

    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT COALESCE(SUM(file_size), 0) FROM document WHERE tenant_code = #{tenantCode} AND is_deleted = 0 AND type = 1")
    Long sumActiveFileSize(String tenantCode);
}
