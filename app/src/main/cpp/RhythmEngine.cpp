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
    const int32_t clickLength = SAMPLE_RATE / 10;
    mClickBuffer.resize(clickLength);

    for (int i = 0; i < clickLength; ++i) {
        float envelope = 1.0f - (float)i / clickLength;
        float sample = 0.5f * sinf(2.0f * M_PI * 440.0f * i / SAMPLE_RATE) * envelope;
        mClickBuffer[i] = sample;
    }
    LOGD("Click buffer generated, size: %d", clickLength);
}

void RhythmEngine::setBpm(int bpm) {
    if (bpm < 40) bpm = 40;
    if (bpm > 200) bpm = 200;
    LOGD("BPM changing from %d to %d", mCurrentBpm.load(), bpm);
    mCurrentBpm = bpm;

    if (mIsPlaying) {
        mNeedsRestart = true;
    }
}

void RhythmEngine::restartStream() {
    if (!mIsPlaying) return;

    LOGD("Restarting stream with new BPM=%d", mCurrentBpm.load());

    // Останавливаем текущий поток
    if (mStream) {
        mStream->stop();
        closeStream();
    }

    // Пересоздаём поток с новыми параметрами
    mFrameCounter = 0;
    mClickPosition = 0;

    oboe::AudioStreamBuilder builder;
    builder.setDirection(oboe::Direction::Output);
    builder.setPerformanceMode(oboe::PerformanceMode::LowLatency);
    builder.setSharingMode(oboe::SharingMode::Exclusive);
    builder.setFormat(oboe::AudioFormat::Float);
    builder.setSampleRate(SAMPLE_RATE);
    builder.setChannelCount(oboe::ChannelCount::Mono);
    builder.setDataCallback(this);

    oboe::Result result = builder.openStream(mStream);
    if (result != oboe::Result::OK) {
        LOGE("Failed to reopen stream: %s", oboe::convertToText(result));
        return;
    }

    result = mStream->start();
    if (result != oboe::Result::OK) {
        LOGE("Failed to restart stream: %s", oboe::convertToText(result));
        return;
    }

    LOGD("Stream restarted successfully with BPM=%d", mCurrentBpm.load());
    mNeedsRestart = false;
}

void RhythmEngine::start(int bpm) {
    LOGD("start() called, BPM=%d, current playing state: %d", bpm, mIsPlaying.load());

    setBpm(bpm);

    if (mIsPlaying) {
        LOGD("Already playing, restarting with new BPM");
        restartStream();
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

    oboe::Result result = builder.openStream(mStream);
    if (result != oboe::Result::OK) {
        LOGE("Failed to open stream: %s", oboe::convertToText(result));
        return;
    }
    LOGD("Stream opened successfully");

    result = mStream->start();
    if (result != oboe::Result::OK) {
        LOGE("Failed to start stream: %s", oboe::convertToText(result));
        return;
    }

    mIsPlaying = true;
    mFrameCounter = 0;
    mClickPosition = 0;
    LOGD("Rhythm started successfully, BPM=%d", mCurrentBpm.load());
}

void RhythmEngine::stop() {
    if (!mIsPlaying) return;

    LOGD("stop() called");
    mIsPlaying = false;
    mNeedsRestart = false;

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

    if (mNeedsRestart) {
        // Отметим, что нужен рестарт, но не делаем его здесь (нельзя блокировать)
        // В следующем цикле main поток должен вызвать restartStream()
        return oboe::DataCallbackResult::Continue;
    }

    float* output = static_cast<float*>(audioData);
    int32_t beatDurationFrames = getBeatDurationFrames();

    for (int i = 0; i < numFrames; ++i) {
        bool isBeatStart = (mFrameCounter % beatDurationFrames == 0);

        if (isBeatStart && mIsPlaying) {
            mClickPosition = 0;
        }

        float sample = 0.0f;
        if (mClickPosition < (int)mClickBuffer.size() && mIsPlaying) {
            sample = mClickBuffer[mClickPosition];
            mClickPosition++;
        }

        output[i] = sample;
        mFrameCounter++;

        if (mFrameCounter >= beatDurationFrames * 8) {
            mFrameCounter = 0;
        }
    }

    return oboe::DataCallbackResult::Continue;
}