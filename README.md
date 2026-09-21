# Birthdays

App Android para guardar cumpleaños y recibir avisos. Kotlin, Jetpack Compose y Material 3, sin backend propio.

- **Paquete:** `com.oscarruiz.birthdates`
- **Mínimo:** Android 8.0 (API 26). **Objetivo:** API 35.
- **Idiomas:** inglés (por defecto) y español (`values-es`).

## Abrir y ejecutar

1. Android Studio → **File → Open** → esta carpeta.
2. Deja que sincronice Gradle. La primera vez descarga dependencias y puede tardar.
3. Ejecuta la configuración `app` en un emulador o en tu móvil.
4. Tests de la lógica de fechas y de la copia: `./gradlew test`.

En depuración se usan los **IDs de prueba de AdMob** y se simula estar en la UE para ver el formulario de consentimiento.

## Estructura

```
app/src/main/java/com/oscarruiz/birthdates/
├── BirthdaysApp / AppContainer   Arranque e inyección de dependencias manual
├── MainActivity                  Actividad única
├── domain/                       Kotlin puro: modelo y cálculo de fechas
├── data/                         Room (cumpleaños) y DataStore (ajustes)
├── notifications/                Worker diario, notificaciones y BootReceiver
├── backup/                       JSON, Google Drive (appDataFolder) y archivo local
├── monetization/                 Play Billing, consentimiento UMP y AdMob
├── ui/                           Pantallas Compose: home, edit, settings, backup, pro
└── util/                         Formatos de fecha
```

## Cómo funcionan los avisos

Un único `ReminderWorker` (WorkManager) se ejecuta cada día a la hora elegida, calcula qué cumpleaños tocan según los días de antelación y se reprograma a sí mismo. También se reprograma al abrir la app, al cambiar los ajustes y al reiniciar el móvil. Si el móvil estaba apagado a la hora del aviso, se envía en cuanto se abre la app.

## Antes de publicar (pendiente)

### 1. AdMob
- Crea la app y un bloque de anuncios de tipo **banner** en AdMob.
- En `app/build.gradle.kts`, bloque `release`, sustituye `admobAppId` y `BANNER_AD_UNIT_ID` por tus IDs reales. Ahora están los de prueba.
- Publica `app-ads.txt` en tu web de desarrollador.
- En AdMob → Privacidad y mensajes, crea el mensaje RGPD (el que muestra el SDK UMP).

### 2. Compra Pro (Google Play Console)
- Crea un producto de compra única con el ID **`remove_ads_pro`**.
- Añade tu cuenta como tester de licencias para probar compras sin pagar.
- Las compras solo funcionan con la app subida a Play (por ejemplo, en una pista de prueba interna).

### 3. Copia en Google Drive
- En Google Cloud Console, crea un proyecto y **activa la API de Google Drive**.
- Configura la **pantalla de consentimiento OAuth** con el ámbito `.../auth/drive.appdata`.
- Crea un **ID de cliente OAuth de tipo Android** con el paquete `com.oscarruiz.birthdates` y la huella **SHA-1** de tu certificado. Obtén la de depuración con `./gradlew signingReport`. Añade también la SHA-1 de firma de Play Console (Integridad de la app).
- No hace falta añadir ningún archivo de configuración a la app.

### 4. Privacidad y ficha de Play
- Publica una política de privacidad y pon su URL en `PRIVACY_POLICY_URL` (`ui/settings/SettingsScreen.kt`).
- En Play Console: marca **Contiene anuncios** y rellena **Seguridad de los datos**. AdMob usa el identificador de publicidad. La copia se guarda en el Drive del propio usuario, no en tus servidores.

## Pendiente para próximas versiones
- Widget, colores extra y aviso por persona (funciones Pro, ya anunciadas en la interfaz).
- Botón «Felicitar» en la notificación.
- Tipografías del diseño (Bricolage Grotesque y Figtree) mediante Google Fonts descargables. Ahora usa la fuente del sistema.
