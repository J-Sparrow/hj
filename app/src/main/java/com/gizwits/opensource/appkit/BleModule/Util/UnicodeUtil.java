package com.gizwits.opensource.appkit.BleModule.Util;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;

/**
 * 作者：Lin on 18/2/26 14:00
 * 邮箱：yuanwenlin2014@foxmail.com
 */
public class UnicodeUtil {


    /**
     * 陀螺仪传感器的字符串倒转一下，再转成double
     * @param sensorResult
     * @return
     */
    public static double gyroString2double(String sensorResult){
        String result1 = sensorResult.substring(0, 2);
        String result2 = sensorResult.substring(2, 4);
        String result3 = sensorResult.substring(4, 6);
        String result4 = sensorResult.substring(6, 8);
        String result = result4 + result3 + result2 + result1;
        double value = hexString2double(result);
        return value;
    }

    public static String stringUpsideDown(String sensorResult){
        sensorResult = sensorResult.trim();
        String result1 = sensorResult.substring(0, 2);
        String result2 = sensorResult.substring(2, 4);
        String result3 = sensorResult.substring(4, 6);
        String result4 = sensorResult.substring(6, 8);
        String result = result4 + result3 + result2 + result1;
        return result;
    }



    /**
     * 16进制转double
     * @param str
     * @return
     */
    public static double hexString2double(String str){
        String binaryString = hexString2binaryString(str);
        String flag = binaryString.substring(0,1);
        double value = 0;
        if(flag.equals("0")){
             value = Float.intBitsToFloat(Integer.valueOf(str.trim(), 16));
        }else if(flag.equals("1")){
            String newstr = "0" + binaryString.substring(1);
            value = - Float.intBitsToFloat(Integer.valueOf(newstr.trim(), 2));
        }
        return  value;
    }


    /**
     * 2进制转16机制
     * @param bString
     * @return
     */
    public static String binaryString2hexString(String bString)
    {
        if (bString == null || bString.equals("") || bString.length() % 8 != 0)
            return null;
        StringBuffer tmp = new StringBuffer();
        int iTmp = 0;
        for (int i = 0; i < bString.length(); i += 4)
        {
            iTmp = 0;
            for (int j = 0; j < 4; j++)
            {
                iTmp += Integer.parseInt(bString.substring(i + j, i + j + 1)) << (4 - j - 1);
            }
            tmp.append(Integer.toHexString(iTmp));
        }
        return tmp.toString();
    }



    /**
     * 16进制转2进制字符串
     * @param hexString
     * @return
     */
    public static String hexString2binaryString(String hexString)
    {
        if (hexString == null || hexString.length() % 2 != 0)
            return null;
        String bString = "", tmp;
        for (int i = 0; i < hexString.length(); i++)
        {
            tmp = "0000"
                    + Integer.toBinaryString(Integer.parseInt(hexString
                    .substring(i, i + 1), 16));
            bString += tmp.substring(tmp.length() - 4);
        }
        return bString;
    }

    /**
     * XML取到的double型字符串转int
      * @param str
     * @return
     */
    public static int strD2int(String str){
        double timesDouble = Double.parseDouble(str);
        int times = 0;
        if (timesDouble > Integer.MAX_VALUE) {
            times = Integer.MAX_VALUE;
        } else if (timesDouble < Integer.MIN_VALUE) {
            times = Integer.MIN_VALUE;
        } else {
            times = (int) timesDouble;
        }
        return times;
    }
    /**
     * 16进制转byte，用于通过蓝牙发送
     * @param hex 需要转16进制的string串
     * @return 转了16进制的byte码
     */
    public static byte[] hex2bytes(String hex) {
        String digital = "0123456789ABCDEF";
        String hex1 = hex.replace(" ", "");
        char[] hex2char = hex1.toCharArray();
        byte[] bytes = new byte[hex1.length() / 2];
        byte temp;
        for (int p = 0; p < bytes.length; p++) {
            temp = (byte) (digital.indexOf(hex2char[2 * p]) * 16);
            temp += digital.indexOf(hex2char[2 * p + 1]);
            bytes[p] = (byte) (temp & 0xff);
        }
        return bytes;
    }

    /**
     * 10进制int转2位16进制
     * @param value 10进制Int
     * @return 16进制String
     */
    public static String to2lengthHexString(int value){
        String str = null;
        if(value >=256)   value = 256;
        if(value < 0)     value = 0;
        if (value<16){
            str = Integer.toHexString(value);
            str = "0" + str;
        }
        else {
            str = Integer.toHexString(value);
        }
        return str;
    }

