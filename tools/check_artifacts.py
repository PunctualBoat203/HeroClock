from pathlib import Path
import zipfile


def check():
    directory = Path(__file__).resolve().parents[1] / "build/libs"
    api_paths = list(directory.glob("*-api.jar"))
    runtime_paths = [path for path in directory.glob("*.jar") if path not in api_paths]
    assert len(api_paths) == len(runtime_paths) == 1, "Expected one runtime and one API artifact"
    with zipfile.ZipFile(api_paths[0]) as api, zipfile.ZipFile(runtime_paths[0]) as runtime:
        names = {name for name in api.namelist() if not name.endswith("/")}
        classes = {name for name in names if name.endswith(".class")}
        assert classes and all(name.startswith("com/heroclock/api/") for name in classes)
        assert names == classes | {"META-INF/MANIFEST.MF"}, "Implementation or mod resources leaked into API artifact"
        for name in ("HeroClockAPI", "HeroWorkAPI", "HeroFunctionAPI", "HeroIntegrationAPI"):
            assert f"com/heroclock/api/{name}.class" in classes
        assert classes <= set(runtime.namelist()), "Runtime does not provide the advertised API"
        assert b'authors="PunctualBoat"' in runtime.read("META-INF/mods.toml")
        assert "heroclock.refmap.json" in runtime.namelist()
        assert "compatibility/contracts.json" in runtime.namelist()
        assert not any(name.startswith(("com/heroclock/gametest/", "patches/")) for name in runtime.namelist())
        for archive in (api, runtime):
            assert b"Implementation-Vendor: PunctualBoat" in archive.read("META-INF/MANIFEST.MF")
    print("Runtime and API artifact boundaries verified")


if __name__ == "__main__":
    check()
