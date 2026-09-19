#!/usr/bin/env python
"""Utilidad de línea de comandos de Django (migraciones, pruebas, servidor de desarrollo)."""

import os
import sys


def main():
    if sys.argv[1:2] == ["test"]:
        # Las pruebas NUNCA deben leer tu .env ni tocar la base real (ver settings_test.py).
        os.environ["DJANGO_SETTINGS_MODULE"] = "wanchaq_limpio.settings_test"
    else:
        os.environ.setdefault("DJANGO_SETTINGS_MODULE", "wanchaq_limpio.settings")
    from django.core.management import execute_from_command_line

    execute_from_command_line(sys.argv)


if __name__ == "__main__":
    main()
