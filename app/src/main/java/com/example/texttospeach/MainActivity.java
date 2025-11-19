package com.example.texttospeach;

import android.app.Activity;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.util.Log;
import android.widget.Toast;


import java.util.Locale;

import com.google.mlkit.nl.translate.Translator;
import com.google.mlkit.nl.translate.TranslatorOptions;
import com.google.mlkit.nl.translate.Translation;
import com.google.mlkit.nl.translate.TranslateLanguage;

public class MainActivity extends Activity {

    private Button speakNowButton;
    private Button translateButton;
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
        translateButton = findViewById(R.id.translate_btn); // новая кнопка
        startMusicButton = findViewById(R.id.start_music);
        stopMusicButton = findViewById(R.id.stop_music);
        langSpinner = findViewById(R.id.lang_spinner);

        ttsManager = new TTSManager();

        // Инициализация MediaPlayer с музыкой из res/raw/music.mp3
        mediaPlayer = MediaPlayer.create(this, R.raw.music);
        mediaPlayer.setLooping(true);
        mediaPlayer.setVolume(0.3f, 0.3f);

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

            ttsManager.init(MainActivity.this, locale);

            new Handler().postDelayed(() -> {
                ttsManager.speak(text);
            }, 100);
        });

        // Кнопка для перевода текста
        translateButton.setOnClickListener(v -> {
            String text = editText.getText().toString();
            String selectedLang = langSpinner.getSelectedItem().toString();

            String sourceLang;
            String targetLang;
            Locale targetLocale;

            if (selectedLang.equals("Русский")) {
                sourceLang = TranslateLanguage.RUSSIAN;
                targetLang = TranslateLanguage.ENGLISH;
                targetLocale = Locale.US;
                langSpinner.setSelection(0);
            } else {
                sourceLang = TranslateLanguage.ENGLISH;
                targetLang = TranslateLanguage.RUSSIAN;
                targetLocale = new Locale("ru", "RU");
                langSpinner.setSelection(1);
            }

            TranslatorOptions options = new TranslatorOptions.Builder()
                    .setSourceLanguage(sourceLang)
                    .setTargetLanguage(targetLang)
                    .build();
            Translator translator = Translation.getClient(options);

            Log.d("DEBUG", "Начинаем загрузку модели: " + sourceLang + " -> " + targetLang);

            translator.downloadModelIfNeeded()
                    .addOnSuccessListener(unused -> {
                        Log.d("DEBUG", "Модель успешно загружена");
                        //Toast.makeText(MainActivity.this, "Модель загружена", Toast.LENGTH_SHORT).show();

                        translator.translate(text)
                                .addOnSuccessListener(translatedText -> {
                                    Log.d("DEBUG", "Перевод успешен: " + translatedText);
                                    editText.setText(translatedText);

                                    ttsManager.init(MainActivity.this, targetLocale);
                                    ttsManager.speak(translatedText);
                                })
                                .addOnFailureListener(e -> {
                                    Log.e("DEBUG", "Ошибка перевода", e);
                                    editText.setText("Ошибка перевода: " + e.getMessage());
                                });
                    })
                    .addOnFailureListener(e -> {
                        Log.e("DEBUG", "Не удалось загрузить модель", e);
                        Toast.makeText(MainActivity.this, "Ошибка загрузки модели", Toast.LENGTH_SHORT).show();
                        editText.setText("Не удалось загрузить модель: " + e.getMessage());
                    });
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
                mediaPlayer.seekTo(0);
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
