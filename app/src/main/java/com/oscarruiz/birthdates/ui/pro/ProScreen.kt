package com.oscarruiz.birthdates.ui.pro

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.oscarruiz.birthdates.AppContainer
import com.oscarruiz.birthdates.R
import com.oscarruiz.birthdates.ui.common.findActivity
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProRoute(container: AppContainer, isPro: Boolean, onBack: () -> Unit) {
    val price by container.billingRepository.proPrice.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                title = { Text(stringResource(R.string.pro_title)) },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            Column(
                Modifier
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                if (isPro) {
                    Surface(
                        shape = RoundedCornerShape(25.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                    ) {
                        Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Check, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.pro_active), style = MaterialTheme.typography.labelLarge)
                        }
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = price ?: "—",
                            style = MaterialTheme.typography.headlineMedium,
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = stringResource(R.string.pro_one_time),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = {
                            val activity = context.findActivity()
                            val launched = activity != null && container.billingRepository.launchPurchase(activity)
                            if (!launched) {
                                scope.launch { snackbar.showSnackbar(context.getString(R.string.pro_unavailable)) }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                    ) { Text(stringResource(R.string.pro_buy)) }
                    TextButton(
                        onClick = {
                            scope.launch {
                                val restored = container.billingRepository.refresh()
                                val msg = when (restored) {
                                    true -> R.string.pro_restored
                                    false -> R.string.pro_restore_none
                                    null -> R.string.pro_unavailable
                                }
                                snackbar.showSnackbar(context.getString(msg))
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(stringResource(R.string.pro_restore)) }
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(56.dp)) {
                Icon(
                    if (isPro) Icons.Default.Check else Icons.Default.Star,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onTertiary,
                    modifier = Modifier.padding(14.dp),
                )
            }
            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(if (isPro) R.string.pro_headline_thanks else R.string.pro_headline),
                style = MaterialTheme.typography.displaySmall,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(if (isPro) R.string.pro_sub_thanks else R.string.pro_sub),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))
            listOf(R.string.pro_perk_no_ads, R.string.pro_perk_colors, R.string.pro_perk_widget, R.string.pro_perk_custom)
                .forEach { perk ->
                    Row(Modifier.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(12.dp))
                        Text(stringResource(perk), style = MaterialTheme.typography.bodyLarge)
                    }
                }
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            ) {
                Row(Modifier.padding(14.dp)) {
                    Icon(Icons.Default.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Text(stringResource(R.string.pro_trust), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
