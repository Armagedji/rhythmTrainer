#include <jni.h>
#include "RhythmEngine.h"
#include <android/log.h>

#define LOG_TAG "RhythmJNI"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

extern "C" {

JNIEXPORT void JNICALL
Java_com_example_rhythmtrainer_MainActivity_startRhythm(JNIEnv* env, jobject /* this */) {
    LOGD("startRhythm called from Kotlin - JNI function entered");
    RhythmEngine::getInstance()->start();
    LOGD("startRhythm completed");
}

JNIEXPORT void JNICALL
Java_com_example_rhythmtrainer_MainActivity_stopRhythm(JNIEnv* env, jobject /* this */) {
    LOGD("stopRhythm called from Kotlin");
    RhythmEngine::getInstance()->stop();
}

JNIEXPORT jboolean JNICALL
Java_com_example_rhythmtrainer_MainActivity_isRhythmPlaying(JNIEnv* env, jobject /* this */) {
    jboolean isPlaying = RhythmEngine::getInstance()->isPlaying();
    LOGD("isRhythmPlaying: %d", isPlaying);
    return isPlaying;
}

} // extern "C"