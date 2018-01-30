package net.davidcie.gyroscopebandaid;

import java.lang.reflect.Array;

@SuppressWarnings({"unused", "WeakerAccess"})
public class FifoArray<T> {
    private int mSize;
    private T[] underlyingArray;

    public FifoArray(final int size) {
        mSize = size;
        @SuppressWarnings("unchecked")
        final T[] a = (T[]) new Object[size];
        underlyingArray = a;
    }

    public T[] getUnderlyingArray() {
        return underlyingArray;
    }

    public void addToEnd(T elem) {
        System.arraycopy(underlyingArray, 1, underlyingArray, 0, mSize - 1);
        underlyingArray[mSize - 1] = elem;
    }

    public void addToBeginning(T elem) {
        System.arraycopy(underlyingArray, 0, underlyingArray, 1, mSize - 1);
        underlyingArray[0] = elem;
    }

    public void add(T elem) {
        addToEnd(elem);
    }

    public T get(int index) {
        return underlyingArray[index];
    }

    public void set(int index, T item) {
        if (index > (mSize - 1)) throw new IndexOutOfBoundsException();
        underlyingArray[index] = item;
    }

    public int size() {
        return mSize;
    }
}
