package lateral.lang;

final public class EmptySequence extends Sequence {
    public static final Sequence EMPTY_SEQUENCE = new EmptySequence();

    private EmptySequence() {}

    public Object first() {
        return null;
    }

    public Sequence rest() {
        return this;
    }

    public Object nth(int n) {
        return null;
    }

    public int length() {
        return 0;
    }

    public boolean equals(Object obj) {
        return obj == this || obj instanceof EmptySequence;
    }

    public boolean isEmpty() {
        return true;
    }

    public String toString() {
        return "()";
    }
}
