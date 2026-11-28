# Aceptación Android de Beta 3

Equipo objetivo Samsung Galaxy Tab S9 FE, Exynos 1380 y 8 GB de RAM. Anota el modelo exacto, la versión de Android, el commit, el SHA-256 del APK y si usa Sherpa 1.13.8 o Sherpa 2.0.1 con UNAM.

## Suite de dispositivo

Instala el APK de la app y el APK de pruebas del mismo workflow de GitHub Actions. Usan la firma de depuración del runner. Usa un dispositivo de pruebas o un entorno de Android compatible con esa firma. Conserva la biblioteca existente si el equipo ya usa otra firma.

Con la app y el APK de pruebas instalados, activa el modo avión y verifica que Wi-Fi esté apagado. Ejecuta desde la computadora conectada por USB:

```sh
adb shell am instrument -w com.voxnatural.app.test/androidx.test.runner.AndroidJUnitRunner
```

La suite prueba el extractor que usa la app para PDF, DOCX y TXT, entradas corruptas y vacías, y la síntesis por cada uno de los cuatro motores. Verifica PCM, codifica MP3 con JNI y abre ambos formatos con MediaPlayer. Guarda la salida completa de la instrumentación.

## Flujos de usuario

Con la red desactivada, registra el resultado y tiempo de cada caso.

| Caso | Verificación |
| --- | --- |
| Importación | Importar un PDF digital, un DOCX con varios párrafos y un TXT UTF-8 con tildes. Verificar en la biblioteca título y contenido. Probar un PDF escaneado sin OCR, un DOCX corrupto y un TXT vacío. |
| Persistencia | Editar título y autor, avanzar en lectura, cerrar la app y volver a abrirla. Comprobar documento, datos y posición. |
| Conversión extendida | Usar el mismo libro de dominio público en ambas variantes y ejecutar los cuatro motores por separado. Registrar fragmentos, tiempo y cualquier error. |
| Interrupción | Interrumpir un trabajo en una instalación de pruebas, volver a abrir la app y reanudar la misma conversión. Confirmar que conserva los fragmentos válidos, rehace los dañados y que el WAV final se abre. |
| Reproducción | Reproducir WAV y MP3, pausar, reanudar, mover el control de posición y comprobar la recuperación al volver a abrir. |
| Exportación | Exportar WAV y MP3 a Archivos con el selector Android. Abrir ambos en un reproductor fuera de VoxNatural. |
| Escucha | Comparar voces y variantes a volumen equivalente. Anotar ejemplos concretos de español latinoamericano, palabras acentuadas, pausas y errores. |

Corpus breve de escucha:

> ¿La niña recibió 2,5 mg de medicación? ¡Sí! El médico explicó la recuperación. En México, la anestesiología atiende a pacientes con hipertensión. El 25 de noviembre se registraron 1.250 dosis. ¿Qué ocurrió después? Escúchame… aún quedan dos páginas.

## Medición de referencia

Durante audiolibros largos registra memoria máxima con `adb shell dumpsys meminfo com.voxnatural.app`, condición térmica con `adb shell dumpsys thermalservice`, bytes ocupados antes y después, y el tiempo de conversión frente a la duración del audio. Registra temperatura ambiente, estado de carga y pantalla. Usa esos resultados y la evaluación auditiva para comparar cambios de Beta 4 tras el cierre de Beta 3.
