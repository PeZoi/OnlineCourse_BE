package com.springboot.courses.config;

import com.springboot.courses.utils.AppConstants;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class OAuth2LoginSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws ServletException, IOException {
        this.setAlwaysUseDefaultTargetUrl(true);
        this.setDefaultTargetUrl(AppConstants.LOCALHOST);
        super.onAuthenticationSuccess(request, response, authentication);
    }


}
