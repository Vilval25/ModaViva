"""Busca claves y secretos dentro de un APK (HT-02 CA-06, Definition of Done).

    python scripts/verificar_apk.py app/build/outputs/apk/release/app-release-unsigned.apk

Abre el APK (es un zip), recorre el código compilado (classes*.dex), los
recursos y los assets, y busca patrones de claves de servicios externos: IA,
LLM, pasarelas de pago, nubes y claves privadas. Termina con código 1 si
encuentra alguno.

La clave API de Firebase (AIza..., en google-services.json y en el recurso
google_api_key) se informa pero no cuenta como hallazgo: Firebase la exige en
la app, solo identifica el proyecto y no da acceso por sí misma. El acceso lo
controlan las reglas de seguridad (firestore.rules, storage.rules).
"""

import re
import sys
import zipfile

PATRONES = {
    # Exige el cuerpo en base64: las librerías de red (gRPC) incluyen los
    # encabezados BEGIN/END sueltos como constantes para leer certificados.
    "clave privada (PEM)": rb"-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----\s*[A-Za-z0-9+/=\s]{100,}",
    "cuenta de servicio de Google": rb"\"type\"\s*:\s*\"service_account\"|private_key_id",
    "OpenAI": rb"sk-(?:proj-)?[A-Za-z0-9_-]{20,}",
    "Anthropic": rb"sk-ant-[A-Za-z0-9_-]{20,}",
    "Stripe": rb"(?:sk|rk)_(?:live|test)_[A-Za-z0-9]{16,}",
    "AWS": rb"AKIA[0-9A-Z]{16}",
    "GitHub": rb"gh[pousr]_[A-Za-z0-9]{30,}",
    "Slack": rb"xox[baprs]-[A-Za-z0-9-]{10,}",
    "Hugging Face": rb"hf_[A-Za-z0-9]{30,}",
    "Replicate": rb"r8_[A-Za-z0-9]{30,}",
    "Pixabay": rb"\b\d{8}-[0-9a-f]{25}\b",
    "asignación de secreto": rb"(?i)(?:secret|api_?key|access_?token|client_?secret)\s*[:=]\s*[\"'][^\"'\s]{12,}[\"']",
}
FIREBASE_API_KEY = re.compile(rb"AIza[0-9A-Za-z_-]{35}")


def revisar(ruta_apk: str) -> int:
    hallazgos = []
    claves_firebase = set()
    with zipfile.ZipFile(ruta_apk) as apk:
        entradas = [n for n in apk.namelist() if not n.endswith("/")]
        for nombre in entradas:
            contenido = apk.read(nombre)
            for etiqueta, patron in PATRONES.items():
                for m in re.finditer(patron, contenido):
                    muestra = m.group(0)[:12].decode("latin-1") + "…"
                    hallazgos.append(f"{nombre}: {etiqueta} ({muestra})")
            claves_firebase.update(k.decode() for k in FIREBASE_API_KEY.findall(contenido))

    print(f"APK: {ruta_apk}")
    print(f"Archivos revisados: {len(entradas)}")
    print(f"Clave API de Firebase presente (esperada, no es secreta): {'sí' if claves_firebase else 'no'}"
          f"{f' — {len(claves_firebase)} distinta(s)' if claves_firebase else ''}")
    if hallazgos:
        print(f"\nPOSIBLES SECRETOS ({len(hallazgos)}):")
        for h in hallazgos:
            print(f"  {h}")
        return 1
    print("\nSin claves de servicios externos ni secretos.")
    return 0


if __name__ == "__main__":
    if len(sys.argv) != 2:
        sys.exit(__doc__)
    sys.exit(revisar(sys.argv[1]))
