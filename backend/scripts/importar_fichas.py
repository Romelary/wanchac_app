import os
import sys
import csv
import django

# Añadir el directorio base al path (un nivel arriba de scripts/)
BASE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
sys.path.append(BASE_DIR)

os.environ.setdefault('DJANGO_SETTINGS_MODULE', 'wanchaq_limpio.settings')
django.setup()

from api.models import FichaObservacion

def str_to_bool(val):
    return str(val).strip().lower() in ('si', 'sí', 'true', '1', 'yes', 'y')

def importar_csv(ruta_archivo):
    if not os.path.exists(ruta_archivo):
        print(f"Error: No se encuentra el archivo {ruta_archivo}")
        return

    print(f"Leyendo archivo: {ruta_archivo}")
    with open(ruta_archivo, newline='', encoding='utf-8-sig') as csvfile:
        reader = csv.DictReader(csvfile)
        
        # Validar cabeceras esperadas
        expected_cols = {'codigo_agrupacion', 'confirmado'}
        if not expected_cols.issubset(set(reader.fieldnames)):
            print(f"Error: El CSV debe contener al menos las columnas: {expected_cols}")
            print(f"Columnas encontradas: {reader.fieldnames}")
            return

        count = 0
        for row in reader:
            foco_id = int(row['codigo_agrupacion'].strip())
            
            # Limpiar valores que puedan estar vacíos
            dist = row.get('distancia_m', '').strip()
            distancia_m = float(dist) if dist else None
            
            # El str_to_bool usa True por defecto para campos de auditoría si vienen vacíos
            foto_valida = str_to_bool(row.get('foto_valida', 'si')) if row.get('foto_valida') else True
            registro_completo = str_to_bool(row.get('registro_completo', 'si')) if row.get('registro_completo') else True
            
            FichaObservacion.objects.update_or_create(
                foco_id=foco_id,
                defaults={
                    'confirmado': str_to_bool(row.get('confirmado', '')),
                    'distancia_m': distancia_m,
                    'tipo_residuo': row.get('tipo_residuo', '').strip(),
                    'volumen': row.get('volumen', '').strip(),
                    'entorno': row.get('entorno', '').strip(),
                    'categoria_observador': row.get('categoria_observador', '').strip(),
                    'foto_valida': foto_valida,
                    'registro_completo': registro_completo,
                }
            )
            count += 1
            print(f"- Guardada/Actualizada observación para el Foco #{foco_id}")
            
    print(f"\n¡Éxito! Se importaron/actualizaron {count} fichas de observación en la base de datos.")

if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("Uso: python importar_fichas.py <ruta_al_archivo.csv>")
        sys.exit(1)
        
    importar_csv(sys.argv[1])
