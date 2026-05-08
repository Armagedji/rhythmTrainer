#include <jni.h>
#include "RhythmEngine.h"
#include <android/log.h>

#define LOG_TAG "RhythmJNI"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)

static jobject g_javaObject = nullptr;
static jmethodID g_updateScoreMethod = nullptr;
static jmethodID g_updateResultMethod = nullptr;
static jmethodID g_updateCalibrationMethod = nullptr;
static jmethodID g_updateAllNotesProgressMethod = nullptr;
static jmethodID g_levelCompleteMethod = nullptr;

extern "C" {

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void* reserved) {
    LOGD("JNI_OnLoad called");
    RhythmEngine::getInstance()->setJavaVM(vm);
    return JNI_VERSION_1_6;
}

JNIEXPORT void JNICALL
Java_com_example_rhythmtrainer_MainActivity_nativeInit(JNIEnv* env, jobject thiz) {
    g_javaObject = env->NewGlobalRef(thiz);
    LOGD("nativeInit called");

    if (g_javaObject != nullptr) {
        env->DeleteGlobalRef(g_javaObject);
    }
    g_javaObject = env->NewGlobalRef(thiz);


    jclass clazz = env->GetObjectClass(g_javaObject);
    g_updateScoreMethod = env->GetMethodID(clazz, "updateScore", "(I)V");
    g_updateResultMethod = env->GetMethodID(clazz, "updateResult", "(Ljava/lang/String;)V");
    g_updateCalibrationMethod = env->GetMethodID(clazz, "updateCalibration", "(II)V");
    g_updateAllNotesProgressMethod = env->GetMethodID(clazz, "updateAllNotesProgress", "([F)V");

    RhythmEngine::getInstance()->setScoreUpdateCallback([](int score, const char* result) {
        JNIEnv* env = nullptr;
        JavaVM* vm = RhythmEngine::getInstance()->getJavaVM();

        if (vm == nullptr) {
            LOGD("JavaVM is null, cannot update UI");
            return;
        }

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

    RhythmEngine::getInstance()->setCalibrationCallback([](int tapCount, int avgDeviation, bool finished) {
        LOGD("Calibration callback: tapCount=%d, avgDeviation=%d, finished=%d", tapCount, avgDeviation, finished);
        JNIEnv* env = nullptr;
        JavaVM* vm = RhythmEngine::getInstance()->getJavaVM();

        if (vm == nullptr) {
            LOGD("JavaVM is null");
            return;
        }

        int attached = vm->GetEnv((void**)&env, JNI_VERSION_1_6);
        if (attached == JNI_EDETACHED) {
            vm->AttachCurrentThread(&env, nullptr);
        }

        if (env != nullptr && g_javaObject != nullptr) {
            if (finished) {
                // Вызываем onCalibrationComplete
                jclass clazz = env->GetObjectClass(g_javaObject);
                jmethodID method = env->GetMethodID(clazz, "onCalibrationComplete", "(II)V");
                if (method != nullptr) {
                    env->CallVoidMethod(g_javaObject, method, tapCount, avgDeviation);
                }
            } else {
                // Вызываем updateCalibration
                if (g_updateCalibrationMethod != nullptr) {
                    env->CallVoidMethod(g_javaObject, g_updateCalibrationMethod, tapCount, avgDeviation);
                }
            }
        }

        if (attached == JNI_EDETACHED) {
            vm->DetachCurrentThread();
        }
    });

    g_levelCompleteMethod = env->GetMethodID(
            clazz, "onLevelComplete", "()V"
    );

    RhythmEngine::getInstance()->setLevelCompleteCallback([]() {
        JNIEnv* env = nullptr;
        JavaVM* vm = RhythmEngine::getInstance()->getJavaVM();
        if (vm == nullptr) return;

        int attached = vm->GetEnv((void**)&env, JNI_VERSION_1_6);
        bool needDetach = (attached == JNI_EDETACHED);
        if (needDetach) vm->AttachCurrentThread(&env, nullptr);

        if (env != nullptr && g_javaObject != nullptr && g_levelCompleteMethod != nullptr) {
            env->CallVoidMethod(g_javaObject, g_levelCompleteMethod);
        }

        if (needDetach) vm->DetachCurrentThread();
    });


    LOGD("nativeInit completed successfully");
}

JNIEXPORT void JNICALL
Java_com_example_rhythmtrainer_MainActivity_startRhythm(JNIEnv* env, jobject, jint bpm) {
    LOGD("startRhythm called with BPM=%d", bpm);
    RhythmEngine::getInstance()->start((int)bpm);
}

JNIEXPORT void JNICALL
Java_com_example_rhythmtrainer_MainActivity_stopRhythm(JNIEnv* env, jobject) {
    LOGD("stopRhythm called");
    RhythmEngine::getInstance()->stop();
}

JNIEXPORT void JNICALL
Java_com_example_rhythmtrainer_MainActivity_onTap(JNIEnv* env, jobject) {
    LOGD("onTap called");
    RhythmEngine::getInstance()->onTap();
}

JNIEXPORT jboolean JNICALL
Java_com_example_rhythmtrainer_MainActivity_isRhythmPlaying(JNIEnv* env, jobject) {
    jboolean isPlaying = RhythmEngine::getInstance()->isPlaying();
    LOGD("isRhythmPlaying: %d", isPlaying);
    return isPlaying;
}

JNIEXPORT void JNICALL
Java_com_example_rhythmtrainer_MainActivity_startCalibration(JNIEnv* env, jobject, jint bpm) {
    LOGD("startCalibration called with BPM=%d", bpm);
    RhythmEngine::getInstance()->startCalibration((int)bpm);
}

JNIEXPORT void JNICALL
Java_com_example_rhythmtrainer_MainActivity_stopCalibration(JNIEnv* env, jobject) {
    LOGD("stopCalibration called");
    RhythmEngine::getInstance()->stopCalibration();
}

JNIEXPORT jint JNICALL
Java_com_example_rhythmtrainer_MainActivity_getCalibrationOffset(JNIEnv* env, jobject) {
    jint offset = RhythmEngine::getInstance()->getCalibrationOffset();
    LOGD("getCalibrationOffset: %d", offset);
    return offset;
}

JNIEXPORT void JNICALL
Java_com_example_rhythmtrainer_MainActivity_setCalibrationOffset(JNIEnv* env, jobject, jint offsetMs) {
    LOGD("setCalibrationOffset called with %d ms", offsetMs);
    RhythmEngine::getInstance()->setCalibrationOffset((int)offsetMs);
}

JNIEXPORT void JNICALL
Java_com_example_rhythmtrainer_MainActivity_loadSong(JNIEnv* env, jobject, jint bpm, jint totalNotes) {
    LOGD("loadSong called with BPM=%d, notes=%d", bpm, totalNotes);
    RhythmEngine::getInstance()->loadSong((int)bpm, (int)totalNotes);
}

JNIEXPORT jint JNICALL
Java_com_example_rhythmtrainer_MainActivity_getTotalNotes(JNIEnv* env, jobject) {
    return RhythmEngine::getInstance()->getTotalNotes();
}

JNIEXPORT void JNICALL
Java_com_example_rhythmtrainer_MainActivity_setNotePositionCallback(JNIEnv* env, jobject) {
    LOGD("setNotePositionCallback called");
    RhythmEngine::getInstance()->setNotePositionCallback([](int noteIndex, float progress) {
        JNIEnv* env = nullptr;
        JavaVM* vm = RhythmEngine::getInstance()->getJavaVM();
        if (vm == nullptr) return;

        int attached = vm->GetEnv((void**)&env, JNI_VERSION_1_6);
        bool needDetach = (attached == JNI_EDETACHED);
        if (needDetach) vm->AttachCurrentThread(&env, nullptr);

        jclass clazz = env->GetObjectClass(g_javaObject);
        jmethodID method = env->GetMethodID(clazz, "updateNotePosition", "(IF)V");
        if (method != nullptr) {
            env->CallVoidMethod(g_javaObject, method, noteIndex, progress);
        }

        if (needDetach) vm->DetachCurrentThread();
    });
}

JNIEXPORT void JNICALL
Java_com_example_rhythmtrainer_MainActivity_setAllNotesProgressCallback(JNIEnv* env, jobject) {
    LOGD("setAllNotesProgressCallback called");
    RhythmEngine::getInstance()->setAllNotesProgressCallback([](const std::vector<float>& progresses) {
        JNIEnv* env = nullptr;
        JavaVM* vm = RhythmEngine::getInstance()->getJavaVM();
        if (vm == nullptr) return;
        int attached = vm->GetEnv((void**)&env, JNI_VERSION_1_6);
        bool needDetach = (attached == JNI_EDETACHED);
        if (needDetach) vm->AttachCurrentThread(&env, nullptr);

        jfloatArray array = env->NewFloatArray(progresses.size());
        env->SetFloatArrayRegion(array, 0, progresses.size(), progresses.data());

        jclass clazz = env->GetObjectClass(g_javaObject);
        jmethodID method = g_updateAllNotesProgressMethod;
        if (method != nullptr) {
            env->CallVoidMethod(g_javaObject, method, array);
        } else {
            LOGD("updateAllNotesProgress method not found");
        }
        env->DeleteLocalRef(array);
        if (needDetach) vm->DetachCurrentThread();
    });
}

JNIEXPORT void JNICALL
Java_com_example_rhythmtrainer_MainActivity_pauseRhythm(JNIEnv* env, jobject) {
    LOGD("pauseRhythm called");
    RhythmEngine::getInstance()->pause();
}

JNIEXPORT void JNICALL
Java_com_example_rhythmtrainer_MainActivity_resumeRhythm(JNIEnv* env, jobject) {
    LOGD("resumeRhythm called");
    RhythmEngine::getInstance()->resume();
}

JNIEXPORT jboolean JNICALL
Java_com_example_rhythmtrainer_MainActivity_isRhythmPaused(JNIEnv* env, jobject) {
    return RhythmEngine::getInstance()->isPaused();
}

} // extern "C"