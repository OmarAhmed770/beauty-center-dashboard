package com.omar.beautycenter.auth;

import com.omar.beautycenter.common.error.BusinessRuleException;
import com.omar.beautycenter.user.User;
import com.omar.beautycenter.user.UserRepository;
import com.omar.beautycenter.user.UserService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        String normalizedUsername;
        try {
            normalizedUsername = UserService.normalizeUsername(username);
        } catch (BusinessRuleException exception) {
            throw new UsernameNotFoundException("User not found.");
        }

        User user = userRepository.findByUsername(normalizedUsername)
                .orElseThrow(() -> new UsernameNotFoundException("User not found."));
        return UserPrincipal.from(user);
    }
}
