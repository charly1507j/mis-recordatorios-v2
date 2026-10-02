# Mis Recordatorios V2 — Android

Versión nativa para Android enfocada en alarmas reales del sistema.

## Funciones
- Crear, editar y eliminar recordatorios.
- Fecha y hora.
- Alarmas exactas mediante AlarmManager.
- Sonido y vibración mediante canal de alarma.
- Repetición diaria, semanal y mensual.
- Reprogramación después de reiniciar el teléfono.
- Datos guardados localmente en el teléfono.
- Sin servidor y sin cuenta obligatoria.

## Requisitos
Android Studio reciente + Android SDK 36.

La app solicita `POST_NOTIFICATIONS` en Android 13+ y acceso especial de "Alarmas y recordatorios" en Android 12+ para poder entregar alarmas exactas.

## Importante
Android puede aplicar restricciones propias del fabricante. Para máxima confiabilidad, permite notificaciones y, si el teléfono ofrece la opción, permite a la app ejecutarse sin restricciones de batería.
