# Downstream API Notes

This note is for repos that consume the Modular Machinery: Community Edition More Extensions API branch instead of editing Modular Machinery: Community Edition More Extensions sources directly.

## Recommended upstream order

1. Check out the Modular Machinery: Community Edition More Extensions repository from the remote source.
2. Prefer the active API branch. At the moment this work is based on `feature/horizontal-slot-scroll`; switch this note and downstream CI to `api` only after that branch exists remotely.
3. Build and publish `mmce-gui-ext` to the local Maven cache.
4. Then build or validate the downstream repo that depends on Modular Machinery: Community Edition More Extensions.

The current monorepo wrapper already supports this from the repository root:

```powershell
.\mmce-src\gradlew.bat -p .\mmce-gui-ext publishToMavenLocal -x test
```

## Contract the downstream repo should assume

- `mmce_gui_ext` is the machine JSON root used by the addon examples.
- Standalone controller style files can live under `config/mmceguiext/styles/` with the same `registryname` and `mmce_gui_ext` shape, so downstream addons do not need to edit MMCE machine JSON files.
- `client.cfg` values remain the first place for global defaults.
- Downstream scripts should read smart-interface data first and only fall back to custom data when needed.
- The example files and the parser tests should stay aligned with the documented field names.

## Keep stable

When the downstream pack changes example JSON or config samples, keep the following behavior stable:

- the sample JSON stays strict and parseable
- the sample `client.cfg` still mentions the keys the pack needs
- the API bridge still prefers Modular Machinery: Community Edition More Extensions smart-interface data before the fallback path
