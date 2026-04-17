#ifndef RHYTHMENGINE_H
#define RHYTHMENGINE_H

#include <oboe/Oboe.h>
#include <atomic>
#include <thread>
#include <vector>
#include <functional>

class RhythmEngine : public oboe::AudioStreamDataCallback {
public:
    static RhythmEngine* getInstance();

    void start(int bpm = 120);
    void stop();
    bool isPlaying() const { return mIsPlaying; }
    void setBpm(int bpm);
    int getBpm() const { return mCurrentBpm; }

    // AudioStreamDataCallback interface
    oboe::DataCallbackResult onAudioReady(
            oboe::AudioStream* oboeStream,
            void* audioData,
            int32_t numFrames) override;

private:
    RhythmEngine();
    ~RhythmEngine();

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

    static constexpr int32_t SAMPLE_RATE = 48000;
    int32_t getBeatDurationFrames() const { return (60 * SAMPLE_RATE) / mCurrentBpm; }
};

#endif // RHYTHMENGINE_H