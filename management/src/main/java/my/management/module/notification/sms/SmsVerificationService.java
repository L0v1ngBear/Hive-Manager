package my.management.module.notification.sms;

import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class SmsVerificationService {

    @Resource
    private SmsSender smsSender;

    public boolean sendCode(String phone, String sceneTitle, String code, long expireMinutes) {
        if (phone == null || phone.isBlank() || code == null || code.isBlank()) {
            return false;
        }
        String content = "您的验证码是 " + code + "，" + expireMinutes + " 分钟内有效。如非本人操作，请忽略。";
        return smsSender.send(new SmsMessage(phone.trim(), sceneTitle, content, Map.of("code", code)));
    }
}
