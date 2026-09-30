# Candelio: contexto del proyecto

> **Nombre: Candelio** (decidido el 22/09/2026; renombrado el 27/09/2026).
> Ya están renombrados: `app_name`, los textos de la interfaz, `rootProject.name`, el tema `Theme.Candelio`
> y las clases `CandelioApp` / `CandelioTheme`. **El paquete `com.oscarruiz.birthdates` NO cambia**, y tampoco
> los nombres internos `birthdays.db` (Room) ni `birthdays.json` (copia en Drive): son invisibles para el usuario
> y cambiarlos rompería las instalaciones y copias existentes.
> Título en Google Play: «Candelio: cumpleaños y fechas» / «Candelio: Birthday Reminder».
> Dominio previsto: `candelio.app` (libre el 22/09/2026, pendiente de registrar). Antes, comprobar la marca en TMview.

App Android para guardar los cumpleaños de familiares y amigos y avisar cuando se acercan. Autor: Óscar (OscarRP en GitHub). Repositorio: https://github.com/OscarRP/birthdates (ramas `master` y `develop`).

Responde a Óscar en **español**. El código, los identificadores y los comentarios técnicos pueden ir en inglés o español; los textos de la app están en inglés (por defecto) y español (`values-es`).

## Producto (decisiones cerradas)

- **Sin backend propio.** Todo vive en el móvil. Nada de cuentas ni registro.
- **MVP:** alta/edición/borrado de cumpleaños (nombre, día, mes, año opcional, relación, notas), lista ordenada por próximo cumpleaños con cuenta atrás y edad, avisos locales configurables (el mismo día, 1, 3 o 7 días antes, a una hora elegida) que sobreviven a reinicios.
- **Año opcional:** si falta, no se muestra la edad. Un año vacío en el formulario cuenta como desconocido.
- **29 de febrero:** en años no bisiestos se celebra el 28/02 (por defecto) o el 1/03, configurable.
- **Copia de seguridad:** JSON en la carpeta oculta de la app en el **Google Drive del usuario** (`appDataFolder`, ámbito `drive.appdata`), con copia automática diaria. También exportar/importar JSON local y la copia automática de Android. Se descartó Firebase de momento (obligaciones RGPD de guardar datos de terceros). Puede llegar en v3 si se quiere sincronizar o compartir listas.
- **Monetización (freemium):** banner adaptable de AdMob solo en Inicio + compra única **Pro** (`remove_ads_pro`, ~3,99 €, sin suscripción). La compra va ligada a la cuenta de Google y se restaura con `queryPurchasesAsync`, sin cuentas propias.

### Reglas que no se deben romper
- Avisos y copia de seguridad **nunca** van tras el pago.
- **Nunca** anuncios en: la sección «Hoy», el estado vacío, Añadir/Editar, Ajustes, notificaciones.
- Sin interstitials en el lanzamiento.
- Un usuario Pro nunca ve anuncios ni el formulario de consentimiento.
- **Nunca** se envían datos de los cumpleaños (nombres, fechas, notas) al SDK de anuncios ni a ningún servidor.
- El FAB «Añadir» debe quedar separado del banner (evitar clics accidentales).

### Funciones Pro previstas (aún no implementadas)
Widget de próximos cumpleaños, colores extra de la app, aviso personalizado por persona. En la interfaz ya aparecen con la etiqueta «Pro».

## Diseño

Maqueta completa de todas las pantallas: `docs/design/screens.html` (ábrela en un navegador; tiene modo oscuro y vista Pro).

