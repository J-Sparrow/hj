package com.gizwits.opensource.appkit.BleModule.Util;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.util.Log;

import com.gizwits.opensource.appkit.GosApplication;
import com.gizwits.opensource.appkit.BleModule.SearchActivity;
import com.gizwits.opensource.appkit.BleModule.bluetooth.BleInterface;
import com.gizwits.opensource.appkit.R;

/**
 * 作者：Lin on 18/2/24 16:45
 * 邮箱：yuanwenlin2014@foxmail.com
 */
public class DeviceControlUtil {
    private static final String TAG = DeviceControlUtil.class.getSimpleName();
    public static final byte[] foward = {16, 1, 1, 1, 1, 1, 2};//前进指令
    public static final byte[] backward = {16, 1, 1, 1, 1, 1, 2};//后退指令
    public static final byte[] left = {16, 1, 1, 1, 1, 1, 2};
    public static final byte[] right = {16, 1, 1, 1, 1, 1, 2};
    public static final String colorHead = "BB 66 09 00 02 04 00 00 ";
    public static final String soundeMi = "0B 02";

    private static boolean ifDialog = false;

    /**
     * 小车复位
     *
     * @param msgRequest
     * @param context
     */
    public static void reset(BleInterface.MessageCallback msgRequest, Context context) {
        String resetMsg = "01 02 F5 00 00";
        send(resetMsg, msgRequest, context);
    }

    /**
     * 蓝牙改名
     *
     * @param bleName
     * @param msgRequest
     * @param context
     */
    public static void changeBleName(String bleName, BleInterface.MessageCallback msgRequest, Context context) {

        String lenth = UnicodeUtil.toHexString(String.valueOf(bleName.length()));
        String nameHex = UnicodeUtil.stringToAscii(bleName);
        String msg = "04 02 F3 00 00 " + lenth + nameHex;
        send(msg, msgRequest, context);
    }

    /**
     * 同时控制左右电机的指令
     *
     * @param leftSpeed  左轮转速 顺时针为正 -255 ~ 255
     * @param rightSpeed 右轮转速 顺时针为正 -255 ~ 255
     */
    public static void moveMonitor(int leftSpeed, int rightSpeed, BleInterface.MessageCallback msgRequest, Context context) {
        leftSpeed = leftSpeed > 255 ? 255 : leftSpeed;
        rightSpeed = rightSpeed > 255 ? 255 : rightSpeed;
        leftSpeed = leftSpeed < -255 ? -255 : leftSpeed;
        rightSpeed = rightSpeed < -255 ? -255 : rightSpeed;
        String clockwise = " 00";
        String conterclockwise = " FF";
        String leftSpeedStr = "";
        String rightSpeedStr = "";
        if (leftSpeed >= 0) {
            leftSpeedStr = Integer.toHexString(leftSpeed);
            if (leftSpeedStr.equals("0"))
                leftSpeedStr = "00";
            leftSpeedStr = leftSpeedStr + clockwise;
        } else {
            String arg0 = Integer.toHexString(leftSpeed).toUpperCase();
            arg0 = arg0.replace("FFFFFF", " ");
            leftSpeedStr = arg0 + conterclockwise;
        }

        if (rightSpeed >= 0) {
            rightSpeedStr = Integer.toHexString(rightSpeed);
            if (rightSpeedStr.equals("0"))
                rightSpeedStr = "00";
            rightSpeedStr = rightSpeedStr + clockwise;
        } else {
            String arg0 = Integer.toHexString(rightSpeed).toUpperCase();
            arg0 = arg0.replace("FFFFFF", " ");
            rightSpeedStr = arg0 + conterclockwise;
        }
        String motorMsg = "00 02 31 01 00 04 00 " + leftSpeedStr + " " + rightSpeedStr;
        Log.d(TAG, "MotorMsg: " + motorMsg);
        if (leftSpeed == 0 && rightSpeed == 0) {
            send(motorMsg, msgRequest, 300l, true, context);
        } else {
            send(motorMsg, msgRequest, 200l, false, context);
        }
    }

