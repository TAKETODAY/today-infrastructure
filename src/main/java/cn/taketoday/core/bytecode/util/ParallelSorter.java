/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */
package cn.taketoday.core.bytecode.util;

import java.util.Comparator;

import cn.taketoday.core.bytecode.ClassVisitor;
import cn.taketoday.core.bytecode.core.AbstractClassGenerator;
import cn.taketoday.core.bytecode.core.ClassesKey;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.ReflectionUtils;

/**
 * For the efficient sorting of multiple arrays in parallel.
 * <p>
 * Given two arrays of equal length and varying types, the standard technique
 * for sorting them in parallel is to create a new temporary object for each
 * row, store the objects in a temporary array, sort the array using a custom
 * comparator, and the extract the original values back into their respective
 * arrays. This is wasteful in both time and memory.
 * <p>
 * This class generates bytecode customized to the particular set of arrays you
 * need to sort, in such a way that both arrays are sorted in-place,
 * simultaneously.
 * <p>
 * Two sorting algorithms are provided. Quicksort is best when you only need to
 * sort by a single column, as it requires very few comparisons and swaps.
 * Mergesort is best used when sorting multiple columns, as it is a "stable"
 * sort--that is, it does not affect the relative order of equal objects from
 * previous sorts.
 * <p>
 * The mergesort algorithm here is an "in-place" variant, which while slower,
 * does not require a temporary array.
 *
 * @author Chris Nokleberg
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
abstract public class ParallelSorter extends SorterTemplate {

  protected Object[] a;
  private Comparer comparer;

  protected ParallelSorter() { }

  abstract public ParallelSorter newInstance(Object[] arrays);

  /**
   * Create a new ParallelSorter object for a set of arrays. You may sort the
   * arrays multiple times via the same ParallelSorter object.
   *
   * @param arrays An array of arrays to sort. The arrays may be a mix of primitive
   * and non-primitive types, but should all be the same length.
   */
  public static ParallelSorter create(Object[] arrays) {
    Generator gen = new Generator();
    gen.setArrays(arrays);
    return gen.create();
  }

  private int len() {
    return ((Object[]) a[0]).length;
  }

  /**
   * Sort the arrays using the quicksort algorithm.
   *
   * @param index array (column) to sort by
   */
  public void quickSort(int index) {
    quickSort(index, 0, len(), null);
  }

  /**
   * Sort the arrays using the quicksort algorithm.
   *
   * @param index array (column) to sort by
   * @param lo starting array index (row), inclusive
   * @param hi ending array index (row), exclusive
   */
  public void quickSort(int index, int lo, int hi) {
    quickSort(index, lo, hi, null);
  }

  /**
   * Sort the arrays using the quicksort algorithm.
   *
   * @param index array (column) to sort by
   * @param cmp Comparator to use if the specified column is non-primitive
   */
  public void quickSort(int index, Comparator cmp) {
    quickSort(index, 0, len(), cmp);
  }

  /**
   * Sort the arrays using the quicksort algorithm.
   *
   * @param index array (column) to sort by
   * @param lo starting array index (row), inclusive
   * @param hi ending array index (row), exclusive
   * @param cmp Comparator to use if the specified column is non-primitive
   */
  public void quickSort(int index, int lo, int hi, Comparator cmp) {
    chooseComparer(index, cmp);
    super.quickSort(lo, hi - 1);
  }

  /**
   * @param index array (column) to sort by
   */
  public void mergeSort(int index) {
    mergeSort(index, 0, len(), null);
  }

  /**
   * Sort the arrays using an in-place merge sort.
   *
   * @param index array (column) to sort by
   * @param lo starting array index (row), inclusive
   * @param hi ending array index (row), exclusive
   */
  public void mergeSort(int index, int lo, int hi) {
    mergeSort(index, lo, hi, null);
  }

  /**
   * Sort the arrays using an in-place merge sort.
   *
   * @param index array (column) to sort by
   */
  public void mergeSort(int index, Comparator cmp) {
    mergeSort(index, 0, len(), cmp);
  }

  /**
   * Sort the arrays using an in-place merge sort.
   *
   * @param index array (column) to sort by
   * @param lo starting array index (row), inclusive
   * @param hi ending array index (row), exclusive
   * @param cmp Comparator to use if the specified column is non-primitive
   */
  public void mergeSort(int index, int lo, int hi, Comparator cmp) {
    chooseComparer(index, cmp);
    super.mergeSort(lo, hi - 1);
  }

  private void chooseComparer(int index, Comparator cmp) {
    final Object array = a[index];
    final Class<?> type = array.getClass();
    if (type == int[].class) {
      comparer = new IntComparer((int[]) array);
    }
    else if (type == long[].class) {
      comparer = new LongComparer((long[]) array);
    }
    else if (type == double[].class) {
      comparer = new DoubleComparer((double[]) array);
    }
    else if (type == float[].class) {
      comparer = new FloatComparer((float[]) array);
    }
    else if (type == short[].class) {
      comparer = new ShortComparer((short[]) array);
    }
    else if (type == byte[].class) {
      comparer = new ByteComparer((byte[]) array);
    }
    else if (cmp != null) {
      comparer = new ComparatorComparer((Object[]) array, cmp);
    }
    else {
      comparer = new ObjectComparer((Object[]) array);
    }
  }

  @Override
  protected int compare(int i, int j) {
    return comparer.compare(i, j);
  }

  interface Comparer {
    int compare(int i, int j);
  }

  static class ComparatorComparer implements Comparer {
    private final Object[] a;
    private final Comparator cmp;

    public ComparatorComparer(Object[] a, Comparator cmp) {
      this.a = a;
      this.cmp = cmp;
    }

    @Override
    public int compare(int i, int j) {
      return cmp.compare(a[i], a[j]);
    }
  }

  static class ObjectComparer implements Comparer {
    private final Object[] a;

    public ObjectComparer(Object[] a) {
      this.a = a;
    }

    @Override
    public int compare(int i, int j) {
      return ((Comparable) a[i]).compareTo(a[j]);
    }
  }

  static class IntComparer implements Comparer {
    private final int[] a;

    public IntComparer(int[] a) {
      this.a = a;
    }

    @Override
    public int compare(int i, int j) {
      return a[i] - a[j];
    }
  }

  static class LongComparer implements Comparer {
    private final long[] a;

    public LongComparer(long[] a) {
      this.a = a;
    }

    @Override
    public int compare(int i, int j) {
      long vi = a[i];
      long vj = a[j];
      return Long.compare(vi, vj);
    }
  }

  static class FloatComparer implements Comparer {
    private final float[] a;

    public FloatComparer(float[] a) {
      this.a = a;
    }

    @Override
    public int compare(int i, int j) {
      float vi = a[i];
      float vj = a[j];
      return Float.compare(vi, vj);
    }
  }

  static class DoubleComparer implements Comparer {
    private final double[] a;

    public DoubleComparer(double[] a) {
      this.a = a;
    }

    @Override
    public int compare(int i, int j) {
      double vi = a[i];
      double vj = a[j];
      return Double.compare(vi, vj);
    }
  }

  static class ShortComparer implements Comparer {
    private final short[] a;

    public ShortComparer(short[] a) {
      this.a = a;
    }

    @Override
    public int compare(int i, int j) {
      return a[i] - a[j];
    }
  }

  static class ByteComparer implements Comparer {
    private final byte[] a;

    public ByteComparer(byte[] a) {
      this.a = a;
    }

    @Override
    public int compare(int i, int j) {
      return a[i] - a[j];
    }
  }

  public static class Generator extends AbstractClassGenerator {
    private Object[] arrays;

    public Generator() {
      super(ParallelSorter.class);
    }

    protected ClassLoader getDefaultClassLoader() {
      return null; // TODO
    }

    public void setArrays(Object[] arrays) {
      this.arrays = arrays;
    }

    public ParallelSorter create() {
      return (ParallelSorter) super.create(ClassesKey.create(arrays));
    }

    @Override
    public void generateClass(ClassVisitor v) throws Exception {
      final Object[] arrays = this.arrays;
      if (arrays.length == 0) {
        throw new IllegalArgumentException("No arrays specified to sort");
      }
      for (final Object array : arrays) {
        if (!ObjectUtils.isArray(array)) {
          throw new IllegalArgumentException(array.getClass() + " is not an array");
        }
      }
      new ParallelSorterEmitter(v, getClassName(), arrays);
    }

    @Override
    protected Object firstInstance(Class type) {
      return ((ParallelSorter) ReflectionUtils.newInstance(type)).newInstance(arrays);
    }

    @Override
    protected Object nextInstance(Object instance) {
      return ((ParallelSorter) instance).newInstance(arrays);
    }
  }
}
