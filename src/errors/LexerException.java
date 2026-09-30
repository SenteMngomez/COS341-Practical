package errors;

public class LexerException extends Exception{
    private final int line;
    private final int col;

    public LexerException(String message, int line, int col){
        super(message);
        this.line = line;
        this.col = col;
    }

    public int getLine(){ return line; }

    public int getCol(){ return col; }

    @Override
    public String toString(){
        return String.format("Lexical Error: %s (Line: %d, Col: %d)", getMessage(), line, col);
    }
}