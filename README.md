# Janitorr for Windows — native Windows fork

> [!IMPORTANT]
> This is the native Windows fork of [Schaka/janitorr](https://github.com/Schaka/janitorr). It adds Windows drive and UNC path handling, a self-contained Windows executable, background Task Scheduler support, and downloadable Windows releases. See this fork's [Releases](https://github.com/vicente-higino/janitorr-windows/releases) for ready-to-run packages; use the upstream repository for the original Docker-focused project.

<p align="center">
    <img src="images/logos/janitorr_icon.png" width=384>
</p>

### Inspiration

This application is heavily inspired by (but not a fork of) [Maintainerr](https://github.com/jorenn92/Maintainerr).
If you're within the Plex ecosystem, want an easy to use GUI and more sophisticated functionality, you're better off using it instead.

### Warning

Please use at your own risk.
You may enable dry-run mode. This is enabled in the config template by default.
Unless you disable dry-run mode, nothing will be deleted.
Refer to the logging section to see what actions Janitorr will take.

If you still don't trust Janitorr, you may enable Recycle Bin in the *arrs and disable Jellyfin/Emby.
This way, no deletes will be triggered on Jellyfin and everthing triggered in the *arrs will only go to the Recycle Bin.

### Introduction

**Janitorr** manages your media and cleans up after you.

- Do you hate being the janitor of your server?
- Do you have a lot of media that never gets watched?
- Do your users constantly request media, and let it sit there afterward never to be touched again?

Then you need Janitorr for Jellyfin and Emby.
It's THE solution for cleaning up your server and freeing up space before you run into issues.

## Features

- Remote deletion, disk space aware deletion as well as tag based delete schedules
- Exclude items from deletion via tags in Sonarr/Radarr
- Configure expiration times for your media in the *arrs - optionally via Jellystat
- Season by season removal for TV shows, removing entire shows or only keep a minimum number of episodes for weekly shows
- Unmonitor Jellyfin-watched movies and individual episodes in Radarr/Sonarr to prevent post-watch upgrades
- Clear requests from Seerr and clean up leftover metadata in Jellyfin so no orphaned files are left
- Show a collection, containing rule matched media, on the Jellyfin home screen for a specific duration before deletion. Think: "Leaving soon"

<img src="images/leaving_soon_01.png" width=60%>

### Important notes

- Janitorr does **not** delete items after they were watched. Look into [Jellyfin Media Cleaner](https://github.com/shemanaev/jellyfin-plugin-media-cleaner) for that.
- **I don't use Emby. I implemented and tested it, but for maintenance I rely on bug reports**
- Only one of Jellyfin or Emby can be enabled at a time
- Only one of Jellystat or Streamystats can be enabled at a time
- [janitorr-stats](https://github.com/Schaka/janitorr-stats) can run alongside either as a fallback, or on its own
- "Leaving Soon" Collections are *always* created and do not care for dry-run settings
- Jellyfin and Emby require user access to delete files, an API key is not enough - I recommend creating a user specifically for this task
- **For media to be picked up, it needs to have been downloaded by the Radarr/Sonarr**
- Jellyfin/Emby and Seerr are not required, but if you don't supply them, you may end up with orphaned folders,  metadata, etc

### Logging
You may check the container logs for Janitorr to observe what the application wants to do.
Janitorr logs to stdout, so you can view your logs in Docker. However, it is recommended to enable file logging in your config instead.
If file logging is enabled, please make sure the location you've chosen for the log file is mapped into the container, so that Janitorr can write log files to the host and not inside the container.

To enable debug logging, change `INFO` in the following line in `application.yml` to either `DEBUG` or `TRACE`:

```yml
    com.github.schaka: INFO
```

### Troubleshooting
Before you create a new issue, please check previous issues to make sure nobody has faced the same problem before.
[The Wiki](https://github.com/Schaka/janitorr/wiki) also contains a troubleshooting section with commons errors.

If you have any questions, consult the [FAQ section](https://github.com/Schaka/janitorr/wiki/FAQ) before starting a [new discussion](https://github.com/Schaka/janitorr/discussions).

### Unmonitor after watch

Janitorr can receive `PlaybackStop` events directly from Jellyfin and unmonitor media once Jellyfin marks the playback as completed. This uses Jellyfin's own played-percentage setting; it does not poll Jellystat, Streamystats, or janitorr-stats. Movies are unmonitored in Radarr, while only the watched episode is unmonitored in Sonarr. Episodes sharing the same physical file are handled together so that file cannot be replaced by an upgrade. Items carrying an `application.exclusion-tags` tag are skipped.

Enable the receiver with a long random secret:

```yml
server:
  port: ${SERVER_PORT:9797}

application:
  unmonitor-after-watch:
    enabled: true
    webhook-secret: "replace-with-a-long-random-secret"
```

Install Jellyfin's official [**Webhook** plugin](https://github.com/jellyfin/jellyfin-plugin-webhook) and add a **Generic Destination** with:

- Webhook URL: `http://janitorr:9797/api/webhooks/jellyfin` when both applications share a Docker network, or `http://127.0.0.1:9797/api/webhooks/jellyfin` for a native same-machine installation.
- Notification type: **Playback Stop** only.
- **Send All Properties** enabled.
- Header `Content-Type: application/json`.
- Header `X-Janitorr-Webhook-Secret` set to the same secret as `application.yml`.

The endpoint must be reachable from Jellyfin, and Janitorr must run continuously with `application.run-once: false`. Janitorr listens on port `9797` by default. Change `server.port` in `application.yml` or set the `SERVER_PORT` environment variable to use another port, and use the same port in the webhook URL. Publish the port only when Jellyfin cannot reach Janitorr through the existing container network. `application.dry-run: true` accepts and logs completed playbacks without changing Sonarr or Radarr. This is event-driven and has no historical backfill: only completed-playback webhooks received while the feature is enabled can change monitoring.

## Setup

Currently, the code is only published as a docker image to [GitHub](https://github.com/Schaka/janitorr/pkgs/container/janitorr).
If you cannot use Docker, you'll have to compile it yourself from source.

### Native Windows

This fork includes a ready-to-run, 64-bit native Windows package. It preserves drive-letter and UNC paths returned by Sonarr and Radarr, runs without Docker, and can stay hidden in the background through Task Scheduler. The release contains `janitorr.exe` and does **not** require Java, a JDK, a JAR launcher, or PowerShell.

#### Requirements

- Windows Developer Mode enabled, an account with the **Create symbolic links** right, or an elevated account. Leaving Soon libraries use symbolic links.
- A Windows account that can read the Sonarr/Radarr media paths, write to the Leaving Soon directory, and reach the configured services.

Keep `application.dry-run: true` until the logs show exactly what Janitorr would delete. Leaving Soon library updates are not covered by dry-run mode.

#### Download and configure

1. Download the newest ZIP from this fork's [Windows releases](https://github.com/vicente-higino/janitorr-windows/releases).
2. Extract it to a permanent directory such as `C:\janitorr-windows`.
3. Edit the extracted `application.yml`. Replace every example path, URL, API key, username, and password.
4. Leave `file-system.access: false` until the paths are correct and symbolic-link support is enabled. Then change it to `true` to enable Leaving Soon links.

Forward slashes are recommended inside YAML drive paths:

```yml
file-system:
  access: true
  leaving-soon-dir: "D:/Media/leaving-soon"
  media-server-leaving-soon-dir: "D:/Media/leaving-soon"
  free-space-check-dir: "D:/Media"
```

UNC shares are supported. Prefer UNC paths over mapped drives for scheduled tasks because drive mappings belong to a login session:

```yml
file-system:
  leaving-soon-dir: "//nas/media/leaving-soon"
  media-server-leaving-soon-dir: "//nas/media/leaving-soon"
  free-space-check-dir: "//nas/media"
```

Sonarr, Radarr, Janitorr, and Jellyfin/Emby must all be able to access their configured paths. With an entirely native Windows stack, `leaving-soon-dir` and `media-server-leaving-soon-dir` should normally be identical.

#### Test in the foreground

Double-click `janitorr.exe`, or open Command Prompt in the extracted directory and run:

```bat
janitorr.exe
```

The executable automatically loads `application.yml` from its own directory and writes `logs\janitorr.log` there. Keep `application.dry-run: true`, inspect that log, and stop the foreground process with `Ctrl+C` when the configuration is confirmed.

#### Run in the background

Use the Task Scheduler GUI so Janitorr runs without an open console window:

1. Open **Task Scheduler**, choose **Create Task** (not *Create Basic Task*), and name it `Janitorr`.
2. On **General**, select **Run whether user is logged on or not** and **Hidden**. Enable **Run with highest privileges** only if the account needs elevation to create symbolic links.
3. On **Triggers**, add **At startup** or **At log on**, depending on when Janitorr should start.
4. On **Actions**, add **Start a program**. Set **Program/script** to `C:\janitorr-windows\janitorr.exe`, leave **Add arguments** empty, and set **Start in** to `C:\janitorr-windows`.
5. On **Settings**, enable automatic restart after failure, select **Do not start a new instance** when already running, and disable any unwanted time limit.
6. Save the task, enter the selected Windows account's password if prompted, then right-click the task and choose **Run**.

The scheduled account must have access to all local and network paths. Use UNC paths instead of mapped drives when the task runs outside an interactive login. Confirm startup in `C:\janitorr-windows\logs\janitorr.log`; the task does not need console redirection because application logging goes directly to this file.

When updating, stop the task, replace `janitorr.exe` in the same directory, preserve your edited `application.yml`, and start the task again.

#### Jellyfin Leaving Soon troubleshooting

The symlinks existing on disk only proves that Janitorr created them. If Jellyfin does not show their contents, check all of the following:

- `file-system.access` is `true`, the Jellyfin client is enabled, and the configured Jellyfin user has permission to manage libraries.
- The Windows account running the **Jellyfin Server** service can read both the Leaving Soon directory and every symlink target. Test the paths as that account; Janitorr's account and Jellyfin's account can have different access.
- For a native Windows Jellyfin installation, `leaving-soon-dir` and `media-server-leaving-soon-dir` normally contain the same absolute drive or UNC path. A Docker-hosted Jellyfin needs its own container-visible value for `media-server-leaving-soon-dir`.
- In Jellyfin Dashboard > Libraries, the Leaving Soon libraries contain the generated paths such as `D:\Media\leaving-soon\movies\media` and `D:\Media\leaving-soon\tv\media`, not only the parent directory. Run **Scan All Libraries** after correcting a path or permission.
- `logs\janitorr.log` contains no failed Jellyfin API calls, access-denied errors, or symbolic-link errors. Set `logging.level.com.github.schaka: DEBUG` temporarily for more detail.

Native Windows Jellyfin paths are registered with backslashes. Existing equivalent Leaving Soon paths using forward slashes are migrated automatically on the next Janitorr run.

For more troubleshooting, including symbolic-link permissions and pre-login scheduled tasks, see the complete [Windows setup guide](docs/windows.md).

#### Build from source

Building is only necessary for contributors. Install GraalVM for JDK 25 and Visual Studio 2022 Build Tools with the C++ workload, then run this from Command Prompt in the repository:

```bat
gradlew.bat test windowsDistZip
```

The ZIP is written to `build\distributions\janitorr-windows-native-<version>.zip`.

Depending on the configuration, files will be deleted if they are older than x days. Age is determined by your grab
history in the *arr apps. By default, it will choose the oldest file in the history.
If Jellystat or Streamystats is set up, the most recent watch date overwrites the grab history, if it exists.

### Watch history via janitorr-stats

[janitorr-stats](https://github.com/Schaka/janitorr-stats) is an optional companion microservice that stores Jellyfin
play history keyed by stable external IDs (IMDB/TMDB/TVDB). Unlike Jellystat or Streamystats, the history survives
library rescans, file moves and server migrations that change Jellyfin's internal item IDs.

Important caveats:
- **janitorr-stats only records what is watched while it runs.** It does not backfill historical activity,
  so on day one it knows nothing. The longer it runs, the more useful it becomes.
- When both Jellystat/Streamystats and janitorr-stats are enabled, Janitorr queries the primary first and only
  falls back to janitorr-stats when the primary returns no result.
- If you are setting up Janitorr fresh and don't already have Jellystat or Streamystats running, enable
  janitorr-stats from the start and skip the others. There is no reason to set up a stats app you're planning
  to migrate away from.

Enable it by pointing Janitorr at the janitorr-stats container:

```yml
clients:
  janitorr-stats:
    enabled: true
    url: "http://janitorr-stats:8080"
```

See the [janitorr-stats README](https://github.com/Schaka/janitorr-stats) for container setup.

To exclude media from being considered from deletion, set the `janitorr_keep` tag in Sonarr/Radarr. The actual tag
Janitorr looks for can be adjusted in your config file.

### Setting up Docker

- follow the mapping for `application.yml` examples below
- within that host folder, put a copy of [application.yml](https://github.com/Schaka/janitorr/blob/develop/src/main/resources/application-template.yml) from this repository
- adjust said copy with your own info like *arr, Jellyfin and Seerr API keys and your preferred port

If using Jellyfin with **filesystem access**, ensure that Janitorr has access to the exact directory structure for the leaving-soon-dir as Jellyfin.
Additionally, make sure the *arrs directories are mapped into your container the same way for Janitorr as well.
Janitorr receives info about where files are located by the *arrs - so the path needs to be available to both.

Janitorr creates symlinks from whatever directory it receives from the arrs' API into the `leaving-soon-dir`.
If Radarr finds movies at `/data/media/movies` Janitorr needs to find them at `/data/media/movies` too.
You need to ensure links can be created from the source (in the *arrs' library) to the destination (leaving-soon).

The only exception is your `leaving-soon-dir`. If Jellyfin and Janitorr know this directory under different paths, you can just this.
By default, both `media-server-leaving-soon-dir` and `leaving-soon-dir` should be identical if your volume mappings are identical.

Leaving Soon timing is controlled by `application.leaving-soon` (days before deletion), and you can optionally start populating
Leaving Soon earlier with `application.leaving-soon-threshold-offset-percent`. For disk-threshold deletion, this means Janitorr will
start populating Leaving Soon when free disk is within N% of the next deletion threshold, but will only delete once the actual threshold is reached.


If Janitorr's mapping looks like this:
`/share_media/media/leaving-soon:/data/media/leaving-soon`

And Jellyfin's like this:
`/share_media/media/leaving-soon:/library/leaving-soon`

Then your `application.yml` should look like:
```
leaving-soon-dir: "/data/media/leaving-soon"
media-server-leaving-soon-dir: "/library/leaving-soon"
```

**You may also check out [this example](examples/example-compose.yml) of a full stack setup.**

### Docker config

Before using this, please make sure you've created the `application.yml` file and put it in the correct config directory you intend to map.
The application requires it. You need to supply it, or Janitorr will not start correctly.
You don't have to publish any ports on the host machine when every caller shares Janitorr's Docker network. If Jellyfin must reach Janitorr through the host, publish the configured HTTP port (for example `9797:9797`).
If you're seeing any problems, consult [the Wiki](https://github.com/Schaka/janitorr/wiki/Troubleshooting).

An example of a `docker-compose.yml` may look like this:

```yml
services:
  janitorr:
    container_name: janitorr
    image: ghcr.io/schaka/janitorr:jvm-stable
    user: 1000:1000 # Replace with your user who should own your application.yml file
    mem_limit: 256M # is used to dynamically calculate heap size, can go as low as 200MB, but 256 is recommended - higher for very large libraries
    mem_swappiness: 0
    volumes:
      - /appdata/janitorr/config/application.yml:/config/application.yml
      - /appdata/janitorr/logs:/logs
      - /share_media:/data
```

In extremely memory constrained environments or if you're a seasoned developer on the JVM developed, you can supply your own `JAVA_TOOL_OPTIONS` as an environment variable for.
It is possible to lower overall memory consumption to about 150MB, but I do not recommend it because you're right at the edge of stability.
However, if you're on an extremely old device and every few megabytes count - you may experiment with the options as follows, by adding it to your `docker-compose.yml` under `environment`.
`- JAVA_TOOL_OPTIONS=-Xms10m -Xmx30m -XX:+UseSerialGC -XX:+UnlockExperimentalVMOptions -XX:+UseCompactObjectHeaders -XX:MaxDirectMemorySize=10M -XX:MaxMetaspaceSize=20M -XX:ReservedCodeCacheSize=10M -Xss150K`

My recommendations:
- don't try to reduce the heap much more unless your library size is small - 20MB will not work
- code cache can be reduced at the expense of more CPU cycles
- 100K stack size worked well in my limited testing
- MetaSpaceSize can't be dropped much lower than 20M

#### Bleeding edge development image

**Attention: The develop branch is experimental. Logical errors and breaking changes may happen.**
To get the latest build as found in the development branch, grab the following image: `ghcr.io/schaka/janitorr:jvm-develop`.


## Local Development

For instructions on running Janitorr locally with a full stack of real service containers, see [docs/local-development.md](docs/local-development.md).

## JetBrains
Thank you to [<img src="images/logos/jetbrains.svg" alt="JetBrains" width="32"> JetBrains](http://www.jetbrains.com/) for providing us with free licenses to their great tools.

* [<img src="images/logos/idea.svg" alt="Idea" width="32"> IntelliJ Idea](https://www.jetbrains.com/idea/)
* [<img src="images/logos/webstorm.svg" alt="WebStorm" width="32"> WebStorm](http://www.jetbrains.com/webstorm/)
* [<img src="images/logos/rider.svg" alt="Rider" width="32"> Rider](http://www.jetbrains.com/rider/)
