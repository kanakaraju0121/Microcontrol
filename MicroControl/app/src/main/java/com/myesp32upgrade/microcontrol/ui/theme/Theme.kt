package com.myesp32upgrade.microcontrol.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.myesp32upgrade.microcontrol.ui.theme.*

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40
)

private val BlueDarkColorScheme = darkColorScheme(
    primary = Blue80,
    secondary = BlueGrey80,
    tertiary = Cyan80
)

private val BlueLightColorScheme = lightColorScheme(
    primary = Blue40,
    secondary = BlueGrey40,
    tertiary = Cyan40
)

private val GreenDarkColorScheme = darkColorScheme(
    primary = Green80,
    secondary = GreenGrey80,
    tertiary = Lime80
)

private val GreenLightColorScheme = lightColorScheme(
    primary = Green40,
    secondary = GreenGrey40,
    tertiary = Lime40
)

private val RedDarkColorScheme = darkColorScheme(
    primary = Red80,
    secondary = RedGrey80,
    tertiary = DeepRed80
)

private val RedLightColorScheme = lightColorScheme(
    primary = Red40,
    secondary = RedGrey40,
    tertiary = DeepRed40
)

private val OrangeDarkColorScheme = darkColorScheme(
    primary = Orange80,
    secondary = OrangeGrey80,
    tertiary = Amber80
)

private val OrangeLightColorScheme = lightColorScheme(
    primary = Orange40,
    secondary = OrangeGrey40,
    tertiary = Amber40
)

private val YellowDarkColorScheme = darkColorScheme(
    primary = Yellow80,
    secondary = YellowGrey80,
    tertiary = Gold80
)

private val YellowLightColorScheme = lightColorScheme(
    primary = Yellow40,
    secondary = YellowGrey40,
    tertiary = Gold40
)

private val TealDarkColorScheme = darkColorScheme(
    primary = Teal80,
    secondary = TealGrey80,
    tertiary = Aqua80
)

private val TealLightColorScheme = lightColorScheme(
    primary = Teal40,
    secondary = TealGrey40,
    tertiary = Aqua40
)

private val PinkDarkColorScheme = darkColorScheme(
    primary = PinkBright80,
    secondary = PinkGrey80,
    tertiary = Magenta80
)

private val PinkLightColorScheme = lightColorScheme(
    primary = PinkBright40,
    secondary = PinkGrey40,
    tertiary = Magenta40
)

@Composable
fun MicroControlTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    themeType: String = "Default", // "Default", "Blue", "Green", ...
    content: @Composable () -> Unit
) {
    val colorScheme = when (themeType) {
        "Blue" -> if (darkTheme) BlueDarkColorScheme else BlueLightColorScheme
        "Green" -> if (darkTheme) GreenDarkColorScheme else GreenLightColorScheme
        "Red" -> if (darkTheme) RedDarkColorScheme else RedLightColorScheme
        "Orange" -> if (darkTheme) OrangeDarkColorScheme else OrangeLightColorScheme
        "Yellow" -> if (darkTheme) YellowDarkColorScheme else YellowLightColorScheme
        "Teal" -> if (darkTheme) TealDarkColorScheme else TealLightColorScheme
        "Pink" -> if (darkTheme) PinkDarkColorScheme else PinkLightColorScheme
        else -> when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
        }
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
} 