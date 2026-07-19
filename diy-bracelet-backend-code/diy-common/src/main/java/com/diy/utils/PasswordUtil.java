package com.diy.utils;

import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;

/**
 * 密码加密工具类
 */
public class PasswordUtil {

    /**
     * 使用MD5对密码进行加密
     * @param password 明文密码
     * @return MD5加密后的密文
     */
    public static String encrypt(String password) {
        return DigestUtils.md5DigestAsHex(password.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 验证密码是否匹配
     * @param plainPassword 明文密码
     * @param encryptedPassword 加密后的密码
     * @return 是否匹配
     */
    public static boolean matches(String plainPassword, String encryptedPassword) {
        return encrypt(plainPassword).equals(encryptedPassword);
    }

    /**
     * 测试生成MD5密码
     * 运行这个main方法可以生成MD5密码，用于更新数据库
     */
    public static void main(String[] args) {
        // 示例：生成常用密码的MD5值
        System.out.println("123456 的MD5: " + encrypt("123456"));
        System.out.println("admin 的MD5: " + encrypt("admin"));
        System.out.println("password 的MD5: " + encrypt("password"));
        
        // 你可以修改这里来生成你需要的密码
        String yourPassword = "123456";
        System.out.println("\n你的密码 '" + yourPassword + "' 的MD5是: " + encrypt(yourPassword));
    }
}
