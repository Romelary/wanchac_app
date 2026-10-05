import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)
    id("com.google.gms.google-services")
}

// Valores de CONFIGURACIÓN LOCAL: se leen de `local.properties` (ignorado por git) o de -P<nombre> en la línea de
// comandos. `local.properties` es el sitio de la clave de Google Maps: NUNCA va a git ni al chat (docs/despliegue-mapa.md).
val propiedadesLocales =
    Properties().apply {
        val archivo = rootProject.file("local.properties")
        if (archivo.exists()) archivo.inputStream().use { load(it) }
    }

fun propiedadLocal(nombre: String): String? {
    val valor = providers.gradleProperty(nombre).orNull ?: propiedadesLocales.getProperty(nombre)
    return valor?.trim()?.takeIf { it.isNotEmpty() }
}

// Dirección del backend (Render). NO es un secreto, pero cambia por entorno: depuración usa por defecto la del emulador
// (10.0.2.2 = el PC anfitrión); para probar contra Render o con un teléfono real se define `baseUrl` en local.properties.
// Release EXIGE una dirección https (ver verificarDireccionDelServidor, abajo). Retrofit exige la "/" final.
val baseUrl: String? = propiedadLocal("baseUrl")?.trimEnd('/')?.plus("/")

// Clave de Google Maps SDK para Android (SECRETO de facturación: va restringida a este paquete y a su huella SHA-1).
// Sin ella la app compila y funciona, pero el mapa se ve gris (en depuración se avisa en pantalla).
val claveDeMaps: String? = propiedadLocal("MAPS_API_KEY")

// Modo de prueba: ubicación SIMULADA dentro de Wanchaq, para probar el reporte en un dispositivo que no está en el
// distrito o sin buena señal GPS (RF-02 rechaza a propósito lo que no cumple). SOLO existe en compilaciones de
// depuración y está apagado por defecto: se activa con -PubicacionSimulada=true (ver docs/prueba-en-dispositivo.md).
// Nunca puede llegar a una versión de publicación: release fija la bandera en false y, además, assembleRelease y
// bundleRelease FALLAN si la propiedad está activa (ver verificarSinUbicacionSimulada, abajo).
val ubicacionSimulada = providers.gradleProperty("ubicacionSimulada").map { it.toBoolean() }.getOrElse(false)
val modoPrueba = providers.gradleProperty("modoPrueba").map { it.toBoolean() }.getOrElse(false)

// Firma de la versión de publicación/distribución (el APK que se reparte al equipo). Los cuatro datos de la llave los
// escribe el USUARIO en local.properties (nunca en git ni en el chat; ver docs/distribucion-apk.md). Sin ellos,
// assembleRelease y bundleRelease FALLAN con un mensaje claro (ver verificarFirmaDeLaVersion, abajo): un APK sin firmar no
// se puede instalar, y firmarlo con la llave de depuración (la de cada PC) impediría actualizarlo después.
val datosDeFirma =
    listOf("RELEASE_STORE_FILE", "RELEASE_STORE_PASSWORD", "RELEASE_KEY_ALIAS", "RELEASE_KEY_PASSWORD")
        .associateWith { propiedadLocal(it) }
val firmaCompleta = datosDeFirma.values.all { it != null }

