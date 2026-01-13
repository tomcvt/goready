package com.tomcvt.goready.ui.composables

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.unit.dp
import androidx.core.text.isDigitsOnly

@Composable
fun TextInputCard(
    onTextChange: (String) -> Unit,
    onFocusLost: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    var internalText by remember { mutableStateOf("") }

    OutlinedTextField(
        value = internalText,
        onValueChange = { internalText = it; onTextChange(it) },
        placeholder = { Text(placeholder) },
        modifier = modifier
            .fillMaxWidth()
            .onFocusChanged { state ->
                if (!state.isFocused) {
                    // Call parent lambda when focus leaves
                    onFocusLost(internalText)
                }
            },
        textStyle = MaterialTheme.typography.bodyLarge,
        singleLine = false,        // allows multiple lines
        maxLines = 5              // or any number you want
    )
}

@Composable
fun NumbersInput(
    onNumbersChange: (String) -> Unit,
    onFocusLost: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    var internalText by remember { mutableStateOf("") }

    OutlinedTextField(
        value = internalText,
        onValueChange = { if (it.isDigitsOnly()) internalText = it;
                            onNumbersChange(it)},
        placeholder = { Text(placeholder) },
        modifier = Modifier
            .size(200.dp)
            .onFocusChanged { state ->
                if(!state.isFocused) {
                    // Call parent lambda when focus leaves
                    onFocusLost(internalText)
                }
            },
        textStyle = MaterialTheme.typography.bodyLarge,
        singleLine = true
    )
}