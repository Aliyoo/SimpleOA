package com.example.simpleoa.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;

public class SimplePasswordEncoder {
    
    public static String encode(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean matches(String rawPassword, String encodedPassword) {
        return encode(rawPassword).equals(encodedPassword);
    }

    // 测试方法
    public static void main(String[] args) {
        String password = "123456";
        String encoded = encode(password);
        System.out.println("Original password: " + password);
        System.out.println("MD5 encoded: " + encoded);
        System.out.println("Length: " + encoded.length());
        System.out.println("Matches: " + matches(password, encoded));
        
        // 验证多次加密结果是否相同
        String encoded2 = encode(password);
        System.out.println("\nEncoding same password again: " + encoded2);
        System.out.println("Same result: " + encoded.equals(encoded2));
    }
}
