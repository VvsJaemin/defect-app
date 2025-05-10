package com.group.defectapp.util;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.UUID;

import static java.util.UUID.randomUUID;

/**
 * <pre>
 * 암복호화 유틸
 * </pre>
 *
 * @author Kim Juon
 */

@Component
public class AES256Cipher {

    private static volatile AES256Cipher INSTANCE;


    static String SECRET_KEY = "Y7k@3r!9Vm*Qz#1xTg^Wp$2nBv&E4hLs";
    static String IV = "";
    final static int ITERATION = 2;

    public static AES256Cipher getINSTANCE() {
        if (ObjectUtils.isEmpty(INSTANCE)) {
            synchronized (AES256Cipher.class) {
                if (ObjectUtils.isEmpty(INSTANCE)) {
                    INSTANCE = new AES256Cipher();
                }
            }
        }

        return INSTANCE;
    }

    private AES256Cipher() {
        IV = SECRET_KEY.substring(0, 16);
    }

    /**
     * Enctyption
     *
     * @param str
     * @return
     * @throws Exception
     */
    public static String encode(String str) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException {

        String salt = randomUUID().toString();
        byte[] keyData = SECRET_KEY.substring(16).getBytes();
        SecretKey key = new SecretKeySpec(keyData, "AES");

        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(IV.getBytes()));

        String valToEnc;
        String eVal = str;
        for (int i = 0; i < ITERATION; i++) {
            valToEnc = salt + eVal;
            byte[] encrypted = cipher.doFinal(valToEnc.getBytes(StandardCharsets.UTF_8));
            eVal = new String(Base64.getEncoder().encode(encrypted));
        }

        return eVal;
    }

    /**
     * @param str
     * @return
     * @throws Exception
     */
    public static String decode(String str) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException {

        String salt = randomUUID().toString();
        byte[] keyData = SECRET_KEY.substring(16).getBytes();
        SecretKey key = new SecretKeySpec(keyData, "AES");

        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(IV.getBytes(StandardCharsets.UTF_8)));

        String dVal;
        String valToDec = str;
        for (int i = 0; i < ITERATION; i++) {
            byte[] decrypted = Base64.getDecoder().decode(valToDec.getBytes());
            dVal = new String(cipher.doFinal(decrypted), StandardCharsets.UTF_8).substring(salt.length());
            valToDec = dVal;
        }

        return valToDec;
    }

}
