# Backups

HeadlessMc can back up profiles and servers with the `backup <profile/server>` command.
Servers are profiles as well, so both are backed up the same way.
Use `--type` to choose what to back up:

| Type     | Contents                                                                    |
|----------|-----------------------------------------------------------------------------|
| `full`   | The entire game directory and the profile itself. This is the default.     |
| `worlds` | All worlds of the profile.                                                  |
| `world`  | A single world, specified with `--world`. The default if `--world` is given. |

``` title="Backing up a server"
> backup server-paper-1.21.5
Created full backup server-paper-1.21.5-2026-10-28_12-30-00 of server-paper-1.21.5 (1532 files, 61.2 MiB) at /home/me/.local/share/headlessmc/backups/profile/server-paper-1.21.5/server-paper-1.21.5-2026-10-28_12-30-00.zip
```

``` title="Backing up a single world of a client"
> backup fabric-1.21.5 --world "New World"
Created world backup fabric-1.21.5-2026-10-28_12-31-00 of fabric-1.21.5 (84 files, 3.1 MiB) at ...
```

Worlds are found differently for clients and servers:

- For clients, every directory inside the `saves` folder of the game directory is a world.
- For servers, every directory in the server folder that contains a `level.dat` or a `datapacks`
  folder is a world, e.g. `world`, `world_nether` and `world_the_end` on Paper.
  `backup <server> --type world` without `--world` backs up the `level-name` of the `server.properties`,
  or `world` if it is not set.

## Running games

HeadlessMc refuses to back up or restore a profile or server while it is running,
because files may change while they are being backed up or restored.
A profile counts as running if HeadlessMc launched it and the process is still alive, even if it was launched
by another HeadlessMc instance, or if one of its worlds is open. Since Minecraft 1.16 an open world locks its
`session.lock` file, which also detects games started by other launchers.
Use `--force` to back up or restore anyway.

## Automatic backups

Set `hmc.backup.auto` to `worlds` or `full` to back up profiles and servers every time before they are launched.
Automatic backups are off by default (`none`).
The backup is created before anything is installed or started, so it also contains the worlds as they were
before a version upgrade or newly added mods touched them.
No backup is created if nothing changed since the last backup of the same type,
or if the profile is already running in another HeadlessMc instance.
If the backup fails, the launch fails as well. Launch with `--no-backup` to skip the backup once:

``` title="Launching without an automatic backup"
> launch server-paper-1.21.5 --no-backup
```

Automatically created backups, the ones created before launching and the ones `backup restore` creates,
are deleted automatically: only the newest `hmc.backup.auto-keep` (default 5) automatic backups of each
profile or server are kept. Backups you create with `backup` are never deleted automatically,
use `backup remove` or `backup prune` for those.

## Ignoring files

The `hmc.backup.ignore` config property contains comma-separated glob patterns of paths
that are left out of backups, by default `logs,crash-reports`.
Patterns are relative to the game directory and use `/` as separator:
`*` matches anything but `/`, `**` matches across directories and `?` matches a single character.
If a pattern matches a directory, everything inside of it is ignored.
Full backups of clients also leave out the `assets`, `libraries` and `versions` folders,
because HeadlessMc downloads them again when needed.
`session.lock` files are never backed up.

You can ignore more paths for a single backup with `--ignore`,
or back up everything with `--no-default-ignore`:

``` title="Ignoring screenshots and log files"
> backup fabric-1.21.5 --ignore screenshots,**/*.log
```

The ignored patterns are stored with the backup, so `backup diff` and `backup restore`
leave ignored files alone as well.

## Listing backups

``` title="Listing backups"
> backup list
id                                        target                type    origin   contents          created               files   size
server-paper-1.21.5-2026-10-28_12-30-00   server-paper-1.21.5   full    manual   *                 2026-10-28 12:30:00   1532    61.2 MiB
fabric-1.21.5-2026-10-28_12-31-00         fabric-1.21.5         world   manual   saves/New World   2026-10-28 12:31:00   84      3.1 MiB
fabric-1.21.5-2026-10-28_13-05-42         fabric-1.21.5         worlds  launch   saves/New World   2026-10-28 13:05:42   84      3.1 MiB
```

`backup list <profile/server>` only lists the backups of a single profile or server.
The origin shows what created a backup: `manual` for the `backup` command, `launch` for automatic backups
before launching and `restore` for the backups `backup restore` creates.

## Comparing a backup with the files on disk

`backup diff <backup>` shows which files have been added, removed or modified since a backup.
Instead of the id of a backup you can also pass the name of a profile or server, to compare its latest backup.
The diff of a `worlds` backup also contains worlds that have been created after the backup.
Files whose size and modification time did not change are considered unchanged,
use `--checksum` to compare their contents as well.

``` title="Comparing the latest backup of a server"
> backup diff server-paper-1.21.5
change     file
added      ops.json
modified   world/level.dat
1 added, 0 removed, 1 modified since backup server-paper-1.21.5-2026-10-28_12-30-00
```

## Restoring a backup

`backup restore <backup>` makes the backed up files match the backup again:
files that were added since the backup are deleted, removed and modified files are restored.
Restoring a `full` backup also restores the profile or server itself.
Ignored files are kept, and so are worlds created after a `worlds` backup.

Before anything is changed, the files that are about to be changed are backed up,
so a restore can be undone by restoring that backup. Use `--no-backup` to skip this,
and `--keep-new` to keep files that were added since the backup instead of deleting them.

``` title="Restoring a backup"
> backup restore server-paper-1.21.5-2026-10-28_12-30-00
Backed up the current files as server-paper-1.21.5-2026-10-28_14-02-11
Restored backup server-paper-1.21.5-2026-10-28_12-30-00 to /home/me/.local/share/headlessmc/server/server-paper-1.21.5: deleted 1, restored 0 and reverted 1 files
```

## Removing backups

``` title="Removing a backup"
> backup remove server-paper-1.21.5-2026-10-28_12-30-00
Removed backup server-paper-1.21.5-2026-10-28_12-30-00
```

## Pruning backups

`backup prune` deletes old backups. `--keep <n>` keeps the newest `n` backups of each profile or server,
`--older-than <duration>` only deletes backups older than e.g. `30d`, `12h` or `2w`.
Combined, backups are only deleted if they are older and not among the newest ones.
`backup prune <profile/server>` only prunes the backups of one profile or server,
`--origin manual,launch,restore` only prunes backups with these origins.

``` title="Keeping the 3 newest backups of each profile, deleting older ones after a week"
> backup prune --keep 3 --older-than 7d
Removed backup fabric-1.21.5-2026-10-20_18-12-01
Removed 1 backups.
```

## Other kinds of backups

What `backup` can back up is extensible: profiles and servers are backed up by the `profile` provider,
which is the default. Plugins can add more providers, e.g. for containers, by implementing
`io.github.headlesshq.headlessmc.launcher.backup.BackupProvider` as a bean.
A provider decides what to back up, HeadlessMc archives, compares, restores and prunes the files.
Select a provider with `--provider`:

``` title="Backing up with another provider"
> backup --provider container my-container
```

## Where backups are stored

Backups are placed in the `backups` folder of the HeadlessMc data directory,
e.g. `~/.local/share/headlessmc/backups`, you can change this with the `hmc.backup.dir` config property.
Each provider gets its own folder, containing a folder for each profile or server with a zip archive
of the backed up files and a json file for every backup.
The json file stores the provider, type and origin of the backup, when it was created, the directory it was created from,
the backed up folders, the ignored patterns and the profile itself, as it was when the backup was created.
