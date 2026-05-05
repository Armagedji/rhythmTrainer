#include "RhythmEngine.h"
#include <android/log.h>
#include <cmath>
#include <chrono>

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

    if (mStream) {
        mStream->stop();
        closeStream();
    }

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
    LOGD("start() called, BPM=%d", bpm);

    setBpm(bpm);
    resetGame();

    if (mIsPlaying) {
        restartStream();
        return;
    }

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
    mGameStartTime = getCurrentTimeMs();
    LOGD("Rhythm started successfully, BPM=%d, gameStartTime=%lld", mCurrentBpm.load(), mGameStartTime.load());
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

void RhythmEngine::resetGame() {
    mCurrentScore = 0;
    LOGD("Game reset, score=%d", mCurrentScore.load());
}

void RhythmEngine::setScoreUpdateCallback(std::function<void(int, const char*)> callback) {
    mScoreCallback = callback;
}

long long RhythmEngine::getCurrentTimeMs() const {
    return std::chrono::duration_cast<std::chrono::milliseconds>(
            std::chrono::steady_clock::now().time_since_epoch()
    ).count();
}

const char* RhythmEngine::getResultText(int deviationMs) {
    int absDev = std::abs(deviationMs);
    if (absDev <= 60) return "Perfect!";
    if (absDev <= 120) return "Good!";
    return "Miss...";
}

int RhythmEngine::calculateScore(int deviationMs) {
    int absDev = std::abs(deviationMs);
    if (absDev <= 60) return 10;
    if (absDev <= 120) return 5;
    return 0;
}

void RhythmEngine::processTap(long long tapTimeMs) {
    if (!mIsPlaying) {
        LOGD("Tap ignored - game not playing");
        return;
    }

    // Если это первое нажатие после старта
    if (mGameStartTime == 0) {
        mGameStartTime = tapTimeMs;
        LOGD("First tap, setting gameStartTime to %lld", tapTimeMs);
        return; // Не оцениваем первое нажатие
    }

    long long elapsed = tapTimeMs - mGameStartTime;
    long long beatDurationMs = (60 * 1000) / mCurrentBpm;

    long long beatNumber = (elapsed + beatDurationMs / 2) / beatDurationMs;
    long long idealTimeMs = beatNumber * beatDurationMs;

    // Учитываем калибровочное смещение
    int deviationMs = (int)(elapsed - idealTimeMs) + mCalibrationOffset;
    LOGD("Tap: offset=%d, final_dev=%d", mCalibrationOffset, deviationMs);
    int score = calculateScore(deviationMs);
    const char* resultText = getResultText(deviationMs);

    if (score > 0) {
        mCurrentScore += score;
    }

    LOGD("Tap: elapsed=%lld, ideal=%lld, raw_dev=%lld, offset=%d, final_dev=%d, result=%s, score=%d, total=%d",
         elapsed, idealTimeMs, (elapsed - idealTimeMs), mCalibrationOffset, deviationMs, resultText, score, mCurrentScore.load());

    if (mScoreCallback) {
        mScoreCallback(mCurrentScore.load(), resultText);
    }
}

void RhythmEngine::onTap() {
    long long tapTime = getCurrentTimeMs();
    LOGD("onTap() called at %lld", tapTime);

    if (mCalibrationStartTime != 0 && mIsPlaying) {
        addCalibrationTap();
    } else {
        processTap(tapTime);
    }
}

oboe::DataCallbackResult RhythmEngine::onAudioReady(
        oboe::AudioStream* oboeStream,
        void* audioData,
        int32_t numFrames) {

    if (mNeedsRestart) {
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

void RhythmEngine::setCalibrationOffset(int offsetMs) {
    mCalibrationOffset = offsetMs;
    LOGD("Calibration offset set to %d ms", offsetMs);
}

void RhythmEngine::startCalibration(int bpm) {
    LOGD("startCalibration() called, BPM=%d", bpm);
    mCalibrationDeviations.clear();
    mCalibrationTapCount = 0;
    mCalibrationAverageDeviation = 0;
    setBpm(bpm);

    if (mIsPlaying) {
        restartStream();
    } else {
        start(bpm);
    }

    mCalibrationStartTime = getCurrentTimeMs();
    LOGD("Calibration started at %lld", mCalibrationStartTime);
}

void RhythmEngine::stopCalibration() {
    LOGD("stopCalibration() called");
    stop();

    if (!mCalibrationDeviations.empty()) {
        int sum = 0;
        for (int dev : mCalibrationDeviations) {
            sum += dev;
        }
        mCalibrationAverageDeviation = sum / mCalibrationDeviations.size();
        LOGD("Calibration complete: %d taps, average deviation = %d ms",
             (int)mCalibrationDeviations.size(), mCalibrationAverageDeviation);
    } else {
        mCalibrationAverageDeviation = 0;
        LOGD("Calibration complete: no taps recorded");
    }

    if (mCalibrationCallback) {
        mCalibrationCallback(mCalibrationTapCount, mCalibrationAverageDeviation);
    }

    mCalibrationStartTime = 0;
}

void RhythmEngine::addCalibrationTap() {
    if (!mIsPlaying || mCalibrationStartTime == 0) {
        LOGD("Calibration tap ignored - not in calibration mode");
        return;
    }

    long long currentTime = getCurrentTimeMs();
    long long elapsed = currentTime - mCalibrationStartTime;
    long long beatDurationMs = (60 * 1000) / mCurrentBpm;

    long long beatNumber = (elapsed + beatDurationMs / 2) / beatDurationMs;
    long long idealTimeMs = beatNumber * beatDurationMs;
    int deviationMs = (int)(elapsed - idealTimeMs);

    mCalibrationDeviations.push_back(deviationMs);
    mCalibrationTapCount++;

    LOGD("Calibration tap: elapsed=%lld, ideal=%lld, dev=%d, total taps=%d",
         elapsed, idealTimeMs, deviationMs, mCalibrationTapCount);

    if (mCalibrationCallback) {
        mCalibrationCallback(mCalibrationTapCount, 0);
    }
}