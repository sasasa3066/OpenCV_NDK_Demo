#include <jni.h>
#include <string>

extern "C"
JNIEXPORT jstring

JNICALL
Java_com_example_asus_demondk_MainActivity_getTestString(
        JNIEnv *env,
        jobject /* this */) {
    std::string hello = "This is test message";
    return env->NewStringUTF(hello.c_str());
}
//
// Created by asus on 2018/8/8.
//