    public static void motorMonitor(String port, int speed, BleInterface.MessageCallback msgRequest, Context context) {
        speed = speed > 255 ? 255 : speed;
        speed = speed < -255 ? -255 : speed;
        String clockwise = " 00";
        String conterclockwise = " FF";
        String speedStr = "";
        if (speed >= 0) {
            speedStr = Integer.toHexString(speed);
            if (speedStr.equals("0"))
                speedStr = "00";
            speedStr = speedStr + clockwise;
        } else {
            String arg0 = Integer.toHexString(speed).toUpperCase();
            arg0 = arg0.replace("FFFFFF", " ");
            speedStr = arg0 + conterclockwise;
        }
        String motorMsg = "00 02 01 " + port + " 00 " + speedStr;
        Log.d(TAG, "MotorMsg: " + motorMsg);
        if (speed == 0) {
            send(motorMsg, msgRequest, 300l, true, context);
        } else {
            send(motorMsg, msgRequest, 200l, false, context);
        }
    }

    public static void motor(String port, String dir, double speedDouble, BleInterface.MessageCallback msgRequest, Context context) {
        speedDouble = speedDouble * 2.55;
        int speed = (new Double(speedDouble)).intValue();
        speed = speed > 0 ? speed : 0;
        if ("01".equals(dir)) {
            speed = -speed;
        }
        motorMonitor(port, speed, msgRequest, context);
    }

    public static void servo(String port, String speed, double angleDouble, BleInterface.MessageCallback msgRequest, Context context) {
        int angle = (int) angleDouble;
        angle = angle > 180 ? 180 : angle;
        angle = angle < 0 ? 0 : angle;
        String angleStr = Integer.toHexString(angle);
        if (angleStr.equals("0"))
            angleStr = "00";
        String servoMsg = "01 02 02 " + port + "00 01" + angleStr + speed;
        send(servoMsg, msgRequest, 200l, false, context);

    }

    /**
     * 开启寻线模式
     */
    public static void findLineType(BleInterface.MessageCallback msgRequest, Context context) {
        String find_lineMsg = "00 02 F2 00 00 02";
        send(find_lineMsg, msgRequest, context);
        Log.d(TAG, "findlineMsg" + find_lineMsg);
    }

    /**
     * 开启壁障模式
     */
    public static void avoidType(BleInterface.MessageCallback msgRequest, Context context) {

        String find_lineMsg = "00 02 F2 00 00 03";
        send(find_lineMsg, msgRequest, context);
        Log.d(TAG, "avoidType" + find_lineMsg);
    }

    /**
     * 开启蓝牙遥控模式
     */
    public static void telecontrolType(BleInterface.MessageCallback msgRequest, Context context) {
        String find_lineMsg = "00 02 F2 00 00 01";
        send(find_lineMsg, msgRequest, context);
        Log.d(TAG, "telecontrol" + find_lineMsg);
    }


    /**
     * 调用巡线传感器判断两个巡线传感器的状态是否与选择值相同
     *
     * @param find_line_options
     */
    public static void find_line(String find_line_options, BleInterface.MessageCallback mBleCallback, Context context) {
        String find_lineMsg = "01 01 0B 01 00 ";
        //发送巡线传感器请求，然后等待机器人回复巡线传感器结果，进行判断
        send(find_lineMsg, mBleCallback, context);
        //find_line_Result和find_line_options的值都是00,01,02,03中的一个。
    }

    public static void sensorSend(String sensorType, String port, BleInterface.MessageCallback mBleCallback, Context context) {
        String msg = "";
        switch (sensorType) {
            case "find_line":
                msg = "01 01 0B" + port + "00";
                break;
            case "find_line_value":
                msg = "01 01 0B" + port + "00";
                break;
            case "sensor_ultrasonic":
                msg = "01 01 0D" + port + "00";
                break;
            case "sensor_ultrasonic_value":
                msg = "01 01 0D" + port + "00";
                break;
            case "sensor_potentiometer":
                msg = "01 01 12" + port + "00";
                break;
            case "sensor_light":
                msg = "01 01 08" + port + "00 02";
                break;
            case "sensor_light_value":
                msg = "01 01 08" + port + "00 01";
                break;
            case "sensor_hot":
                msg = "01 01 09" + port + "00 02";
                break;
            case "sensor_hot_value":
                msg = "01 01 09" + port + "00 01";
                break;
            case "sensor_avoid":
                msg = "01 01 0A" + port + "00 01";
                break;
            case "sensor_vibrate":
                msg = "01 01 0F" + port + "00 01";
                break;
            case "sensor_gyro":
                msg = "02 01 07" + "00" + "00";
                break;
            case "sensor_temperature":
                msg = "01 01 0C" + port + "00 01";
                break;
            case "sensor_humidity":
                msg = "01 01 0C" + port + "00 02";
                break;
            default:
                break;
        }
        send(msg, mBleCallback, context);
    }

