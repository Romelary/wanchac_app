import os
from PIL import Image
import glob

icono_dir = r"C:\Users\romel\wanchac_app_clone\icono"
capturas_dir = os.path.join(icono_dir, "capturas")

# 1. Convert Icon to 32-bit PNG (RGBA)
icon_files = glob.glob(os.path.join(icono_dir, "icono_512x512_store.*"))
for icon_path in icon_files:
    if "32bit" in icon_path: continue
    print(f"Processing icon: {icon_path}")
    try:
        img = Image.open(icon_path)
        img = img.convert("RGBA")
        new_icon_path = os.path.join(icono_dir, "icono_512x512_store_32bit.png")
        img.save(new_icon_path, format="PNG")
        print(f"Saved 32-bit icon to: {new_icon_path}")
    except Exception as e:
        print(f"Error processing icon: {e}")

# 2. Convert Screenshots to JPEG (24-bit, no alpha)
screenshot_files = glob.glob(os.path.join(capturas_dir, "*.png"))
for screenshot_path in screenshot_files:
    print(f"Processing screenshot: {screenshot_path}")
    try:
        img = Image.open(screenshot_path)
        img = img.convert("RGB") # Removes alpha channel
        base_name = os.path.splitext(os.path.basename(screenshot_path))[0]
        new_screenshot_path = os.path.join(capturas_dir, f"{base_name}.jpg")
        img.save(new_screenshot_path, format="JPEG", quality=95)
        print(f"Saved screenshot to: {new_screenshot_path}")
    except Exception as e:
        print(f"Error processing screenshot: {e}")
