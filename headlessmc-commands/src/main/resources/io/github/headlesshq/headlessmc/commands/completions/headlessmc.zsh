#compdef headlessmc hmc

# Dynamic zsh completion for HeadlessMc.
#
# Asks HeadlessMc itself for candidates by calling the hidden command
#   headlessmc __complete <args...> <current word>
# which prints one candidate per line as "<name>\t<description>", lines without a tab are ignored,
# followed by a last line ":<directive>", a bitmask of
#   1 = error, do not complete anything
#   2 = do not append a space after the completion
#   4 = do not fall back to file completion if there are no candidates
#
# Options are shown even if no "-" has been typed yet, unless the prefix-needed style is set, e.g.
#   zstyle ':completion:*:*:headlessmc:*:options' prefix-needed true

_headlessmc() {
  local -a args lines options values
  local cur out line name desc entry
  local -i directive=0

  args=("${(@Q)words[2,CURRENT-1]}")
  cur="${(Q)PREFIX}"

  out="$("${words[1]}" __complete "${args[@]}" "$cur" 2>/dev/null)" || return 1
  lines=("${(@f)out}")

  if [[ ${lines[-1]} == :<-> ]]; then
    directive=${lines[-1]#:}
    lines[-1]=()
  fi

  (( directive & 1 )) && return 1

  for line in $lines; do
    [[ $line == *$'\t'* ]] || continue
    name=${line%%$'\t'*}
    desc=${line#*$'\t'}
    entry=${name//:/\\:}
    [[ -n $desc ]] && entry+=":$desc"

    if [[ $name == -* ]]; then
      options+=("$entry")
    else
      values+=("$entry")
    fi
  done

  if (( ${#options} + ${#values} == 0 )); then
    (( directive & 4 )) || _files
    return
  fi

  local -a compadd_opts option_tag=(-O)
  (( directive & 2 )) && compadd_opts=(-S '')
  zstyle -t ":completion:${curcontext}:options" prefix-needed && option_tag=(-t options)

  local ret=1
  (( ${#values} )) && _describe -t values 'value' values "${compadd_opts[@]}" && ret=0
  (( ${#options} )) && _describe "${option_tag[@]}" 'option' options "${compadd_opts[@]}" && ret=0
  return ret
}

if [[ $zsh_eval_context[-1] == loadautofunc ]]; then
  _headlessmc "$@"
else
  compdef _headlessmc headlessmc hmc
fi
