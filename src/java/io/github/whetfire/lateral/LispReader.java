package io.github.whetfire.lateral;

import java.io.*;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;

// TODO: add line and column metadata to symbols
public class LispReader {
    private Reader stream;
    private Deque<Character> deque = new ArrayDeque<>();

    static public Symbol QUOTE = Symbol.makeSymbol("quote");
    static public Symbol UNQUOTE = Symbol.makeSymbol("unquote");
    static public Symbol UNQUOTE_SPLICING = Symbol.makeSymbol("unquote-splicing");
    static public Symbol LIST = Symbol.makeSymbol("list");
    static public Symbol CONCAT = Symbol.makeSymbol("concat");

    public LispReader(Reader reader) {
        this.stream = reader;
    }

    public static LispReader fileReader(String path) throws IOException {
        return new LispReader(new BufferedReader(new FileReader(path)));
    }

    public static LispReader stringReader(String value) {
        return new LispReader(new StringReader(value));
    }

    private boolean hasNextChar() throws IOException {
        if (deque.isEmpty()) {
            int next = stream.read();
            if (next == -1)
                return false;
            else {
                deque.addLast((char) next);
                return true;
            }
        } else {
            return true;
        }
    }

    private char nextChar() throws IOException {
        if(!deque.isEmpty()) {
            return deque.removeFirst();
        } else {
            return (char)stream.read();
        }
    }

    private char peekChar() throws IOException {
        if(deque.isEmpty()) {
            int next = stream.read();
            if(next != -1)
                deque.addLast((char) next);
            return (char) next;
        } else {
            return deque.peekFirst();
        }
    }

    private void consumeWhitespace() throws IOException {
        while(hasNextChar()) {
            char c = peekChar();
            if(c != ' ' && c != '\n') {
                break;
            }
            nextChar();
        }
    }

    private void consumeComment() throws IOException {
        while(hasNextChar()) {
            char c = peekChar();
            if(c == '\n')
                break;
            nextChar();
        }
    }

    private String consumeString() throws IOException {
        StringBuilder sb = new StringBuilder();
        while(hasNextChar()) {
            char c = peekChar();
            if(c == '"') {
                nextChar();
                break;
            } else if (c == '\\'){
                // escape sequences
                throw new RuntimeException();
            }
            sb.append(c);
            nextChar();
        }
        return sb.toString();
    }

    Object readAtom(String value) {
        if(value == null || value.length() == 0) {
            throw new RuntimeException();
        }

        if(value.charAt(0) == ':') {
            return Keyword.makeKeyword(value.substring(1));
        } else if('0' <= value.charAt(0) && value.charAt(0) <= '9') {
            // TODO: other numerical literals
            return Integer.parseInt(value);
        } else {
            return Symbol.makeSymbol(value);
        }
    }

    Sequence readList() throws IOException {
        ArrayList<Object> forms = new ArrayList<>();
        Object form;
        while(hasNextChar()) {
            form = readForm();
            if(form != null && form.equals(')')) {
                // end of list
                return ArraySequence.makeList(forms.toArray());
            } else {
                forms.add(form);
            }
        }
        throw new RuntimeException("got EOF while reading list");
    }

    Object readForm() throws IOException {
        while(hasNextChar()) {
            consumeWhitespace();
            if(hasNextChar() && peekChar() == ';') {
                consumeComment();
            } else {
                break;
            }
        }

        if(!hasNextChar())
            return null;

        char c = nextChar();
        if(c == '"') {
            return consumeString();
        } else if(c == '\'') {
            return LinkedList.makeList(QUOTE, readForm());
        } else if(c == '`') {
            return readQuasiQuote();
        } else if(c == ',') {
            if(hasNextChar() && peekChar() == '@') {
                nextChar(); // consume '@'
                return LinkedList.makeList(UNQUOTE_SPLICING, readForm());
            } else {
                return LinkedList.makeList(UNQUOTE, readForm());
            }
        }
        // reader macros here

        else if(c == '(') {
            return readList();
        } else if(c == ')') {
            return ')';
        }

        StringBuilder sb = new StringBuilder();
        sb.append(c);
        read:
        while(hasNextChar()) {
            c = peekChar();
            switch (c) {
                case '(':
                case ')':
                case ' ':
                case '\n':
                    break read;
                default:
                    sb.append(c);
                    nextChar();
                    break;
            }
        }
        return readAtom(sb.toString());
    }

    /**
     * Ahh, the glorious quasiquote, the crown jewel of programs that write programs.
     * I have a truly marvelous explanation of quasiquote which cannot fit in this doc comment.
     * @param list The body of the quasiquoted expression
     * @return An expanded representation of list
     */
    private Sequence quasiQuoteHelper(Sequence list) {
        ArrayList<Object> forms = new ArrayList<>();
        forms.add(CONCAT);
        while(!list.isEmpty()) {
            Object head = list.first();
            if(head instanceof Sequence) {
                Sequence inner = (Sequence) head;
                if (inner.first().equals(QUOTE)) {
                    // `(... (quote x) ...) -> (concat ... (list (quote (quote x))) ...)
                    forms.add(LinkedList.makeList(LIST, inner));
                } else if (inner.first().equals(UNQUOTE_SPLICING)) {
                    // `(... (uqs (a b c)) ...) -> (concat ... (a b c) ...)
                    // inner.first() is "unquote-splcing", inner.second() is the inner body
                    forms.add(inner.second());
                } else if (inner.first().equals(UNQUOTE)) {
                    // `(... (unquote x) ...) -> (concat ... (list x) ...)
                    forms.add(LinkedList.makeList(LIST, inner.second()));
                } else {
                    // `(... (a b c) ...) -> (concat ... (list (concat ~expand a b c~)) ...)
                    forms.add(LinkedList.makeList(LIST, quasiQuoteHelper(inner)));
                }
            } else {
                // `(... x ...) -> (concat ... (list (quote x)) ...)
                forms.add(LinkedList.makeList(LIST, LinkedList.makeList(QUOTE, head)));
            }
            list = list.rest();
        }
        return new ArraySequence(forms.toArray());
    }

    private Object readQuasiQuote() throws IOException {
        Object quoteBody = readForm();
        if(quoteBody == null) {
            throw new RuntimeException("Unexpected EOF in readQuasiQuote");
        } else if(quoteBody instanceof Sequence) {
            Sequence seqBody = (Sequence) quoteBody;
            if(seqBody.first().equals(UNQUOTE)) {
                // TODO: not sure if this is in the right place
                return seqBody.second();
            } else if(seqBody.first().equals(UNQUOTE_SPLICING)) {
                throw new RuntimeException("pretty sure quasiquote - unquote splicing is illegal");
            } else {
                return quasiQuoteHelper((Sequence) quoteBody);
            }
        } else {
            // simple form is just quoted since it can't contain unquotes
            return LinkedList.makeList(QUOTE, quoteBody);
        }
    }

    public static void main(String[] args) throws IOException {
        LispReader reader = fileReader("./src/lisp/test.lisp");
        Object form;
        while((form = reader.readForm()) != null) {
            System.out.println(form);
        }
    }
}
