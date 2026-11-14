package com.lx.tracklist.ui.theme

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class ThemePreset(
    val id: String,
    val displayName: String,
    val category: String, // "default" or "advanced"
    val description: String,
    val previewAssetPath: String? = null,
    val backgroundAssetPath: String? = null
) {
    DEFAULT_DARK(
        id = "default_dark",
        displayName = "Default Dark",
        category = "default",
        description = "Classic LX Track List with vibrant neon green accents on pitch black."
    ),
    LIGHT(
        id = "light",
        displayName = "Clean Light",
        category = "default",
        description = "Crisp white and slate gray with refreshing emerald green accents."
    ),
    DARK_RED(
        id = "dark_red",
        displayName = "Crimson Dark",
        category = "default",
        description = "Deep scarlet red accents on dark charcoal and obsidian black."
    ),
    DARK_BLUE_GREY(
        id = "dark_blue_grey",
        displayName = "Slate Blue & Grey",
        category = "default",
        description = "Ice sky blue accents on slate navy and dark grey surfaces."
    ),
    BEYOND(
        id = "beyond",
        displayName = "Beyond",
        category = "advanced",
        description = "Arcane fantasy with glowing crimson runes, starry night wallpaper & magic orb.",
        previewAssetPath = "file:///android_asset/themes/beyond/preview.jpg",
        backgroundAssetPath = "file:///android_asset/themes/beyond/background.jpg"
    ),
    SLIME(
        id = "slime",
        displayName = "Slime",
        category = "advanced",
        description = "Playful slime meadow with bubbly fluid frames, cyan ripples & mascot styling.",
        previewAssetPath = "file:///android_asset/themes/slime/preview.jpg",
        backgroundAssetPath = "file:///android_asset/themes/slime/background.jpg"
    ),
    GATE(
        id = "gate",
        displayName = "Gate",
        category = "advanced",
        description = "Dungeon portal vortex with heavy industrial riveted steel & hazard amber.",
        previewAssetPath = "file:///android_asset/themes/gate/preview.jpg",
        backgroundAssetPath = "file:///android_asset/themes/gate/background.jpg"
    ),
    SKY(
        id = "sky",
        displayName = "Sky",
        category = "advanced",
        description = "Floating Castle Aincrad with holographic cyan HUD scanlines & neon reticle.",
        previewAssetPath = "file:///android_asset/themes/sky/preview.jpg",
        backgroundAssetPath = "file:///android_asset/themes/sky/background.jpg"
    )
}

data class LXThemeColors(
    val primary: Color,
    val primaryDark: Color,
    val primaryLight: Color,
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val surfaceElevated: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textDisabled: Color,
    val border: Color,
    val progressTrack: Color,
    val error: Color,
    val scoreGold: Color,
    val airingBlue: Color,
    val accentSecondary: Color,
    val isLight: Boolean = false,
    val cardCornerRadius: Dp = 12.dp,
    val cardBorderWidth: Dp = 1.dp,
    val cardBackgroundColor: Color = surface,
    val fabContainerColor: Color = primary,
    val fabContentColor: Color = if (isLight) Color.White else background,
    val navContainerColor: Color = surface,
    val navSelectedIndicatorColor: Color = primary,
    val navSelectedContentColor: Color = if (isLight) Color.White else background,
    val navUnselectedContentColor: Color = textSecondary,
    val fabBadgeShape: String = "circle" // "circle", "magic_orb", "slime", "shield", "reticle"
)

val DefaultDarkColors = LXThemeColors(
    primary = Color(0xFFA7EF60),
    primaryDark = Color(0xFF78C82B),
    primaryLight = Color(0xFFC4FF8E),
    background = Color(0xFF000000),
    surface = Color(0xFF121212),
    surfaceVariant = Color(0xFF1E1E1E),
    surfaceElevated = Color(0xFF252525),
    textPrimary = Color(0xFFFFFFFF),
    textSecondary = Color(0xFFB0B0B0),
    textDisabled = Color(0xFF6E6E6E),
    border = Color(0xFF2C2C2C),
    progressTrack = Color(0xFF262626),
    error = Color(0xFFFF5252),
    scoreGold = Color(0xFFFFD166),
    airingBlue = Color(0xFF4EA8DE),
    accentSecondary = Color(0xFFC4FF8E),
    isLight = false,
    fabBadgeShape = "circle"
)

