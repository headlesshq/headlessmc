#!/bin/bash
# Smoke tests an installed deb, rpm or archlinux package, run by the packages job of the lifecycle workflow
# inside a container of the distribution, after the package, bash-completion, zsh and fish have been installed.
set -euxo pipefail

test -f /usr/share/headlessmc/package
headlessmc help

# bash: bash-completion lazily loads /usr/share/bash-completion/completions/headlessmc,
# without set -e, which bash_completion does not support
bash -c '
  source /usr/share/bash-completion/bash_completion
  if declare -F _comp_load >/dev/null; then
    _comp_load headlessmc
  else
    __load_completion headlessmc
  fi
  complete -p headlessmc | grep -q _headlessmc || { echo "bash completion not loaded" >&2; exit 1; }
  COMP_WORDS=(headlessmc hel)
  COMP_CWORD=1
  COMP_LINE="headlessmc hel"
  COMP_POINT=${#COMP_LINE}
  _headlessmc headlessmc hel headlessmc
  printf "%s\n" "${COMPREPLY[@]}" | grep -qx help || { echo "bash completion failed: ${COMPREPLY[*]}" >&2; exit 1; }
'

# zsh: compinit finds _headlessmc in the fpath directory of the distribution, without any zshrc
zsh -fc 'autoload -Uz compinit && compinit -u -D && [[ ${_comps[headlessmc]} == _headlessmc ]]'

# fish: loads vendor_completions.d automatically, this also runs headlessmc __complete
fish -c "complete -C 'headlessmc hel'" | grep -q '^help'
