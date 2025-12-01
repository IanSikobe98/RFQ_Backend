package com.kingdom_bank.RFQBackend.config.security;



import com.kingdom_bank.RFQBackend.entity.User;
import com.kingdom_bank.RFQBackend.service.DatabaseService;
import com.kingdom_bank.RFQBackend.util.ConstantUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {
    private final DatabaseService databaseService;
    private final ConstantUtil constantUtil;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = databaseService.getUserByUsername(username,constantUtil);
        if(user == null) throw new UsernameNotFoundException("User not Found");
        return new SecurityUser(user,constantUtil);
    }
}
