package com.lx.tracklist.data.remote

/**
 * Source state for an API credential slot shown in Settings.
 */
enum class ApiKeyState(val label: String) {
    BUILT_IN("Built-in"),
    CUSTOM("Custom"),
    MISSING("Missing");

    val isConfigured: Boolean
        get() = this != MISSING
}

/**
 * Resolves whether an API key is supplied by a user override ([ApiKeyState.CUSTOM]),
 * compiled into the build ([ApiKeyState.BUILT_IN]), or absent ([ApiKeyState.MISSING]).
 */
fun resolveApiKeyState(customValue: String?, builtInValue: String?): ApiKeyState = when {
    !customValue.isNullOrBlank() -> ApiKeyState.CUSTOM
    !builtInValue.isNullOrBlank() -> ApiKeyState.BUILT_IN
    else -> ApiKeyState.MISSING
}