    public static void sensorSend(String sensorType, String port, String key, BleInterface.MessageCallback mBleCallback, Context context) {
        String msg = "";
        switch (sensorType) {
            case "sensor_button":
                msg = "01 01 13" + port + "00"+ key ;
                break;
            default:
                break;
        }
        send(msg, mBleCallback, context);
    }

    /**
     * 调用巡线传感器判断两个巡线传感器的状态是否与选择值相同
     *
     * @param
     */
    public static void vibrationSensor(BleInterface.MessageCallback mBleCallback, Context context) {
        String vibrationMsg = "01 01 0B 01 00 ";
        //发送巡线传感器请求，然后等待机器人回复巡线传感器结果，进行判断
        send(vibrationMsg, mBleCallback, context);
        //find_line_Result和find_line_options的值都是00,01,02,03中的一个。
    }

    /**
     * 调用巡线传感器判断两个巡线传感器的状态是否与选择值相同
     *
     * @param
     */
    public static void temperatureSensor(BleInterface.MessageCallback mBleCallback, Context context) {
        String vibrationMsg = "01 01 0B 01 00 ";
        //发送巡线传感器请求，然后等待机器人回复巡线传感器结果，进行判断
        send(vibrationMsg, mBleCallback, context);
        //find_line_Result和find_line_options的值都是00,01,02,03中的一个。
    }

    /**
     * 调用巡线传感器判断两个巡线传感器的状态是否与选择值相同
     *
     * @param
     */
    public static void lightSensor(BleInterface.MessageCallback mBleCallback, Context context) {
        String vibrationMsg = "01 01 0B 01 00 ";
        //发送巡线传感器请求，然后等待机器人回复巡线传感器结果，进行判断
        send(vibrationMsg, mBleCallback, context);
        //find_line_Result和find_line_options的值都是00,01,02,03中的一个。
    }


//    /**
//     * 运动功能，同时控制两个电机
//     * 速度为下拉列表选择模式，已更替为填入变量模式，


//     * @param dir
//     * @param speed
//     */
//    public static void move(final String dir, final String speed,BleInterface.MessageCallback msgRequest,Context context) {
//
//        int leftSpeed = 0;
//        int rightSpeed = 0;
//
//        if (dir.equals("forward")) {
//            if (speed.equals("fast")) {
//                leftSpeed = -255;
//                rightSpeed =255 ;
//            } else if (speed.equals("slow")) {
//                leftSpeed = -150;
//                rightSpeed = 150;
//            } else {
//                leftSpeed = 0;
//                rightSpeed = 0;
//            }
//        } else if (dir.equals("back")) {
//            if (speed.equals("fast")) {
//                leftSpeed = 255;
//                rightSpeed = -255;
//            } else if (speed.equals("slow")) {
//                leftSpeed = 150;
//                rightSpeed = -150;
//            } else {
//                leftSpeed = 0;
//                rightSpeed = 0;
//            }
//        } else if (dir.equals("left")) {
//            if (speed.equals("fast")) {
//                leftSpeed = 255;
//                rightSpeed = 255;
//            } else if (speed.equals("slow")) {
//                leftSpeed = 150;
//                rightSpeed = 150;
//            } else {
//                leftSpeed = 0;
//                rightSpeed = 0;
//            }
//        } else if (dir.equals("right")) {
//            if (speed.equals("fast")) {
//                leftSpeed = -255;
//                rightSpeed = -255;
//            } else if (speed.equals("slow")) {
//                leftSpeed = -150;
//                rightSpeed = -150;
//            } else {
//                leftSpeed = 0;
//                rightSpeed = 0;
//            }
//        }
//        moveMonitor( leftSpeed, rightSpeed,msgRequest,context);
//    }