android {
    namespace = "com.wanchaqlimpio.app"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.wanchaqlimpio.app"
        minSdk = 26
        targetSdk = 36
        // Cada APK que se reparte debe tener un versionCode MAYOR que el anterior, o Android no lo instala encima
        // (-PversionCode=2). Por defecto 1.
        versionCode = providers.gradleProperty("versionCode").map { it.toInt() }.getOrElse(1)
        versionName = providers.gradleProperty("versionName").getOrElse("1.0")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Dirección del backend: `baseUrl` de local.properties, o la del emulador si no se define (ver arriba).
        buildConfigField("String", "BASE_URL", "\"${baseUrl ?: "http://10.0.2.2:8000/"}\"")
        buildConfigField("boolean", "MODO_PRUEBA", "$modoPrueba")
        
        val appName = if (modoPrueba) "Wanchaq Limpio (prueba)" else "@string/app_name"
        manifestPlaceholders["APP_NAME"] = appName
        
        // La clave de Maps se inyecta en el manifiesto (meta-data com.google.android.geo.API_KEY); nunca se escribe en el
        // código. La bandera solo sirve para AVISAR en pantalla, en depuración, que falta configurarla.
        manifestPlaceholders["MAPS_API_KEY"] = claveDeMaps ?: "CLAVE_DE_MAPS_NO_CONFIGURADA"
        buildConfigField("boolean", "MAPS_CLAVE_CONFIGURADA", "${claveDeMaps != null}")

        // Correo de contacto público que se muestra en Ajustes (definido en gradle.properties).
        buildConfigField("String", "CONTACT_EMAIL", "\"${providers.gradleProperty("contactEmail").getOrElse("")}\"")
    }

    signingConfigs {
        create("release") {
            if (firmaCompleta) {
                storeFile = file(datosDeFirma.getValue("RELEASE_STORE_FILE")!!)
                storePassword = datosDeFirma.getValue("RELEASE_STORE_PASSWORD")
                keyAlias = datosDeFirma.getValue("RELEASE_KEY_ALIAS")
                keyPassword = datosDeFirma.getValue("RELEASE_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        debug {
            buildConfigField("boolean", "UBICACION_SIMULADA", "$ubicacionSimulada")
            // Visible en Ajustes → "Versión 1.0-ubicacion-simulada": nadie puede olvidar que la ubicación es falsa.
            if (ubicacionSimulada) versionNameSuffix = "-ubicacion-simulada"
        }
        release {
            // Publicación: NUNCA la dirección del emulador. Sin `baseUrl` la cadena queda vacía y assembleRelease falla.
            buildConfigField("String", "BASE_URL", "\"${baseUrl ?: ""}\"")
            buildConfigField("boolean", "UBICACION_SIMULADA", "false")
            if (firmaCompleta) signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    // El modelo .tflite se lee con memoria mapeada (mmap): no debe comprimirse dentro del APK.
    androidResources {
        noCompress += "tflite"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            all {
                // Robolectric (usado por ConsentimientoRepositoryImplTest) necesita
                // acceso a internos del JDK que estan cerrados por defecto desde Java 17+.
                it.jvmArgs(
                    "--add-opens=java.base/java.lang=ALL-UNNAMED",
                    "--add-opens=java.base/java.util=ALL-UNNAMED",
                    "--add-opens=java.base/java.io=ALL-UNNAMED",
                    "--add-opens=java.base/java.net=ALL-UNNAMED",
                    "--add-opens=java.base/java.security=ALL-UNNAMED",
                    "--add-opens=java.base/java.text=ALL-UNNAMED",
                    "--add-opens=java.base/jdk.internal.access=ALL-UNNAMED",
                    "--add-opens=java.desktop/java.awt.font=ALL-UNNAMED",
                    "--add-opens=jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED",
                )
            }
        }
    }
}

dependencies {
    implementation(platform("com.google.firebase:firebase-bom:34.19.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.material3.adaptive.navigation.suite)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    // Inyección de dependencias (ver docs/plan.md #1.1)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    // Persistencia local offline-first (Módulo 2, placeholder por ahora)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // Comunicación REST con el backend (Módulo 4, apunta a BASE_URL mock por ahora)
    implementation(libs.retrofit.core)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp.logging.interceptor)

    // Corrutinas
    implementation(libs.kotlinx.coroutines.android)

    // Onboarding / consentimiento (Módulo 1)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    // Captura y ubicación (Módulo 2)
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.play.services.location)

    // Clasificación de residuos on-device (Módulo 3b, ver ADR-0003)
    implementation(libs.litert)

    // Mapa público de focos confirmados (Módulo 5, ver ADR-0005)
    implementation(libs.maps.compose)
    implementation(libs.maps.compose.utils)

    // Sincronización diferida (Módulo 4)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.hilt.work)
    implementation(libs.gson)
    ksp(libs.androidx.hilt.compiler)

    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.androidx.room.testing)
    testImplementation(libs.robolectric)
    // Compose UI Testing en la JVM (con Robolectric): pruebas de adaptación a distintos tamaños de pantalla
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
    // ApplicationProvider (necesario para Room in-memory con Robolectric)
    testImplementation(libs.androidx.junit)
    testImplementation(libs.androidx.work.testing)
    testImplementation(libs.okhttp.mockwebserver3)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.mockk)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}

ktlint {
    android.set(true)
}

detekt {
    buildUponDefaultConfig = true
    parallel = true
    config.setFrom(files("$rootDir/config/detekt/detekt.yml"))
}

// Guardia de publicación: el consentimiento (Anexo 9) promete un "correo de contacto" para
// que el usuario ejerza sus derechos sobre sus datos (Ley N.º 29733). Publicar sin él
// dejaría esa promesa vacía, así que la compilación release falla hasta que se defina
// contactEmail en gradle.properties. Las compilaciones debug no se ven afectadas.
val verificarCorreoContacto by tasks.registering {
    val correo = providers.gradleProperty("contactEmail").orElse("")
    doLast {
        check(correo.get().isNotBlank()) {
            "Falta 'contactEmail' en gradle.properties: no se puede compilar release sin el correo " +
                "de contacto que promete el consentimiento informado (Anexo 9)."
        }
    }
}

// Segundo seguro (el primero es que release fija UBICACION_SIMULADA en false): que quien compile una versión de
// publicación con la propiedad activa se entere, en vez de que el modo de prueba pase inadvertido.
val verificarSinUbicacionSimulada by tasks.registering {
    val activa = providers.gradleProperty("ubicacionSimulada").orElse("false")
    doLast {
        check(!activa.get().toBoolean()) {
            "'ubicacionSimulada' está activa: es un modo de PRUEBA y no debe usarse al compilar una versión de " +
                "publicación. Quita -PubicacionSimulada (o esa línea de tu gradle.properties de usuario)."
        }
    }
}

// Una versión de publicación que apunta al emulador (o a http sin cifrar) no podría hablar con el servidor real.
val verificarDireccionDelServidor by tasks.registering {
    val direccion = providers.provider { baseUrl.orEmpty() }
    doLast {
        check(direccion.get().startsWith("https://")) {
            "Falta 'baseUrl' en local.properties (o -PbaseUrl=...), o no es https: no se puede compilar release " +
                "sin la dirección HTTPS del servidor. Ejemplo: baseUrl=https://tu-servicio.onrender.com/"
        }
    }
}

// Una versión de publicación sin clave de Maps mostraría el mapa gris a todos los ciudadanos.
val verificarClaveDeMaps by tasks.registering {
    val clave = providers.provider { claveDeMaps.orEmpty() }
    doLast {
        check(clave.get().isNotBlank()) {
            "Falta 'MAPS_API_KEY' en local.properties: no se puede compilar release sin la clave de Google Maps " +
                "(restringida a com.wanchaqlimpio.app y a la huella SHA-1 de la firma de publicación). Ver docs/despliegue-mapa.md."
        }
    }
}

// Sin llave de firma no hay APK instalable. La llave se crea UNA vez, se guarda FUERA del repositorio y con copia de
// seguridad: si se pierde, los teléfonos que ya tienen la app no podrán actualizarla (habría que desinstalarla).
val verificarFirmaDeLaVersion by tasks.registering {
    val faltantes = providers.provider { datosDeFirma.filterValues { it == null }.keys.toList() }
    val archivoDeLlave = providers.provider { datosDeFirma["RELEASE_STORE_FILE"]?.let { file(it) } }
    doLast {
        check(faltantes.get().isEmpty()) {
            "Falta la firma de la versión de publicación: define en local.properties " +
                "${faltantes.get().joinToString()} (la llave se crea una vez con keytool; ver docs/distribucion-apk.md)."
        }
        check(archivoDeLlave.get()?.isFile == true) {
            "El archivo de llave RELEASE_STORE_FILE no existe: ${archivoDeLlave.get()}. " +
                "Revisa la ruta en local.properties."
        }
    }
}

tasks
    .matching { it.name == "assembleRelease" || it.name == "bundleRelease" }
    .configureEach {
        dependsOn(
            verificarCorreoContacto,
            verificarSinUbicacionSimulada,
            verificarDireccionDelServidor,
            verificarClaveDeMaps,
            verificarFirmaDeLaVersion,
        )
    }
