package io.github.linghaolu.apkcomment;

import javax.crypto.Cipher;
import java.io.BufferedReader;
import java.io.FileReader;
import java.security.Key;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

/**
 *  rsa 加解密工具类
 * Created by caoht on 2016/3/31.
 */
public class RsaUtil {

    /**
     * 加密算法。
     */
    private static final String ALGORITHM_RSA = "RSA";

    public static String readPem(String pemPath) {

        String result = null;

        try {
            BufferedReader br = new BufferedReader(new FileReader(pemPath));

            String str = null;
            StringBuilder sb = new StringBuilder();

            boolean firstLine = true;

            while ((str = br.readLine()) != null) {
                if (str.contains("---")) {
                    continue;
                }

                if (!firstLine) {
                    sb.append("\r\n");
                }

                firstLine = false;

                sb.append(str);

            }

            result = sb.toString();

        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }


    public static Key generatePrivateKey(String pemString) {

        Key key = null;

        // 对密钥解密
        byte[] keyBytes = Base64.decode(pemString.getBytes());

        // 取得私钥
        PKCS8EncodedKeySpec pkcs8KeySpec = new PKCS8EncodedKeySpec(keyBytes);
        try {
            KeyFactory keyFactory = KeyFactory.getInstance(ALGORITHM_RSA);
            key = keyFactory.generatePrivate(pkcs8KeySpec);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
        }

        return key;
    }


    public static Key generatePublicKey(String pemString) {

        Key key = null;

        // 对公钥解密
        byte[] keyBytes = Base64.decode(pemString.getBytes());

        // 取得公钥
        X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = null;
        try {
            keyFactory = KeyFactory.getInstance(ALGORITHM_RSA);
            key = keyFactory.generatePublic(x509KeySpec);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
        }

        return key;
    }

    public static byte[] encryptByPrivateKey(byte[] data, Key privateKey)
             {
        byte[] result = null;

        try {
            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.ENCRYPT_MODE, privateKey);
            result = cipher.doFinal(data);
        } catch (Exception e) {
            e.printStackTrace();
        }


        return result;
    }

    public static byte[] encryptByPublicKey(byte[] data, Key publicKey){
        byte[] result = null;
        try {
            // 对数据加密
            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);
            result = cipher.doFinal(data);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }

    public static byte[] decryptByPublicKey(byte[] data, Key publicKey){
        byte[] result = null;
        try {
            // 对数据解密
            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.DECRYPT_MODE, publicKey);
            result = cipher.doFinal(data);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }

    public static byte[] decryptByPrivateKey(byte[] data, Key privateKey){
        byte[] result = null;
        try {
            // 对数据解密
            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.DECRYPT_MODE, privateKey);
            result = cipher.doFinal(data);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;

    }
}