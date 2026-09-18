# ADR-0001: La app no tiene login; usa un identificador anónimo generado en el dispositivo

- **Estado**: Aceptado
- **Fecha**: 2026-09-17

## Contexto

El Capítulo IV del documento de investigación (pantalla de inicio, entidad `Usuario`, historias de usuario) describía un flujo de registro/login con nombre completo, correo electrónico y contraseña, y una historia de usuario de "cerrar sesión".

Al revisar el diseño contra los Capítulos I–III del mismo documento, se encontró una contradicción directa:

- El apartado de Ética (3.4) establece minimización de datos: *"no se solicita nombre real, documento de identidad ni número telefónico"*, y que *"cada usuario recibe un identificador anónimo generado en el dispositivo"*.
- El Anexo 9 (consentimiento informado), ya validado por juicio de expertos, está redactado asumiendo ese modelo anónimo.
- El concepto de "cerrar sesión" solo tiene sentido si existe un login con credenciales previo — que el propio marco ético del estudio descarta.

## Decisión

El cliente Android **no implementa login ni cuentas de usuario**. En su lugar:

- Al aceptar el consentimiento informado (Módulo 1), se genera un UUID v4 localmente en el dispositivo (`id_anonimo`).
- Ese identificador se usa únicamente para: (a) el filtro de reportes duplicados del mismo dispositivo (< 30 min, < 20 m), y (b) la función "Mis reportes" / eliminación de datos propios.
- No se recolecta nombre, correo, teléfono ni contraseña en ningún flujo.
- Se elimina la historia de usuario "cerrar sesión" y se reemplaza por "eliminar mis reportes / revocar mi consentimiento" (ya prevista en Ética como derecho del titular).

## Consecuencias

**Positivas**
- Consistencia total con el Anexo 9 ya validado — no se requiere revalidar el instrumento con los expertos.
- Menor fricción de uso, alineado con el argumento central de la tesis (ciencia ciudadana, participación de bajo costo).
- Menor superficie de desarrollo: no hay backend de autenticación, recuperación de contraseña ni gestión de sesiones.
- Cumple mejor el principio de proporcionalidad de la Ley N.º 29733.

**Negativas / trade-offs asumidos**
- Si el usuario reinstala la app o cambia de dispositivo, pierde el vínculo con sus reportes anteriores (el `id_anonimo` no se puede recuperar). Se acepta este costo porque el caso de uso principal es "reportar y listo", no mantener un historial personal a largo plazo.
- No hay mecanismo de moderación por cuenta (p. ej., banear a un usuario reincidente) — si esto se vuelve necesario, se abordaría por dispositivo/instalación, no por cuenta personal, para no reintroducir PII.

## Alcance de la decisión

Esta decisión afecta al diseño técnico documentado en `diseno.md` (§0, punto 1 y 3) y al plan de `plan.md` (Módulo 1). **No se ha modificado aún el documento de investigación (`.docx`) entregado** — queda pendiente decidir si se actualiza el Capítulo IV para reflejar este ajuste, de modo que la tesis y el sistema construido queden alineados.
