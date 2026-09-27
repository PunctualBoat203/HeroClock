import copy
import hashlib
import io
import struct
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
        for variant in ("palladium", "palladium-relabeled", "palladium-changed"):
            with zipfile.ZipFile(TARGET / (variant + "-4.5.9.jar"), "w", zipfile.ZIP_DEFLATED) as development:
                for entry in original.infolist():
                    if entry.filename.startswith(("META-INF/jarjar/", "META-INF/jars/")):
                        continue
                    content = original.read(entry)
                    if variant == "palladium-relabeled" and entry.filename == "META-INF/mods.toml":
                        assert b'version = "4.5.9"' in content
                        content = content.replace(b'version = "4.5.9"', b'version = "99.0.0"', 1)
                    if variant == "palladium-changed" and entry.filename == "net/threetag/palladium/power/PowerHandler.class":
                        content = rename_constant(content, b"powers", b"heroclock_fixture_powers")
                    development.writestr(copy.copy(entry), content)


def rename_constant(data, old, new):
    assert data[:4] == b"\xca\xfe\xba\xbe"
    count = struct.unpack_from(">H", data, 8)[0]
    result = bytearray(data[:10])
    cursor, index, replaced = 10, 1, False
    sizes = {3: 4, 4: 4, 5: 8, 6: 8, 7: 2, 8: 2, 9: 4, 10: 4,
             11: 4, 12: 4, 15: 3, 16: 2, 17: 4, 18: 4, 19: 2, 20: 2}
    while index < count:
        tag = data[cursor]
        result.append(tag)
        cursor += 1
        if tag == 1:
            size = struct.unpack_from(">H", data, cursor)[0]
            content = data[cursor + 2:cursor + 2 + size]
            cursor += 2 + size
            if content == old:
                content, replaced = new, True
            result += struct.pack(">H", len(content)) + content
        else:
            size = sizes[tag]
            result += data[cursor:cursor + size]
            cursor += size
            if tag in (5, 6):
                index += 1
        index += 1
    assert replaced
    return bytes(result) + data[cursor:]


def prepare_curios():
    url = "https://maven.theillusivec4.top/top/theillusivec4/curios/curios-forge/5.14.1+1.20.1/curios-forge-5.14.1+1.20.1.jar"
    with urllib.request.urlopen(url, timeout=60) as response:
        data = response.read()
    if hashlib.sha256(data).hexdigest() != "6d77ae8ad532fdf303390f404b0081b3b4ac4f61e7f1f4b4d4a9077e132dae4f":
        raise ValueError("Curios download does not match the audited method-contract input")
    (TARGET / "curios-5.14.1.jar").write_bytes(data)



if __name__ == "__main__":
    with urllib.request.urlopen(URL, timeout=60) as response:
        prepare(response.read())
    prepare_curios()
