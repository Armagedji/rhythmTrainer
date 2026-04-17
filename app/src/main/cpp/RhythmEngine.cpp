#include "RhythmEngine.h"
#include <android/log.h>
#include <cmath>

#define LOG_TAG "RhythmEngine"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

static RhythmEngine* instance = nullptr;

RhythmEngine* RhythmEngine::getInstance() {
    if (instance == nullptr) {
        instance = new RhythmEngine();
    }
    return instance;
}

RhythmEngine::RhythmEngine() {
    LOGD("RhythmEngine created");
    generateClickBuffer();
}

RhythmEngine::~RhythmEngine() {
    stop();
    closeStream();
}

void RhythmEngine::generateClickBuffer() {
    // Создаём короткий звук "клик" (синусоида + затухание)
    const int32_t clickLength = SAMPLE_RATE / 10; // 100 мс
    mClickBuffer.resize(clickLength);

    for (int i = 0; i < clickLength; ++i) {
        float envelope = 1.0f - (float)i / clickLength;
        float sample = 0.5f * sinf(2.0f * M_PI * 440.0f * i / SAMPLE_RATE) * envelope;
        mClickBuffer[i] = sample;
    }
    LOGD("Click buffer generated, size: %d", clickLength);
}

void RhythmEngine::start() {
    LOGD("start() called, current playing state: %d", mIsPlaying.load());

    if (mIsPlaying) {
        LOGD("Already playing");
        return;
    }

    LOGD("Creating audio stream...");
    oboe::AudioStreamBuilder builder;
    builder.setDirection(oboe::Direction::Output);
    builder.setPerformanceMode(oboe::PerformanceMode::LowLatency);
    builder.setSharingMode(oboe::SharingMode::Exclusive);
    builder.setFormat(oboe::AudioFormat::Float);
    builder.setSampleRate(SAMPLE_RATE);
    builder.setChannelCount(oboe::ChannelCount::Mono);
    builder.setDataCallback(this);

    LOGD("Opening stream...");
    oboe::Result result = builder.openStream(mStream);
    if (result != oboe::Result::OK) {
        LOGE("Failed to open stream: %s", oboe::convertToText(result));
        return;
    }
    LOGD("Stream opened successfully");

    LOGD("Starting stream...");
    result = mStream->start();
    if (result != oboe::Result::OK) {
        LOGE("Failed to start stream: %s", oboe::convertToText(result));
        return;
    }

    mIsPlaying = true;
    mFrameCounter = 0;
    mClickPosition = 0;
    LOGD("Rhythm started successfully, BPM=%d", BPM);
}

void RhythmEngine::stop() {
    if (!mIsPlaying) return;

    mIsPlaying = false;
    if (mStream) {
        mStream->stop();
        closeStream();
    }
    LOGD("Rhythm stopped");
}

void RhythmEngine::closeStream() {
    if (mStream) {
        mStream->close();
        mStream.reset();
    }
}

oboe::DataCallbackResult RhythmEngine::onAudioReady(
        oboe::AudioStream* oboeStream,
        void* audioData,
        int32_t numFrames) {

    float* output = static_cast<float*>(audioData);

    for (int i = 0; i < numFrames; ++i) {
        // Определяем, нужно ли сыграть клик на этой фрейме
        bool isBeatStart = (mFrameCounter % BEAT_DURATION_FRAMES == 0);

        if (isBeatStart && mIsPlaying) {
            mClickPosition = 0;
        }

        // Воспроизводим клик, если мы внутри его длительности
        float sample = 0.0f;
        if (mClickPosition < (int)mClickBuffer.size()) {
            sample = mClickBuffer[mClickPosition];
            mClickPosition++;
        }

        output[i] = sample;
        mFrameCounter++;

        // Предотвращаем переполнение счётчика
        if (mFrameCounter >= BEAT_DURATION_FRAMES * 8) {
            mFrameCounter = 0;
        }
    }

    return oboe::DataCallbackResult::Continue;
}