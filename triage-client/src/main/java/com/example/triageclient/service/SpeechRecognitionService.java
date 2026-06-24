package com.example.triageclient.service;

import com.example.triageclient.config.ClientConfig;
import com.k2fsa.sherpa.onnx.FeatureConfig;
import com.k2fsa.sherpa.onnx.OnlineModelConfig;
import com.k2fsa.sherpa.onnx.OnlineRecognizer;
import com.k2fsa.sherpa.onnx.OnlineRecognizerConfig;
import com.k2fsa.sherpa.onnx.OnlineStream;
import com.k2fsa.sherpa.onnx.OnlineTransducerModelConfig;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.function.Consumer;

public class SpeechRecognitionService {
    private static final int SAMPLE_RATE = 16000;
    private static final int FEATURE_DIM = 80;
    private static final int AUDIO_BUFFER_BYTES = 3200;

    private final Path modelDir;

    private volatile boolean listening;
    private volatile TargetDataLine activeLine;
    private Thread recognitionThread;

    public SpeechRecognitionService() {
        this(ClientConfig.load().speechModelDir());
    }

    SpeechRecognitionService(String configuredModelDir) {
        this.modelDir = resolveModelDir(configuredModelDir);
    }

    public synchronized void start(String initialText,
                                   Consumer<String> textConsumer,
                                   Consumer<String> statusConsumer,
                                   Consumer<Throwable> errorConsumer) {
        Objects.requireNonNull(textConsumer, "textConsumer");
        Objects.requireNonNull(statusConsumer, "statusConsumer");
        Objects.requireNonNull(errorConsumer, "errorConsumer");
        if (listening) {
            return;
        }

        listening = true;
        String baseText = initialText == null ? "" : initialText.trim();
        recognitionThread = new Thread(
                () -> recognize(baseText, textConsumer, statusConsumer, errorConsumer),
                "speech-recognition");
        recognitionThread.setDaemon(true);
        recognitionThread.start();
    }

    public synchronized void stop() {
        listening = false;
        TargetDataLine line = activeLine;
        if (line != null) {
            line.stop();
            line.close();
        }
    }

    public boolean isListening() {
        return listening;
    }

    private void recognize(String baseText,
                           Consumer<String> textConsumer,
                           Consumer<String> statusConsumer,
                           Consumer<Throwable> errorConsumer) {
        OnlineRecognizer recognizer = null;
        OnlineStream stream = null;
        TargetDataLine line = null;
        try {
            validateModelFiles();
            statusConsumer.accept("正在加载本地语音识别模型，请稍候……");
            recognizer = createRecognizer();
            stream = recognizer.createStream();

            line = openMicrophone();
            activeLine = line;
            line.start();
            statusConsumer.accept("正在聆听，请说出您的症状描述。");

            byte[] audioBuffer = new byte[AUDIO_BUFFER_BYTES];
            String lastText = "";
            while (listening) {
                int bytesRead = line.read(audioBuffer, 0, audioBuffer.length);
                if (bytesRead <= 0) {
                    continue;
                }
                stream.acceptWaveform(toSamples(audioBuffer, bytesRead), SAMPLE_RATE);
                while (recognizer.isReady(stream)) {
                    recognizer.decode(stream);
                }
                String recognizedText = recognizer.getResult(stream).getText().trim();
                if (!recognizedText.equals(lastText)) {
                    lastText = recognizedText;
                    textConsumer.accept(mergeText(baseText, recognizedText));
                }
            }

            stream.inputFinished();
            while (recognizer.isReady(stream)) {
                recognizer.decode(stream);
            }
            String finalText = recognizer.getResult(stream).getText().trim();
            textConsumer.accept(mergeText(baseText, finalText));
            statusConsumer.accept(finalText.isBlank() ? "未识别到语音内容，请靠近麦克风后重试。"
                    : "语音识别已停止，请检查文字是否准确。");
        } catch (Throwable throwable) {
            errorConsumer.accept(throwable);
        } finally {
            listening = false;
            activeLine = null;
            if (line != null) {
                line.stop();
                line.close();
            }
            if (stream != null) {
                stream.release();
            }
            if (recognizer != null) {
                recognizer.release();
            }
        }
    }

