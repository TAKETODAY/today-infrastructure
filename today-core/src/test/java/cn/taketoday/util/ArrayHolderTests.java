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

package cn.taketoday.util;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/1/12 15:44
 */
class ArrayHolderTests {

  @Test
  void test() {
    ArrayHolder<String> valueOf = ArrayHolder.valueOf("123");

    assertThat(valueOf.get()).hasSize(1).contains("123");

    valueOf.addAll(List.of("456"));
    assertThat(valueOf.getRequired().getClass()).isEqualTo(String[].class);
  }

  @Test
  void stream() {
    ArrayHolder<String> valueOf = ArrayHolder.valueOf("123");
    assertThat(valueOf.stream()).contains("123");

    valueOf.set((String[]) null);
    assertThat(valueOf.stream()).isEmpty();
  }

  @Test
  void orElse() {
    ArrayHolder<String> valueOf = ArrayHolder.valueOf("123");
    assertThat(valueOf.stream()).contains("123");

    assertThat(valueOf.orElse(null)).contains("123");

    valueOf.set((String[]) null);

    assertThat(valueOf.orElse(null)).isNull();
    assertThat(valueOf.orElse(new String[] {})).isEmpty();
  }

  @Test
  void orElseGet() {
    ArrayHolder<String> valueOf = ArrayHolder.valueOf("123");
    assertThat(valueOf.orElseGet(null)).contains("123");

    valueOf.set((String[]) null);

    assertThat(valueOf.orElseGet(valueOf)).isNull();
    valueOf.set("456");

    assertThat(valueOf.orElseGet(valueOf)).contains("456");
  }

  @Test
  void getOptional() {
    ArrayHolder<String> arrayHolder = ArrayHolder.valueOf("123");
    assertThat(arrayHolder.getOptional().isEmpty()).isFalse();
    assertThat(arrayHolder.getOptional().get()).contains("123");

    arrayHolder.clear();
    assertThat(arrayHolder.getOptional().isEmpty()).isTrue();
  }

  @Test
  void getRequired() {
    ArrayHolder<String> valueOf = ArrayHolder.valueOf("123");
    assertThat(valueOf.getRequired()).contains("123");

    valueOf.clear();
    assertThatThrownBy(valueOf::getRequired).isInstanceOf(IllegalStateException.class);
  }

  @Test
  void add() {
    ArrayHolder<String> valueOf = ArrayHolder.valueOf("123");
    assertThat(valueOf.getRequired()).contains("123");

    valueOf.clear();
    valueOf.add("123");
    assertThat(valueOf.getRequired()).contains("123");

  }

  @Test
  @Disabled
  void testIterator() {

    // 准备数据阶段
    ArrayHolder<Integer> arrayHolder = ArrayHolder.valueOf();

    List<Integer> list = new ArrayList<>();
    for (int i = 0; i < 100000; i++) {
      list.add(i);
    }
    arrayHolder.addAll(list);

    // 测试阶段
    int runCounts = 1000; // 执行次s数

    // For循环的测试
    long startTime1 = System.currentTimeMillis();
    for (int i = 0; i < runCounts; i++) {
      loopOfFor(list);
    }
    long endTime1 = System.currentTimeMillis();

    // Foreach循环的测试
    long startTime2 = System.currentTimeMillis();
    for (int i = 0; i < runCounts; i++) {
      loopOfForeach(list);
    }
    long endTime2 = System.currentTimeMillis();

    // Iterator迭代器的测试
    long startTime3 = System.currentTimeMillis();
    for (int i = 0; i < runCounts; i++) {
      loopOfIterator(list);
    }
    long endTime3 = System.currentTimeMillis();

    // Iterator迭代器的测试
    long startTime4 = System.currentTimeMillis();
    for (int i = 0; i < runCounts; i++) {
      loopOfArray(arrayHolder.getRequired());
    }
    long endTime4 = System.currentTimeMillis();

    long startTime5 = System.currentTimeMillis();
    for (int i = 0; i < runCounts; i++) {
      loopOfArrayHolder(arrayHolder);
    }
    long endTime5 = System.currentTimeMillis();

    System.out.println("loopOfFor: " + (endTime1 - startTime1) + "ms");
    System.out.println("loopOfForeach: " + (endTime2 - startTime2) + "ms");
    System.out.println("loopOfIterator: " + (endTime3 - startTime3) + "ms");
    System.out.println("loopOfArray: " + (endTime4 - startTime4) + "ms");
    System.out.println("loopOfArrayHolder: " + (endTime5 - startTime5) + "ms");
  }

  public static void loopOfFor(List<Integer> list) {
    int value;
    int size = list.size();
    // 基本的for
    for (int i = 0; i < size; i++) {
      value = list.get(i);
    }
  }

  /**
   * 使用forecah方法遍历数组
   *
   * @param list
   */
  public static void loopOfForeach(List<Integer> list) {
    int value;
    // foreach
    for (Integer integer : list) {
      value = integer;
    }
  }

  /**
   * 通过迭代器方式遍历数组
   *
   * @param list
   */
  public static void loopOfIterator(List<Integer> list) {
    int value;
    // iterator
    for (Iterator<Integer> iterator = list.iterator(); iterator.hasNext(); ) {
      value = iterator.next();
    }
  }

  public static void loopOfArray(Integer[] array) {
    int value;
    // iterator
    for (Integer integer : array) {
      value = integer;
    }
  }

  public static void loopOfArrayHolder(ArrayHolder<Integer> array) {
    int value;
    // iterator
    for (Integer integer : array) {
      value = integer;
    }
  }

}
