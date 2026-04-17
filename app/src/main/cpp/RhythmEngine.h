#ifndef RHYTHMENGINE_H
#define RHYTHMENGINE_H

#include <oboe/Oboe.h>
#include <atomic>
#include <thread>
#include <vector>

class RhythmEngine : public oboe::AudioStreamDataCallback {
public:
    static RhythmEngine* getInstance();

    void start();
    void stop();
    bool isPlaying() const { return mIsPlaying; }

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

    std::shared_ptr<oboe::AudioStream> mStream;
    std::vector<float> mClickBuffer;
    int32_t mClickPosition = 0;
    int32_t mFrameCounter = 0;
    int32_t mBeatsPerBar = 4;
    std::atomic<bool> mIsPlaying{false};

    static constexpr int32_t SAMPLE_RATE = 48000;
    static constexpr int32_t BPM = 120;
    static constexpr int32_t BEAT_DURATION_FRAMES = (60 * SAMPLE_RATE) / BPM;
};

#endif // RHYTHMENGINE_H