package com.kingdom_bank.RFQBackend.config.security;


import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor
public class DynamicAuthenticationProvider implements AuthenticationProvider {

    private final AuthenticationProvider ldapAuthenticationProvider;
//    private final AuthenticationProvider daoAuthenticationProvider;


    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        return  ldapAuthenticationProvider.authenticate(authentication);
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return true; // Supports all authentication types
    }
}
