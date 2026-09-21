import hashlib
import io
from pathlib import Path
import urllib.request
import zipfile

URL = "https://cdn.modrinth.com/data/lt2zd42r/versions/cR8uBzEl/palladium-4.5.9%2B1.20.1-forge.jar"
SHA512 = "0c44a6d30fbfd57c1c44231ebaf0ca43011cd9996bcf2ae3d789228db7206f047b430ab2f2c261895677e2b7244a79968c156c41e61959e2d5613f89b884aca4"
TARGET = Path(__file__).resolve().parents[1] / "build/test-dependencies"
NESTED = {
    "palladiumcore-1.0.1.jar": "META-INF/jars/palladiumcore-forge-1.0.1+1.20.1-forge.jar",
    "player-animation-1.0.2.jar": "META-INF/jars/player-animation-lib-forge-1.0.2-rc1+1.20.jar",
    "mixinextras-0.2.0.jar": "META-INF/jars/mixinextras-forge-0.2.0.jar",
}


def prepare(data):
    if hashlib.sha512(data).hexdigest() != SHA512:
        raise ValueError("Palladium download does not match the audited 4.5.9 JAR")
    TARGET.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(io.BytesIO(data)) as original:
        for filename, entry in NESTED.items():
            (TARGET / filename).write_bytes(original.read(entry))
        with zipfile.ZipFile(TARGET / "palladium-4.5.9.jar", "w", zipfile.ZIP_DEFLATED) as development:
            for entry in original.infolist():
                if not entry.filename.startswith(("META-INF/jarjar/", "META-INF/jars/")):
                    development.writestr(entry, original.read(entry))


if __name__ == "__main__":
    with urllib.request.urlopen(URL, timeout=60) as response:
        prepare(response.read())
