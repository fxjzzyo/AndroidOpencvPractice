#include <jni.h>
#include <android/log.h>
#include <string>
#include <iostream>
#include <stdio.h>
#include <stdlib.h>
#include <opencv2/opencv.hpp>
#include <opencv2/objdetect/objdetect.hpp>


#define TAG    "myhello-jni-test" // 这个是自定义的LOG的标识
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,TAG ,__VA_ARGS__) // 定义LOGI类型

using namespace cv;
using namespace std;

string ImageHashValue(IplImage* src);
double ImageSimilarity(string &str1, string &str2);

//计算图片的指纹信息
string ImageHashValue(IplImage* src)
{
    string resStr(64, '\0');
    IplImage* image = cvCreateImage(cvGetSize(src), src->depth, 1);
    //step one : 灰度化
    if (src->nChannels == 3)  cvCvtColor(src, image, CV_BGR2GRAY);
    else  cvCopy(src, image);
    //step two : 缩小尺寸 8*8
    IplImage* temp = cvCreateImage(cvSize(8, 8), image->depth, 1);
    cvResize(image, temp);
    //step three : 简化色彩
    uchar* pData;
    for (int i = 0; i < temp->height; i++)
    {
        pData = (uchar*)(temp->imageData + i * temp->widthStep);
        for (int j = 0; j < temp->width; j++)
            pData[j] = pData[j] / 4;
    }
    //step four : 计算平均灰度值
    int average = cvAvg(temp).val[0];
    //step five : 计算哈希值
    int index = 0;
    for (int i = 0; i < temp->height; i++)
    {
        pData = (uchar*)(temp->imageData + i * temp->widthStep);
        for (int j = 0; j < temp->width; j++)
        {
            if (pData[j] >= average)
                resStr[index++] = '1';
            else
                resStr[index++] = '0';
        }
    }
    return resStr;
}

//根据指纹信息计算两幅图像的相似度
double ImageSimilarity(string &str1, string &str2)
{
    double similarity = 1.0;
    for (int i = 0; i < 64; i++)
    {
        char c1 = str1[i];
        char c2 = str2[i];
        if (c1 != c2)
            similarity = similarity - 1.0 / 64;
    }
    return similarity;
}


extern "C"
JNIEXPORT jstring
JNICALL
Java_com_example_v_1fanlulin_childrenreaderrobotdemo_MainActivity_stringFromJNI(
        JNIEnv *env,
        jobject /* this */) {
    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}


//比较图片相似度byte[] 指纹法
IplImage * iplImage1;
extern "C"
JNIEXPORT jdouble JNICALL
Java_com_example_v_1fanlulin_childrenreaderrobotdemo_MainActivity_judgeImgByte(JNIEnv *env,
                                                                               jobject instance,
                                                                               jbyteArray buf_,
                                                                               jint w, jint h,
                                                                               jint angle) {
    jbyte *buf = env->GetByteArrayElements(buf_, NULL);

    double similarity = -1;//初始化相似度为-1，如果返回-1，说明是第一次调用，只有一张图片，不进行比较
//    LOGI("-----------H = %d",h);
//    LOGI("-----------W = %d",w);
//    Mat img(h,w,CV_8UC1,(unsigned char *)buf);
    Mat img(h+h/2,w,CV_8UC1,(unsigned char *)buf);

    //转为bgr彩图
//    Mat mBgr;
//    cvtColor(img,mBgr,CV_YUV2BGR_NV21);

    //转为IplImage
    IplImage imgTmp = img;
    IplImage *iplImage2 = cvCloneImage(&imgTmp);
    if(iplImage1!=NULL){//当有了前一张图片时，再来了后一张图片，就进行比较
        //计算图片的指纹
        string imgPrint1 = ImageHashValue(iplImage1);
        string imgPrint2 = ImageHashValue(iplImage2);
        //根据指纹信息计算两幅图像的相似度
        similarity = ImageSimilarity(imgPrint1, imgPrint2);
        cvReleaseImage(&iplImage1);//释放内存
        //将img2赋值给img1
        iplImage1 = cvCloneImage(iplImage2);

    } else{//前一张图片为空时，来了一张图片，就进行赋值
        //赋值
        iplImage1 = cvCloneImage(iplImage2);
    }

    //释放内存
    img.release();
//    mBgr.release();
    cvReleaseImage(&iplImage2);

    env->ReleaseByteArrayElements(buf_, buf, 0);
    return similarity;

}


//获得图片byte[] 指纹信息
extern "C"
JNIEXPORT jstring JNICALL
Java_com_example_v_1fanlulin_childrenreaderrobotdemo_MainActivity_getImgFingerprint(JNIEnv *env,
                                                                                    jobject instance,
                                                                                    jbyteArray buf_,
                                                                                    jint w, jint h,
                                                                                    jint angle) {
    jbyte *buf = env->GetByteArrayElements(buf_, NULL);

    Mat img(h+h/2,w,CV_8UC1,(unsigned char *)buf);

    //旋转图片
    if(angle!=0){
        Point2f center(img.cols / 2, img.rows / 2);
        Mat rot = getRotationMatrix2D(center, angle, 1);
        Rect bbox = RotatedRect(center, img.size(), angle).boundingRect();
        rot.at<double>(0, 2) += bbox.width / 2.0 - center.x;
        rot.at<double>(1, 2) += bbox.height / 2.0 - center.y;
        Mat dst;
        warpAffine(img, img, rot, bbox.size());
    }

    //转为IplImage
    IplImage imgTmp = img;
    IplImage *iplImage = cvCloneImage(&imgTmp);

    //计算图片的指纹
    string imgPrint = ImageHashValue(iplImage);

    //释放内存
    env->ReleaseByteArrayElements(buf_, buf, 0);
    cvReleaseImage(&iplImage);
    img.release();
    return env->NewStringUTF(imgPrint.c_str());
}

//获得图片int[] 指纹信息
extern "C"
JNIEXPORT jstring JNICALL
Java_com_example_v_1fanlulin_childrenreaderrobotdemo_MainActivity_getImgFingerprintInt(JNIEnv *env,
                                                                                       jobject instance,
                                                                                       jintArray buf_,
                                                                                       jint w,
                                                                                       jint h,
                                                                                       jint angle) {
    jint *buf = env->GetIntArrayElements(buf_, NULL);

    Mat img(h,w,CV_8UC3,(unsigned char *)buf);

    //旋转图片
    if(angle!=0){
        Point2f center(img.cols / 2, img.rows / 2);
        Mat rot = getRotationMatrix2D(center, angle, 1);
        Rect bbox = RotatedRect(center, img.size(), angle).boundingRect();
        rot.at<double>(0, 2) += bbox.width / 2.0 - center.x;
        rot.at<double>(1, 2) += bbox.height / 2.0 - center.y;
        Mat dst;
        warpAffine(img, img, rot, bbox.size());
    }

    //转为IplImage
    IplImage imgTmp = img;
    IplImage *iplImage = cvCloneImage(&imgTmp);
    //计算图片的指纹
    string imgPrint = ImageHashValue(iplImage);

    //释放内存
    env->ReleaseIntArrayElements(buf_, buf, 0);
    cvReleaseImage(&iplImage);
    img.release();
    return env->NewStringUTF(imgPrint.c_str());
}