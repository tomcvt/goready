package com.tomcvt.goready.ui.composables

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

data class TypographyItem(
    val name: String,
    val style: TextStyle
)

@Composable
fun typographyItems(): List<TypographyItem> {
    val t = MaterialTheme.typography
    return listOf(
        TypographyItem("displayLarge", t.displayLarge),
        TypographyItem("displayMedium", t.displayMedium),
        TypographyItem("displaySmall", t.displaySmall),
        TypographyItem("headlineLarge", t.headlineLarge),
        TypographyItem("headlineMedium", t.headlineMedium),
        TypographyItem("headlineSmall", t.headlineSmall),
        TypographyItem("titleLarge", t.titleLarge),
        TypographyItem("titleMedium", t.titleMedium),
        TypographyItem("titleSmall", t.titleSmall),
        TypographyItem("bodyLarge", t.bodyLarge),
        TypographyItem("bodyMedium", t.bodyMedium),
        TypographyItem("bodySmall", t.bodySmall),
        TypographyItem("labelLarge", t.labelLarge),
        TypographyItem("labelMedium", t.labelMedium),
        TypographyItem("labelSmall", t.labelSmall),
    )
}

@Preview(showBackground = true)
@Composable
fun MaterialThemeTypographyPreview() {
    val textStyles = typographyItems()
    MaterialTheme {
        Column {
            textStyles.forEach {
                Text(text = it.name, style = it.style)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

    }
}