package com.example.texttospeach;
import android.app.Activity;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import java.util.Locale;

public class MainActivity extends Activity {

    private Button speakNowButton;
    private Button startMusicButton;
    private Button stopMusicButton;
    private EditText editText;
    private Spinner langSpinner;
    private TTSManager ttsManager;
    private MediaPlayer mediaPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        editText = findViewById(R.id.input_text);
        speakNowButton = findViewById(R.id.speak_now);
        startMusicButton = findViewById(R.id.start_music);
        stopMusicButton = findViewById(R.id.stop_music);
        langSpinner = findViewById(R.id.lang_spinner);

        ttsManager = new TTSManager();

        // Инициализация MediaPlayer с музыкой из res/raw/music.mp3
        mediaPlayer = MediaPlayer.create(this, R.raw.music);
        mediaPlayer.setLooping(true);          // музыка по кругу
        mediaPlayer.setVolume(0.3f, 0.3f);     // музыка тише речи

        // Кнопка для озвучивания текста
        speakNowButton.setOnClickListener(v -> {
            String text = editText.getText().toString();
            String selectedLang = langSpinner.getSelectedItem().toString();

            Locale locale;
            if (selectedLang.equals("Русский")) {
                locale = new Locale("ru", "RU");
            } else {
                locale = Locale.US;
            }

            // Инициализация TTS
            ttsManager.init(MainActivity.this, locale);

            // Запуск речи с небольшой задержкой
            new Handler().postDelayed(() -> {
                ttsManager.speak(text);
            }, 100);
        });

        // Кнопка для запуска музыки
        startMusicButton.setOnClickListener(v -> {
            if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
                mediaPlayer.start();
            }
        });

        // Кнопка для остановки музыки
        stopMusicButton.setOnClickListener(v -> {
            if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                mediaPlayer.pause();
                mediaPlayer.seekTo(0); // перемотка в начало
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ttsManager.shutDown();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }
}
