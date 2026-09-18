# ADR-0002: Se mantiene la eliminación de reportes pese a que la app no tiene login

- **Estado**: Aceptado
- **Fecha**: 2026-09-18

## Contexto

Al planificar el Módulo 6 ("Mis reportes"), el usuario planteó quitar la opción de eliminar reportes con este razonamiento: *como la app no tiene login (ADR-0001), el dato queda completamente anónimo, así que no haría falta poder borrarlo.*

Se evaluó la propuesta contra lo ya comprometido en el documento de investigación y contra cómo son realmente los datos.

### Hallazgos

1. **La app ya promete la eliminación al usuario.** El texto del Anexo 9 (consentimiento informado, ya validado por juicio de expertos) y la pantalla de onboarding implementada en el Módulo 1 dicen: *"Puede eliminar sus reportes desde la aplicación en cualquier momento y solicitar el acceso, la rectificación o la cancelación de sus datos…, conforme a la Ley N.º 29733"* (`strings.xml`, `onboarding_derechos_texto`). Sin la función, la app contradiría su propio consentimiento, y eso invalida el consentimiento como "informado". La eliminación también aparece en la Ética (3.4, "Derechos del titular"), en RNF-05, en el Módulo 7 ("borrar todos mis datos") y en lo que se iba a declarar en el formulario de Data Safety de Google Play.
2. **Los datos son seudónimos, no anónimos.** No hay nombre, correo ni teléfono (correcto y valioso), pero cada reporte incluye ubicación GPS precisa, fecha y hora, una fotografía y un `id_anonimo` persistente que enlaza todos los reportes del mismo teléfono. Una traza de ubicaciones ligada a un identificador estable puede hacer identificable a una persona, más aún en un distrito pequeño como Wanchaq. La Ley N.º 29733, que el propio marco legal de la tesis cita, define dato personal como el que *identifica o hace identificable* a una persona. Además, la Ética reconoce que una foto puede capturar rostros o placas, lo que introduce datos personales aunque no se pidan. (Esta lectura jurídica conviene confirmarla con el asesor; no es asesoría legal.)

## Opciones consideradas

- **A. Mantener la eliminación, versión mínima** — botón eliminar en "Mis reportes": borra la copia local (y su foto) y solicita la baja al backend.
- **B. Quitar el botón y pasar a solicitud por correo** — exigiría editar el Anexo 9 (instrumento validado), registrar una corrección y consultar al asesor.
- **C. Quitar la eliminación sin alternativa** — habría que reescribir Ética y Anexo 9; la opción más débil legal y éticamente.

## Decisión

Se adopta la **opción A**. En el Módulo 6:

- "Mis reportes" lista los reportes del dispositivo (por `id_anonimo`) y permite eliminarlos previa confirmación explícita.
- Eliminar borra el registro local **y la foto del almacenamiento de la app** (la foto es evidencia; no debe quedar huérfana).
- Se encola siempre una solicitud de baja al backend (`DELETE /reportes/{idAnonimo}/{idLocal}`) mediante WorkManager, con reintento y backoff, aunque el reporte figure como `PENDIENTE`: el servidor pudo haberlo recibido y la respuesta perderse por red inestable, y el cliente no puede saberlo. Un `404` cuenta como éxito (el dato ya no existe).
- El Módulo 7 reutilizará este mismo mecanismo para "borrar todos mis datos".

## Consecuencias

**Positivas**
- El consentimiento validado sigue siendo veraz: la app cumple lo que promete.
- No se toca ningún instrumento validado, así que no se requiere re-validación con los expertos.
- Coherencia con la Ley N.º 29733 y con lo que se declarará en Google Play.

**Límites asumidos (deben quedar en la tesis, no ocultarse)**
- **Reinstalación**: al reinstalar la app se pierde el `id_anonimo` y, con él, la capacidad de eliminar los reportes anteriores desde la app (consecuencia ya aceptada en ADR-0001). Esos reportes solo podrían darse de baja por el canal de contacto del Anexo 9, si la persona conserva su código.
- **Sin autenticación**: el par `(idAnonimo, idLocal)` actúa como credencial de capacidad para la baja. Es aceptable porque `idAnonimo` es un UUID aleatorio que nunca se expone públicamente (el mapa público muestra solo `Punto_critico`, no reportes individuales), pero debe documentarse como limitación de seguridad.
- **Datos ya agrupados o verificados**: eliminar un reporte después de haber sido usado por el agrupamiento (DBSCAN) o la verificación en campo puede alterar el conjunto de datos del estudio. **Pendiente de decidir con el asesor** una política explícita (por ejemplo: las eliminaciones posteriores al cierre de la ventana de observación no modifican resultados ya publicados, pero se registran). No es motivo para quitar la función; sí para definir la política.
- **Eliminación remota "best effort"**: depende de que exista conectividad y de que el backend implemente el contrato de `diseno.md` §4.2 (baja idempotente con lápida). Hasta que el backend exista, la solicitud queda encolada.

## Alcance

Afecta a `plan.md` (Módulos 6 y 7), `diseno.md` (§4.2, contrato de la API), `pruebas.md` y al código del Módulo 6. **No** modifica el documento de investigación ni el Anexo 9. Ver además la observación sobre el término "anónimo" en `correcciones-documento.md`.
