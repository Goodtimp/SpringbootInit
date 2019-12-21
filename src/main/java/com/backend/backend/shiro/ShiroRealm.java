package com.backend.backend.shiro;

import com.backend.backend.common.utils.StringUtil;
import com.backend.backend.enums.SysDictEnum;
import com.backend.backend.jwt.JwtToken;
import com.backend.backend.jwt.JwtUtil;
import com.backend.backend.model.entity.sys.SysPermission;
import lombok.RequiredArgsConstructor;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.backend.backend.enums.TokenEnum.PAYLOAD_USER_ID_TAG;

/**
 * @Author: goodtimp
 * @Date: 2019/10/7 20:46
 * @description :  shiro认证
 */
@Service
@RequiredArgsConstructor(onConstructor = @_(@Autowired))
public class ShiroRealm extends AuthorizingRealm {

    Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * 必须重写此方法，不然Shiro会报错
     */
    @Override
    public boolean supports(AuthenticationToken token) {
        return token instanceof JwtToken;
    }

    /**
     * 只有当需要检测用户权限的时候才会调用此方法，例如checkRole,checkPermission之类的
     *
     * @param principalCollection doGetAuthenticationInfo中传的token
     * @return
     */
    @Override
    protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principalCollection) {
        SimpleAuthorizationInfo simpleAuthorizationInfo = new SimpleAuthorizationInfo();
        // 获取当前登录的用户权限
        List<SysPermission> temp = JwtUtil.getPermissionByToken(null);
        // temp==null 时未登录
        if (temp != null) {
            // 从redis中获取角色对应的数据
            // ------- code --------
            temp.stream().forEach(e -> simpleAuthorizationInfo.addStringPermission(e.getPerCode()));

            // 用户已登录权限
            simpleAuthorizationInfo.addStringPermission(SysDictEnum.PERMISSION_USER_LOGGED_ON.getCode());
        }
        return simpleAuthorizationInfo;
    }

    /**
     * 默认使用此方法进行用户名正确与否验证，错误抛出异常即可。
     *
     * @param authenticationToken
     * @return
     * @throws AuthenticationException
     */
    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken authenticationToken) throws AuthenticationException {
        String token = (String) authenticationToken.getCredentials();
        // 解密获得account，用于和数据库进行对比
        String userId = JwtUtil.getClaim(token, PAYLOAD_USER_ID_TAG.getCode());
        // 帐号为空
        if (StringUtil.isBlank(userId)) {
            throw new AuthenticationException("Token中帐号为空(The account in Token is empty.)");
        }
        // 查询用户是否存在 todo: 是否可以去除用户查询，当删除用户时refreshToken也会被删除？
//        UserEnum user = userServiceImpl.getById(Long.parseLong(userId));
//        if (user == null) {
//            throw new AuthenticationException("该帐号不存在(The account does not exist.)");
//        }
        // 开始认证，要AccessToken认证通过，且Redis中存在RefreshToken，且两个Token时间戳一致
        if (JwtUtil.verify(token) && JwtUtil.judgeRefreshToken(token)) {
            return new SimpleAuthenticationInfo(token, token, "userRealm");
        }
        throw new AuthenticationException("Token已过期(Token expired or incorrect.)");
    }

}
