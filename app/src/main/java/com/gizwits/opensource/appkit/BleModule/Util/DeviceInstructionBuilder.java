package com.gizwits.opensource.appkit.BleModule.Util;

/**
 * 作者：Lin on 2018/3/27 14:18
 * 邮箱：yuanwenlin2014@foxmail.com
 */
public class DeviceInstructionBuilder {

    /**
     * 根据要执行的code和key 拼接出发送的指令串
     * @param code 需要执行的指令
     * @param key 指令标志位,回执时也会带着该标志位返回
     * @return 返回拼接好的指令串,将会发送给蓝牙设备
     */
    public static byte[] getInstructionBytes(byte code[],int key){


        byte head[] = new byte[]{(byte) 0xbb, (byte) 0x66};
        byte keyCode = (byte) key;
        byte length = (byte) (code.length+1);
        byte result[] = new byte[head.length+2+code.length];

        System.arraycopy(head, 0, result, 0, head.length);
        result[2]=length;
        result[3]=keyCode;
        System.arraycopy(code, 0, result, head.length+2, code.length);

        return result;
    }
}
