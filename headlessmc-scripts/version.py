from os import path, linesep
from sys import argv
import fileinput
import re


def update(file, regex, value):
    print(f"Checking file {file} for {regex}, replacing with {value}...")
    with fileinput.input(file, inplace=True) as f:
        for line in f:
            if re.match(regex, line):
                line = re.sub(regex, f"\\g<1>{value}\\g<2>", line)
            print(line, end='')


if __name__ == '__main__':
    if len(argv) < 2:
        version = input(f"Please input the version to update to...{linesep}")
        used_input = True
    else:
        version = argv[1]
        used_input = False
    if used_input or (len(argv) > 2 and argv[2] == '-f') or input(f"Set version to {version} (y/n)?{linesep}") == 'y':
        base = path.abspath(path.join(path.dirname(__file__), '..'))
        update(path.join(base, 'headlessmc-scripts', 'hmc'), r"(.*headlessmc-launcher-wrapper-).*(.jar.*)", version)
        update(path.join(base, 'headlessmc-scripts', 'hmw'), r"(.*headlessmc-launcher-wrapper-).*(.jar.*)", version)
        update(path.join(base, 'headlessmc-scripts', 'hmc.bat'), r"(.*headlessmc-launcher-wrapper-).*(.jar.*)", version)
        update(path.join(base, 'gradle.properties'), r"(project_version=).*(.*)", version)
        update(path.join(base, 'headlessmc-api', 'src', 'main', 'java', 'io', 'github', 'headlesshq', 'headlessmc',
                         'api', 'HeadlessMc.java'), r"(.*VERSION = \").*(\";.*)", version)
    else:
        print("Cancelled version update!")
