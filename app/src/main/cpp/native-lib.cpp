#include <jni.h>
#include <string>
#include <opencv2/opencv.hpp>
using namespace cv;
extern "C"


JNIEXPORT void JNICALL Java_com_example_asus_demondk_MainActivity_imageProcessing(
        JNIEnv *env, jobject obj/* this */,jlong matAddress) {
    Mat &img = *(Mat*)matAddress;//轉成指標後馬上取指標指向的值
    Mat dst;
    cvtColor(img,dst,COLOR_RGB2GRAY);
    img=dst;
}
