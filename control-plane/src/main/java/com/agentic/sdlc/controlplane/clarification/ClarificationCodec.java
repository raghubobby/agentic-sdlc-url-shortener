package com.agentic.sdlc.controlplane.clarification;

import java.util.Arrays;
import java.util.List;

final class ClarificationCodec {
    private static final String SEP = "\u001F";
    private ClarificationCodec() {}

    static String encode(List<String> values) { return String.join(SEP, values); }
    static List<String> decode(String value) {
        if (value == null || value.isBlank()) return List.of();
        return Arrays.asList(value.split(SEP, -1));
    }
}