    /**
     * @param dir
     * @param
     * @param msgRequest
     * @param context
     */
    public static void move(String dir, double speedDouble, BleInterface.MessageCallback msgRequest, Context context) {
        speedDouble = speedDouble * 2.55;
        int speed = (new Double(speedDouble)).intValue();
        speed = speed > 0 ? speed : 0;
        int leftSpeed = 0;
        int rightSpeed = 0;
        if ("FORWARD".equals(dir)) {
            leftSpeed = -speed;
            rightSpeed = speed;
        } else if ("BACK".equals(dir)) {
            leftSpeed = speed;
            rightSpeed = -speed;
        } else if ("TURNLEFT".equals(dir)) {
            leftSpeed = speed;
            rightSpeed = speed;
        } else if ("TURNRIGHT".equals(dir)) {
            leftSpeed = -speed;
            rightSpeed = -speed;
        }
        moveMonitor(leftSpeed, rightSpeed, msgRequest, context);
    }

//        /**
//         * 电机功能，控制单个电机
//         *速度为下拉列表选择模式，已更替为填入变量模式，

//         * @param port    端口
//         * @param dir     方向
//         * @param speed   速度
//         */
//
//    public static void motor(final String port, final String dir, String speed,BleInterface.MessageCallback msgRequest,Context context) {
//        if (dir.equals("00")) {
//            //顺时针00 逆时针01
////                        快 FF 00  慢 96 00   停止 00 00
//            if (speed.equals("fast")) {
//                speed = "FF 00";
//            } else if (speed.equals("slow")) {
//                speed = "96 00";
//            } else {
//                speed = "00 00";
//            }
//        } else {
////                        负 快：01 FF 慢  6A FF 停止 00 00
//            if (speed.equals("fast")) {
//                speed = "01 FF";
//            } else if (speed.equals("slow")) {
//                speed = "6A FF";
//            } else {
//                speed = "00 00";
//            }
//        }
//        String motorMsg = "00 02 01 " + port + " 00 " + speed;
//        Log.d(TAG, "motorMsg" + motorMsg);
//        send(motorMsg,msgRequest,context);
//    }

    /**
     * 停止运动
     *
     * @param msgRequest
     */
    public static void stopMove(BleInterface.MessageCallback msgRequest, Context context) {
        String motorMsg = "00 02 31 01 00 04 00 00 00 00 00 ";
        send(motorMsg, msgRequest, context);
    }


    public static void sevseg(String port, double numberDouble, BleInterface.MessageCallback msgRequest, Context context) {
        Float numberFloat = (float) numberDouble;
        String numberStr = Integer.toHexString(Float.floatToIntBits(numberFloat));
        if("0".equals(numberStr))   numberStr = "00000000";
        numberStr = UnicodeUtil.stringUpsideDown(numberStr);
        String sevsegMsg = "02 02 14 " + port + " 00 " + numberStr;
        Log.d("SEVSEG", numberStr + sevsegMsg);
        send(sevsegMsg, msgRequest, context);
    }

    public static void showNumber(String port, double numberDouble, BleInterface.MessageCallback msgRequest, Context context) {
        Float numberFloat = (float) numberDouble;
        String numberStr = Integer.toHexString(Float.floatToIntBits(numberFloat));
        if("0".equals(numberStr))   numberStr = "00000000";
        numberStr = UnicodeUtil.stringUpsideDown(numberStr);
        String showNumberMsg = "02 02 10 " + port + " 00 04 00 00 04 " + numberStr;
//        Log.d("SEVSEG", numberStr + showMsg);
//        showMsg = "00 02 10 01 00 04 00 00 04 00 00 A0 40";
        send(showNumberMsg, msgRequest, context);
    }

    public static void showCharacters(String port, double x, double y, String characters, BleInterface.MessageCallback msgRequest, Context context) {
        String strX = UnicodeUtil.to2lengthHexString((int)x);
        String strY = UnicodeUtil.to2lengthHexString((int)(y+7));
        if(characters.length() > 4){
            characters = characters.substring(0,4);
        }
        String lenth = UnicodeUtil.to2lengthHexString(characters.length());
        characters = UnicodeUtil.stringToAscii(characters);
        String showCMsg = "04 02 10 " + port + " 00 01 " + strX + strY + lenth + characters;
        send(showCMsg, msgRequest, context);
    }

    public static void showTime(String port, double hour, double minute, String option, BleInterface.MessageCallback msgRequest, Context context) {
        String hourStr = UnicodeUtil.to2lengthHexString((int)hour);
        String minuteStr = UnicodeUtil.to2lengthHexString((int)minute);
        String showTMsg = "01 02 10 " + port + " 00 03 00 00 03 " + option + hourStr + minuteStr;
        send(showTMsg, msgRequest, context);
    }

