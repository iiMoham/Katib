package com.katib.app.ime

/** A single key on the keyboard. */
data class Key(
    val label: String,
    val output: String = label,
    val type: KeyType = KeyType.CHAR,
    val weight: Float = 1f,
)

enum class KeyType { CHAR, BACKSPACE, SPACE, ENTER, SYMBOLS, ABC, MODE }

/**
 * Standard Arabic letter layout (right-to-left). Rows are rendered with the
 * first element on the right. The toolbar (mode toggle + suggestion chips) is
 * drawn separately by the service, above these rows.
 */
object ArabicKeyboardLayout {

    val letterLayer: List<List<Key>> = listOf(
        row("ض ص ث ق ف غ ع ه خ ح ج"),
        row("ش س ي ب ل ا ت ن م ك ط"),
        buildList {
            addAll(row("ئ ء ؤ ر لا ى ة و ز ظ د"))
            add(Key("⌫", type = KeyType.BACKSPACE, weight = 1.4f))
        },
        bottomRow(),
    )

    val symbolLayer: List<List<Key>> = listOf(
        row("١ ٢ ٣ ٤ ٥ ٦ ٧ ٨ ٩ ٠"),
        row("، . ؟ ! @ # % & * -"),
        buildList {
            addAll(row("( ) \" ' : ؛ / + ="))
            add(Key("⌫", type = KeyType.BACKSPACE, weight = 1.4f))
        },
        bottomRow(),
    )

    private fun bottomRow(): List<Key> = listOf(
        Key("123", type = KeyType.SYMBOLS, weight = 1.4f),
        Key("،", output = "،"),
        Key("مسافة", output = " ", type = KeyType.SPACE, weight = 4f),
        Key(".", output = "."),
        Key("⏎", type = KeyType.ENTER, weight = 1.4f),
    )

    private fun row(spaceSeparated: String): List<Key> =
        spaceSeparated.split(" ").filter { it.isNotEmpty() }.map { Key(it) }
}
