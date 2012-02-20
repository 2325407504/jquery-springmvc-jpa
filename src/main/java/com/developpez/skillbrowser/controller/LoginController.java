package com.developpez.skillbrowser.controller;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.RememberMeServices;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.developpez.skillbrowser.model.dto.LoginStatus;

@Controller
@RequestMapping("/login")
public class LoginController {

    @Autowired
    private RememberMeServices rememberMeServices;

    @Autowired
    @Qualifier("authenticationManager")
    AuthenticationManager authenticationManager;

    @RequestMapping(method = RequestMethod.GET)
    @ResponseBody
    public LoginStatus getStatus(HttpServletRequest request, HttpServletResponse response) {
        Authentication authentication = getSessionAuthentication(request);
        if (isAuthenticated(authentication)) {
            return authenticationToLoginStatus(authentication);
        }
        Authentication rememberMeAuthentication = rememberMeServices.autoLogin(request, response);
        if (isAuthenticated(rememberMeAuthentication)) {
            return authenticationToLoginStatus(rememberMeAuthentication);
        }
        return authenticationToLoginStatus(authentication);
    }

    @RequestMapping(method = RequestMethod.POST)
    @ResponseBody
    public LoginStatus login(HttpServletRequest request, HttpServletResponse response, @RequestParam("j_username") String username,
            @RequestParam("j_password") String password, @RequestParam(value = "_spring_security_remember_me", required = false) String rememberMe) {
        Authentication authentication = null;
        try {
            UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(username, password);
            authentication = authenticationManager.authenticate(token);
            setSessionAuthentication(request, authentication);
        } catch (BadCredentialsException e) {
            authentication = SecurityContextHolder.getContext().getAuthentication();
        }
        if (rememberMe != null && rememberMe.equals("on") && isAuthenticated(authentication)) {
            rememberMeServices.loginSuccess(request, response, authentication);
        }
        return authenticationToLoginStatus(authentication);
    }

    @RequestMapping(method = RequestMethod.DELETE)
    @ResponseBody
    public void logout(HttpServletRequest request, HttpServletResponse response) throws IOException {
        SecurityContextHolder.getContext().setAuthentication(null);
        rememberMeServices.loginFail(request, response);
        response.getWriter().println("logged out");
    }

    private Authentication getSessionAuthentication(HttpServletRequest request) {
        SecurityContext securityContext = (SecurityContext) request.getSession().getAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
        if (securityContext == null) {
            return null;
        } else {
            return securityContext.getAuthentication();
        }
    }

    private void setSessionAuthentication(HttpServletRequest request, Authentication authentication) {
        SecurityContextHolder.getContext().setAuthentication(authentication);
        request.getSession().setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, SecurityContextHolder.getContext());
    }

    private boolean isAuthenticated(Authentication authentication) {
        return authentication != null && !authentication.getName().equals("anonymousUser") && authentication.isAuthenticated();
    }

    private LoginStatus authenticationToLoginStatus(Authentication authentication) {
        LoginStatus loginStatus = new LoginStatus();
        if (isAuthenticated(authentication)) {
            loginStatus.setLoggedIn(true);
            loginStatus.setUsername(authentication.getName());
        } else {
            loginStatus.setLoggedIn(false);
            loginStatus.setUsername(null);
        }
        return loginStatus;
    }

}