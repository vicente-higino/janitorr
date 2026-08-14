# Janitorr on Windows

This distribution runs Janitorr directly on Windows, so paths returned by native Windows installations of Sonarr and Radarr remain Windows paths. Docker is not required for Janitorr itself.

## Requirements

- 64-bit Java 25 or newer. The launcher checks `JANITORR_JAVA_HOME`, `JAVA_HOME`, `PATH`, common JDK install directories, and Gradle's downloaded toolchains, then selects a compatible Java automatically.
- Windows Developer Mode enabled, or an account with the **Create symbolic links** right. Janitorr uses symbolic links for Leaving Soon libraries.
- The Windows account running Janitorr must be able to read the media paths and create files in the Leaving Soon directory.

Keep `application.dry-run: true` until the log shows exactly the media you expect. Leaving Soon library updates are not covered by dry-run mode.

## Build the Windows package

Open PowerShell in the repository and run:

```powershell
.\gradlew.bat windowsDistZip
```

The package is written to `build\distributions\janitorr-windows-<version>.zip`. Extract it to a permanent directory such as `C:\Apps\Janitorr`.

## Configure paths

Edit `application.yml` in the extracted directory. The included template uses `D:/Media` as an example; replace every example path with your actual location.

The template starts with `file-system.access: false` so an unedited configuration cannot create Leaving Soon links. Change it to `true` only after the paths are correct and Windows symbolic-link support is enabled.

Prefer forward slashes in YAML drive paths:

```yml
file-system:
  access: true
  leaving-soon-dir: "D:/Media/leaving-soon"
  media-server-leaving-soon-dir: "D:/Media/leaving-soon"
  free-space-check-dir: "D:/Media"
```

UNC shares are supported too:

```yml
file-system:
  leaving-soon-dir: "//nas/media/leaving-soon"
  media-server-leaving-soon-dir: "//nas/media/leaving-soon"
  free-space-check-dir: "//nas/media"
```

Use UNC paths instead of mapped drive letters when Janitorr will run through Task Scheduler or as another Windows account, because drive mappings are scoped to a login session.

Sonarr, Radarr, Janitorr, and Jellyfin/Emby must refer to the media and Leaving Soon directories using paths they can access. With a fully native Windows stack this normally means using the same drive or UNC paths in every application. `media-server-leaving-soon-dir` only needs to differ when Jellyfin/Emby sees that one directory under another path.

Change the client URLs to `localhost` or the Windows hostnames/IP addresses where each service listens, and replace all example API keys and credentials.

## Start Janitorr

Validate Java, the JAR, and the configuration path without starting Janitorr:

```powershell
.\start-janitorr.ps1 -Check
```

If several Java versions are installed and you want to select one explicitly:

```powershell
.\start-janitorr.ps1 -JavaPath "C:\Program Files\Java\jdk-25\bin\java.exe" -Check
```

Then run it in the foreground:

```powershell
.\start-janitorr.ps1
```

If PowerShell blocks the downloaded script, unblock it once:

```powershell
Unblock-File .\start-janitorr.ps1
```

Logs are written to the `logs` directory beside the launcher. Stop the foreground process with `Ctrl+C`.

To use a configuration stored elsewhere:

```powershell
.\start-janitorr.ps1 -ConfigPath "C:\ProgramData\Janitorr\application.yml"
```

After the foreground setup is stable, install an at-logon scheduled task and start it immediately:

```powershell
.\install-scheduled-task.ps1 -StartNow
```

The task runs with the current Windows account, starts Java without a console window, prevents duplicate instances, and restarts Janitorr after a failure. The application log is written to `logs\janitorr.log`. Redirected console output is also captured in `logs\janitorr-console.log` and `logs\janitorr-error.log`; an existing console log is timestamped before a new background process starts.

Check its status or start/stop it with:

```powershell
Get-ScheduledTaskInfo -TaskName "Janitorr"
Start-ScheduledTask -TaskName "Janitorr"
Stop-ScheduledTask -TaskName "Janitorr"
```

Remove it with:

```powershell
Unregister-ScheduledTask -TaskName "Janitorr"
```

This at-logon setup does not store your Windows password. If Janitorr must start before anyone logs in, create the task in the Task Scheduler GUI using **Run whether user is logged on or not**, set **Start in** to the extracted Janitorr directory, and use an account that has access to every configured local or network path.

When replacing the Windows package later, keep the same extracted directory and preserve your edited `application.yml`; the registered task will use the updated launcher and JAR on its next start.

## Jellyfin Leaving Soon paths

For native Windows Jellyfin, Janitorr registers Leaving Soon locations with native backslashes even when the YAML uses the recommended forward-slash form. Existing equivalent locations such as `C:/Media/leaving-soon/movies/media` are migrated to `C:\Media\leaving-soon\movies\media` automatically on the next Janitorr run. This avoids Jellyfin indexing the symlink targets without associating them with the Leaving Soon virtual library.

## Symbolic-link troubleshooting

If the log says Windows could not create a symbolic link, enable Developer Mode in Windows settings and restart Janitorr. In managed environments, grant the service account the **Create symbolic links** user right instead. Running an elevated PowerShell session is also sufficient, but is usually unnecessary after Developer Mode is enabled.