    public static void showDraw(String port, double x, double y, String draw, BleInterface.MessageCallback msgRequest, Context context) {
        String strX = UnicodeUtil.to2lengthHexString((int)x);
        String strY = UnicodeUtil.to2lengthHexString((int)y);
        String drawStr = null;
        switch (draw){
            case "01":
                drawStr ="00 00 40 48 44 42 02 02 02 02 42 44 48 40 00 00";
                break;
            case "02":
                drawStr ="00 00 40 42 44 48 08 08 08 08 48 44 42 40 00 00";
                break;
            case "03":
                drawStr ="18 24 42 42 24 18 08 08 08 08 18 24 42 42 24 18";
                break;
            case "04":
                drawStr ="00 42 24 18 00 00 02 04 02 04 02 00 18 24 42 00";
                break;
                default:
                break;
        }
        String showDMsg = "BB 66 1A 00 01 02 10 " + port + " 00 02 " + strX + strY + " 10 " ;
        send(showDMsg, msgRequest,0l,false, true,context);
        send(drawStr, msgRequest, 250l,false,true,context);
    }

    /**
     * 点亮单个LED灯
     *
     * @param lightNumber
     * @param redDouble
     * @param greenDouble
     * @param blueDouble
     */
    public static void light(double lightNumber, double redDouble, double greenDouble, double blueDouble, BleInterface.MessageCallback msgRequest, Context context) {
        String lightNum = UnicodeUtil.double2Hexstring(lightNumber, 1, 12, 99, 99);
        String red = UnicodeUtil.double2Hexstring(redDouble, 0, 255, 0, 255);
        String green = UnicodeUtil.double2Hexstring(greenDouble, 0, 255, 0, 255);
        String blue = UnicodeUtil.double2Hexstring(blueDouble, 0, 255, 0, 255);

        String lightMsg = "00 02 04 00 00 01 " + lightNum + red + green + blue;
        Log.d(TAG, lightMsg);
        send(lightMsg, msgRequest, context);
    }

    /**
     * 点亮全部LED灯
     *
     * @param redDouble
     * @param greenDouble
     * @param blueDouble
     */
    public static void light_all(double redDouble, double greenDouble, double blueDouble, BleInterface.MessageCallback msgRequest, Context context) {
        String red = UnicodeUtil.double2Hexstring(redDouble, 0, 255, 0, 255);
        String green = UnicodeUtil.double2Hexstring(greenDouble, 0, 255, 0, 255);
        String blue = UnicodeUtil.double2Hexstring(blueDouble, 0, 255, 0, 255);

        String lightMsg = "00 02 04 00 00 01 " + "00 " + red + green + blue;
        Log.d(TAG, lightMsg);
        send(lightMsg, msgRequest, context);
    }

    /**
     * 红光点亮全部LED灯
     */
    public static void redLightAll(BleInterface.MessageCallback msgRequest, Context context) {
        String lightMsg = "00 02 04 00 00 01 " + "00 FF 00 00";
        Log.d(TAG, lightMsg);
        send(lightMsg, msgRequest, context);
    }

    /**
     * 蓝光点亮全部LED灯
     */
    public static void blueLightAll(BleInterface.MessageCallback msgRequest, Context context) {
        String lightMsg = "00 02 04 00 00 01 " + "00 00 FF 00";
        Log.d(TAG, lightMsg);
        send(lightMsg, msgRequest, context);
    }

    /**
     * 绿光点亮全部LED灯
     */
    public static void greenLightAll(BleInterface.MessageCallback msgRequest, Context context) {
        String lightMsg = "00 02 04 00 00 01 " + "00 00 00 FF";
        Log.d(TAG, lightMsg);
        send(lightMsg, msgRequest, context);
    }

    /**
     * 熄灭全部LED灯
     */
    public static void closeLightAll(BleInterface.MessageCallback msgRequest, Context context) {
        String lightMsg = "00 02 04 00 00 01 00 00 00 00";
        Log.d(TAG, lightMsg);
        send(lightMsg, msgRequest, context);
    }

