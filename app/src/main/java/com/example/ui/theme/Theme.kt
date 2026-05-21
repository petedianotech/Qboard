package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  accentIndex: Int = 0,
  content: @Composable () -> Unit,
) {
  val colorScheme = when (accentIndex) {
      1 -> { // Sunset Orange
          if (darkTheme) {
              darkColorScheme(
                  primary = Color(0xFFFFB591),
                  secondary = Color(0xFFE7BEAC),
                  tertiary = Color(0xFFCFC899)
              )
          } else {
              lightColorScheme(
                  primary = Color(0xFF9C4100),
                  secondary = Color(0xFF775747),
                  tertiary = Color(0xFF655F31)
              )
          }
      }
      2 -> { // Emerald Green
          if (darkTheme) {
              darkColorScheme(
                  primary = Color(0xFF6CDB9F),
                  secondary = Color(0xFFB7CCB9),
                  tertiary = Color(0xFFA2CEDD)
              )
          } else {
              lightColorScheme(
                  primary = Color(0xFF006D44),
                  secondary = Color(0xFF4F6354),
                  tertiary = Color(0xFF3C6572)
              )
          }
      }
      3 -> { // Crimson Red
          if (darkTheme) {
              darkColorScheme(
                  primary = Color(0xFFFFB2B1),
                  secondary = Color(0xFFE7BDBB),
                  tertiary = Color(0xFFECC197)
              )
          } else {
              lightColorScheme(
                  primary = Color(0xFFBA1A1A),
                  secondary = Color(0xFF775656),
                  tertiary = Color(0xFF7B5733)
              )
          }
      }
      4 -> { // Royal Purple
          if (darkTheme) {
              darkColorScheme(
                  primary = Color(0xFFECB4FF),
                  secondary = Color(0xFFD1C0E6),
                  tertiary = Color(0xFFFFB1D7)
              )
          } else {
              lightColorScheme(
                  primary = Color(0xFF7A2DAE),
                  secondary = Color(0xFF66587A),
                  tertiary = Color(0xFF8C4F6E)
              )
          }
      }
      else -> { // 0: Cosmic Blue (Custom default color scheme)
          if (darkTheme) {
              darkColorScheme(
                  primary = Color(0xFFA5C8FF),
                  secondary = Color(0xFFBBC7DB),
                  tertiary = Color(0xFFD6BAE4)
              )
          } else {
              lightColorScheme(
                  primary = Color(0xFF005FAF),
                  secondary = Color(0xFF535F70),
                  tertiary = Color(0xFF6B5778)
              )
          }
      }
  }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
