// Creates the npm packages from the native executables of a release: one package per platform with the executable,
// and the main package headlessmc that starts it, see npm/headlessmc.
// Usage: node scripts/npm-packages.mjs [--partial] <dir with the release assets> <output dir>
//   --partial  skip platforms whose executable is missing, to test the packages on CI without a release
// Publish the platform packages before the main package, which depends on them.

import { chmodSync, copyFileSync, cpSync, existsSync, mkdirSync, readFileSync, rmSync, writeFileSync } from 'node:fs';
import { join } from 'node:path';

const args = process.argv.slice(2);
const partial = args[0] === '--partial';
const [dist, out] = partial ? args.slice(1) : args;
if (!dist || !out) {
    console.error('Usage: node scripts/npm-packages.mjs [--partial] <dist> <out>');
    process.exit(1);
}

const main = JSON.parse(readFileSync('npm/headlessmc/package.json', 'utf8'));

// npm platform (process.platform-process.arch) -> name of the release asset
const platforms = {
    'linux-x64': 'headlessmc-launcher-linux-x64',
    'linux-arm64': 'headlessmc-launcher-linux-arm64',
    'darwin-x64': 'headlessmc-launcher-macos-x64',
    'darwin-arm64': 'headlessmc-launcher-macos-arm64',
    'win32-x64': 'headlessmc-launcher-windows-x64.exe',
};

const expected = Object.keys(platforms).map(platform => `headlessmc-${platform}`).sort();
const actual = Object.keys(main.optionalDependencies).sort();
if (JSON.stringify(expected) !== JSON.stringify(actual)) {
    throw new Error(`The optionalDependencies of npm/headlessmc/package.json should be ${expected.join(', ')}`);
}

rmSync(out, { recursive: true, force: true });
let created = 0;
for (const [platform, asset] of Object.entries(platforms)) {
    const [os, cpu] = platform.split('-');
    const name = `headlessmc-${platform}`;
    // release-please bumps these together with the version, see release-please-config.json
    if (main.optionalDependencies[name] !== main.version) {
        throw new Error(`${name} is not an optional dependency of version ${main.version}`);
    }

    const source = join(dist, asset);
    if (!existsSync(source)) {
        if (partial) {
            console.log(`Skipping ${name}, ${source} does not exist`);
            continue;
        }
        throw new Error(`Missing release asset ${source}`);
    }

    const dir = join(out, name);
    mkdirSync(join(dir, 'bin'), { recursive: true });
    const executable = join(dir, 'bin', os === 'win32' ? 'headlessmc.exe' : 'headlessmc');
    copyFileSync(source, executable);
    chmodSync(executable, 0o755);

    writeFileSync(join(dir, 'package.json'), JSON.stringify({
        name,
        version: main.version,
        description: `The ${platform} executable of ${main.name}.`,
        homepage: main.homepage,
        bugs: main.bugs,
        repository: main.repository,
        license: main.license,
        os: [os],
        cpu: [cpu],
        // the Linux executables link against glibc, see MIN_GLIBC in scripts/install.sh
        ...(os === 'linux' ? { libc: ['glibc'] } : {}),
        files: ['bin'],
    }, null, 2) + '\n');
    writeFileSync(join(dir, 'README.md'), `# ${name}\n\nThe ${platform} executable of [${main.name}](https://www.npmjs.com/package/${main.name}), use that package instead.\n`);
    copyFileSync('LICENSE', join(dir, 'LICENSE'));
    created++;
}

if (created === 0) {
    throw new Error(`No executables found in ${dist}`);
}

cpSync('npm/headlessmc', join(out, 'headlessmc'), { recursive: true });
// npm always packs a LICENSE file, regardless of "files"
copyFileSync('LICENSE', join(out, 'headlessmc', 'LICENSE'));
console.log(`Created the npm packages of version ${main.version} in ${out}`);
