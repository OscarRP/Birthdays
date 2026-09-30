package com.oscarruiz.birthdates

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.oscarruiz.birthdates.domain.model.AccentColor
import com.oscarruiz.birthdates.domain.model.AppSettings
import com.oscarruiz.birthdates.domain.model.ThemeMode
import com.oscarruiz.birthdates.ui.navigation.AppNavHost
import com.oscarruiz.birthdates.ui.theme.CandelioTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as CandelioApp).container

        // Comprueba la compra Pro en cada arranque (restaura tras reinstalar o reembolsos).
        container.billingRepository.connect()

        lifecycleScope.launch {
            val settings = container.settingsRepository.settings.first()
            container.rescheduleReminders()
            // El aviso previo de anuncios se muestra una vez en Inicio. Después, se pide
            // el consentimiento en cada arranque, como recomienda Google.
            if (!settings.isPro && settings.adsIntroShown) {
                container.adsController.start(this@MainActivity)
            }
        }

        setContent {
            val settings: AppSettings? by container.settingsRepository.settings
                .collectAsStateWithLifecycle<AppSettings?>(initialValue = null)
            val current = settings
            // Si se pierde Pro (reembolso), no se sigue aplicando un color que ya no corresponde.
            val accent = if (current?.isPro == true) current.accentColor else AccentColor.BERRY
            CandelioTheme(themeMode = current?.themeMode ?: ThemeMode.SYSTEM, accentColor = accent) {
                if (current == null) {
                    Surface(Modifier.fillMaxSize()) {}
                } else {
                    AppNavHost(
                        container = container,
                        settings = current,
                        onStartAds = { container.adsController.start(this@MainActivity) },
                    )
                }
            }
        }
    }
}
