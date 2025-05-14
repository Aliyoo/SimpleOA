package com.example.simpleoa.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.example.simpleoa.util.SimplePasswordEncoder;

@Configuration
public class PasswordConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new PasswordEncoder() {
            @Override
            public String encode(CharSequence rawPassword) {
                return SimplePasswordEncoder.encode(rawPassword.toString());
            }

            @Override
            public boolean matches(CharSequence rawPassword, String encodedPassword) {
                return SimplePasswordEncoder.matches(rawPassword.toString(), encodedPassword);
            }
        };
    }

    // 用于生成测试密码的工具方法
    public static void main(String[] args) {
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        String password = "123456"; // 默认测试密码
        String encoded = passwordEncoder.encode(password);
        System.out.println("Original password: " + password);
        System.out.println("BCrypt encoded: " + encoded);
        
        // 验证密码是否匹配
        boolean matches = passwordEncoder.matches(password, encoded);
        System.out.println("Password matches with new encoded: " + matches);
        
        // 验证与已存在的加密密码是否匹配
        String existingEncodedPassword = "$2a$10$fA6CMy4bqXdLr7QoOetMFu/4Aoc0CGUa2fCHrBuhepZnoRwMM8K0i";
        boolean matchesExisting = passwordEncoder.matches(password, existingEncodedPassword);
        System.out.println("Password matches with existing encoded: " + matchesExisting);
    }
}
