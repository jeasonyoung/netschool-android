package com.examw.netschool.util;

import android.util.Base64;
import android.util.Log;

import org.apache.commons.lang3.StringUtils;

import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 16进制工具类。
 * Created by jeasonyoung on 16/3/18.
 */
public final class Hex {

    /**
     * UTF-8字符编码。
     */
    public static final Charset DEFAULT_CHARSET = Charset.forName("UTF-8");

    //hex字符数组
    private static final char[] digits = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

    /**
     * 将字节数组转换为Hex字符数组。
     * @param data
     * 字节数组
     * @return Hex字符数组
     */
    public static char[] encodeHex(final byte[] data){
        final int len;
        if(data != null && (len = data.length) > 0){
            final char[] out = new char[len << 1];
            for(int i = 0,j = 0; i < len; i++){
                out[j++] = digits[(0xF0 & data[i]) >>> 4];
                out[j++] = digits[(0x0F & data[i])];
            }
            return out;
        }
        return null;
    }

    /**
     * 将字节数组转换为Hex字符串。
     * @param data
     * 字节数组。
     * @return Hex字符串。
     */
    public static String encodeHexString(final byte[] data){
        return new String(encodeHex(data));
    }

    private static int toDigit(final char ch, final int index) throws Exception{
        final int digit = Character.digit(ch,16);
        if(digit == -1){
            throw new Exception("Illegal hexadecimal character " + ch + " at index " + index);
        }
        return digit;
    }

    /**
     * 将Hex字符串转换为字节数组。
     * @param data
     * Hex字符数组。
     * @return 字节数组。
     */
    public static byte[] decodeHex(final char[] data) throws Exception{
        final int len;
        if(data != null && (len = data.length) > 0) {
            if((len & 0x01) != 0) throw new Exception("odd number of characters");
            final byte[] out = new byte[len >> 1];
            for(int i = 0,j = 0; j < len; i++){
                int f = toDigit(data[j], j) << 4;
                j++;
                f = f | toDigit(data[j], j);
                j++;
                out[i] = (byte)(f & 0xFF);
            }
            return out;
        }
        return null;
    }

    /**
     * 字节数组md5加密
     * @param data
     * 明文字节数组
     * @return md5加密后的字节数组
     */
    public static byte[] md5(final byte[] data){
        if(data != null && data.length > 0) {
            try {
                return MessageDigest.getInstance("MD5").digest(data);
            }catch (final NoSuchAlgorithmException e){
                throw new IllegalArgumentException(e);
            }
        }
        return null;
    }

    /**
     * 将字符串md5加密后转换为Hex.
     * @param data
     * 明文字符串。
     * @return hex字符串。
     */
    public static String md5Hex(String data){
        if(!StringUtils.isBlank(data)){
            return encodeHexString(md5(data.getBytes(DEFAULT_CHARSET)));
        }
        return null;
    }

    /**
     * 将数据进行Base64编码。
     * @param data
     * 明文数据。
     * @return Base64字符串。
     */
    public static String base64EncodeToString(String data){
        if(!StringUtils.isBlank(data)){
            final byte[] arrays = data.getBytes(DEFAULT_CHARSET);
            return Base64.encodeToString(arrays, Base64.DEFAULT);
        }
        return null;
    }

    /**
     * 将Base64进行解码。
     * @param base64
     * base64字符串。
     * @return 解密后的明文。
     */
    public static String base64DecodeToString(String base64){
        if(!StringUtils.isBlank(base64)){
            try {
                final byte[] data = base64.getBytes(DEFAULT_CHARSET);
                return new String(Base64.decode(data, Base64.DEFAULT));
            }catch (Exception e){
                Log.e("Hex", "base64DecodeToString: base64[]解密错误=>" + e, e);
            }
        }
        return null;
    }
}
