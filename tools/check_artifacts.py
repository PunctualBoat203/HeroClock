from pathlib import Path
import zipfile


def check():
    directory = Path(__file__).resolve().parents[1] / "build/libs"
    api_paths = list(directory.glob("*-api.jar"))
    runtime_paths = [path for path in directory.glob("*.jar") if path not in api_paths]
    assert len(api_paths) == len(runtime_paths) == 1, "Expected one runtime and one API artifact"
    api_path = api_paths[0]
    runtime_path = runtime_paths[0]
    embedded_api = f"META-INF/heroclock/{api_path.name}"
    with zipfile.ZipFile(api_path) as api, zipfile.ZipFile(runtime_path) as runtime:
        names = {name for name in api.namelist() if not name.endswith("/")}
        classes = {name for name in names if name.endswith(".class")}
        assert classes and all(name.startswith("com/heroclock/api/") for name in classes)
        assert names == classes | {"META-INF/MANIFEST.MF"}, "Implementation or mod resources leaked into API artifact"
        for name in ("HeroClockAPI", "HeroWorkAPI", "HeroFunctionAPI", "HeroIntegrationAPI", "HeroScriptAPI"):
            assert f"com/heroclock/api/{name}.class" in classes
        runtime_names = set(runtime.namelist())
        assert classes <= runtime_names, "Runtime does not provide the advertised API"
        assert embedded_api in runtime_names, "Runtime does not contain the extractable API artifact"
        assert runtime.read(embedded_api) == api_path.read_bytes(), "Embedded API differs from standalone API artifact"
        assert "META-INF/jarjar/metadata.json" not in runtime_names, "Embedded API must remain inert, not a JarJar dependency"
        assert b'authors="PunctualBoat"' in runtime.read("META-INF/mods.toml")
        assert "heroclock.refmap.json" in runtime_names
        assert "compatibility/contracts.json" in runtime_names
        assert not any(name.startswith(("com/heroclock/gametest/", "patches/")) for name in runtime_names)
        for archive in (api, runtime):
            assert b"Implementation-Vendor: PunctualBoat" in archive.read("META-INF/MANIFEST.MF")
    print("Runtime, standalone API, and inert embedded API boundaries verified")


if __name__ == "__main__":
    check()
