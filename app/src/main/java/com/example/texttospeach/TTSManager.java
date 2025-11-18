package com.example.texttospeach;
import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.widget.Toast;
import java.util.Locale;

public class TTSManager {
    private TextToSpeech mTts = null;
    private boolean isLoaded = false;
    private Locale currentLocale = Locale.US;

    public void init(Context context, Locale locale) {
        currentLocale = locale;
        try {
            mTts = new TextToSpeech(context, status -> {
                if (status == TextToSpeech.SUCCESS) {
                    int result = mTts.setLanguage(currentLocale);
                    if (result == TextToSpeech.LANG_MISSING_DATA ||
                            result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Toast.makeText(context,
                                "Выбранный язык не поддерживается или не установлен",
                                Toast.LENGTH_LONG).show();
                        isLoaded = false;
                    } else {
                        isLoaded = true;
                        Log.i("TTS", "Инициализация успешна");
                    }
                } else {
                    Log.e("TTS", "Ошибка инициализации!");
                    isLoaded = false;
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void shutDown() {
        if (mTts != null) {
            mTts.stop();
            mTts.shutdown();
        }
    }

    public void speak(String text) {
        if (isLoaded && mTts != null) {
            mTts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "utteranceId");
        } else {
            Log.e("TTS", "TTS не инициализирован");
        }
    }
}
