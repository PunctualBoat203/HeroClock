import hashlib
from pathlib import Path
import urllib.request

TARGET = Path(__file__).resolve().parents[1] / 'build/test-dependencies'
INPUTS = (
    ('kubejs-2001.6.5-build.26.jar', 'https://cdn.modrinth.com/data/umyGl7zF/versions/hVR2xUSr/kubejs-forge-2001.6.5-build.26.jar', '1769312192fbf9d72f45054ba61130523bfb471b5f480a4bff8c210ec09400bb'),
    ('rhino-2001.2.3-build.10.jar', 'https://cdn.modrinth.com/data/sk9knFPE/versions/uNALdylI/rhino-forge-2001.2.3-build.10.jar', 'fed2211429301bf043864183cab9ab8e92d4cc4dbb9e488ce6c75217c54584a6'),
    ('architectury-9.2.14.jar', 'https://maven.architectury.dev/dev/architectury/architectury-forge/9.2.14/architectury-forge-9.2.14.jar', '47d5eca3d83aae1ac1d4a70116727715bd7ef4c077d228fee873065cbca94687'),
)


def prepare():
    TARGET.mkdir(parents=True, exist_ok=True)
    for name, url, digest in INPUTS:
        target = TARGET / name
        if target.exists() and hashlib.sha256(target.read_bytes()).hexdigest() == digest:
            continue
        with urllib.request.urlopen(url, timeout=60) as response:
            data = response.read()
        if hashlib.sha256(data).hexdigest() != digest:
            raise ValueError('Unverified scripting dependency: ' + name)
        target.write_bytes(data)


if __name__ == '__main__':
    prepare()
