package my.hive.shared.model;

import lombok.Data;
/**
 * EncryptPayload 属于管理端后端通用能力层，定义通用模型。
 */
@Data
public class EncryptPayload {

    private String iv;

    private String ciphertext;

    private String mac;
}
