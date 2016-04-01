package io.github.linghaolu.apkcomment;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * AES 算法工具类 (AES-128-CBC /PKCS5Padding)。
 */
public final class AesUtil {
    
    /** private constructor. */
    private AesUtil() {
    }
    
    /** the name of the transformation to create a cipher for. */
    private static final String TRANSFORMATION = "AES/CBC/PKCS5Padding";

    /** 算法名称 */
    private static final String ALGORITHM_NAME = "AES";
    
    /**
     * aes 加密，AES/CBC/PKCS5Padding
     * 
     * @param key
     *            密钥字符串, 此处使用AES-128-CBC加密模式，key需要为16位
     * @param content
     *            要加密的内容
     * @param cbcIv
     *            初始化向量(CBC模式必须使用) 使用CBC模式，需要一个向量iv，可增加加密算法的强度
     * @return 加密后原始二进制字符串
     * @throws Exception
     *             Exception
     */
    public static byte[] encrypt(byte[] cbcIv, byte[] key, byte[] content) throws Exception {

        SecretKeySpec sksSpec = new SecretKeySpec(key, ALGORITHM_NAME);

        Cipher cipher = Cipher.getInstance(TRANSFORMATION);

        IvParameterSpec iv = new IvParameterSpec(cbcIv);
        
        cipher.init(Cipher.ENCRYPT_MODE, sksSpec, iv);

        byte[] encrypted = cipher.doFinal(content);

        return encrypted;
    }

    /**
     * aes 解密，AES/CBC/PKCS5Padding
     * 
     * @param key
     *            密钥, 此处使用AES-128-CBC加密模式，key需要为16位
     * @param encrypted
     *            密文
     * @param cbcIv
     *            初始化向量(CBC模式必须使用) 使用CBC模式，需要一个向量iv，可增加加密算法的强度
     * @return 明文
     * @throws Exception
     *             异常
     */
    public static byte[] decrypt(byte[] cbcIv, byte[] key, byte[] encrypted) throws Exception {

        SecretKeySpec skeSpect = new SecretKeySpec(key, ALGORITHM_NAME);

        Cipher cipher = Cipher.getInstance(TRANSFORMATION);

        IvParameterSpec iv = new IvParameterSpec(cbcIv);
        
        cipher.init(Cipher.DECRYPT_MODE, skeSpect, iv);

        byte[] decrypted = cipher.doFinal(encrypted);

        return decrypted;
    }

}
