package io.github.headlesshq.headlessmc.api.command.picocli;

class SyntaxError extends RuntimeException {
    private static final long serialVersionUID = 1L;

    private final int line;
    private final int column;

    public SyntaxError(int line, int column, String message) {
        super(message);
        this.line = line;
        this.column = column;
    }

    public int column() {
        return column;
    }

    public int line() {
        return line;
    }

}
