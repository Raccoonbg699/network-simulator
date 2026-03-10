package com.networksim.logic;

import java.util.Locale;
import java.util.ResourceBundle;

public class TranslationService {
    private static ResourceBundle bundle;
    private static Locale currentLocale;

    static {
        setLocale(Locale.ENGLISH);
    }

    public static void setLocale(Locale locale) {
        currentLocale = locale;
        bundle = ResourceBundle.getBundle("messages", locale);
    }

    public static String get(String key) {
        try {
            return bundle.getString(key);
        } catch (Exception e) {
            return key;
        }
    }

    public static Locale getLocale() {
        return currentLocale;
    }
}
