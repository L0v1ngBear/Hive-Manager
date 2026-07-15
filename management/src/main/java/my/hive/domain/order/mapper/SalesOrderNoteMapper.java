package my.hive.domain.order.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import my.hive.domain.order.model.entity.SalesOrderNote;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

@Mapper
@InterceptorIgnore(tenantLine = "true")
public interface SalesOrderNoteMapper extends BaseMapper<SalesOrderNote> {

    @Update("""
            UPDATE sales_order_note
            SET content = #{content},
                updater_user_id = #{updaterUserId},
                updater_name = #{updaterName},
                version = version + 1,
                update_time = #{updateTime}
            WHERE id = #{id}
              AND BINARY tenant_code = BINARY #{tenantCode}
              AND BINARY order_id = BINARY #{orderId}
              AND version = #{expectedVersion}
            """)
    int updateContent(@Param("id") Long id,
                      @Param("tenantCode") String tenantCode,
                      @Param("orderId") String orderId,
                      @Param("expectedVersion") Integer expectedVersion,
                      @Param("content") String content,
                      @Param("updaterUserId") Long updaterUserId,
                      @Param("updaterName") String updaterName,
                      @Param("updateTime") LocalDateTime updateTime);
}
