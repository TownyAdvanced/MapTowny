/*
 * Copyright (c) 2021 Silverwolfg11
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

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
