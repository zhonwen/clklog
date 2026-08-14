package com.zcunsoft.clklog.security.filter;

import com.zcunsoft.clklog.common.model.LoginUser;
import com.zcunsoft.clklog.common.utils.SecurityUtils;
import com.zcunsoft.clklog.common.utils.ServletUtils;
import com.zcunsoft.clklog.security.service.TokenService;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.annotation.Resource;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * token过滤器 验证token有效性
 */
@Component
public class JwtAuthenticationTokenFilter extends OncePerRequestFilter {
    @Resource
    private TokenService tokenService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        LoginUser loginUser = tokenService.getLoginUserByApiKey(request);
        if (loginUser == null) {
            loginUser = tokenService.getLoginUser(request);
        }
        if (loginUser != null && SecurityUtils.getAuthentication() == null) {
            tokenService.verifyToken(loginUser);
            UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(loginUser, null, loginUser.getAuthorities());
            authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authenticationToken);
        }
        if (loginUser != null && loginUser.getUser() != null
                && Boolean.TRUE.equals(loginUser.getUser().getPwdResetRequired())) {
            String path = request.getServletPath();
            if (!"/auth/modifyPassword".equals(path) && !"/logout".equals(path)) {
                ServletUtils.renderString(response, "{\"code\":403,\"msg\":\"必须先修改初始密码\",\"data\":\"\"}");
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
