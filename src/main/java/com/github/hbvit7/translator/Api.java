package com.github.hbvit7.translator;

import java.io.IOException;

public interface Api {
    String translate(String sourceLang, String targetLang1, String text) throws IOException;
}
