package io.github.linghaolu.apkcomment.host;

import io.github.linghaolu.apkcomment.AesUtil;
import io.github.linghaolu.apkcomment.CommentReader;
import io.github.linghaolu.apkcomment.RsaUtil;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Key;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.zip.ZipFile;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

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

    private static File generateChannelFile(String sourceApkPath, String channelId, Key privateKey) {
        File source = new File(sourceApkPath);
        String path = source.getAbsolutePath();
        if (!path.endsWith(".apk")) {
            System.out.println(sourceApkPath + "不是 .apk 文件扩展名");
            return null;
        }

        // 如果已经有 comment 不再处理
        boolean hasComment = hasComment(sourceApkPath);
        if (hasComment) {
            System.out.println(sourceApkPath + "包含 comment，不能再处理。");
            return null;
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
            return null;
        }

        try {
            byte[] comment = getCommentData(channelId, privateKey);

            // apk 默认没有评论,最后连个字节是个short,表示评论长度 0, 我们需要重写为我们实际的comment长度
            raf.seek(length - 2);

            raf.write(short2bytes((short) comment.length));

            raf.write(comment);

            raf.close();

            return dest;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    private static List<String> readChannelsFromFile(String channelsListFile) {
        File file = new File(channelsListFile);
        List<String> result = new ArrayList<>();
        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String channel = null;

            while ((channel = br.readLine()) != null) {
                if (channel.length() > 0) {
                    result.add(channel);
                }
            }

            br.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }


    /**
     * 分两种工作方式, 加密和非加密.
     *
     * 加密模式需要提供rsa非对称私钥,pem格式. 可以使用 openssl生成.
     *
     * channel_list_file.txt 存储的是渠道号列表.
     *
     * 非加密模式(把文本打包)   > java -jar apkcomment.jar source.apk comment
     * 非加密模式(打多个渠道包) > java -jar apkcomment.jar source.apk channel_list_file.txt
     * 加密模式(把文本打包)    > java -jar apkcomment.jar source.apk comment rsa_private_key.pem
     * 加密模式(打多个渠道包   > java -jar apkcomment.jar source.apk channel_list_file.txt rsa_private_key.pem
     * @param args
     */
    public static void main(String[] args) {

        if (args.length < 2) {
            System.out.println("usege:\n" +
                    "非加密模式(单个渠道打包)   > java -jar apkcomment.jar source.apk channelId \n"
                    + "非加密模式(打多个渠道包) > java -jar apkcomment.jar source.apk channel_list_file.txt\n"
                    + "加密模式(单个打包)    > java -jar apkcomment.jar source.apk channelId rsa_private_key.pem\n"
                    + "加密模式(打多个渠道包   > java -jar apkcomment.jar source.apk channel_list_file.txt rsa_private_key.pem\n"
                    + "----- channel_list_file.txt 文件中每个渠道占一行 -----.");

            return;
        }

        String apkPath = args[0];
        String channelOrFile = args[1];
        String privateKeyPath = null;

        if (args.length == 3) {
            privateKeyPath = args[2];
        }

        // 如果第二个参数是个文件,则从文件中读取
        boolean fromFile = new File(channelOrFile).exists();

        File dest = null;
        Key privateKey = null;

        // 加密模式
        if (privateKeyPath != null) {
            String pemString = RsaUtil.readPem(privateKeyPath);
            privateKey = RsaUtil.generatePrivateKey(pemString);
        }

        if (fromFile) {
            // 从渠道文件列表中读取,一行一个
            List<String> channels = readChannelsFromFile(channelOrFile);
            for (String channelId : channels) {
                dest = generateChannelFile(apkPath, channelId, privateKey);
            }
        } else {
            // 只一个渠道包
            dest = generateChannelFile(apkPath, channelOrFile, privateKey);
        }


        // test test test
        String result = CommentReader.readComment(dest.getPath(), null);
        System.out.println("xxx " + result);

    }
}
