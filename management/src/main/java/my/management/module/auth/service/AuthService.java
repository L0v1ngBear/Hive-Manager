package my.management.module.auth.service;

import jakarta.annotation.Resource;
import my.management.common.exception.BusinessException;
import my.management.common.service.DeveloperAccessService;
import my.management.common.utils.EncryptUtil;
import my.management.common.utils.ResponseEncryptUtil;
import my.management.common.utils.TokenUtil;
import my.management.module.auth.mapper.AuthMapper;
import my.management.module.auth.model.dto.LoginRequest;
import my.management.module.auth.model.vo.LoginUserRow;
import my.management.module.auth.model.vo.LoginVO;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
/**
 * AuthService 属于管理端后端认证模块，实现核心业务编排与规则逻辑。
 */
@Service
public class AuthService {

    @Resource
    private AuthMapper authMapper;

    @Resource
    private EncryptUtil encryptUtil;

    @Resource
    private DeveloperAccessService developerAccessService;

    @Resource
    private ResponseEncryptUtil responseEncryptUtil;

    public LoginVO login(LoginRequest request) {
        LoginUserRow loginUser = authMapper.selectLoginUser(request.getUsername().trim());
        if (loginUser == null) {
            throw new BusinessException(401, "账号或密码错误");
        }
        if (!Objects.equals(loginUser.getUserStatus(), 1)) {
            throw new BusinessException(403, "该员工账号已禁用");
        }
        if (!encryptUtil.matches(request.getPassword(), loginUser.getPassword())) {
            throw new BusinessException(401, "账号或密码错误");
        }
        if (!encryptUtil.isBcryptHash(loginUser.getPassword())) {
            authMapper.updatePasswordByUserId(loginUser.getUserId(), encryptUtil.encode(request.getPassword()));
        }

        List<String> permissionList = authMapper.selectPermCodesByUserIdAndTenantCode(loginUser.getUserId(), loginUser.getTenantCode());
        Set<String> permCodes = new LinkedHashSet<>(permissionList == null ? List.of() : permissionList);
        String token = TokenUtil.createToken(loginUser.getUserId(), loginUser.getTenantCode());

        LoginVO loginVO = new LoginVO();
        loginVO.setToken(token);
        loginVO.setUserId(loginUser.getUserId());
        loginVO.setUserName(loginUser.getUserName());
        loginVO.setTenantCode(loginUser.getTenantCode());
        loginVO.setDeveloper("super".equalsIgnoreCase(loginUser.getTenantCode()));
        loginVO.setResponseKey(responseEncryptUtil.buildResponseKey(token));
        loginVO.setPermissions(List.copyOf(permCodes));
        return loginVO;
    }
}
