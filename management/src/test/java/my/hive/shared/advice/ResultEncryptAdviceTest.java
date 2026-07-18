package my.hive.shared.advice;

import my.hive.domain.auth.model.vo.LoginVO;
import my.hive.shared.dto.Result;
import my.hive.shared.utils.ResponseEncryptUtil;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class ResultEncryptAdviceTest {

    @Test
    void leavesTenantSelectionLoginResponsePlainWhenStaleBearerIsPresent() {
        ResponseEncryptUtil responseEncryptUtil = mock(ResponseEncryptUtil.class);
        ResultEncryptAdvice advice = new ResultEncryptAdvice(responseEncryptUtil);
        MockHttpServletRequest servletRequest = new MockHttpServletRequest(
                "POST", "/auth/mini/wechat-login/select");
        servletRequest.addHeader(HttpHeaders.AUTHORIZATION, "Bearer stale-session-token");
        MockHttpServletResponse servletResponse = new MockHttpServletResponse();
        LoginVO login = new LoginVO();
        login.setToken("new-login-token");
        Result<LoginVO> body = Result.success(login);

        Object result = advice.beforeBodyWrite(
                body,
                null,
                MediaType.APPLICATION_JSON,
                null,
                new ServletServerHttpRequest(servletRequest),
                new ServletServerHttpResponse(servletResponse)
        );

        assertThat(result).isSameAs(body);
        assertThat(body.getEncrypted()).isFalse();
        assertThat(body.getData()).isSameAs(login);
        verify(responseEncryptUtil, never()).encrypt(anyString(), any());
    }
}
