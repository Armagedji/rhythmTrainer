#include <jni.h>
#include "RhythmEngine.h"
#include <android/log.h>

#define LOG_TAG "RhythmJNI"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)

// Глобальные ссылки для callback'ов
static jobject g_javaObject = nullptr;
static jmethodID g_updateScoreMethod = nullptr;
static jmethodID g_updateResultMethod = nullptr;

extern "C" {

JNIEXPORT void JNICALL
Java_com_example_rhythmtrainer_MainActivity_nativeInit(JNIEnv* env, jobject thiz) {
    LOGD("nativeInit called");

    // Сохраняем глобальную ссылку на объект MainActivity
    if (g_javaObject != nullptr) {
        env->DeleteGlobalRef(g_javaObject);
    }
    g_javaObject = env->NewGlobalRef(thiz);

    // Получаем методы для callback'ов
    jclass clazz = env->GetObjectClass(g_javaObject);
    g_updateScoreMethod = env->GetMethodID(clazz, "updateScore", "(I)V");
    g_updateResultMethod = env->GetMethodID(clazz, "updateResult", "(Ljava/lang/String;)V");

    // Устанавливаем callback в движок
    RhythmEngine::getInstance()->setScoreUpdateCallback([](int score, const char* result) {
        JNIEnv* env = nullptr;
        JavaVM* vm = RhythmEngine::getInstance()->getJavaVM();

        if (vm == nullptr) {
            LOGD("JavaVM is null, cannot update UI");
            return;
        }

        // Прикрепляем поток к JVM
        int attached = vm->GetEnv((void**)&env, JNI_VERSION_1_6);
        if (attached == JNI_EDETACHED) {
            vm->AttachCurrentThread(&env, nullptr);
        }

        if (env != nullptr && g_javaObject != nullptr && g_updateScoreMethod != nullptr) {
            env->CallVoidMethod(g_javaObject, g_updateScoreMethod, score);

            jstring resultString = env->NewStringUTF(result);
            env->CallVoidMethod(g_javaObject, g_updateResultMethod, resultString);
            env->DeleteLocalRef(resultString);
        }

        if (attached == JNI_EDETACHED) {
            vm->DetachCurrentThread();
        }
    });
}

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void* reserved) {
    LOGD("JNI_OnLoad called");
    RhythmEngine::getInstance()->setJavaVM(vm);
    return JNI_VERSION_1_6;
}

JNIEXPORT void JNICALL
Java_com_example_rhythmtrainer_MainActivity_startRhythm(JNIEnv* env, jobject /* this */, jint bpm) {
    LOGD("startRhythm called with BPM=%d", bpm);
    RhythmEngine::getInstance()->start((int)bpm);
}

JNIEXPORT void JNICALL
Java_com_example_rhythmtrainer_MainActivity_stopRhythm(JNIEnv* env, jobject /* this */) {
    LOGD("stopRhythm called");
    RhythmEngine::getInstance()->stop();
}

JNIEXPORT void JNICALL
Java_com_example_rhythmtrainer_MainActivity_onTap(JNIEnv* env, jobject /* this */) {
    LOGD("onTap called");
    RhythmEngine::getInstance()->onTap();
}

JNIEXPORT jboolean JNICALL
Java_com_example_rhythmtrainer_MainActivity_isRhythmPlaying(JNIEnv* env, jobject /* this */) {
    return RhythmEngine::getInstance()->isPlaying();
}

} // extern "C"