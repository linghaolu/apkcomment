# 用途 #

往apk（zip）的 file comment 区域添加自定义数据，可以用于apk内置不同的数据，启动的时候做相应的事情。
比如对于小说app，用户从web站下载app，需要启动后打开不同的小说。
或者对于应用市场类的app，启动后开始高速下载用户想要下的app。
或者就是简单的渠道号。

这几种都是一个技术手段，往apk中嵌入一块数据。

这里主要演示用于打渠道包的用法。

# 集中内置数据的方法对比 #

- 往res中添加配置文件。这种办法需要apk解压后重新签名。速度最慢。好处就是别人无法删除，无法篡改。
- 往 apk 的 meta-info目录下添加配置文件，这种只需要使用 zip 命令，add一个文件即可，这种速度也很快，也值得推荐。但是apk读取速度比较慢。这种方法因为 meta－info目录下的文件不参与签名校验，所以可以被人删除或篡改。
- 该项目使用的办法，往 apk 的 comment 区域添加自定义数据。这种方法生成apk速度最快，读取速度也最快。这种办法跟2一样，不参与签名，可以被别人删除和篡改。

后边两种办法，无法防止别人删除。但是对于篡改，只需要配合 rsa 非对称加密即可。

# 使用方法 #

支持不加密和加密两种办法。
不加密，对于非机密数据可以使用，读取速度在10ms级别。
加密采用rsa ＋ aes 组合方式，读取速度略慢，主要是第一次load解密clas较慢，100ms级别。非第一次也在 10-20 ms级别。
以上性能在 nexus s，moto g，低端手机上测试结果。

## 不加密 ##

`java -jar apkcomment.jar source.apk channel_id`

以上命令用于生成单个渠道包。

`java -jar apkcomment.jar source.apk channel_list.txt`

以上命令支持从文件中读取渠道号，批量打包。channel_list.txt 每行一个渠道号。

## 加密方式 ##

加密方式用到了RSA非对称算法，可以放置别人伪造数据。所以需要自己生成RSA公私密钥对。

生成办法参考 [http://linghaolu.github.io/openssl/2016/04/02/openssl-rsa-pem.html](http://linghaolu.github.io/openssl/2016/04/02/openssl-rsa-pem.html)

加密方式也支持单渠道和多渠道两种方式，在非加密方式的基础上加上rsa私钥的pem格式的文件即可。

`java -jar apkcomment.jar source.apk channel_id rsa_private_key.pem`

`java -jar apkcomment.jar source.apk channel_list.txt rsa_private_key.pem`
