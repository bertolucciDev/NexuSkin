# Build local rápido (sem GitHub Actions). Requer JDK 17+ e Gradle 8+.

```powershell
cd agent
gradle shadowJar
```

Saída: `agent/build/libs/nxagent.jar`

# Workflow de release

```sh
git tag v1.0.0
git push origin v1.0.0
```

A Action compila e anexa `nxagent.jar` ao Release.

# URL usada pelo launcher

```
https://github.com/SEU-USUARIO/SEU-REPO/releases/download/v1.0.0/nxagent.jar
```

Edite `NexuSkinService.AGENT_URL` no launcher com a URL do seu repo.
