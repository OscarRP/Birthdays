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
import com.oscarruiz.birthdates.domain.model.AppSettings
import com.oscarruiz.birthdates.domain.model.ThemeMode
import com.oscarruiz.birthdates.ui.navigation.AppNavHost
import com.oscarruiz.birthdates.ui.theme.BirthdaysTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as BirthdaysApp).container

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
            BirthdaysTheme(themeMode = current?.themeMode ?: ThemeMode.SYSTEM) {
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