val CleanLightColors = LXThemeColors(
    primary = Color(0xFF10B981),
    primaryDark = Color(0xFF059669),
    primaryLight = Color(0xFF34D399),
    background = Color(0xFFF8FAFC),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFF1F5F9),
    surfaceElevated = Color(0xFFE2E8F0),
    textPrimary = Color(0xFF0F172A),
    textSecondary = Color(0xFF64748B),
    textDisabled = Color(0xFF94A3B8),
    border = Color(0xFFCBD5E1),
    progressTrack = Color(0xFFE2E8F0),
    error = Color(0xFFEF4444),
    scoreGold = Color(0xFFD97706),
    airingBlue = Color(0xFF0284C7),
    accentSecondary = Color(0xFF3B82F6),
    isLight = true,
    cardCornerRadius = 12.dp,
    cardBorderWidth = 1.dp,
    fabBadgeShape = "circle"
)

val CrimsonDarkColors = LXThemeColors(
    primary = Color(0xFFEF4444),
    primaryDark = Color(0xFFB91C1C),
    primaryLight = Color(0xFFF87171),
    background = Color(0xFF0B0B0E),
    surface = Color(0xFF18181F),
    surfaceVariant = Color(0xFF24242F),
    surfaceElevated = Color(0xFF2F2E3E),
    textPrimary = Color(0xFFFFFFFF),
    textSecondary = Color(0xFFD1A5A5),
    textDisabled = Color(0xFF755D5D),
    border = Color(0xFF3B282F),
    progressTrack = Color(0xFF2B1D21),
    error = Color(0xFFFF453A),
    scoreGold = Color(0xFFFBBF24),
    airingBlue = Color(0xFF60A5FA),
    accentSecondary = Color(0xFFF87171),
    isLight = false,
    cardCornerRadius = 12.dp,
    cardBorderWidth = 1.dp,
    fabBadgeShape = "circle"
)

val SlateBlueGreyColors = LXThemeColors(
    primary = Color(0xFF38BDF8),
    primaryDark = Color(0xFF0284C7),
    primaryLight = Color(0xFF7DD3FC),
    background = Color(0xFF0B111E),
    surface = Color(0xFF151E2E),
    surfaceVariant = Color(0xFF1E293B),
    surfaceElevated = Color(0xFF283548),
    textPrimary = Color(0xFFF1F5F9),
    textSecondary = Color(0xFF94A3B8),
    textDisabled = Color(0xFF64748B),
    border = Color(0xFF2E3C52),
    progressTrack = Color(0xFF1E293B),
    error = Color(0xFFF87171),
    scoreGold = Color(0xFFFBBF24),
    airingBlue = Color(0xFF38BDF8),
    accentSecondary = Color(0xFF818CF8),
    isLight = false,
    cardCornerRadius = 12.dp,
    cardBorderWidth = 1.dp,
    fabBadgeShape = "circle"
)

val BeyondThemeColors = LXThemeColors(
    primary = Color(0xFFE53E3E),
    primaryDark = Color(0xFF9B2C2C),
    primaryLight = Color(0xFFFC8181),
    background = Color(0xFF0D0E1A),
    surface = Color(0xFF181928),
    surfaceVariant = Color(0xFF232438),
    surfaceElevated = Color(0xFF2D2E45),
    textPrimary = Color(0xFFFFFFFF),
    textSecondary = Color(0xFFC4B5FD),
    textDisabled = Color(0xFF716F88),
    border = Color(0xFF8C2828),
    progressTrack = Color(0xFF2D1B28),
    error = Color(0xFFFF5252),
    scoreGold = Color(0xFFF6E05E),
    airingBlue = Color(0xFF63B3ED),
    accentSecondary = Color(0xFFF6AD55),
    isLight = false,
    cardCornerRadius = 10.dp,
    cardBorderWidth = 1.5.dp,
    fabContainerColor = Color(0xFFC53030),
    fabContentColor = Color.White,
    navContainerColor = Color(0xFF151624),
    navSelectedIndicatorColor = Color(0xFFC53030),
    navSelectedContentColor = Color.White,
    navUnselectedContentColor = Color(0xFFA78BFA),
    fabBadgeShape = "magic_orb"
)

