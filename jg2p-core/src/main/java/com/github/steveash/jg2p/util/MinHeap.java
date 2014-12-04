/*
 * Copyright 2014 Steve Ash
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.steveash.jg2p.util;

import com.google.common.collect.AbstractIterator;

import java.util.Arrays;
import java.util.Iterator;

/**
 * A bounded min heap that has a fixed size
 *
 * @author Steve Ash
 */
public class MinHeap<T extends Comparable<T>> implements Iterable<T> {

  private final T[] array;
  private int size = 0;

  /**
   * Constructs a new BinaryHeap.
   */
  @SuppressWarnings("unchecked")
  public MinHeap(int size) {
    array = (T[]) new Comparable[size];
  }

  public void add(T value) {
    if (size == array.length) {
      throw new IllegalStateException("Can't add a new element to a full heap");
    }
    array[size] = value;
    size += 1;
    bubbleUp();
  }

  public boolean isEmpty() {
    return size == 0;
  }

  public boolean isFull() {
    return size == array.length;
  }

  public int size() {
    return size;
  }

  public T peek() {
    if (this.isEmpty()) {
      throw new IllegalStateException();
    }
    return array[0];
  }

  public T remove() {
    T result = peek();

    // replace the root with the last entry
    array[0] = array[size - 1];
    array[size - 1] = null;
    size -= 1;
    bubbleDown();

    return result;
  }

  @Override
  public String toString() {
    return Arrays.toString(array);
  }

  private void bubbleDown() {
    int index = 0;

    while (hasLeftChild(index)) {
      int leftChild = leftIndex(index);
      int smallerChild = leftChild;

      if (hasRightChild(index)) {
        int rightChild = rightIndex(index);
        if (isSmaller(rightChild, leftChild)) {
          smallerChild = rightChild;
        }
      }
      if (isSmaller(smallerChild, index)) {
        swap(index, smallerChild);
        index = smallerChild;
      } else {
        // everything is in order!
        return;
      }
    }
  }

  private void bubbleUp() {
    int index = this.size - 1;

    while (hasParent(index)) {
      int parent = parentIndex(index);
      if (isSmaller(index, parent)) {
        swap(index, parent);
        index = parent;
      } else {
        // shape property is maintained
        return;
      }
    }
  }

  private boolean isSmaller(int left, int right) {
    return array[left].compareTo(array[right]) < 0;
  }

  private boolean hasParent(int i) {
    return i > 0;
  }

  private int leftIndex(int i) {
    return (i * 2) + 1;
  }

  private int rightIndex(int i) {
    return (i * 2) + 2;
  }

  private boolean hasLeftChild(int i) {
    return leftIndex(i) < size;
  }

  private boolean hasRightChild(int i) {
    return rightIndex(i) < size;
  }

  private T parent(int i) {
    return array[parentIndex(i)];
  }

  private int parentIndex(int i) {
    if ((i & 1) == 0) {
      return i / 2;
    }
    return (i - 1) / 2;
  }

  private void swap(int index1, int index2) {
    T tmp = array[index1];
    array[index1] = array[index2];
    array[index2] = tmp;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    MinHeap minHeap = (MinHeap) o;
    if (size != minHeap.size) {
      return false;
    }
    if (!Arrays.equals(array, minHeap.array)) {
      return false;
    }
    return true;
  }

  @Override
  public int hashCode() {
    int result = Arrays.hashCode(array);
    result = 31 * result + size;
    return result;
  }

  @Override
  public Iterator<T> iterator() {
    return new AbstractIterator<T>() {
      private int i = 0;

      @Override
      protected T computeNext() {
        if (i < size) {
          T next = array[i];
          i += 1;
          return next;
        }
        return endOfData();
      }
    };
  }
}
