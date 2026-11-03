package com.agentic.sdlc.controlplane.tool;

import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class ToolScopePolicy {
    public static final String PRODUCT_MODULE = "url-shortener";
    private static final Pattern DIFF_PATH = Pattern.compile("^(?:---|\\+\\+\\+) (?:a/|b/)?(.+)$");

    public String productModule(String requested) {
        String module = requested == null ? PRODUCT_MODULE : requested;
        if (!PRODUCT_MODULE.equals(module)) {
            throw new IllegalArgumentException("Tool scope is restricted to module: " + PRODUCT_MODULE);
        }
        return module;
    }

    public void assertSafeProductPatch(String patch) {
        if (patch == null || patch.isBlank()) throw new IllegalArgumentException("Patch content is required");
        if (patch.contains("GIT binary patch") || patch.contains("new file mode 120000")
                || patch.contains("old mode 120000") || patch.contains("new file mode 160000")
                || patch.contains("old mode 160000")) {
            throw new IllegalArgumentException("Binary patches and symbolic-link changes are not allowed");
        }
        boolean sawPath = false;
        for (String line : patch.lines().toList()) {
            Matcher matcher = DIFF_PATH.matcher(line);
            if (!matcher.matches()) continue;
            String path = matcher.group(1).trim();
            if ("/dev/null".equals(path)) continue;
            sawPath = true;
            if (path.startsWith("/") || path.contains("..") || !path.startsWith(PRODUCT_MODULE + "/")) {
                throw new IllegalArgumentException("Patch path outside product scope: " + path);
            }
        }
        if (!sawPath) throw new IllegalArgumentException("Patch does not contain a product file path");
    }
}