val SlimeThemeColors = LXThemeColors(
    primary = Color(0xFF38BDF8),
    primaryDark = Color(0xFF0284C7),
    primaryLight = Color(0xFFBAE6FD),
    background = Color(0xFF073B5E),
    surface = Color(0xFF0A4D78),
    surfaceVariant = Color(0xFF0369A1),
    surfaceElevated = Color(0xFF0284C7),
    textPrimary = Color(0xFFF0F9FF),
    textSecondary = Color(0xFFBAE6FD),
    textDisabled = Color(0xFF7DD3FC),
    border = Color(0xFF38BDF8),
    progressTrack = Color(0xFF082F49),
    error = Color(0xFFEF4444),
    scoreGold = Color(0xFFFACC15),
    airingBlue = Color(0xFF38BDF8),
    accentSecondary = Color(0xFFFDE047),
    isLight = false,
    cardCornerRadius = 16.dp,
    cardBorderWidth = 2.dp,
    fabContainerColor = Color(0xFF38BDF8),
    fabContentColor = Color(0xFF0369A1),
    navContainerColor = Color(0xFF0A4770),
    navSelectedIndicatorColor = Color(0xFF7DD3FC),
    navSelectedContentColor = Color(0xFF082F49),
    navUnselectedContentColor = Color(0xFFBAE6FD),
    fabBadgeShape = "slime"
)

val GateThemeColors = LXThemeColors(
    primary = Color(0xFFF59E0B),
    primaryDark = Color(0xFFB45309),
    primaryLight = Color(0xFFFBBF24),
    background = Color(0xFF141416),
    surface = Color(0xFF222227),
    surfaceVariant = Color(0xFF32323A),
    surfaceElevated = Color(0xFF42424E),
    textPrimary = Color(0xFFFAFAFA),
    textSecondary = Color(0xFFD4D4D8),
    textDisabled = Color(0xFF71717A),
    border = Color(0xFF92400E),
    progressTrack = Color(0xFF27272A),
    error = Color(0xFFEF4444),
    scoreGold = Color(0xFFF59E0B),
    airingBlue = Color(0xFF60A5FA),
    accentSecondary = Color(0xFFEF4444),
    isLight = false,
    cardCornerRadius = 8.dp,
    cardBorderWidth = 1.5.dp,
    fabContainerColor = Color(0xFFB91C1C),
    fabContentColor = Color(0xFFFDE047),
    navContainerColor = Color(0xFF1D1D22),
    navSelectedIndicatorColor = Color(0xFFB45309),
    navSelectedContentColor = Color(0xFFFEF3C7),
    navUnselectedContentColor = Color(0xFFA1A1AA),
    fabBadgeShape = "shield"
)

val SkyThemeColors = LXThemeColors(
    primary = Color(0xFF06B6D4),
    primaryDark = Color(0xFF0891B2),
    primaryLight = Color(0xFF22D3EE),
    background = Color(0xFF0B132B),
    surface = Color(0xFF14203D),
    surfaceVariant = Color(0xFF1E3158),
    surfaceElevated = Color(0xFF2A4172),
    textPrimary = Color(0xFFF8FAFC),
    textSecondary = Color(0xFF94A3B8),
    textDisabled = Color(0xFF64748B),
    border = Color(0xFF0891B2),
    progressTrack = Color(0xFF1A2844),
    error = Color(0xFFF43F5E),
    scoreGold = Color(0xFFFB923C),
    airingBlue = Color(0xFF06B6D4),
    accentSecondary = Color(0xFFFB923C),
    isLight = false,
    cardCornerRadius = 12.dp,
    cardBorderWidth = 1.5.dp,
    fabContainerColor = Color(0xFF0891B2),
    fabContentColor = Color.White,
    navContainerColor = Color(0xFF131D38),
    navSelectedIndicatorColor = Color(0xFF06B6D4),
    navSelectedContentColor = Color(0xFF0B132B),
    navUnselectedContentColor = Color(0xFF94A3B8),
    fabBadgeShape = "reticle"
)

val LocalLXColors = staticCompositionLocalOf { DefaultDarkColors }

object ThemeManager {
    private const val PREFS_NAME = "lx_theme_preferences"
    private const val KEY_THEME_ID = "active_theme_id"
    private const val KEY_CUSTOM_BG_URI = "custom_bg_uri"
    private const val KEY_CUSTOM_BG_DIM = "custom_bg_dim"

    var currentProfileId by mutableStateOf(1L)
        private set

    var currentThemeId by mutableStateOf(ThemePreset.DEFAULT_DARK.id)
        private set

    var currentPreset by mutableStateOf(ThemePreset.DEFAULT_DARK)
        private set

    var currentColors by mutableStateOf(DefaultDarkColors)
        private set

    var customBackgroundUri by mutableStateOf<String?>(null)
        private set

    var backgroundDimAlpha by mutableFloatStateOf(0.60f)
        private set

    var isInitialized by mutableStateOf(false)
        private set

