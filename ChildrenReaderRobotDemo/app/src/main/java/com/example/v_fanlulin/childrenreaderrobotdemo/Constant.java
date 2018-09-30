package com.example.v_fanlulin.childrenreaderrobotdemo;


public class Constant {
    public static final int JNI_RESULT_SUCCESS = 1;
    public static final int JNI_RESULT_FAILURE = 0;

    public static final String BUSINESSCARD_SUCCESS = "success";
    public static final String BUSINESSCARD_FAILURE = "failure";

    public static final int RATIO_MIN = (int) (0.5 * 1024);
    public static final int RATIO_MAX = (int) (0.7 * 1024);
 
    public static final int MAX_PIC_WIDTH = 1280; // 640
    public static final int MAX_PIC_HEIGHT = 720; // 480
    public static final double ASPECT_RATIO = (double) MAX_PIC_WIDTH / MAX_PIC_HEIGHT;

}
