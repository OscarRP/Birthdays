# Hoja de ruta de Candelio

Plazos orientativos desde el lanzamiento. Etiquetas: **[Gratis]**, **[Pro]** (pago único), **[Web]** (fuera de la app), **[Hecho]** (ya en el código base).

## 1.0 · Lanzamiento: lo básico, bien hecho
- [Hecho] Añadir, editar y borrar cumpleaños con deshacer; año opcional; regla del 29 de febrero.
- [Hecho] Inicio por secciones con cuenta atrás, búsqueda y tema claro, oscuro o del sistema.
- [Hecho] Avisos configurables (el día, 1, 3 o 7 días antes) que siguen funcionando tras reiniciar el móvil.
- [Hecho] Copia en Google Drive (`appDataFolder`), exportar e importar JSON y copia automática de Android.
- [Hecho] Banner en Inicio con consentimiento UMP; Pro sin anuncios.
- [Gratis] Medición básica de aperturas, retención y conversión a Pro (herramienta por decidir).
- [Pro] Colores de la app.
- [Web] Web mínima con política de privacidad y `app-ads.txt`.
- [Web] Ficha de Google Play en español e inglés; precio de lanzamiento de Pro a 2,49 € durante un mes.
- **Métrica:** menos del 1 % de fallos y valoración de 4,5 o más.

## 1.1 · Conectar: crecer y volver (+1–1,5 meses)
- [Gratis] **Felicitar:** botón en la notificación y en la ficha que abre WhatsApp, SMS o llamada, con plantillas de mensaje.
- [Gratis] Importar desde Contactos.
- [Gratis] **Compartir mi cumpleaños** desde Ajustes, con recordatorio una semana antes de tu cumpleaños.
- [Gratis] **Compartir el de otra persona** desde su ficha (nombre, día, mes y año opcional; nunca relación ni notas) y **compartir varios** con selección múltiple en un solo enlace.
- [Gratis] **Recibir:** App Link `https://<dominio>/add#n=…&d=…&m=…&y=…&from=…`. Los datos van tras el `#`, así que no pasan por ningún servidor. Si no tiene la app: web → Google Play → Install Referrer → hoja de confirmación editable, con detección de duplicados.
- [Gratis] Resumen semanal opcional.
- [Gratis] Pedir valoración justo después de felicitar a alguien.
- [Web] Página `/add`, `/.well-known/assetlinks.json` y opción de añadir al calendario (.ics) para quien no tenga Android.
- **Métrica:** aperturas por usuario al mes de ~2 a 4 o más; instalaciones que llegan por enlaces compartidos.
- **Bloqueado por:** elegir entre dominio propio y GitHub Pages.

## 1.2 · Felicitar a lo grande (+2,5–3 meses)
- [Gratis] Tarjetas de felicitación: 3 diseños con la marca «Hecho con Candelio».
- [Pro] Tarjetas premium: más de 20 diseños, sin marca, con foto propia y texto personalizado.
- [Pro] Aviso de cumpleaños redondos (18, 30, 40…) con un mes o más de antelación.
- [Pro] Widget con los próximos cumpleaños.
- [Pro] Aviso distinto para cada persona.
- [Gratis] Prueba inversa de Pro durante los primeros 14 días; propina opcional de 1–3 €.
- **Métrica:** conversión a Pro del 2 % o más de las instalaciones.

## 1.3 · Regalos (+4–5 meses)
- [Pro] Planificador de regalos: ideas, estado (pendiente o comprado), presupuesto por persona e historial por año.
- [Pro] Aviso: «Faltan 5 días y no has marcado ningún regalo como comprado».
- [Pro] Resumen de gasto anual.
- [Gratis] «El año pasado le regalaste…» en la ficha.
- Nota: requiere una migración de Room. Conviene aprovecharla para preparar el modelo «evento con tipo» de la 1.4.

## 1.4 · Más fechas (+6 meses)
- [Pro] Aniversarios, fechas en memoria de alguien y fechas propias.
- [Pro] Santos, con santoral español incluido.
- [Pro] Sincronizar con Google Calendar (en local).
- [Gratis] Fotos en los avatares.

## 2.0 · Familia (+9–12 meses, si hay tracción)
- [Pro] Listas compartidas entre varios miembros (Firebase, con suscripción de unos 1,99 €/año).
- [Pro] Grupos y filtros propios; vista de calendario; datos curiosos; bloqueo con huella.
- [Gratis] Resumen del año, fácil de compartir.
- **Condición para empezar:** más de 5.000 usuarios activos al mes o una demanda clara en reseñas. Requiere revisión legal (RGPD).

## Reglas que no cambian
- Avisos, copia de seguridad, compartir e importar son siempre gratis. Sin límite de cumpleaños.
- Nunca anuncios en «Hoy», en formularios, en Ajustes ni en notificaciones.
- Los datos de los cumpleaños nunca van a servidores ni a los anuncios.

## Descartado
Anuncios a pantalla completa, rachas y puntos, notificaciones sin motivo real, limitar el número de cumpleaños, Firebase Dynamic Links (cerrado por Google en 2025) y envío automático de mensajes.

## Decisiones pendientes
1. ~~Nombre final de la app~~ → **Candelio** (decidido el 22/09/2026). Falta comprobar la marca en TMview.
2. Dominio: previsto `candelio.app`, pendiente de registrar.
3. Herramienta de medición.