    /**
     * 选颜色的方式点亮单个LED灯
     *
     * @param lightNumber
     * @param colour
     */
    public static void light_colour(double lightNumber, String colour, BleInterface.MessageCallback msgRequest, Context context) {
        int lightNum = (int) lightNumber;
        if (lightNum > 12 | lightNum < 1) lightNum = 99;
//        String lightN = "0" + Integer.toHexString(lightNum);
        String lightN = UnicodeUtil.toHexString("" + lightNum);
        colour = colour.substring(1);

        String lightMsg = "00 02 04 00 00 01 " + lightN + colour;
        Log.d(TAG, lightMsg);
        send(lightMsg, msgRequest, context);
    }

    /**
     * 选颜色的方式点亮全部LED灯
     *
     * @param colour
     */
    public static void light_colour_all(String colour, BleInterface.MessageCallback msgRequest, Context context) {

        colour = colour.substring(1);
        String lightMsg = "00 02 04 00 00 01 " + "00" + colour;
        Log.d(TAG, lightMsg);
        send(lightMsg, msgRequest, context);
    }

    /**
     * 选颜色的方式点亮部分LED灯
     *
     * @param colour
     */
    public static void light_colour_some(double lightNumber, String colour, BleInterface.MessageCallback msgRequest, Context context) {
        int lightNum = (int) lightNumber;
        if (lightNum > 12 | lightNum < 1) lightNum = 99;
        String lightN = UnicodeUtil.toHexString("" + lightNum);
        colour = colour.substring(1);

        String lightMsg = "00 02 04 00 00 02 " + lightN + colour;
        Log.d(TAG, lightMsg);
        send(lightMsg, msgRequest, context);
    }

    /**
     * 外置LED模块，点亮或者熄灭
     *
     * @param port
     * @param light 灯选择
     * @param state
     */
    public static void led(String port, String light, String state, BleInterface.MessageCallback msgRequest, Context context) {

        String msg = "01 02 0E " + port + " 00 01 " + light + state;

        Log.d(TAG, msg);
        send(msg, msgRequest, context);
    }

    /**
     * 外置LED模块，改变状态
     *
     * @param port
     * @param light
     * @param msgRequest
     * @param context
     */
    public static void ledChange(String port, String light, BleInterface.MessageCallback msgRequest, Context context) {

        String msg = "01 02 0E " + port + " 00 02 " + light;
        Log.d(TAG, msg);
        send(msg, msgRequest, context);
    }

    public static void setMusic(String tones, int beatInt, BleInterface.MessageCallback msgRequest, Context context) {
        String beat = "";
        switch (beatInt) {
            case 1:
                beat = "64 00";
                break;
            case 2:
                beat = "C8 00";
                break;
            case 4:
                beat = "FA 00";
                break;
            case 8:
                beat = "F4 01";
                break;
            case 16:
                beat = "E8 03";
                break;
            default:
                break;
        }

        switch (tones) {
            case "-1"://l_do
                tones = " 83 00";
                break;
            case "-2"://l_re
                tones = " 93 00";
                break;
            case "-3":
                tones = " A5 00";
                break;
            case "-4":
                tones = " AF 00";
                break;
            case "-5":
                tones = " C4 00";
                break;
            case "-6":
                tones = " DD 00";
                break;
            case "-7":
                tones = " F8 00";
                break;
            case "1"://do
                tones = " 06 01";
                break;
            case "2":
                tones = " 26 01";
                break;
            case "3":
                tones = " 4A 01";
                break;
            case "4":
                tones = " 5E 01";
                break;
            case "5":
                tones = " 89 01";
                break;
            case "6":
                tones = " B9 01";
                break;
            case "7":
                tones = " EF 01";
                break;
            case "11"://h_do
                tones = " 0D 02";
                break;
            case "12":
                tones = " 4D 02";
                break;
            case "13":
                tones = " 95 02";
                break;
            case "14":
                tones = " BC 02";
                break;
            case "15":
                tones = " 12 03";
                break;
            case "16":
                tones = " 72 03";
                break;
            case "17":
                tones = " DE 03";
                break;
        }
        String musicMsg = "00 02 03 00 00" + tones + beat;
        Log.d(TAG, "声音指令" + musicMsg);
        send(musicMsg, msgRequest, 400l, false, context);
    }

    public static void setSoundFrequency(String tones, String time, BleInterface.MessageCallback msgRequest, Context context) {
        tones = UnicodeUtil.to4lengthHexString(tones);
        time = UnicodeUtil.to4lengthHexString(time);
        String musicMsg = "00 02 03 00 00" + tones + time;
        Log.d(TAG,tones + time);
        send(musicMsg, msgRequest, 300l, false, context);
    }