- Material 3 con paleta propia (sin Dynamic Color): **baya** `#B3226B` (primario; oscuro `#FF8DC1`) y **azafrán** `#F5B02E` (solo para «Hoy» y Pro). Fondos `#FBF8FD` / `#170F1F`. Tokens completos en `ui/theme/Theme.kt`.
- Inicio: secciones Hoy / Esta semana (1-7 días) / Próximas semanas (8-30) / Más adelante. «Hoy» es una tarjeta azafrán; el resto, filas agrupadas con avatar de inicial, fecha, edad y **cuenta atrás grande a la derecha**. Deslizar a la izquierda borra (con Deshacer), a la derecha edita.
- Botón «Añadir» con texto (no solo «+»).
- Permiso de notificaciones: se pide **en contexto**, tras guardar el primer cumpleaños.
- Aviso previo de anuncios (hoja inferior) una sola vez antes del formulario UMP.
- Sugerencia de Pro una sola vez al llegar a 5 cumpleaños.
- Tipografía: de momento la del sistema. El diseño usa Bricolage Grotesque (títulos/números) y Figtree (texto); pendiente añadirlas.

## Arquitectura

Kotlin + Jetpack Compose, MVVM ligero, **DI manual** (`AppContainer`, sin Hilt). minSdk 26, targetSdk 35.

```
com.oscarruiz.birthdates/
├── BirthdaysApp, AppContainer   Arranque y dependencias
├── MainActivity                 Actividad única; lanza Billing, reprograma avisos y arranca anuncios
├── domain/                      Kotlin puro (sin Android): Birthday, AppSettings, BirthdayCalculator
├── data/                        Room (BirthdayEntity/Dao/AppDatabase) + DataStore (SettingsRepository)
├── notifications/               ReminderScheduler, ReminderWorker, BootReceiver, NotificationHelper
├── backup/                      BackupSerializer (JSON), DriveBackupRepository (REST v3) + BackupWorker, FileBackupRepository (SAF)
├── monetization/                BillingRepository, ConsentManager (UMP), AdsController, AdaptiveBanner
├── ui/                          home, edit, settings, backup, pro, navigation, theme, common
└── util/DateTexts               Formatos de fecha según idioma
```

Decisiones técnicas:
- Día, mes y año se guardan **por separado** (el año es opcional); la próxima fecha se calcula en `BirthdayCalculator`.
- Ids **UUID** + `updatedAt` para fusionar importaciones sin duplicados.
- **Avisos:** un único `OneTimeWorkRequest` diario que se reprograma a sí mismo (`APPEND_OR_REPLACE` desde el worker, `REPLACE` desde la app). Sin AlarmManager exacto ni permiso `SCHEDULE_EXACT_ALARM`. `lastReminderDate` evita duplicados; si el móvil estaba apagado, avisa al abrir la app. **No** reprogramar en `Application.onCreate` (también corre cuando arranca el worker).
- **Drive:** `AuthorizationClient` de Google Identity + REST con `HttpURLConnection` (PATCH vía cabecera `X-HTTP-Method-Override`). Un único archivo `birthdays.json`.
- IDs de AdMob de **prueba** en debug y, por ahora, también en release (TODO).
- Se evitan APIs experimentales frágiles: `quantityString()` en lugar de `pluralStringResource`, `DropdownField` propio en lugar de `ExposedDropdownMenuBox`.

## Comandos

```powershell
./gradlew assembleDebug        # compilar
./gradlew test                 # tests unitarios (fechas y copia JSON)
./gradlew lint
```

## Estado y pendientes

- El código base se generó sin poder compilar en el entorno original: **la lógica de fechas está verificada** (tests), el resto hay que compilarlo y probarlo aquí.
- Pendiente fuera del código (ver README): IDs reales de AdMob, producto `remove_ads_pro` en Play Console, cliente OAuth Android con SHA-1 en Google Cloud para Drive, URL de la política de privacidad (`PRIVACY_POLICY_URL` en `SettingsScreen.kt`), formulario de Seguridad de los datos.
- Hoja de ruta por versiones: `docs/ROADMAP.md`.
- Siguientes pasos sugeridos: que compile y pase lint → probar en emulador → GitHub Action que compile y pase tests en `develop` → tipografías → funciones Pro.
