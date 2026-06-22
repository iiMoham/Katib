package com.katib.app.ui

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

/** Thin alias so screens read a little cleaner. */
@Composable
fun stringResLabel(@StringRes id: Int): String = stringResource(id)
