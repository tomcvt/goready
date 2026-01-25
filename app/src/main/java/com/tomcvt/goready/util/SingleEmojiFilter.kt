package com.tomcvt.goready.util

import android.icu.text.BreakIterator
import android.text.InputFilter
import android.text.Spanned
import androidx.emoji2.text.EmojiCompat
import java.util.Locale

class SingleEmojiFilter : InputFilter {

    override fun filter(
        source: CharSequence,
        start: Int,
        end: Int,
        dest: Spanned,
        dstart: Int,
        dend: Int
    ): CharSequence? {

        val newText = StringBuilder(dest)
            .replace(dstart, dend, source.subSequence(start, end).toString())
            .toString()

        return if (isExactlyOneEmoji(newText)) null else ""
    }
}

fun hasExactlyOneGrapheme(input: String): Boolean {
    val it = BreakIterator.getCharacterInstance(Locale.ROOT)
    it.setText(input)

    var count = 0
    var start = it.first()
    var end = it.next()

    while (end != BreakIterator.DONE) {
        count++
        if (count > 1) return false
        start = end
        end = it.next()
    }
    return count == 1
}

fun isEmoji(input: String): Boolean {
    return input.codePoints().anyMatch { cp ->
        Character.getType(cp).toByte() == Character.OTHER_SYMBOL
    }
}

fun isExactlyOneEmoji(input: String): Boolean {
    return hasExactlyOneGrapheme(input) && isEmoji(input)
}