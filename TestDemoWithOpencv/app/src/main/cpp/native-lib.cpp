#include <jni.h>
#include <string>
#include <iostream>
#include <stdio.h>
#include <stdlib.h>
#include <opencv2/opencv.hpp>

using namespace cv;
using namespace std;


extern "C"
JNIEXPORT jstring

JNICALL
Java_com_example_v_1fanlulin_testdemowithopencv_MainActivity_stringFromJNI(
        JNIEnv *env,
        jobject /* this */) {
    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}
//extern "C"
//JNIEXPORT jintArray JNICALL
//Java_com_example_v_1fanlulin_testdemowithopencv_MainActivity_imgToGray(JNIEnv *env,
//                                                                       jobject instance,
//                                                                       jintArray buf, jint w,
//                                                                       jint h) {
//
//    jint *cbuf = env->GetIntArrayElements(buf, JNI_FALSE );
//    if (cbuf == NULL) {
//        return 0;
//    }
//
//    Mat imgData(h, w, CV_8UC4, (unsigned char *) cbuf);
//
//    uchar* ptr = imgData.ptr(0);
//    for(int i = 0; i < w*h; i ++){
//        //计算公式：Y(亮度) = 0.299*R + 0.587*G + 0.114*B
//        //对于一个int四字节，其彩色值存储方式为：BGRA
//        int grayScale = (int)(ptr[4*i+2]*0.299 + ptr[4*i+1]*0.587 + ptr[4*i+0]*0.114);
//        ptr[4*i+1] = grayScale;
//        ptr[4*i+2] = grayScale;
//        ptr[4*i+0] = grayScale;
//    }
//
//    int size = w * h;
//    jintArray result = env->NewIntArray(size);
//    env->SetIntArrayRegion(result, 0, size, cbuf);
//    env->ReleaseIntArrayElements(buf, cbuf, 0);
//    return result;
//}

extern "C"
JNIEXPORT jintArray JNICALL
Java_com_example_v_1fanlulin_testdemowithopencv_MainActivity_imgToGray(JNIEnv *env,
                                                                       jobject instance,
                                                                       jintArray buf, jint w,
                                                                       jint h) {

    jint *cbuf = env->GetIntArrayElements(buf, JNI_FALSE );
    if (cbuf == NULL) {
        return 0;
    }

    Mat imgData(h, w, CV_8UC4, (unsigned char *) cbuf);
    /*图像处理开始*/
//    Mat dst = imgData.clone();
    cvtColor(imgData,imgData,CV_BGRA2BGR);
    blur(imgData,imgData,Size(20,20));
    cvtColor(imgData,imgData,CV_BGR2BGRA);
//    for ( int i = 1; i < MAX_KERNEL_LENGTH; i = i + 2 )
//    {
//        bilateralFilter ( imgData, dst, i, i*2, i/2 );
//    }
    /*图像处理结束*/
    uchar *ptr = imgData.data;
//    unsigned char *resultArray=new unsigned char[w*h];
//    if (dst.isContinuous())
//        resultArray = dst.data;
//    cout<<"dst row: "<<dst.rows<<"dst cols: "<<dst.cols<<endl;
//    cout<<"imgData row: "<<imgData.rows<<"imgData cols: "<<imgData.cols<<endl;
//    cout<<"w: "<< w << "h: "<< h<<endl;
    int size = w * h;
    jintArray result = env->NewIntArray(size);
    env->SetIntArrayRegion(result, 0, size, (const jint *)ptr);
    env->ReleaseIntArrayElements(buf, cbuf, 0);

//    env->ReleaseIntArrayElements(buf, (jint *)resultArray, 0);
    return result;
}