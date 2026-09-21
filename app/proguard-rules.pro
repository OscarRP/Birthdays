# Room, WorkManager, Billing, AdMob y kotlinx.serialization incluyen sus propias reglas.
# Mantener los modelos serializables de la copia de seguridad.
-keep class com.oscarruiz.birthdates.backup.BackupFile { *; }
-keep class com.oscarruiz.birthdates.backup.BackupBirthday { *; }
