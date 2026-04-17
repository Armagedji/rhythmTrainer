#ifndef RHYTHMENGINE_H
#define RHYTHMENGINE_H

#include <oboe/Oboe.h>
#include <atomic>
#include <vector>
#include <functional>
#include <jni.h>

class RhythmEngine : public oboe::AudioStreamDataCallback {
public:
    static RhythmEngine* getInstance();

    void start(int bpm = 120);
    void stop();
    bool isPlaying() const { return mIsPlaying; }
    void setBpm(int bpm);
    int getBpm() const { return mCurrentBpm; }
    void setJavaVM(JavaVM* vm) { mJavaVM = vm; }
    JavaVM* getJavaVM() const { return mJavaVM; }

    // Игровые методы
    void onTap();
    int getCurrentScore() const { return mCurrentScore; }
    void resetGame();
    void setScoreUpdateCallback(std::function<void(int, const char*)> callback);

    // AudioStreamDataCallback interface
    oboe::DataCallbackResult onAudioReady(
            oboe::AudioStream* oboeStream,
            void* audioData,
            int32_t numFrames) override;

private:
    RhythmEngine();
    ~RhythmEngine();
    JavaVM* mJavaVM = nullptr;


    void generateClickBuffer();
    void closeStream();
    void restartStream();


    std::shared_ptr<oboe::AudioStream> mStream;
    std::vector<float> mClickBuffer;
    int32_t mClickPosition = 0;
    int32_t mFrameCounter = 0;
    std::atomic<bool> mIsPlaying{false};
    std::atomic<int> mCurrentBpm{120};
    std::atomic<bool> mNeedsRestart{false};

    // Игровые переменные
    std::atomic<int> mCurrentScore{0};
    std::atomic<long long> mLastTapTime{0};
    std::atomic<long long> mGameStartTime{0};
    std::function<void(int, const char*)> mScoreCallback;

    static constexpr int32_t SAMPLE_RATE = 48000;
    int32_t getBeatDurationFrames() const { return (60 * SAMPLE_RATE) / mCurrentBpm; }
    long long getCurrentTimeMs() const;
    void processTap(long long tapTimeMs);
    const char* getResultText(int deviationMs);
    int calculateScore(int deviationMs);
};

#endif // RHYTHMENGINE_H