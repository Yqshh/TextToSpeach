package com.example.texttospeach;

import android.app.Activity;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.speech.RecognizerIntent;
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
    private Button voiceInputWithTranslateButton; // NEW: opens separate screen
    private EditText editText;
    private Spinner langSpinner;
    private TTSManager ttsManager;
    private MediaPlayer mediaPlayer;
    private boolean isPlaying = false;
    private int lastPosition = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        editText = findViewById(R.id.input_text);
        speakNowButton = findViewById(R.id.speak_now);
        translateButton = findViewById(R.id.translate_btn);
        toggleMusicButton = findViewById(R.id.toggle_music);
        voiceInputButton = findViewById(R.id.voice_input_btn);
        voiceInputWithTranslateButton = findViewById(R.id.voice_input_with_translate_btn); // ensure this id exists in layout
        langSpinner = findViewById(R.id.lang_spinner);

        ttsManager = new TTSManager();

        // MediaPlayer init
        mediaPlayer = MediaPlayer.create(this, R.raw.music);
        mediaPlayer.setLooping(true);
        mediaPlayer.setVolume(0.3f, 0.3f);

        // Speak text
        speakNowButton.setOnClickListener(v -> {
            String text = editText.getText().toString();
            String selectedLang = langSpinner.getSelectedItem().toString();

            Locale locale = selectedLang.equals("Русский") ? new Locale("ru", "RU") : Locale.US;
            ttsManager.init(MainActivity.this, locale);

            new Handler().postDelayed(() -> ttsManager.speak(text), 100);
        });

        // Manual translate (kept working)
        translateButton.setOnClickListener(v -> {
            String text = editText.getText().toString();
            autoTranslateAndSpeak(text);
        });

        // Music toggle
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

        // Voice input (kept working, but no auto-translation here)
        voiceInputButton.setOnClickListener(v -> startVoiceInput());

        // NEW: Voice input with auto-translation opens separate activity
        voiceInputWithTranslateButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, VoiceTranslateActivity.class);
            startActivity(intent);
        });
    }

    // Voice input flow (no auto-translate here)
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

    // Result: only set text; do NOT auto-translate here
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == VOICE_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (result != null && !result.isEmpty()) {
                String recognizedText = result.get(0);
                editText.setText(recognizedText);
                // intentionally no autoTranslateAndSpeak() here
            }
        }
    }

    // Manual translate + speak (kept in main screen for Translate button)
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
                            .addOnSuccessListener(unused -> translator.translate(text)
                                    .addOnSuccessListener(translatedText -> {
                                        editText.setText(translatedText);
                                        ttsManager.init(MainActivity.this, targetLocale);
                                        ttsManager.speak(translatedText);
                                    })
                                    .addOnFailureListener(e ->
                                            editText.setText("Ошибка перевода: " + e.getMessage())))
                            .addOnFailureListener(e ->
                                    Toast.makeText(this, "Ошибка загрузки модели", Toast.LENGTH_SHORT).show());
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Ошибка определения языка", Toast.LENGTH_SHORT).show());
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
