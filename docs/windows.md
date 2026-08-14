# Janitorr on Windows

This fork packages Janitorr as a self-contained, 64-bit `janitorr.exe`. The release does not require Docker, Java, a JDK, a JAR launcher, or PowerShell. Windows drive-letter and UNC paths returned by native Sonarr and Radarr installations remain Windows paths.

## Install

1. Download the latest ZIP from the [Windows releases](https://github.com/vicente-higino/janitorr-windows/releases).
2. Extract it to a permanent directory such as `C:\janitorr-windows`.
3. Edit `application.yml` beside `janitorr.exe`. Replace every example path, URL, API key, username, and password.
4. Keep `application.dry-run: true` until `logs\janitorr.log` shows exactly what Janitorr would do. Leaving Soon library changes are not covered by dry-run mode.
5. Keep `file-system.access: false` until all paths are correct and the Windows account can create symbolic links. Then change it to `true` to enable Leaving Soon.

Enable Windows Developer Mode, grant the scheduled account the **Create symbolic links** user right, or run it with sufficient privileges. That account must also be able to read the media paths, write to the Leaving Soon directory, and reach every configured service.

## Configure paths

Forward slashes avoid YAML escaping problems in drive paths:

```yml
file-system:
  access: true
  leaving-soon-dir: "D:/Media/leaving-soon"
  media-server-leaving-soon-dir: "D:/Media/leaving-soon"
  free-space-check-dir: "D:/Media"
```

Use UNC paths for network shares, especially from Task Scheduler. Mapped drive letters belong to an interactive login session and might not exist for a background task:

```yml
file-system:
  leaving-soon-dir: "//nas/media/leaving-soon"
  media-server-leaving-soon-dir: "//nas/media/leaving-soon"
  free-space-check-dir: "//nas/media"
```

Sonarr, Radarr, Janitorr, and Jellyfin/Emby must all use paths they can access. For an entirely native Windows stack, `leaving-soon-dir` and `media-server-leaving-soon-dir` should normally be identical. Only use a different media-server path when Jellyfin/Emby runs in a container or on another host and sees the directory under a different path.

## Test in the foreground

Open Command Prompt in the extracted directory and run:

```bat
janitorr.exe
```

The executable loads `application.yml` and creates `logs\janitorr.log` in its own directory. Stop it with `Ctrl+C` after confirming the configuration and service connections.

## Run without a window

Create the task through the Windows Task Scheduler GUI:

1. Choose **Create Task** and name it `Janitorr`.
2. On **General**, select **Run whether user is logged on or not** and **Hidden**. Enable **Run with highest privileges** only when the selected account needs elevation for symbolic links.
3. On **Triggers**, add **At startup** or **At log on**.
4. On **Actions**, add **Start a program** with these values:
   - **Program/script:** `C:\janitorr-windows\janitorr.exe`
   - **Add arguments:** leave empty
   - **Start in:** `C:\janitorr-windows`
5. On **Settings**, enable restart after failure, choose **Do not start a new instance**, and disable any unwanted execution time limit.
6. Save the task, enter the account password if requested, and choose **Run**.

Selecting **Run whether user is logged on or not** runs the executable in a non-interactive session, so no console window appears. Check `logs\janitorr.log` to confirm that it started. When upgrading, stop the task, replace `janitorr.exe`, preserve `application.yml`, and start the task again.

## Jellyfin Leaving Soon troubleshooting

Janitorr creates separate generated source paths such as:

- `D:\Media\leaving-soon\movies\media`
- `D:\Media\leaving-soon\tv\media`
- `D:\Media\leaving-soon\movies\tag-based`
- `D:\Media\leaving-soon\tv\tag-based`

It adds the relevant paths to the configured Jellyfin Leaving Soon libraries. Native Windows paths are registered with backslashes, and equivalent paths previously registered with forward slashes are migrated automatically.

If the symlinks exist but Jellyfin shows no files:

1. Confirm `file-system.access: true`, `clients.jellyfin.enabled: true`, and valid Jellyfin credentials.
2. In Jellyfin Dashboard > Libraries, verify that the Leaving Soon libraries contain the generated child paths, not only the `leaving-soon` parent.
3. Confirm that the Windows account running the **Jellyfin Server** service can traverse the Leaving Soon directory and read every symlink target. This can differ from the account running Janitorr.
4. Run **Scan All Libraries** in Jellyfin.
5. Check `logs\janitorr.log` for Jellyfin API, access-denied, and symbolic-link errors. Temporarily change `logging.level.com.github.schaka` from `INFO` to `DEBUG` in `application.yml` for more detail.

## Build from source

End users should use the release ZIP. Contributors building locally need GraalVM for JDK 25 and Visual Studio 2022 Build Tools with the C++ workload:

```bat
gradlew.bat test windowsDistZip
```

The native ZIP is written to `build\distributions\janitorr-windows-native-<version>.zip`.
