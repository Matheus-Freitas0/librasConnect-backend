package com.librasConnect.system.signs;

import java.text.Normalizer;
import java.util.Locale;

import org.springframework.http.HttpStatus;

import com.librasConnect.system.exception.ApiException;

public final class SignLabelSlug {

    private static final int MAX_ID_LEN = 50;

    private SignLabelSlug() {
    }

    public static String normalizeLabelWhitespace(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.trim().replaceAll("\\s+", " ");
    }

    public static String toSignId(String labelNormalizedSingleLine) {
        String folded = Normalizer.normalize(labelNormalizedSingleLine.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
        String lower = folded.toLowerCase(Locale.ROOT);
        String slug = lower.replaceAll("[^a-z0-9]+", "-");
        slug = slug.replaceAll("^-+", "").replaceAll("-+$", "");
        slug = slug.replaceAll("-{2,}", "-");
        if (slug.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "label não gera um identificador válido");
        }
        if (slug.length() > MAX_ID_LEN) {
            slug = slug.substring(0, MAX_ID_LEN).replaceAll("-+$", "");
        }
        if (slug.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "label não gera um identificador válido");
        }
        return slug;
    }
}
