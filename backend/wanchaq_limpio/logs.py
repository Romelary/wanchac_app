"""Filtro de registros que oculta los identificadores de dispositivo (docs/diseno.md §4.2, ADR-0002).

`idAnonimo` funciona como credencial de capacidad para dar de baja un reporte, y viaja en la URL del DELETE
(`/reportes/{idAnonimo}/{idLocal}`). Django registra la ruta de toda respuesta 4xx/5xx (p. ej. "Not Found: /reportes/…"),
así que sin este filtro el seudónimo acabaría en los registros de Render. Todo UUID se reemplaza por "[id]".
"""

import logging
import re

_UUID = re.compile(r"[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")


class OcultarIdentificadores(logging.Filter):
    def filter(self, record):
        mensaje = record.getMessage()
        redactado = _UUID.sub("[id]", mensaje)
        if redactado != mensaje:
            record.msg, record.args = redactado, ()
        # El traceback también puede llevar un UUID (p. ej. en el mensaje de una excepción): se formatea ya redactado.
        if record.exc_info and not record.exc_text:
            record.exc_text = _UUID.sub("[id]", logging.Formatter().formatException(record.exc_info))
        return True
