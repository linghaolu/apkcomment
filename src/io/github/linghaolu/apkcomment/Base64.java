package io.github.linghaolu.apkcomment;
import java.io.UnsupportedEncodingException;

/**
 * This class implements Base64 encoding/decoding functionality
 * as specified in RFC 2045 (http://www.ietf.org/rfc/rfc2045.txt).
 */
public final class Base64 {
    
    /** utility class should not have a public constructor. */
    private Base64() { }
    
    /**
     * 对输入进行base64编码。
     * @param in 输入
     * @return base64编码字符串。
     */
    public static byte[] decode(byte[] in) {
        return decode(in, in.length);
    }
    
    /**
     * 对base64编码数据进行解码。
     * @param in base64编码输入数据
     * @param len 长度
     * @return 解码后的数据
     */
    public static byte[] decode(byte[] in, int len) {
        // approximate output length
        int length = len / 4 * 3; // SUPPRESS CHECKSTYLE
        // return an empty array on emtpy or short input without padding
        if (length == 0) {
            return new byte[0];
        }
        // temporary array
        byte[] out = new byte[length];
        // number of padding characters ('=')
        int pad = 0;
        byte chr;
        // compute the number of the padding characters
        // and adjust the length of the input
        for ( ;; len--) {
            chr = in[len - 1];
            // skip the neutral characters
            if ((chr == '\n') || (chr == '\r') || (chr == ' ') || (chr == '\t')) {
                continue;
            }
            if (chr == '=') {
                pad++;
            } else {
                break;
            }
        }
        // index in the output array
        int outIndex = 0;
        // index in the input array
        int inIndex = 0;
        // holds the value of the input character
        int bits = 0;
        // holds the value of the input quantum
        int quantum = 0;
        for (int i = 0; i < len; i++) {
            chr = in[i];
            // skip the neutral characters
            if ((chr == '\n') || (chr == '\r') || (chr == ' ') || (chr == '\t')) {
                continue;
            }
            if ((chr >= 'A') && (chr <= 'Z')) {
                // char ASCII value
                //  A    65    0
                //  Z    90    25 (ASCII - 65)
                bits = chr - 65; // SUPPRESS CHECKSTYLE
            } else if ((chr >= 'a') && (chr <= 'z')) {
                // char ASCII value
                //  a    97    26
                //  z    122   51 (ASCII - 71)
                bits = chr - 71; // SUPPRESS CHECKSTYLE
            } else if ((chr >= '0') && (chr <= '9')) {
                // char ASCII value
                //  0    48    52
                //  9    57    61 (ASCII + 4)
                bits = chr + 4; // SUPPRESS CHECKSTYLE
            } else if (chr == '+') {
                bits = 62; // SUPPRESS CHECKSTYLE
            } else if (chr == '/') {
                bits = 63; // SUPPRESS CHECKSTYLE
            } else {
                return null;
            }
            // append the value to the quantum
            quantum = (quantum << 6) | (byte) bits; // SUPPRESS CHECKSTYLE
            if (inIndex%4 == 3) { // SUPPRESS CHECKSTYLE
                // 4 characters were read, so make the output:
                out[outIndex++] = (byte) ((quantum & 0x00FF0000) >> 16); // SUPPRESS CHECKSTYLE
                out[outIndex++] = (byte) ((quantum & 0x0000FF00) >> 8); // SUPPRESS CHECKSTYLE
                out[outIndex++] = (byte) (quantum & 0x000000FF); // SUPPRESS CHECKSTYLE
            }
            inIndex++;
        }
        if (pad > 0) {
            // adjust the quantum value according to the padding
            quantum = quantum << (6 * pad); // SUPPRESS CHECKSTYLE
            // make output
            out[outIndex++] = (byte) ((quantum & 0x00FF0000) >> 16);// SUPPRESS CHECKSTYLE
            if (pad == 1) {
                out[outIndex++] = (byte) ((quantum & 0x0000FF00) >> 8); // SUPPRESS CHECKSTYLE
            }
        }
        // create the resulting array
        byte[] result = new byte[outIndex];
        System.arraycopy(out, 0, result, 0, outIndex);
        return result;
    }

    /**
     * BASE 64 MAP.
     */
    private static final byte[] MAP = new byte[]
        {'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 
         'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', 'a', 'b', 
         'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 
         'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', '0', '1', '2', '3', 
         '4', '5', '6', '7', '8', '9', '+', '/'};
    
    /**
     * base64编码。
     * @param in 原始输入
     * @param charsetName 字符串集
     * @return base64编码后的字符串。
     * @throws UnsupportedEncodingException UnsupportedEncodingException
     */
    public static String encode(byte[] in, String charsetName) throws UnsupportedEncodingException {
        int length = in.length * 4 / 3;  // SUPPRESS CHECKSTYLE
        length += length / 76 + 3; // SUPPRESS CHECKSTYLE // for crlr 
        byte[] out = new byte[length];
        int index = 0, i, crlr = 0, end = in.length - in.length%3; // SUPPRESS CHECKSTYLE
        for (i=0; i<end; i+=3) { // SUPPRESS CHECKSTYLE
            out[index++] = MAP[(in[i] & 0xff) >> 2]; // SUPPRESS CHECKSTYLE
            out[index++] = MAP[((in[i] & 0x03) << 4)  // SUPPRESS CHECKSTYLE
                                | ((in[i+1] & 0xff) >> 4)]; // SUPPRESS CHECKSTYLE
            out[index++] = MAP[((in[i+1] & 0x0f) << 2)  // SUPPRESS CHECKSTYLE
                                | ((in[i+2] & 0xff) >> 6)]; // SUPPRESS CHECKSTYLE
            out[index++] = MAP[(in[i+2] & 0x3f)]; // SUPPRESS CHECKSTYLE
            if (((index - crlr)%76 == 0) && (index != 0)) { // SUPPRESS CHECKSTYLE
                out[index++] = '\n'; 
                crlr++;
                //out[index++] = '\r';
                //crlr++;
            }
        }
        
        switch (in.length % 3) { // SUPPRESS CHECKSTYLE
            case 1:
                out[index++] = MAP[(in[end] & 0xff) >> 2]; // SUPPRESS CHECKSTYLE
                out[index++] = MAP[(in[end] & 0x03) << 4]; // SUPPRESS CHECKSTYLE
                out[index++] = '=';
                out[index++] = '=';
                break;
            case 2:
                out[index++] = MAP[(in[end] & 0xff) >> 2]; // SUPPRESS CHECKSTYLE
                out[index++] = MAP[((in[end] & 0x03) << 4)  // SUPPRESS CHECKSTYLE
                                    | ((in[end+1] & 0xff) >> 4)]; // SUPPRESS CHECKSTYLE
                out[index++] = MAP[((in[end+1] & 0x0f) << 2)];     // SUPPRESS CHECKSTYLE 
                out[index++] = '=';
                break;
        }
        return new String(out, 0, index, charsetName);
    }
}

