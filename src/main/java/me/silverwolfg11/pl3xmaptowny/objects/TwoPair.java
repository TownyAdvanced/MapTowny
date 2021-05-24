package me.silverwolfg11.pl3xmaptowny.objects;

// Weird name to avoid the many many conflicts
public final class TwoPair<L,R> {

    private final L left;
    private final R right;

    private TwoPair(L left, R right) {
        this.left = left;
        this.right = right;
    }

    public boolean hasLeft() {
        return left != null;
    }

    public L getLeft() {
        return left;
    }

    public boolean hasFirst() {
        return hasLeft();
    }

    public L getFirst() {
        return getLeft();
    }

    public boolean hasRight() {
        return right != null;
    }

    public R getRight() {
        return right;
    }

    public boolean hasSecond() {
        return hasRight();
    }

    public R getSecond() {
        return getRight();
    }


    public static <A, B> TwoPair<A,B> of(A left, B right) {
        return new TwoPair<>(left, right);
    }
}
