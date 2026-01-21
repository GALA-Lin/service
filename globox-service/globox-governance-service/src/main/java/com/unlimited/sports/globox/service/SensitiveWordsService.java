package com.unlimited.sports.globox.service;

import toolgood.words.WordsSearchEx;

/**
 * 敏感词 service
 */
public interface SensitiveWordsService {

    void reload();

    boolean containsSensitiveWords(String text);
}
