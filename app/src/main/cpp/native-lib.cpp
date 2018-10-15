#include <jni.h>
#include <opencv2/opencv.hpp>
#include <iostream>

using namespace cv;
using namespace std;

void graphRotate(Mat &src);
void gaussian(Mat &src);
void grayScale(Mat &src);
void sobel(Mat &src);
void twoValue(Mat &src);
void close(Mat &src);
void contour(Mat &src,Mat &cpy,int &x,int &y);
void findCycle(Mat &src,Mat &cpy);
void colorfilter(Mat &src);//Not completed
void people(Mat &src){
    vector<Rect> found;
    vector<double> weights;
    HOGDescriptor hog;
    try{
        hog.setSVMDetector(HOGDescriptor::getDefaultPeopleDetector());
        //hog.detectMultiScale(src,found);
    }catch(...){
        grayScale(src);
        //sobel(src);
        //twoValue(src);//二值化
        //close(src);//閉操作
        graphRotate(src);
    }
    /*for (int i = 0; i < found.size(); i++){
        Rect r = found[i];
        rectangle(src, r, Scalar(0, 255, 0), 1);
    }*/
}
extern "C"

JNIEXPORT jintArray JNICALL Java_com_example_asus_demondk_MainActivity_imageProcessing(
        JNIEnv *env, jobject obj,jlong matAddress) {
    Mat &src = *(Mat*)matAddress;//轉成指標後馬上取指標指向的值
    Mat cpy;//--------------有問題
    cpy=src.clone();
    //src.copyTo(cpy);
    /*gaussian(src);
    grayScale(src);
    //sobel(src);
    twoValue(src);//二值化
    //close(src);//閉操作
    findCycle(src,cpy);*/

    //findCycle(src,cpy);
    //graphRotate(src);
    colorfilter(src);//------------------------------------------------------------

    //findCycle(src,cpy);
    jintArray jarr = env->NewIntArray(2);
    jint *arr = env->GetIntArrayElements(jarr, NULL);
    contour(src,cpy,arr[0],arr[1]);
    env->ReleaseIntArrayElements(jarr, arr, 0);
    return jarr;

}
//reference:https://www.google.com.tw/search?authuser=1&source=hp&ei=3T2EW_7IIIOg-QaL0ZLoDQ&q=%E5%9C%96%E7%89%87%E6%97%8B%E8%BD%89+%E8%8B%B1%E6%96%87&oq=%E5%9C%96%E7%89%87%E6%97%8B%E8%BD%89+%E8%8B%B1%E6%96%87&gs_l=psy-ab.3...1391.12628.0.12785.29.24.2.2.2.0.98.1217.23.24.0....0...1c.1j4.64.psy-ab..1.9.386.0..0j0i131k1.39.cgTkZ2jq9SQ
void graphRotate(Mat &src){//雖然水平了但圖片被拉長了
    Mat dst;
    transpose(src, dst);//圖片從垂直變水平
    resize(dst,src,Size(src.cols,src.rows));
    flip(src,dst,1);//1:以y軸翻轉，0:以x軸翻轉
    src=dst;
}
void gaussian(Mat &src){
    Mat dst;
    GaussianBlur(src,dst,Size(3,3),0, 0, BORDER_DEFAULT);
    src=dst;
}
void grayScale(Mat &src){
    Mat dst;
    cvtColor(src, dst, CV_RGB2GRAY);//灰階化
    src=dst;
}
void sobel(Mat &src){//沒有全部複製，有部分好像沒有使用到尚未研究
    Mat dst;
    Mat grad_x, grad_y;
    Mat abs_grad_x, abs_grad_y;
    Sobel(src, src, CV_8U, 1, 0, 3, 1, 1, BORDER_DEFAULT);//位元深度要注意符合android才行
    convertScaleAbs(src, dst);
    src=dst;
}
void twoValue(Mat &src){
    Mat dst;
    threshold(src, dst, 0, 255, CV_THRESH_OTSU + CV_THRESH_BINARY);
    //adaptiveThreshold(src, dst, 255, ADAPTIVE_THRESH_GAUSSIAN_C, THRESH_BINARY, 65, 0);
    src=dst;
}
void close(Mat &src){
    Mat dst;
    Mat element = getStructuringElement(MORPH_RECT, Size(17, 5));
    morphologyEx(src, dst, cv::MORPH_CLOSE, element);
    src=dst;
}
void contour(Mat &src,Mat &cpy,int &x,int &y){
    vector<vector<Point>> contours;
    vector<Vec4i> hierarchy;
    findContours(src, contours, hierarchy, CV_RETR_CCOMP, CV_CHAIN_APPROX_SIMPLE);
    double maxArea=0;
    double area=0;
    int index=0;
    Rect rect;
    for(int j = 0; j<contours.size(); j++){
        area=contourArea(contours[j],false);
        if(maxArea<area){
            maxArea=area;
            index=j;
            rect=boundingRect(contours[j]);
        }
    }
    rectangle(cpy, rect,  Scalar(0,255,0),2, 8,0);
    x=rect.x+(rect.width/2);
    y=rect.y+(rect.height/2);
    /*try{//******************************************************************
        vector<Moments> mu(1);
        if(contours.size()!=NULL){
            for( int i = 0; i <contours.size() ;i++) {
                mu[i] = moments( contours[index], false );//一定要放在for裡面，不然android會終止
                x=(mu[0].m10/mu[0].m00);
                y=(mu[0].m01/mu[0].m00);
            }
        }

    }catch(...){

    }*/
    drawContours(cpy, contours, index, Scalar(0,255,0), 2, 8, hierarchy);
    putText(cpy,"X",Point(x,y), 0, 1, Scalar(0,0,0),3);
    //putText(cpy, string("X"),Point(280,280), 0, 1, Scalar(0,0,0),3);
    src=cpy;

}
void colorfilter(Mat &src){
    Mat hsvMat;
    cvtColor(src,hsvMat,COLOR_BGR2HSV);
    //src = Mat(hsvMat.rows, hsvMat.cols,CV_8U);
    Scalar hsv_min = Scalar(24,100,100, 0);
    Scalar hsv_max = Scalar(44,255,255, 0);
    inRange(hsvMat,hsv_min,hsv_max,src);
}
void findCycle(Mat &src,Mat &cpy){
    vector<Vec3f> circles;
    //霍夫圆
    //HoughCircles(src, circles, CV_HOUGH_GRADIENT, 1.5, 50, 200, 100, 20, 200);
    int radius;
    HoughCircles(src, circles, CV_HOUGH_GRADIENT, 1.5, 10, 200, 100, 0, 0);
    for (size_t i = 0; i < circles.size(); i++) {
        Point center(cvRound(circles[i][0]), cvRound(circles[i][1]));
        radius = cvRound(circles[i][2]);
        //绘制圆心
        circle(cpy, center, 3, Scalar(0, 255, 0), -1, 8, 0);
        //绘制圆轮廓
        circle(cpy, center, radius, Scalar(155, 50, 255), 3, 8, 0);
    }
    putText(cpy, string("r:"+radius), Point(280,280), 0, 1, Scalar(0,0,0),3);

    src=cpy;

}