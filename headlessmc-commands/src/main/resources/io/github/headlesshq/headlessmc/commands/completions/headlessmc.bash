# Dynamic bash completion for HeadlessMc.
#
# Asks HeadlessMc itself for candidates by calling the hidden command
#   headlessmc __complete <args...> <current word>
# which prints one candidate per line as "<name>\t<description>", lines without a tab are ignored,
# followed by a last line ":<directive>", a bitmask of
#   1 = error, do not complete anything
#   2 = do not append a space after the completion
#   4 = do not fall back to file completion if there are no candidates
#
# Bash cannot show descriptions next to candidates, so if there are multiple candidates
# they are listed as "<name>  (<description>)" and only the name is inserted.

_headlessmc() {
  local cur cword
  local -a words
  if declare -F _get_comp_words_by_ref >/dev/null; then
    _get_comp_words_by_ref -n =: cur words cword
  else
    cur="${COMP_WORDS[COMP_CWORD]}"
    words=("${COMP_WORDS[@]}")
    cword=$COMP_CWORD
  fi

  local out
  out="$("${words[0]}" __complete "${words[@]:1:cword-1}" "$cur" 2>/dev/null)" || return

  local line directive=0
  local -a names descriptions
  while IFS= read -r line; do
    if [[ $line =~ ^:([0-9]+)$ ]]; then
      directive=${BASH_REMATCH[1]}
    elif [[ $line == *$'\t'* ]]; then
      names+=("${line%%$'\t'*}")
      descriptions+=("${line#*$'\t'}")
    fi
  done <<< "$out"

  (( directive & 1 )) && return

  COMPREPLY=()
  if (( ${#names[@]} == 0 )); then
    (( directive & 4 )) || compopt -o default
    return
  fi

  (( directive & 2 )) && compopt -o nospace

  if (( ${#names[@]} == 1 )); then
    COMPREPLY=("${names[0]}")
    declare -F __ltrim_colon_completions >/dev/null && __ltrim_colon_completions "$cur"
    return
  fi

  local i width=0
  for i in "${!names[@]}"; do
    (( ${#names[i]} > width )) && width=${#names[i]}
  done

  local entry columns=${COLUMNS:-80}
  for i in "${!names[@]}"; do
    entry="${names[i]}"
    if [[ -n ${descriptions[i]} ]]; then
      printf -v entry '%-*s  (%s)' "$width" "${names[i]}" "${descriptions[i]}"
      if (( ${#entry} > columns - 2 )); then
        entry="${entry:0:columns-5}...)"
      fi
    fi
    COMPREPLY+=("$entry")
  done
}

complete -F _headlessmc headlessmc hmc
