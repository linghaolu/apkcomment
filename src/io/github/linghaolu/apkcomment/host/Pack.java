package io.github.linghaolu.apkcomment.host;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Key;
import java.util.Random;
import java.util.zip.ZipFile;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

import io.github.linghaolu.apkcomment.AesUtil;
import io.github.linghaolu.apkcomment.Reader;
import io.github.linghaolu.apkcomment.RsaUtil;

/**
 * 桌面打包程序
 * Created by caoht on 2016/3/30.
 */
public class Pack {

    private static final short VERSION = 1;

    /**
     * 把short 转为byte数组（little endian）
     * @param data
     * @return
     */
    private static byte[] short2bytes(short data) {
        ByteBuffer buffer = ByteBuffer.allocate(2);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.putShort(data);
        buffer.flip();
        return buffer.array();
    }

    /**
     * 使用 NIO 接口实现快速拷贝文件。
     *
     * @param source
     * @param target
     *
     * @return
     */
    private static File copy(String source, String target) {
        Path sourcePath = Paths.get(source);

        Path targetPath = Paths.get(target);

        try {
            return Files.copy(sourcePath, targetPath, REPLACE_EXISTING).toFile();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    private static byte[] randomKey() {

        byte[] key = new byte[16];

        Random random = new Random();
        random.nextBytes(key);

        return key;
    }


    private static byte[] getCommentData(String content, Key privateKey) {
        byte[] aesKey = randomKey();

        boolean encrypt = false;

        if (privateKey != null) {
            encrypt = true;
        }

        try {

            byte[] data = new byte[0];
            if (encrypt) {
                data = AesUtil.encrypt(aesKey, aesKey, content.getBytes());
            } else {
                data = content.getBytes();
            }

            byte[] encryptdKey = new byte[0];;
            if (encrypt) {
                encryptdKey = RsaUtil.encryptByPrivateKey(aesKey, privateKey);
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            short keyLength = (short) encryptdKey.length;

            // write version
            baos.write(short2bytes(VERSION));

            // enccrypt or not (byte)
            baos.write(encrypt ? 1 : 0);

            // write encrypted aes key
            baos.write(short2bytes(keyLength));
            baos.write(encryptdKey);


            // write encrypted / content data
            short dataLength = (short)  (data.length);
            baos.write(short2bytes(dataLength));
            baos.write(data);


            short commentLength = (short) ( 2 /* version(short) */
                                           + 1 /* encrypt or not */
                                           + 2 /* enrypted aes key length(short) */
                                           + keyLength /* enrypted aes key */
                                           + 2 /* encrypted data length */
                                           + dataLength /* encrypted data */
                                           + 2 /* total commentLength (short) */
                                            );

            baos.write(short2bytes(commentLength));

            baos.close();

            return baos.toByteArray();

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * 判断源文件是否有 comment
     * @param filePath
     * @return
     */
    private static boolean hasComment(String filePath) {
        boolean hasComment = false;

        try {
            ZipFile zipFile = new ZipFile(filePath);
            String comment = zipFile.getComment();

            if (comment != null) {
                hasComment = true;
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException iae) {
            // 如果 commnet 区域是非法字符串 比如自己格式二进制，会出错。
/*            "Exception in thread \"main\" java.lang.IllegalArgumentException: MALFORMED\n"
                    + "at java.util.zip.ZipCoder.toString(ZipCoder.java:58)\n"
                    + "at java.util.zip.ZipFile.getComment(ZipFile.java:292)\n"
                    + "at com.fxiaoke.channelspack.Pack.hasComment(Pack.java:131)\n"
                    + "at com.fxiaoke.channelspack.Pack.generateChannelFile(Pack.java:151)\n"
                    + "at com.fxiaoke.channelspack.Pack.main(Pack.java:207)\n"
                    + "at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)\n"
                    + "at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)\n"
                    + "at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)\n"
                    + "at java.lang.reflect.Method.invoke(Method.java:497)\n"
                    + "at com.intellij.rt.execution.application.AppMain.main(AppMain.java:144)"*/
            hasComment = true;

            iae.printStackTrace();
        }

        return hasComment;
    }

    private static boolean generateChannelFile(String sourceApkPath, String channelId, Key privateKey) {
        File source = new File(sourceApkPath);
        String path = source.getAbsolutePath();
        if (!path.endsWith(".apk")) {
            System.out.println(sourceApkPath + "不是 .apk 文件扩展名");
            return false;
        }

        // 如果已经有 comment 不再处理
        boolean hasComment = hasComment(sourceApkPath);
        if (hasComment) {
            System.out.println(sourceApkPath + "包含 comment，不能再处理。");
            return false;
        }

        int index = path.lastIndexOf(".");

        // channel 包路径
        path = path.substring(0, index) + "_" + channelId + ".apk";
        // 先拷贝文件
        File dest = copy(sourceApkPath, path);


        long length = dest.length();

        RandomAccessFile raf = null;

        try {
            raf = new RandomAccessFile(dest, "rw");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        if (raf == null) {
            System.out.println("RandomAccessFile not found");
            return false;
        }

        try {
            raf.seek(length - 2);

            byte[] comment = getCommentData(channelId, privateKey);

            raf.write(short2bytes((short) comment.length));

            raf.write(comment);

            raf.close();

        } catch (Exception e) {
            e.printStackTrace();
        }

        return true;
    }



    public static void main(String[] args) {
        System.out.println("xxx");

        String pemString = RsaUtil.readPem("rsa_private_key.pem");
        Key privateKey = RsaUtil.generatePrivateKey(pemString);


        generateChannelFile("app-debug.apk", "haha", null);



        pemString = RsaUtil.readPem("rsa_public_key.pem");

        String result = Reader.readChannel("app-debug_haha.apk", pemString);


        System.out.println("xxx11 " + result);
    }
}
