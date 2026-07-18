package my.hive.shared.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
/**
 * Result 属于管理端后端通用能力层，定义通用传输对象。
 */
@Data
public class Result<T> {

    private Integer code;

    private String reason;

    private String msg;

    private T data;

    @JsonIgnore
    private Boolean encrypted = false;

    @JsonIgnore
    private String alg;

    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.setCode(200);
        result.setMsg("success");
        result.setData(data);
        result.setEncrypted(false);
        return result;
    }

    public static <T> Result<T> fail(Integer code, String msg) {
        return fail(code, null, msg, null);
    }

    public static <T> Result<T> fail(Integer code, String reason, String msg, T data) {
        Result<T> result = new Result<>();
        result.setCode(code);
        result.setReason(reason);
        result.setMsg(msg);
        result.setData(data);
        result.setEncrypted(false);
        return result;
    }

    public static <T> Result<T> fail(Integer code, String reason, String msg) {
        return fail(code, reason, msg, null);
    }

    public static <T> Result<T> fail(String msg) {
        return fail(500, msg);
    }
}
