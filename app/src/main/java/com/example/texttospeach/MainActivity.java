package com.example.texttospeach;

import android.app.Activity;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Locale;

import com.google.mlkit.nl.languageid.LanguageIdentification;
import com.google.mlkit.nl.languageid.LanguageIdentifier;
import com.google.mlkit.nl.translate.Translator;
import com.google.mlkit.nl.translate.TranslatorOptions;
import com.google.mlkit.nl.translate.Translation;
import com.google.mlkit.nl.translate.TranslateLanguage;

public class MainActivity extends Activity {

    private static final int VOICE_REQUEST_CODE = 100;

    private Button speakNowButton;
    private Button translateButton;
    private Button toggleMusicButton;
    private Button voiceInputButton;
    private EditText editText;
    private Spinner langSpinner;
    private TTSManager ttsManager;
    private MediaPlayer mediaPlayer;
    private boolean isPlaying = false; // состояние музыки
    private int lastPosition = 0;      // позиция воспроизведения

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        editText = findViewById(R.id.input_text);
        speakNowButton = findViewById(R.id.speak_now);
        translateButton = findViewById(R.id.translate_btn);
        toggleMusicButton = findViewById(R.id.toggle_music);
        voiceInputButton = findViewById(R.id.voice_input_btn);
        langSpinner = findViewById(R.id.lang_spinner);

        ttsManager = new TTSManager();

        // Инициализация MediaPlayer
        mediaPlayer = MediaPlayer.create(this, R.raw.music);
        mediaPlayer.setLooping(true);
        mediaPlayer.setVolume(0.3f, 0.3f);

        // Озвучивание текста
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

        // Перевод текста вручную
        translateButton.setOnClickListener(v -> {
            String text = editText.getText().toString();
            autoTranslateAndSpeak(text);
        });

        // Управление музыкой одной кнопкой (с запоминанием позиции)
        toggleMusicButton.setOnClickListener(v -> {
            if (mediaPlayer != null) {
                if (isPlaying) {
                    lastPosition = mediaPlayer.getCurrentPosition();
                    mediaPlayer.pause();
                    toggleMusicButton.setText("Включить музыку");
                    isPlaying = false;
                } else {
                    mediaPlayer.seekTo(lastPosition);
                    mediaPlayer.start();
                    toggleMusicButton.setText("Выключить музыку");
                    isPlaying = true;
                }
            }
        });

        // Голосовой ввод
        voiceInputButton.setOnClickListener(v -> startVoiceInput());
    }

    private void startVoiceInput() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Скажите текст...");

        try {
            startActivityForResult(intent, VOICE_REQUEST_CODE);
        } catch (Exception e) {
            Toast.makeText(this, "Голосовой ввод недоступен", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == VOICE_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (result != null && !result.isEmpty()) {
                String recognizedText = result.get(0);
                editText.setText(recognizedText);
                // сразу переводим и озвучиваем
                autoTranslateAndSpeak(recognizedText);
            }
        }
    }

    // --- Автоматический перевод и озвучивание ---
    private void autoTranslateAndSpeak(String text) {
        if (text == null || text.isEmpty()) return;

        LanguageIdentifier languageIdentifier = LanguageIdentification.getClient();
        languageIdentifier.identifyLanguage(text)
                .addOnSuccessListener(languageCode -> {
                    String sourceLang, targetLang;
                    Locale targetLocale;

                    if (languageCode.equals("ru")) {
                        sourceLang = TranslateLanguage.RUSSIAN;
                        targetLang = TranslateLanguage.ENGLISH;
                        targetLocale = Locale.US;
                    } else {
                        sourceLang = TranslateLanguage.ENGLISH;
                        targetLang = TranslateLanguage.RUSSIAN;
                        targetLocale = new Locale("ru", "RU");
                    }

                    TranslatorOptions options = new TranslatorOptions.Builder()
                            .setSourceLanguage(sourceLang)
                            .setTargetLanguage(targetLang)
                            .build();
                    Translator translator = Translation.getClient(options);

                    translator.downloadModelIfNeeded()
                            .addOnSuccessListener(unused -> {
                                translator.translate(text)
                                        .addOnSuccessListener(translatedText -> {
                                            editText.setText(translatedText);
                                            ttsManager.init(MainActivity.this, targetLocale);
                                            ttsManager.speak(translatedText);
                                        })
                                        .addOnFailureListener(e -> {
                                            editText.setText("Ошибка перевода: " + e.getMessage());
                                        });
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Ошибка загрузки модели", Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Ошибка определения языка", Toast.LENGTH_SHORT).show();
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
