#ifndef RHYTHMENGINE_H
#define RHYTHMENGINE_H

#include <oboe/Oboe.h>
#include <atomic>
#include <vector>
#include <functional>
#include <memory>
#include <jni.h>
#include <mutex>

class RhythmEngine : public oboe::AudioStreamDataCallback {
public:
    static RhythmEngine* getInstance();

    void start(int bpm = 120);
    void stop();
    bool isPlaying() const { return mIsPlaying; }
    void setBpm(int bpm);
    void pause();
    void resume();
    bool isPaused() const { return mPaused; }

    // Игровые методы
    void onTap();
    void resetGame();
    void setScoreUpdateCallback(std::function<void(int, const char*)> callback);
    void setNotePositionCallback(std::function<void(int index, float progress)> callback);

    void setAllNotesProgressCallback(std::function<void(const std::vector<float>&)> callback);
    std::vector<float> mAllProgresses;
    std::function<void(const std::vector<float>&)> mAllNotesProgressCallback;

    // Калибровка
    void startCalibration(int bpm = 120);
    void stopCalibration();
    int getCalibrationOffset() const { return mCalibrationOffset; }
    void setCalibrationOffset(int offsetMs);
    void setCalibrationCallback(std::function<void(int, int)> callback);

    // JVM для callback'ов
    void setJavaVM(JavaVM* vm) { mJavaVM = vm; }
    JavaVM* getJavaVM() const { return mJavaVM; }

    // AudioStreamDataCallback interface
    oboe::DataCallbackResult onAudioReady(
            oboe::AudioStream* oboeStream,
            void* audioData,
            int32_t numFrames) override;

    int getTotalNotes() const { return mTotalNotes; }
    void loadSong(int bpm, int totalNotes);



private:
    std::mutex mMutex;
    RhythmEngine();
    ~RhythmEngine();

    void generateClickBuffer();
    void closeStream();
    void restartStream();
    void addCalibrationTap();
    void updateNotePosition(long long elapsedMs);

    std::shared_ptr<oboe::AudioStream> mStream;
    std::vector<float> mClickBuffer;
    int32_t mClickPosition = 0;
    int32_t mFrameCounter = 0;
    std::atomic<bool> mIsPlaying{false};
    std::atomic<int> mCurrentBpm{120};
    std::atomic<bool> mNeedsRestart{false};

    // Игровые переменные
    std::atomic<int> mCurrentScore{0};
    std::atomic<long long> mGameStartTime{0};
    std::function<void(int, const char*)> mScoreCallback;
    std::function<void(int, float)> mNotePositionCallback;


    // Таймлайн
    std::vector<long long> mTimeline;  // идеальные моменты нажатий в мс от начала
    int mCurrentNoteIndex = 0;
    int mTotalNotes = 32;

    // Калибровка
    std::vector<int> mCalibrationDeviations;
    int mCalibrationAverageDeviation = 0;
    int mCalibrationOffset = 0;
    long long mCalibrationStartTime = 0;
    int mCalibrationTapCount = 0;
    std::function<void(int, int)> mCalibrationCallback;

    // JVM
    JavaVM* mJavaVM = nullptr;

    static constexpr int32_t SAMPLE_RATE = 48000;
    int32_t getBeatDurationFrames() const { return (60 * SAMPLE_RATE) / mCurrentBpm; }
    long long getCurrentTimeMs() const;
    void processTap(long long tapTimeMs);
    const char* getResultText(int deviationMs);
    int calculateScore(int deviationMs);

    std::atomic<bool> mPaused{false};
    long long mPausedDurationMs = 0;      // сколько времени были на паузе (суммарно)
    long long mPauseStartTime = 0;        // когда началась текущая пауза

};

#endif // RHYTHMENGINE_H