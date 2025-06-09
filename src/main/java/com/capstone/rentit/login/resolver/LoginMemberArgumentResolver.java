package com.capstone.rentit.login.resolver;

import com.capstone.rentit.file.service.FileStorageService;
import com.capstone.rentit.login.annotation.Login;
import com.capstone.rentit.login.dto.MemberDetails;
import com.capstone.rentit.member.dto.MemberDto;
import lombok.RequiredArgsConstructor;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@RequiredArgsConstructor
public class LoginMemberArgumentResolver implements HandlerMethodArgumentResolver {

    private final FileStorageService fileStorageService;

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(Login.class) &&
                MemberDto.class.isAssignableFrom(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parameter,
                                  ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest,
                                  WebDataBinderFactory binderFactory) throws Exception {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof MemberDetails details)) {
            return null; // 인증되지 않은 경우, 필요시 예외 처리 가능
        }
        return MemberDto.fromEntity(details.getMember(), fileStorageService.generatePresignedUrl(details.getMember().getProfileImg()));
    }
}
