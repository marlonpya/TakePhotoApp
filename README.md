# TakePhotoApp — Curso Android / Android Course

> Proyecto pedagógico para aprender a usar la cámara en Android con Kotlin moderno.
>
> Pedagogical project for learning how to use the camera in Android with modern Kotlin.

---

## Índice / Table of Contents

- [Español](#español)
- [English](#english)

---

# Español

## ¿Qué aprenderás?

- Cómo solicitar permisos en tiempo de ejecución
- Cómo abrir la cámara con un Intent implícito
- Cómo guardar fotos con FileProvider
- Cómo pasar datos entre Activities con Intent Extras

## Arquitectura de la aplicación

```
MainActivity
    │
    ├── Solicita permisos (CAMERA, WRITE_EXTERNAL_STORAGE en API ≤ 28)
    │
    ├── Crea archivo en getExternalFilesDir()/Pictures/AndroidCoursePictures/
    │
    ├── Convierte File → URI segura con FileProvider
    │
    ├── Lanza la cámara con ActivityResultContracts.TakePicture()
    │
    └── [foto tomada] → Intent explícito → PreviewActivity
                                                │
                                                ├── Lee URI desde Intent Extra
                                                ├── Carga imagen con setImageURI()
                                                └── Muestra ruta del archivo
```

## Estructura de archivos

```
app/src/main/
├── AndroidManifest.xml            — Permisos, FileProvider, Activities
├── java/com/android/takephotoapp/
│   ├── MainActivity.kt            — Pantalla principal: permisos + cámara
│   └── PreviewActivity.kt         — Segunda pantalla: previsualización
└── res/
    ├── layout/
    │   ├── activity_main.xml      — Layout de la pantalla principal
    │   └── activity_preview.xml   — Layout de la previsualización
    ├── values/
    │   └── strings.xml            — Textos de la aplicación
    └── xml/
        └── provider_paths.xml     — Rutas permitidas para FileProvider
```

## Archivos fuente principales

### `MainActivity.kt`

| Elemento | Nombre | Propósito |
|---------|------|---------|
| Propiedad | `photoUri` | Guarda la URI entre el lanzamiento de la cámara y el resultado |
| Vista | `btnTakePhoto` | Botón que inicia el flujo de permisos + cámara |
| Vista | `tvPermissionsStatus` | Muestra el estado actual de los permisos al usuario |
| Launcher | `requestPermissionsLauncher` | Maneja el resultado de la solicitud de permisos |
| Launcher | `takePictureLauncher` | Maneja el resultado de la captura de la cámara |
| Método | `checkAndRequestPermissions()` | Punto de entrada: verifica y solicita permisos faltantes |
| Método | `buildPermissionsList()` | Construye la lista correcta de permisos según el nivel de API |
| Método | `launchCamera()` | Crea el archivo, obtiene la URI y lanza la cámara |
| Método | `createPhotoFile()` | Crea el archivo destino en `AndroidCoursePictures/` |
| Método | `navigateToPreview(uri)` | Inicia `PreviewActivity` con la URI como Extra |
| Método | `updatePermissionsStatus()` | Actualiza el `TextView` de estado al abrir la app |

### `PreviewActivity.kt`

| Elemento | Nombre | Propósito |
|---------|------|---------|
| Constante | `EXTRA_PHOTO_URI` | Clave para pasar la URI entre Activities |
| Vista | `ivCapturedPhoto` | `ImageView` que muestra la foto capturada |
| Vista | `tvFilePath` | Muestra la ruta real del archivo en disco |
| Vista | `btnBack` | Llama a `finish()` para regresar a `MainActivity` |
| Método | `loadPhotoFromIntent()` | Lee el extra de URI y carga la imagen |
| Método | `displayFilePath(uri)` | Reconstruye y muestra la ruta real del archivo |

---

## Conceptos clave

### ¿Qué es una Activity?

Una Activity representa **una pantalla** de la aplicación con su interfaz de usuario. Hereda de `AppCompatActivity` para tener compatibilidad con versiones antiguas de Android.

Ciclo de vida simplificado:

```
onCreate → onStart → onResume → [app visible y activa]
onPause  → onStop  → onDestroy → [app cerrada]
```

---

### Permisos en tiempo de ejecución

Android distingue dos tipos de permisos:

1. **Permisos normales**: se otorgan automáticamente al instalar.
2. **Permisos peligrosos**: el usuario DEBE aprobarlos mientras la app está en uso.

`CAMERA` y `WRITE_EXTERNAL_STORAGE` son permisos peligrosos.

Declararlos en el `AndroidManifest.xml` es el **primer paso obligatorio**. El **segundo paso** es solicitarlos al usuario desde el código Kotlin. Si no están declarados en el Manifest, nunca se podrán otorgar.

**API moderna (Activity Result API) vs deprecated:**

```kotlin
// ❌ Antes (deprecated):
requestPermissions(...)
override fun onRequestPermissionsResult(...) { ... }

// ✅ Ahora (moderno, desde AndroidX Activity 1.2):
val requestPermissionsLauncher = registerForActivityResult(
    ActivityResultContracts.RequestMultiplePermissions()
) { permissions -> ... }
```

> `registerForActivityResult` **DEBE** llamarse antes de que `onCreate()` complete. No puede llamarse dentro de un listener de click.

El callback recibe un `Map<String, Boolean>`:
- `key` = nombre del permiso (ej: `"android.permission.CAMERA"`)
- `value` = `true` si fue concedido, `false` si fue denegado

**`shouldShowRequestPermissionRationale()`**:
- `true` → el usuario denegó antes pero no marcó "no preguntar más". Podemos explicarle por qué necesitamos el permiso.
- `false` → el usuario marcó "no preguntar más" o es la primera vez. Debemos dirigirlo a Ajustes del sistema.

---

### Compatibilidad Scoped Storage

| API Level | Permiso para `getExternalFilesDir()` |
|-----------|--------------------------------------|
| 24 – 28   | `WRITE_EXTERNAL_STORAGE` requerido   |
| 29 – 32   | No se necesita permiso               |
| 33+       | No se necesita permiso               |

Como usamos `getExternalFilesDir()` (carpeta privada de la app), en API 29+ **no necesitamos ningún permiso de almacenamiento** para escribir. Solo lo pedimos en API ≤ 28.

`READ_MEDIA_IMAGES` y `READ_EXTERNAL_STORAGE` solo serían necesarios si quisiéramos acceder a fotos de **otras apps** (galería). No aplica en este proyecto.

---

### Intent implícito vs explícito

| Tipo | Descripción | Ejemplo |
|------|-------------|---------|
| **Implícito** | No especifica qué app debe manejar la acción. El sistema encuentra la app adecuada. | `Intent(MediaStore.ACTION_IMAGE_CAPTURE)` |
| **Explícito** | Especifica exactamente qué Activity abrir. | `Intent(this, PreviewActivity::class.java)` |

`ActivityResultContracts.TakePicture()` usa internamente:

```kotlin
val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
```

`MediaStore.EXTRA_OUTPUT` + la URI indica a la cámara **dónde guardar** la foto de alta resolución. Sin esto, la cámara solo devuelve una miniatura (thumbnail).

---

### FileProvider — URIs seguras

Desde Android 7 (API 24), Android **prohíbe** compartir URIs del tipo `file://` entre aplicaciones. Si se intenta, lanza `FileUriExposedException` y la app se cierra.

**La solución es FileProvider**: convierte una ruta de archivo local en una URI segura `content://` que el sistema puede compartir de forma controlada.

```
❌ SIN FileProvider:  file:///storage/emulated/0/photo.jpg
✅ CON FileProvider:  content://com.android.takephotoapp.fileprovider/my_images/photo.jpg
```

**Atributos importantes del `<provider>` en el Manifest:**

| Atributo | Valor | Por qué |
|----------|-------|---------|
| `android:authorities` | `${applicationId}.fileprovider` | Debe ser único globalmente |
| `android:exported` | `false` | FileProvider NO debe ser público |
| `android:grantUriPermissions` | `true` | Permite permisos temporales a otras apps (la cámara) |
| `android:resource` | `@xml/provider_paths` | Define qué carpetas puede compartir |

**`provider_paths.xml`** es la "lista blanca" de carpetas que FileProvider puede compartir. El atributo `name` es la parte pública de la URI (no revela la ruta real del archivo).

---

### `getExternalFilesDir()`

Devuelve la carpeta externa **privada** de la app. Ruta típica:

```
/storage/emulated/0/Android/data/com.android.takephotoapp/files/Pictures/
```

**Ventajas:**
- No necesita `WRITE_EXTERNAL_STORAGE` en API 29+
- Los archivos se eliminan automáticamente cuando se desinstala la app
- No aparece en la galería del sistema por defecto

Puede retornar `null` si el almacenamiento externo no está disponible (ej: tarjeta SD removida).

---

### `Uri` vs `File`

| | `File` | `Uri` |
|---|--------|-------|
| **Representa** | Ruta del sistema de archivos | Dirección segura a un recurso |
| **Ejemplo** | `/storage/emulated/0/.../photo.jpg` | `content://...fileprovider/.../photo.jpg` |
| **Compartible entre apps (API 24+)** | No ❌ | Sí ✅ |

**`Uri.fromFile(file)`** genera una URI `file://` — **prohibida** para compartir desde API 24.

**`FileProvider.getUriForFile(context, authority, file)`** genera una URI `content://` — **segura** para compartir.

---

### `findViewById` — Conectar XML con Kotlin

```kotlin
// En activity_main.xml:
// android:id="@+id/btnTakePhoto"

// En MainActivity.kt:
val button = findViewById<Button>(R.id.btnTakePhoto)
```

- `R` es una clase generada automáticamente por Android Studio con los IDs de todos los recursos.
- El genérico `<Button>` indica el tipo esperado. Si no coincide con el tipo real en el XML, lanza una excepción.
- Se usan con `lateinit` porque se inicializan en `onCreate()`, no en la declaración de la variable.

---

### Intent Extras — Pasar datos entre Activities

Los Extras son pares clave-valor para pasar datos entre Activities. Es como un "sobre" con información adicional.

```kotlin
// Enviar (en MainActivity):
val intent = Intent(this, PreviewActivity::class.java)
intent.putExtra(PreviewActivity.EXTRA_PHOTO_URI, uri.toString())
startActivity(intent)

// Recibir (en PreviewActivity):
val uriAsString = intent.getStringExtra(EXTRA_PHOTO_URI)
```

**Por qué usar constantes en lugar de Strings directos:**

```kotlin
// ❌ Malo (String mágico — propenso a errores tipográficos):
intent.putExtra("photo_uri", uri.toString())   // en MainActivity
intent.getStringExtra("photo_uri")             // en PreviewActivity

// ✅ Bueno (constante compartida en companion object):
intent.putExtra(PreviewActivity.EXTRA_PHOTO_URI, uri.toString())
intent.getStringExtra(PreviewActivity.EXTRA_PHOTO_URI)
```

**`companion object`** en Kotlin contiene miembros estáticos (equivalente a `static` en Java). Se accede con `NombreClase.CONSTANTE`.

---

### `setImageURI()` — Cargar imagen desde URI

```kotlin
ivCapturedPhoto.setImageURI(null) // Limpiar imagen previa (fuerza recarga)
ivCapturedPhoto.setImageURI(uri)  // Cargar la nueva imagen
```

El sistema abre el `ContentProvider` (FileProvider), lee el `InputStream` de la imagen y la decodifica en un `Bitmap` que se muestra en el `ImageView`.

> Para producción se recomienda **Glide** o **Coil** porque:
> - Manejan imágenes grandes sin `OutOfMemoryError`
> - Tienen caché automático
> - Operan en hilos secundarios (no bloquean la UI)

---

### `finish()` — Cerrar una Activity

```kotlin
btnBack.setOnClickListener {
    finish() // Cierra PreviewActivity y regresa a MainActivity
}
```

`finish()` cierra la Activity actual y la saca del back stack, regresando a la Activity anterior. Es equivalente a que el usuario presione el botón "Atrás".

---

## Errores comunes y soluciones

| Error | Causa | Solución |
|-------|-------|----------|
| `FileUriExposedException` | Usar `Uri.fromFile()` para compartir con otra app | Usar `FileProvider.getUriForFile()` |
| La foto sale en negro / vacía | `photoUri` no se guardó antes de lanzar la cámara | Declarar `photoUri` como variable de clase, no local |
| `NullPointerException` en `getExternalFilesDir()` | Almacenamiento externo no disponible | Verificar con `?: return null` |
| `IllegalArgumentException` en FileProvider | El `authority` no coincide con el del Manifest | Verificar que `"${packageName}.fileprovider"` sea igual en ambos lados |
| App no disponible para dispositivos sin cámara | `<uses-feature required="true">` | Cambiar a `android:required="false"` |
| Permisos no solicitados aunque están en el Manifest | `registerForActivityResult` llamado tarde | Mover fuera de listeners, a nivel de clase |

---

## Requisitos técnicos

- **Min SDK**: API 24 (Android 7.0 Nougat)
- **Target SDK**: API 36
- **Lenguaje**: Kotlin
- **Sin librerías externas** más allá de AndroidX
- **Sin permiso de internet** requerido
- **Sin ViewBinding / DataBinding**

## Dependencias

Todas las dependencias de AndroidX, sin librerías de terceros:

```toml
androidx.core:core-ktx:1.18.0
androidx.appcompat:appcompat:1.7.1
com.google.android.material:material:1.13.0
androidx.activity:activity:1.13.0
androidx.constraintlayout:constraintlayout:2.2.1
```

---

---

# English

## What will you learn?

- How to request runtime permissions
- How to open the camera with an implicit Intent
- How to save photos with FileProvider
- How to pass data between Activities with Intent Extras

## Arquitectura de la aplicación

```
MainActivity
    │
    ├── Solicita permisos (CAMERA, WRITE_EXTERNAL_STORAGE en API ≤ 28)
    │
    ├── Crea archivo en getExternalFilesDir()/Pictures/AndroidCoursePictures/
    │
    ├── Convierte File → URI segura con FileProvider
    │
    ├── Lanza la cámara con ActivityResultContracts.TakePicture()
    │
    └── [foto tomada] → Intent explícito → PreviewActivity
                                                │
                                                ├── Lee URI desde Intent Extra
                                                ├── Carga imagen con setImageURI()
                                                └── Muestra ruta del archivo
```

## Estructura de archivos

```
app/src/main/
├── AndroidManifest.xml            — Permisos, FileProvider, Activities
├── java/com/android/takephotoapp/
│   ├── MainActivity.kt            — Pantalla principal: permisos + cámara
│   └── PreviewActivity.kt         — Segunda pantalla: previsualización
└── res/
    ├── layout/
    │   ├── activity_main.xml      — Layout de la pantalla principal
    │   └── activity_preview.xml   — Layout de la previsualización
    ├── values/
    │   └── strings.xml            — Textos de la aplicación
    └── xml/
        └── provider_paths.xml     — Rutas permitidas para FileProvider
```

## Archivos fuente principales

### `MainActivity.kt`

| Elemento | Nombre | Propósito |
|---------|------|---------|
| Propiedad | `photoUri` | Guarda la URI entre el lanzamiento de la cámara y el resultado |
| Vista | `btnTakePhoto` | Botón que inicia el flujo de permisos + cámara |
| Vista | `tvPermissionsStatus` | Muestra el estado actual de los permisos al usuario |
| Launcher | `requestPermissionsLauncher` | Maneja el resultado de la solicitud de permisos |
| Launcher | `takePictureLauncher` | Maneja el resultado de la captura de la cámara |
| Método | `checkAndRequestPermissions()` | Punto de entrada: verifica y solicita permisos faltantes |
| Método | `buildPermissionsList()` | Construye la lista correcta de permisos según el nivel de API |
| Método | `launchCamera()` | Crea el archivo, obtiene la URI y lanza la cámara |
| Método | `createPhotoFile()` | Crea el archivo destino en `AndroidCoursePictures/` |
| Método | `navigateToPreview(uri)` | Inicia `PreviewActivity` con la URI como Extra |
| Método | `updatePermissionsStatus()` | Actualiza el `TextView` de estado al abrir la app |

### `PreviewActivity.kt`

| Elemento | Nombre | Propósito |
|---------|------|---------|
| Constante | `EXTRA_PHOTO_URI` | Clave para pasar la URI entre Activities |
| Vista | `ivCapturedPhoto` | `ImageView` que muestra la foto capturada |
| Vista | `tvFilePath` | Muestra la ruta real del archivo en disco |
| Vista | `btnBack` | Llama a `finish()` para regresar a `MainActivity` |
| Método | `loadPhotoFromIntent()` | Lee el extra de URI y carga la imagen |
| Método | `displayFilePath(uri)` | Reconstruye y muestra la ruta real del archivo |

---

## Conceptos clave

### ¿Qué es una Activity?

Una Activity representa **una pantalla** de la aplicación con su interfaz de usuario. Hereda de `AppCompatActivity` para tener compatibilidad con versiones antiguas de Android.

Ciclo de vida simplificado:

```
onCreate → onStart → onResume → [app visible y activa]
onPause  → onStop  → onDestroy → [app cerrada]
```

---

### Permisos en tiempo de ejecución

Android distingue dos tipos de permisos:

1. **Permisos normales**: se otorgan automáticamente al instalar.
2. **Permisos peligrosos**: el usuario DEBE aprobarlos mientras la app está en uso.

`CAMERA` y `WRITE_EXTERNAL_STORAGE` son permisos peligrosos.

Declararlos en el `AndroidManifest.xml` es el **primer paso obligatorio**. El **segundo paso** es solicitarlos al usuario desde el código Kotlin. Si no están declarados en el Manifest, nunca se podrán otorgar.

**API moderna (Activity Result API) vs deprecated:**

```kotlin
// ❌ Antes (deprecated):
requestPermissions(...)
override fun onRequestPermissionsResult(...) { ... }

// ✅ Ahora (moderno, desde AndroidX Activity 1.2):
val requestPermissionsLauncher = registerForActivityResult(
    ActivityResultContracts.RequestMultiplePermissions()
) { permissions -> ... }
```

> `registerForActivityResult` **DEBE** llamarse antes de que `onCreate()` complete. No puede llamarse dentro de un listener de click.

El callback recibe un `Map<String, Boolean>`:
- `key` = nombre del permiso (ej: `"android.permission.CAMERA"`)
- `value` = `true` si fue concedido, `false` si fue denegado

**`shouldShowRequestPermissionRationale()`**:
- `true` → el usuario denegó antes pero no marcó "no preguntar más". Podemos explicarle por qué necesitamos el permiso.
- `false` → el usuario marcó "no preguntar más" o es la primera vez. Debemos dirigirlo a Ajustes del sistema.

---

### Compatibilidad Scoped Storage

| API Level | Permiso para `getExternalFilesDir()` |
|-----------|--------------------------------------|
| 24 – 28   | `WRITE_EXTERNAL_STORAGE` requerido   |
| 29 – 32   | No se necesita permiso               |
| 33+       | No se necesita permiso               |

Como usamos `getExternalFilesDir()` (carpeta privada de la app), en API 29+ **no necesitamos ningún permiso de almacenamiento** para escribir. Solo lo pedimos en API ≤ 28.

`READ_MEDIA_IMAGES` y `READ_EXTERNAL_STORAGE` solo serían necesarios si quisiéramos acceder a fotos de **otras apps** (galería). No aplica en este proyecto.

---

### Intent implícito vs explícito

| Tipo | Descripción | Ejemplo |
|------|-------------|---------|
| **Implícito** | No especifica qué app debe manejar la acción. El sistema encuentra la app adecuada. | `Intent(MediaStore.ACTION_IMAGE_CAPTURE)` |
| **Explícito** | Especifica exactamente qué Activity abrir. | `Intent(this, PreviewActivity::class.java)` |

`ActivityResultContracts.TakePicture()` usa internamente:

```kotlin
val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
```

`MediaStore.EXTRA_OUTPUT` + la URI indica a la cámara **dónde guardar** la foto de alta resolución. Sin esto, la cámara solo devuelve una miniatura (thumbnail).

---

### FileProvider — URIs seguras

Desde Android 7 (API 24), Android **prohíbe** compartir URIs del tipo `file://` entre aplicaciones. Si se intenta, lanza `FileUriExposedException` y la app se cierra.

**La solución es FileProvider**: convierte una ruta de archivo local en una URI segura `content://` que el sistema puede compartir de forma controlada.

```
❌ SIN FileProvider:  file:///storage/emulated/0/photo.jpg
✅ CON FileProvider:  content://com.android.takephotoapp.fileprovider/my_images/photo.jpg
```

**Atributos importantes del `<provider>` en el Manifest:**

| Atributo | Valor | Por qué |
|----------|-------|---------|
| `android:authorities` | `${applicationId}.fileprovider` | Debe ser único globalmente |
| `android:exported` | `false` | FileProvider NO debe ser público |
| `android:grantUriPermissions` | `true` | Permite permisos temporales a otras apps (la cámara) |
| `android:resource` | `@xml/provider_paths` | Define qué carpetas puede compartir |

**`provider_paths.xml`** es la "lista blanca" de carpetas que FileProvider puede compartir. El atributo `name` es la parte pública de la URI (no revela la ruta real del archivo).

---

### `getExternalFilesDir()`

Devuelve la carpeta externa **privada** de la app. Ruta típica:

```
/storage/emulated/0/Android/data/com.android.takephotoapp/files/Pictures/
```

**Ventajas:**
- No necesita `WRITE_EXTERNAL_STORAGE` en API 29+
- Los archivos se eliminan automáticamente cuando se desinstala la app
- No aparece en la galería del sistema por defecto

Puede retornar `null` si el almacenamiento externo no está disponible (ej: tarjeta SD removida).

---

### `Uri` vs `File`

| | `File` | `Uri` |
|---|--------|-------|
| **Representa** | Ruta del sistema de archivos | Dirección segura a un recurso |
| **Ejemplo** | `/storage/emulated/0/.../photo.jpg` | `content://...fileprovider/.../photo.jpg` |
| **Compartible entre apps (API 24+)** | No ❌ | Sí ✅ |

**`Uri.fromFile(file)`** genera una URI `file://` — **prohibida** para compartir desde API 24.

**`FileProvider.getUriForFile(context, authority, file)`** genera una URI `content://` — **segura** para compartir.

---

### `findViewById` — Conectar XML con Kotlin

```kotlin
// En activity_main.xml:
// android:id="@+id/btnTakePhoto"

// En MainActivity.kt:
val button = findViewById<Button>(R.id.btnTakePhoto)
```

- `R` es una clase generada automáticamente por Android Studio con los IDs de todos los recursos.
- El genérico `<Button>` indica el tipo esperado. Si no coincide con el tipo real en el XML, lanza una excepción.
- Se usan con `lateinit` porque se inicializan en `onCreate()`, no en la declaración de la variable.

---

### Intent Extras — Pasar datos entre Activities

Los Extras son pares clave-valor para pasar datos entre Activities. Es como un "sobre" con información adicional.

```kotlin
// Enviar (en MainActivity):
val intent = Intent(this, PreviewActivity::class.java)
intent.putExtra(PreviewActivity.EXTRA_PHOTO_URI, uri.toString())
startActivity(intent)

// Recibir (en PreviewActivity):
val uriAsString = intent.getStringExtra(EXTRA_PHOTO_URI)
```

**Por qué usar constantes en lugar de Strings directos:**

```kotlin
// ❌ Malo (String mágico — propenso a errores tipográficos):
intent.putExtra("photo_uri", uri.toString())   // en MainActivity
intent.getStringExtra("photo_uri")             // en PreviewActivity

// ✅ Bueno (constante compartida en companion object):
intent.putExtra(PreviewActivity.EXTRA_PHOTO_URI, uri.toString())
intent.getStringExtra(PreviewActivity.EXTRA_PHOTO_URI)
```

**`companion object`** en Kotlin contiene miembros estáticos (equivalente a `static` en Java). Se accede con `NombreClase.CONSTANTE`.

---

### `setImageURI()` — Cargar imagen desde URI

```kotlin
ivCapturedPhoto.setImageURI(null) // Limpiar imagen previa (fuerza recarga)
ivCapturedPhoto.setImageURI(uri)  // Cargar la nueva imagen
```

El sistema abre el `ContentProvider` (FileProvider), lee el `InputStream` de la imagen y la decodifica en un `Bitmap` que se muestra en el `ImageView`.

> Para producción se recomienda **Glide** o **Coil** porque:
> - Manejan imágenes grandes sin `OutOfMemoryError`
> - Tienen caché automático
> - Operan en hilos secundarios (no bloquean la UI)

---

### `finish()` — Cerrar una Activity

```kotlin
btnBack.setOnClickListener {
    finish() // Cierra PreviewActivity y regresa a MainActivity
}
```

`finish()` cierra la Activity actual y la saca del back stack, regresando a la Activity anterior. Es equivalente a que el usuario presione el botón "Atrás".

---

## Errores comunes y soluciones

| Error | Causa | Solución |
|-------|-------|----------|
| `FileUriExposedException` | Usar `Uri.fromFile()` para compartir con otra app | Usar `FileProvider.getUriForFile()` |
| La foto sale en negro / vacía | `photoUri` no se guardó antes de lanzar la cámara | Declarar `photoUri` como variable de clase, no local |
| `NullPointerException` en `getExternalFilesDir()` | Almacenamiento externo no disponible | Verificar con `?: return null` |
| `IllegalArgumentException` en FileProvider | El `authority` no coincide con el del Manifest | Verificar que `"${packageName}.fileprovider"` sea igual en ambos lados |
| App no disponible para dispositivos sin cámara | `<uses-feature required="true">` | Cambiar a `android:required="false"` |
| Permisos no solicitados aunque están en el Manifest | `registerForActivityResult` llamado tarde | Mover fuera de listeners, a nivel de clase |

---

## Requisitos técnicos

- **Min SDK**: API 24 (Android 7.0 Nougat)
- **Target SDK**: API 36
- **Lenguaje**: Kotlin
- **Sin librerías externas** más allá de AndroidX
- **Sin permiso de internet** requerido
- **Sin ViewBinding / DataBinding**

## Dependencias

Todas las dependencias de AndroidX, sin librerías de terceros:

```toml
androidx.core:core-ktx:1.18.0
androidx.appcompat:appcompat:1.7.1
com.google.android.material:material:1.13.0
androidx.activity:activity:1.13.0
androidx.constraintlayout:constraintlayout:2.2.1
```
