package com.kingdom_bank.RFQBackend.config.security;


import com.kingdom_bank.RFQBackend.entity.User;
import com.kingdom_bank.RFQBackend.util.ConstantUtil;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Objects;


@Getter
public class SecurityUser implements UserDetails {
    private final User user;
    private final ConstantUtil constantUtil;

    public SecurityUser(User user, ConstantUtil constantUtil) {
        this.user = user;
        this.constantUtil = constantUtil;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return null;
    }

    @Override
    public String getPassword() {
        return "";
    }

//    @Override
//    public String getPassword() { return user.getPassword();}

    @Override
    public String getUsername() { return user.getUsername(); }

    @Override
    public boolean isAccountNonExpired() {
        return (user.getStatus() != constantUtil.INACTIVE)  ;
    }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() {
        return (Objects.equals(user.getStatus().getStatusId(), constantUtil.ACTIVE.getStatusId()));
    }

}
