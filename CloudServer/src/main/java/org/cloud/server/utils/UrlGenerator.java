package org.cloud.server.utils;

import org.apache.commons.text.RandomStringGenerator;

public class UrlGenerator {
    private RandomStringGenerator randomStringGenerator;

    public UrlGenerator() {
        this.randomStringGenerator = new RandomStringGenerator.Builder()
                .filteredBy(UrlGenerator::isLatinLetterOrDigit)
                .build();
    }

    private static boolean isLatinLetterOrDigit(int codePoint) {
        return ('a' <= codePoint && codePoint <= 'z')
                || ('A' <= codePoint && codePoint <= 'Z')
                || ('0' <= codePoint && codePoint <= '9')
                || ('+' == codePoint)
                || ('_' == codePoint)
                || ('-' == codePoint);

    }

    public String generate(int length) {
        return randomStringGenerator.generate(length);
    }
}
