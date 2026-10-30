# Dynamic fish completion for HeadlessMc.
#
# Asks HeadlessMc itself for candidates by calling the hidden command
#   headlessmc __complete <args...> <current word>
# which prints one candidate per line as "<name>\t<description>", lines without a tab are ignored,
# followed by a last line ":<directive>", a bitmask of
#   1 = error, do not complete anything
#   2 = do not append a space after the completion
#   4 = do not fall back to file completion if there are no candidates

function __headlessmc_complete
    set -l tokens (commandline -opc)
    set -l current (commandline -ct)
    set -l command $tokens[1]
    set -e tokens[1]

    set -l out ($command __complete $tokens "$current" 2>/dev/null)
    or return

    set -l directive 0
    if string match -qr '^:\d+$' -- $out[-1]
        set directive (string sub -s 2 -- $out[-1])
        set -e out[-1]
    end

    if test (math "bitand($directive, 1)") -ne 0
        return
    end

    set -l candidates (string match -- '*'\t'*' $out)
    if test (count $candidates) -eq 0
        if test (math "bitand($directive, 4)") -eq 0
            __fish_complete_path "$current"
        end
        return
    end

    printf '%s\n' $candidates
end

complete -c headlessmc -f -a '(__headlessmc_complete)'
complete -c hmc -f -a '(__headlessmc_complete)'
