package com.oscarruiz.birthdates.ui.common

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.annotation.PluralsRes
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.oscarruiz.birthdates.R
import com.oscarruiz.birthdates.domain.model.Relation
import com.oscarruiz.birthdates.ui.theme.LocalAvatarColors
import kotlin.math.abs

/** Texto en plural según la cantidad (equivale a pluralStringResource, sin API experimental). */
@Composable
fun quantityString(@PluralsRes id: Int, count: Int, vararg args: Any): String {
    LocalConfiguration.current // Recompone si cambia el idioma.
    return LocalContext.current.resources.getQuantityString(id, count, *args)
}

/** Crea un ViewModel con dependencias manuales. */
@Composable
inline fun <reified VM : ViewModel> appViewModel(key: String? = null, crossinline create: () -> VM): VM =
    viewModel(key = key, factory = viewModelFactory { initializer { create() } })

fun Context.findActivity(): Activity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}

@Composable
fun Avatar(name: String, seed: String, size: Dp = 40.dp, modifier: Modifier = Modifier) {
    val palette = LocalAvatarColors.current.pairs
    val (bg, fg) = palette[abs(seed.hashCode() % palette.size)]
    Box(
        modifier = modifier.size(size).clip(CircleShape).background(bg),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = name.trim().take(1).uppercase(),
            color = fg,
            fontWeight = FontWeight.Bold,
            fontSize = (size.value * 0.45f).sp,
        )
    }
}

/** Cuenta atrás grande a la derecha de cada fila. */
@Composable
fun Countdown(days: Int, modifier: Modifier = Modifier) {
    Column(modifier = modifier.widthIn(min = 44.dp), horizontalAlignment = Alignment.End) {
        Text(
            text = days.toString(),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.End,
        )
        Text(
            text = quantityString(R.plurals.days_label, days),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun RelationTag(relation: Relation, modifier: Modifier = Modifier) {
    Text(
        text = stringResource(relation.labelRes()),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
            .clip(RoundedCornerShape(7.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 7.dp, vertical = 1.dp),
    )
}

fun Relation.labelRes(): Int = when (this) {
    Relation.FAMILY -> R.string.relation_family
    Relation.FRIEND -> R.string.relation_friend
    Relation.WORK -> R.string.relation_work
    Relation.OTHER -> R.string.relation_other
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier, trailing: String? = null) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        if (trailing != null) {
            Text(
                text = trailing,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