    fun init(context: Context, profileId: Long? = null) {
        val targetProfileId = profileId ?: run {
            val mainPrefs = context.getSharedPreferences("com.lx.tracklist_preferences", Context.MODE_PRIVATE)
            mainPrefs.getLong("active_user_id", 1L)
        }
        loadForProfile(targetProfileId, context)
        isInitialized = true
    }

    /**
     * Loads theme settings specifically for the given profile.
     * Prevents any cross bleed between user profiles.
     */
    fun loadForProfile(profileId: Long, context: Context) {
        currentProfileId = profileId
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val profileThemeKey = "theme_id_$profileId"
        val profileBgKey = "custom_bg_uri_$profileId"
        val profileDimKey = "custom_bg_dim_$profileId"

        // Read profile-specific setting, falling back to legacy global only if this profile was the only one
        val savedThemeId = prefs.getString(profileThemeKey, null)
            ?: (if (profileId == 1L) prefs.getString(KEY_THEME_ID, ThemePreset.DEFAULT_DARK.id) else null)
            ?: ThemePreset.DEFAULT_DARK.id

        val savedBgUri = prefs.getString(profileBgKey, null)
            ?: (if (profileId == 1L) prefs.getString(KEY_CUSTOM_BG_URI, null) else null)

        val savedDim = if (prefs.contains(profileDimKey)) {
            prefs.getFloat(profileDimKey, 0.60f)
        } else if (profileId == 1L && prefs.contains(KEY_CUSTOM_BG_DIM)) {
            prefs.getFloat(KEY_CUSTOM_BG_DIM, 0.60f)
        } else {
            0.60f
        }

        customBackgroundUri = savedBgUri
        backgroundDimAlpha = savedDim.coerceIn(0.10f, 0.90f)
        applyThemeInternal(savedThemeId)
    }

    fun setTheme(themeId: String, context: Context, profileId: Long = currentProfileId) {
        currentProfileId = profileId
        applyThemeInternal(themeId)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString("theme_id_$profileId", themeId)
            .apply()
    }

    fun setCustomBackground(uriString: String?, context: Context, profileId: Long = currentProfileId) {
        currentProfileId = profileId
        customBackgroundUri = uriString
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        if (uriString == null) {
            editor.remove("custom_bg_uri_$profileId")
        } else {
            editor.putString("custom_bg_uri_$profileId", uriString)
        }
        editor.apply()
    }

    fun setBackgroundDim(dimAlpha: Float, context: Context, profileId: Long = currentProfileId) {
        currentProfileId = profileId
        val clamped = dimAlpha.coerceIn(0.10f, 0.90f)
        backgroundDimAlpha = clamped
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putFloat("custom_bg_dim_$profileId", clamped)
            .apply()
    }

    fun resetTheme(context: Context, profileId: Long = currentProfileId) {
        setTheme(ThemePreset.DEFAULT_DARK.id, context, profileId)
        setCustomBackground(null, context, profileId)
        setBackgroundDim(0.60f, context, profileId)
    }

    fun deleteForProfile(profileId: Long, context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .remove("theme_id_$profileId")
            .remove("custom_bg_uri_$profileId")
            .remove("custom_bg_dim_$profileId")
            .apply()
    }

    private fun applyThemeInternal(themeId: String) {
        val preset = ThemePreset.entries.find { it.id == themeId } ?: ThemePreset.DEFAULT_DARK
        currentThemeId = preset.id
        currentPreset = preset

        currentColors = when (preset) {
            ThemePreset.DEFAULT_DARK -> DefaultDarkColors
            ThemePreset.LIGHT -> CleanLightColors
            ThemePreset.DARK_RED -> CrimsonDarkColors
            ThemePreset.DARK_BLUE_GREY -> SlateBlueGreyColors
            ThemePreset.BEYOND -> BeyondThemeColors
            ThemePreset.SLIME -> SlimeThemeColors
            ThemePreset.GATE -> GateThemeColors
            ThemePreset.SKY -> SkyThemeColors
        }
    }

    /**
     * Resolves the effective background image to render.
     * Custom user background takes priority; otherwise falls back to preset's background asset if any.
     */
    val effectiveBackgroundImage: String?
        get() = customBackgroundUri ?: currentPreset.backgroundAssetPath

    /**
     * Whether the active theme preset is the Advanced "Beyond" theme.
     */
    val isBeyondTheme: Boolean
        get() = currentThemeId == ThemePreset.BEYOND.id

    /**
     * Whether the active theme preset is the Advanced "Slime" theme.
     */
    val isSlimeTheme: Boolean
        get() = currentThemeId == ThemePreset.SLIME.id
}
