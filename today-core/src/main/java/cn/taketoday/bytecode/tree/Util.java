/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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
package cn.taketoday.bytecode.tree;

import java.util.ArrayList;
import java.util.List;

import cn.taketoday.util.CollectionUtils;

/**
 * Utility methods to convert an array of primitive or object values to a mutable ArrayList, not
 * baked by the array (unlike {@link java.util.Arrays#asList}).
 *
 * @author Eric Bruneton
 */
final class Util {

  private Util() { }

  static <T> List<T> add(final List<T> list, final T element) {
    List<T> newList = list == null ? new ArrayList<>(1) : list;
    newList.add(element);
    return newList;
  }

  static <T> List<T> asArrayList(final int length) {
    List<T> list = new ArrayList<>(length);
    for (int i = 0; i < length; ++i) {
      list.add(null);
    }
    return list;
  }

  static <T> List<T> asArrayList(final T[] array) {
    return CollectionUtils.newArrayList(array);
  }

  static List<Byte> asArrayList(final byte[] byteArray) {
    if (byteArray == null) {
      return new ArrayList<>();
    }
    ArrayList<Byte> byteList = new ArrayList<>(byteArray.length);
    for (byte b : byteArray) {
      byteList.add(b);
    }
    return byteList;
  }

  static List<Boolean> asArrayList(final boolean[] booleanArray) {
    if (booleanArray == null) {
      return new ArrayList<>();
    }
    ArrayList<Boolean> booleanList = new ArrayList<>(booleanArray.length);
    for (boolean b : booleanArray) {
      booleanList.add(b);
    }
    return booleanList;
  }

  static List<Short> asArrayList(final short[] shortArray) {
    if (shortArray == null) {
      return new ArrayList<>();
    }
    ArrayList<Short> shortList = new ArrayList<>(shortArray.length);
    for (short s : shortArray) {
      shortList.add(s);
    }
    return shortList;
  }

  static List<Character> asArrayList(final char[] charArray) {
    if (charArray == null) {
      return new ArrayList<>();
    }
    ArrayList<Character> charList = new ArrayList<>(charArray.length);
    for (char c : charArray) {
      charList.add(c);
    }
    return charList;
  }

  static List<Integer> asArrayList(final int[] intArray) {
    if (intArray == null) {
      return new ArrayList<>();
    }
    ArrayList<Integer> intList = new ArrayList<>(intArray.length);
    for (int i : intArray) {
      intList.add(i);
    }
    return intList;
  }

  static List<Float> asArrayList(final float[] floatArray) {
    if (floatArray == null) {
      return new ArrayList<>();
    }
    ArrayList<Float> floatList = new ArrayList<>(floatArray.length);
    for (float f : floatArray) {
      floatList.add(f);
    }
    return floatList;
  }

  static List<Long> asArrayList(final long[] longArray) {
    if (longArray == null) {
      return new ArrayList<>();
    }
    ArrayList<Long> longList = new ArrayList<>(longArray.length);
    for (long l : longArray) {
      longList.add(l);
    }
    return longList;
  }

  static List<Double> asArrayList(final double[] doubleArray) {
    if (doubleArray == null) {
      return new ArrayList<>();
    }
    ArrayList<Double> doubleList = new ArrayList<>(doubleArray.length);
    for (double d : doubleArray) {
      doubleList.add(d);
    }
    return doubleList;
  }

  static <T> List<T> asArrayList(final int length, final T[] array) {
    ArrayList<T> list = new ArrayList<>(length);
    for (int i = 0; i < length; ++i) {
      list.add(array[i]); // NOPMD(UseArraysAsList): we convert a part of the array.
    }
    return list;
  }
}
