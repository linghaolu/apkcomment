package io.github.linghaolu.apkcomment;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.Key;

/**
 *
 * 从 apk中读取 添加的数据
 *
 * Created by caoht on 2016/3/31.
 */
public class CommentReader {

    /**
     * 把从zip中毒出来的2个字节转为 short
     * @param bytes
     * @param offset
     * @return
     */
    private static short bytes2Short(byte[] bytes, int offset) {
        ByteBuffer buffer = ByteBuffer.allocate(2);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.put(bytes[offset]);
        buffer.put(bytes[offset + 1]);
        return buffer.getShort(0);
    }

    public static String readComment(String apkPath, String pemPublicKey) {
        RandomAccessFile raf = null;
        try {
            File file = new File(apkPath);

            byte[] lengthByte = new byte[2];

            raf = new RandomAccessFile(file, "r");

            long fileLength = file.length();

            // 读取总长度
            raf.seek(fileLength - 2); // 读取我们放在文件最后的两个字节，是我们添加的comment数据的长度。
            raf.read(lengthByte);
            short dataLength = bytes2Short(lengthByte, 0);

            if (dataLength == 0) {
                // 这种情况应该是读到了 apk 默认的 0 没有comment， 说明没有我们自己追加的 comment
                return null;
            }

            // 指针移动到 comments数据开始地方
            raf.seek(fileLength - dataLength);

            // read version
            raf.read(lengthByte);
            short version = bytes2Short(lengthByte, 0);

            // read encrypt flag
            byte encryptFlag = raf.readByte();
            boolean encrypt = (encryptFlag == 1);


            // read encrypted aes key length
            raf.read(lengthByte);
            short keyLength = bytes2Short(lengthByte, 0);

            // read enctypted aes key
            byte[] encryptedKey = new byte[keyLength];
            raf.read(encryptedKey);

            // read encrypted data length
            raf.read(lengthByte);
            short encryptedLength = bytes2Short(lengthByte, 0);

            // read data
            byte[] encrypted = new byte[encryptedLength];
            raf.read(encrypted);

            byte[] content = null;
            if (encrypt) {
                Key publicKey = RsaUtil.generatePublicKey(pemPublicKey);
                // rsa 解密 aes key
                byte[] aesKey = RsaUtil.decryptByPublicKey(encryptedKey, publicKey);
                // aes 解密 data，aes 密钥和 iv 初始化向量使用相同数据
                content = AesUtil.decrypt(aesKey, aesKey, encrypted);
            } else {
                content = encrypted;
            }


            return new String(content);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (raf != null) {
                try {
                    raf.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return null;
    }



}
