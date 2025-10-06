package com.example.gateway.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class DetailsService implements UserDetailsService {

    private final Map<String, String> users = new HashMap<>();
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public DetailsService() {
        // Hardcoded users (for testing)
        users.put("admin", passwordEncoder.encode("password"));
        users.put("user", passwordEncoder.encode("user123"));
        users.put("converter", passwordEncoder.encode("convert123"));
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        String password = users.get(username);
        if (password == null) {
            throw new UsernameNotFoundException("User not found: " + username);
        }

        // Assign roles
        List<GrantedAuthority> authorities = new ArrayList<>();
        if ("admin".equals(username)) {
            authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
        } else if ("converter".equals(username)) {
            authorities.add(new SimpleGrantedAuthority("ROLE_CONVERTER"));
        } else {
            authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
        }

        // Return the user with roles attached
        return new User(username, password, authorities);
    }

    // Add a new user (Use later?)
    public void addUser(String username, String rawPassword) {
        users.put(username, passwordEncoder.encode(rawPassword));
    }
}
