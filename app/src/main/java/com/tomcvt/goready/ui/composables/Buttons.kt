package com.tomcvt.goready.ui.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.unit.dp

@Composable
fun SimpleDeleteButton(
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    IconButton(
        onClick = onDelete,
        modifier = Modifier
            //.align(Alignment.TopEnd) // Positions it at the top right
            .offset(-(10.dp), (10.dp))
            .padding(4.dp)
            .background(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = RoundedCornerShape(8.dp) // Square rounded
            )
            .layoutId("delete_button")
            .size(32.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Close,
            contentDescription = "Delete Alarm",
            tint = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.size(18.dp)
        )
    }
}