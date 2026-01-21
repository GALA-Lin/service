package com.unlimited.sports.globox.holder;

import org.springframework.stereotype.Component;
import toolgood.words.WordsSearchEx;

import java.util.concurrent.atomic.AtomicReference;

@Component
public class SensitiveWordsHolder {
    private final AtomicReference<WordsSearchEx> ref = new AtomicReference<>(new WordsSearchEx());
    private volatile String version = "NONE";
    private volatile int keywordCount = 0;

    public WordsSearchEx engine() {return ref.get();}

    public String version() {return version;}

    public int keywordCount() {return keywordCount;}

    public void swap(WordsSearchEx newEngine, String newVersion, int newCount) {
        ref.set(newEngine);
        version = newVersion;
        keywordCount = newCount;
    }
}