    /**
     * 10进制String转2位16进制
     * @param str 10进制String
     * @return 16进制String
     */
    public static String toHexString(String str){
        if (Integer.parseInt(str)<16){
            str = Integer.toHexString(Integer.parseInt(str));
            str = "0" + str;
        }
        else{
            str = Integer.toHexString(Integer.parseInt(str));
        }
        return str;
    }


    /**
     * 10进制String转4位16进制,
     * @param str 10进制String
     * @return 16进制String
     */
    public static String to4lengthHexString(String str){
        if (Integer.parseInt(str)<16 & Integer.parseInt(str)> 0){
            str = Integer.toHexString(Integer.parseInt(str));
            str = str.toUpperCase();
            str = "0" + str + "00";
        }else if(Integer.parseInt(str)<256 & Integer.parseInt(str)>=16){
            str = Integer.toHexString(Integer.parseInt(str));
            str = str.toUpperCase();
            str = str + "00";
        }else if(Integer.parseInt(str)<4096 & Integer.parseInt(str)>=256){
            str = Integer.toHexString(Integer.parseInt(str));
            str = str.toUpperCase();
            str = str.substring(1,3) + "0" + str.substring(0,1);
        }else if(Integer.parseInt(str)<65536 & Integer.parseInt(str)>=4096){
            str = Integer.toHexString(Integer.parseInt(str));
            str = str.toUpperCase();
            str = str.substring(2,4) + str.substring(0,2);
        }else if(Integer.parseInt(str)>=65536){
            str = "FFFF";
        }else {
            str = "0000";
        }
        return str;
    }


    /**
     *     将字符串转成ASCII，并用16进制string表示
     * @param value
     * @return
     */
    public static String stringToAscii(String value)
    {
        StringBuffer sbu = new StringBuffer();
        char[] chars = value.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            if(i != chars.length - 1)
            {
                sbu.append(toHexString(String.valueOf((int)chars[i]))).append(" ");
            }
            else {
                sbu.append(toHexString(String.valueOf((int)chars[i])));           }       }
        return sbu.toString();
    }

    /**
     * double转Hexstring
     * @param valueDouble
     * @param min
     * @param max
     * @param defMin
     * @param defMax
     * @return valueString
     */
    public static String double2Hexstring(double valueDouble, int min, int max, int defMin, int defMax) {
        int value = (int) valueDouble;
        if (value > max ) value = defMax;
        if ( value < min) value = defMin;

        String valueString = UnicodeUtil.toHexString("" + value);
        return valueString;
    }

    /**
     *  bytes转换成16进制String串
     * @param bs 需要转String的Byte[]
     * @return 转成16进制的Sting串
     */
    public static String bytes2HexStr(byte[] bs)
    {
        char[] chars = "0123456789ABCDEF".toCharArray();
        StringBuilder sb = new StringBuilder("");
        //    byte[] bs = str.getBytes();
        int bit;

        for (int i = 0; i < bs.length; i++)
        {
            bit = (bs[i] & 0x0f0) >> 4;
            sb.append(chars[bit]);
            bit = bs[i] & 0x0f;
            sb.append(chars[bit]);
            sb.append(' ');
        }
        return sb.toString().trim();
    }




    /**
     * bytes转成char数组
     * @param bytes 需要转的bytes
     * @return char数组
     */
    public static char[] bytes2chars (byte[] bytes) {
        Charset cs = Charset.forName ("UTF-8");
        ByteBuffer bb = ByteBuffer.allocate (bytes.length);
        bb.put (bytes);
        bb.flip ();
        CharBuffer cb = cs.decode (bb);

        return cb.array();
    }

    public static char[] hex2chars(String str){
        return bytes2chars(hex2bytes(str));
    }

    /**
     * 求负数的补码的方法。 注意： 负数的补码是在其原码的基础上，符号位不变，其余位取反，然后加1
     * @param hexString 取反的值
     * @author lhever 2017年4月4日 下午8:42:47
     * @since v1.0
     */
    public static String getComplementCode(String hexString)
    {

        byte[] value = hex2bytes(hexString);
        System.out.println();
        for (int i = 0; i < 32; i++)
        {
            // 0x80000000 是一个首位为1，其余位数为0的整数
//            value = (value & 0x80000000 >>> i) >>> (31 - i);
        }
        return String.valueOf(value);
    }
}
