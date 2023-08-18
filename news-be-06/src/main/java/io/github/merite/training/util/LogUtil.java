package io.github.merite.training.util;

import java.util.regex.Pattern;

public interface LogUtil {

    Pattern SANITIZER = Pattern.compile("[\r\n]");
    String EMPTY = "";

    static String sanitize(String value) {
        return value == null ? null : SANITIZER.matcher(value).replaceAll(EMPTY);
    }
}