    /**
     * 控制蜂鸣器发出单个音符
     *
     * @param tones 音阶 do h_do re h_re mi ......
     */
    public static void setSound(String tones, String beat, BleInterface.MessageCallback msgRequest, Context context) {
        switch (beat) {

            case "1":
                beat = "7d 00";
                break;
            case "2":
                beat = "FA 00";
                break;
            case "4":
                beat = "F4 01";
                break;
            case "8":
                beat = "E8 03";
                break;
            case "16":
                beat = "D0 07";
                break;
            default:
                break;
        }
        switch (tones) {
            case "l_do":
                tones = " 83 00";
                break;
            case "l_re":
                tones = " 93 00";
                break;
            case "l_mi":
                tones = " A5 00";
                break;
            case "l_fa":
                tones = " AF 00";
                break;
            case "l_so":
                tones = " C4 00";
                break;
            case "l_la":
                tones = " DD 00";
                break;
            case "l_xi":
                tones = " F8 00";
                break;
            case "do":
                tones = " 06 01";
                break;
            case "re":
                tones = " 26 01";
                break;
            case "mi":
                tones = " 4A 01";
                break;
            case "fa":
                tones = " 5E 01";
                break;
            case "so":
                tones = " 89 01";
                break;
            case "la":
                tones = " B9 01";
                break;
            case "xi":
                tones = " EF 01";
                break;
            case "h_do":
                tones = " 0D 02";
                break;
            case "h_re":
                tones = " 4D 02";
                break;
            case "h_mi":
                tones = " 95 02";
                break;
            case "h_fa":
                tones = " BC 02";
                break;
            case "h_so":
                tones = " 12 03";
                break;
            case "h_la":
                tones = " 72 03";
                break;
            case "h_xi":
                tones = " DE 03";
                break;
        }
        String musicMsg = "00 02 03 00 00" + tones + beat;
        Log.d(TAG, "声音指令" + musicMsg);
        send(musicMsg, msgRequest, 250l, false, context);
    }

    /**
     * 控制蜂鸣器发出单个音符
     *
     * @param tones 音阶 do h_do re h_re mi ......
     */
    public static void setSound(String tones, BleInterface.MessageCallback msgRequest, Context context) {
        String beat = " FA 00";
        setSound(tones, beat, msgRequest, context);
    }

    public static void ultrasonic(Integer port, BleInterface.MessageCallback msgRequest, Context context) {
        String ultrasonicMsg = "01 01 0D " + String.format("%02d", port) + " 00";
        Log.d(TAG, "超声波指令:" + ultrasonicMsg);
        send(ultrasonicMsg, msgRequest, null, true, context);
    }

    public static void send(String msg, BleInterface.MessageCallback msgRequest, Context context) {
        send(msg, msgRequest, null, null,false, context);
    }

    public static void send(String msg, BleInterface.MessageCallback msgRequest, Long timeout, Boolean timeoutResent, Context context) {
        send(msg, msgRequest, timeout, timeoutResent,false, context);
    }


    /**
     * 发送消息指令
     *
     * @param msg           需要发送的消息
     * @param msgRequest    发送消息的回执
     * @param timeout       收回复消息的超时设置 可为空
     * @param timeoutResend 收回复超时时是否进行重发处理 可为空
     */
    public static void send(String msg, BleInterface.MessageCallback msgRequest, Long timeout, Boolean timeoutResend, Boolean isLongByte, final Context context) {

        if (GosApplication.getmBleClient().isConnected()) {
            try {
                msg = msg.toUpperCase();
                GosApplication.getmBleClient().addMsg(UnicodeUtil.hex2bytes(msg), msgRequest, timeout, timeoutResend,isLongByte);
                Log.d("CHCarBleClient", msg);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            Log.d(TAG, "当前蓝牙未连接");
            if (!ifDialog) {
                new AlertDialog.Builder(context)
                        .setTitle("蓝牙未连接")
                        .setMessage("请选择设备")
                        .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                ifDialog = true;
                            }
                        })
                        .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Intent intent = new Intent(context, SearchActivity.class);
                                context.startActivity(intent);
                            }
                        })
                        .create().show();
            }
        }
    }


}
