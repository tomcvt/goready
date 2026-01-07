package com.tomcvt.goready.ui.composables

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tomcvt.goready.Greeting

@Composable
fun HomeScreen(modifier: Modifier = Modifier) {
    Greeting(
        name = "Home",
    )
}