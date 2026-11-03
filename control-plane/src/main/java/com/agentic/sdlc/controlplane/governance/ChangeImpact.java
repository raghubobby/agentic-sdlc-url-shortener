package com.agentic.sdlc.controlplane.governance;

public enum ChangeImpact {
    API_CONTRACT,
    DATA_MODEL,
    SECURITY_SENSITIVE,
    EXTERNAL_INTEGRATION,
    DESTRUCTIVE,
    DIRECT_PRODUCTION_MUTATION,
    RELEASE_ACTION
}
