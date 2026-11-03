package com.agentic.sdlc.controlplane.tool;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

final class ToolHashing {
    private ToolHashing() {}

    static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest((value == null ? "" : value).getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is unavailable", ex);
        }
    }

    static String actionHash(ToolAction action) {
        return sha256(action.tool() + "\n" + nullSafe(action.path()) + "\n"
                + nullSafe(action.module()) + "\n" + nullSafe(action.content()));
    }

    private static String nullSafe(String value) { return value == null ? "" : value; }
}
