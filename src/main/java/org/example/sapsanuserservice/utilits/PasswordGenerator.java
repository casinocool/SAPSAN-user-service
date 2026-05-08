package org.example.sapsanuserservice.utilits;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class PasswordGenerator {
    private static final String CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*" ;
    public char[] generate(int length){
        SecureRandom random = new SecureRandom();
        char[] password =new char[length];
        for(int i=0;i<length;i++){
            password[i]=CHARS.charAt(random.nextInt(CHARS.length()));
        }
        return password;
    }
}
