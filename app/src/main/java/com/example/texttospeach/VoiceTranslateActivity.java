package com.example.texttospeach;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.speech.RecognizerIntent;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Locale;

import com.google.mlkit.nl.languageid.LanguageIdentification;
import com.google.mlkit.nl.languageid.LanguageIdentifier;
import com.google.mlkit.nl.translate.Translator;
import com.google.mlkit.nl.translate.TranslatorOptions;
import com.google.mlkit.nl.translate.Translation;
import com.google.mlkit.nl.translate.TranslateLanguage;

public class VoiceTranslateActivity extends Activity {

    private static final int VOICE_REQUEST_CODE = 200;
    private EditText editText;
    private Button voiceInputButton;
    private TTSManager ttsManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_voice_translate);

        editText = findViewById(R.id.voice_input_text);
        voiceInputButton = findViewById(R.id.voice_input_btn);
        ttsManager = new TTSManager();

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

                                            // Инициализация TTS и озвучка с задержкой
                                            ttsManager.init(VoiceTranslateActivity.this, targetLocale);
                                            new Handler().postDelayed(() -> {
                                                ttsManager.speak(translatedText);
                                            }, 300);
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
    }
}
