package com.example.tuitionmanager.view.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tuitionmanager.R

/**
 * Google Sign-In Button following Google's branding guidelines.
 * Supports both light and dark themes with proper styling.
 */
@Composable
fun GoogleSignInButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false
) {
    val isDarkTheme = isSystemInDarkTheme()
    
    // Google brand colors based on theme
    val containerColor = if (isDarkTheme) {
        Color(0xFF131314) // Dark mode: Dark gray background
    } else {
        Color.White // Light mode: White background
    }
    
    val contentColor = if (isDarkTheme) {
        Color(0xFFE3E3E3) // Dark mode: Light gray text
    } else {
        Color(0xFF1F1F1F) // Light mode: Dark text
    }
    
    val borderColor = if (isDarkTheme) {
        Color(0xFF8E918F) // Dark mode: Gray border
    } else {
        Color(0xFFDADCE0) // Light mode: Light gray border
    }

    OutlinedButton(
        onClick = onClick,
        enabled = enabled && !isLoading,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(26.dp), // Pill shape per Google guidelines
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = containerColor.copy(alpha = 0.6f),
            disabledContentColor = contentColor.copy(alpha = 0.5f)
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (enabled) borderColor else borderColor.copy(alpha = 0.5f)
        ),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                color = Color(0xFF4285F4), // Google Blue
                strokeWidth = 2.dp
            )
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                // Google Logo from drawable resource
                Image(
                    painter = painterResource(id = R.drawable.ic_google_logo),
                    contentDescription = "Google Logo",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Sign in with Google",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = contentColor
                )
            }
        }
    }
}