    private OnlineRecognizer createRecognizer() {
        OnlineTransducerModelConfig transducer = OnlineTransducerModelConfig.builder()
                .setEncoder(modelDir.resolve("encoder.int8.onnx").toString())
                .setDecoder(modelDir.resolve("decoder.onnx").toString())
                .setJoiner(modelDir.resolve("joiner.int8.onnx").toString())
                .build();
        OnlineModelConfig modelConfig = OnlineModelConfig.builder()
                .setTransducer(transducer)
                .setTokens(modelDir.resolve("tokens.txt").toString())
                .setNumThreads(Math.max(2, Runtime.getRuntime().availableProcessors() / 2))
                .setProvider("cpu")
                .setDebug(false)
                .build();
        OnlineRecognizerConfig recognizerConfig = OnlineRecognizerConfig.builder()
                .setFeatureConfig(FeatureConfig.builder()
                        .setSampleRate(SAMPLE_RATE)
                        .setFeatureDim(FEATURE_DIM)
                        .build())
                .setOnlineModelConfig(modelConfig)
                .setDecodingMethod("greedy_search")
                .build();
        return new OnlineRecognizer(recognizerConfig);
    }

    private TargetDataLine openMicrophone() throws LineUnavailableException {
        AudioFormat format = new AudioFormat(
                AudioFormat.Encoding.PCM_SIGNED,
                SAMPLE_RATE,
                16,
                1,
                2,
                SAMPLE_RATE,
                false);
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
        if (!AudioSystem.isLineSupported(info)) {
            throw new LineUnavailableException("当前系统不支持 16kHz 单声道麦克风录音。");
        }
        TargetDataLine line = (TargetDataLine) AudioSystem.getLine(info);
        line.open(format, AUDIO_BUFFER_BYTES * 4);
        return line;
    }

    private float[] toSamples(byte[] audioBuffer, int bytesRead) {
        int sampleCount = bytesRead / 2;
        float[] samples = new float[sampleCount];
        for (int i = 0; i < sampleCount; i++) {
            int low = audioBuffer[i * 2] & 0xff;
            int high = audioBuffer[i * 2 + 1];
            short sample = (short) ((high << 8) | low);
            samples[i] = sample / 32768.0f;
        }
        return samples;
    }

    private String mergeText(String baseText, String recognizedText) {
        if (recognizedText == null || recognizedText.isBlank()) {
            return baseText;
        }
        if (baseText == null || baseText.isBlank()) {
            return recognizedText;
        }
        return baseText + "\n" + recognizedText;
    }

    private void validateModelFiles() {
        requireFile("encoder.int8.onnx");
        requireFile("decoder.onnx");
        requireFile("joiner.int8.onnx");
        requireFile("tokens.txt");
    }

    private void requireFile(String fileName) {
        Path file = modelDir.resolve(fileName);
        if (!Files.isRegularFile(file)) {
            throw new IllegalStateException("缺少语音识别模型文件：" + file);
        }
    }

    private Path resolveModelDir(String configuredModelDir) {
        Path configured = Path.of(configuredModelDir);
        if (configured.isAbsolute() && Files.isDirectory(configured)) {
            return configured;
        }

        Path workingDir = Path.of("").toAbsolutePath().normalize();
        Path[] candidates = {
                workingDir.resolve(configured).normalize(),
                workingDir.resolve("triage-client").resolve(configured).normalize(),
                workingDir.resolve("..").resolve(configured).normalize()
        };
        for (Path candidate : candidates) {
            if (Files.isDirectory(candidate)) {
                return candidate;
            }
        }
        return candidates[0];
    }
}
