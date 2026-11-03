package com.agentic.sdlc.controlplane.tool;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ToolScopePolicyTest {
    private final ToolScopePolicy policy = new ToolScopePolicy();

    @Test
    void acceptsPatchInsideProductModule() {
        policy.assertSafeProductPatch("""
                diff --git a/url-shortener/src/main/java/example/A.java b/url-shortener/src/main/java/example/A.java
                new file mode 100644
                --- /dev/null
                +++ b/url-shortener/src/main/java/example/A.java
                @@ -0,0 +1 @@
                +class A {}
                """);
    }

    @Test
    void rejectsPatchThatAttemptsToModifyControlPlane() {
        assertThatThrownBy(() -> policy.assertSafeProductPatch("""
                diff --git a/control-plane/src/main/java/example/A.java b/control-plane/src/main/java/example/A.java
                --- a/control-plane/src/main/java/example/A.java
                +++ b/control-plane/src/main/java/example/A.java
                @@ -1 +1 @@
                -class A {}
                +class A { int x; }
                """))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("outside product scope");
    }

    @Test
    void rejectsSymlinkPatch() {
        assertThatThrownBy(() -> policy.assertSafeProductPatch("""
                diff --git a/url-shortener/link b/url-shortener/link
                new file mode 120000
                --- /dev/null
                +++ b/url-shortener/link
                @@ -0,0 +1 @@
                +../../control-plane
                """))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("symbolic-link");
    }
}
