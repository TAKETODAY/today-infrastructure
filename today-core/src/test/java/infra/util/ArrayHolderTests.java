/*
 * Copyright 2017 - 2025 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package infra.util;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

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
  void setArrayShouldStoreElements() {
    ArrayHolder<Integer> holder = new ArrayHolder<>();
    holder.set(1, 2, 3);
    assertThat(holder.array).containsExactly(1, 2, 3);
  }

  @Test
  void addElementToEmptyHolder() {
    ArrayHolder<String> holder = new ArrayHolder<>();
    holder.add("test");
    assertThat(holder.array).containsExactly("test");
  }

  @Test
  void addElementToNonEmptyHolder() {
    ArrayHolder<String> holder = ArrayHolder.valueOf("first");
    holder.add("second");
    assertThat(holder.array).containsExactly("first", "second");
  }

  @Test
  void addArrayShouldAppendElements() {
    ArrayHolder<Integer> holder = ArrayHolder.valueOf(1, 2);
    holder.addAll(List.of(3, 4));
    assertThat(holder.array).containsExactly(1, 2, 3, 4);
  }

  @Test
  void setAtIndexShouldReplaceElement() {
    ArrayHolder<String> holder = ArrayHolder.valueOf("a", "b", "c");
    holder.set(1, "x");
    assertThat(holder.array).containsExactly("a", "x", "c");
  }

  @Test
  void addAtIndexShouldInsertElement() {
    ArrayHolder<String> holder = ArrayHolder.valueOf("a", "c");
    holder.add(1, "b");
    assertThat(holder.array).containsExactly("a", "b", "c");
  }

  @Test
  void sortShouldOrderElements() {
    ArrayHolder<Integer> holder = ArrayHolder.valueOf(3, 1, 2);
    holder.sort(Integer::compareTo);
    assertThat(holder.array).containsExactly(1, 2, 3);
  }

  @Test
  void clearShouldRemoveAllElements() {
    ArrayHolder<String> holder = ArrayHolder.valueOf("test");
    holder.clear();
    assertThat(holder.array).isNull();
  }

  @Test
  void sizeShouldReturnNumberOfElements() {
    ArrayHolder<Integer> holder = ArrayHolder.valueOf(1, 2, 3);
    assertThat(holder.size()).isEqualTo(3);

    holder.clear();
    assertThat(holder.size()).isZero();
  }

  @Test
  void isPresentShouldCheckForElements() {
    ArrayHolder<String> holder = new ArrayHolder<>();
    assertThat(holder.isPresent()).isFalse();

    holder.add("test");
    assertThat(holder.isPresent()).isTrue();

    holder.clear();
    assertThat(holder.isPresent()).isFalse();
  }

  @Test
  void isEmptyShouldCheckForNoElements() {
    ArrayHolder<String> holder = new ArrayHolder<>();
    assertThat(holder.isEmpty()).isTrue();

    holder.add("test");
    assertThat(holder.isEmpty()).isFalse();

    holder.clear();
    assertThat(holder.isEmpty()).isTrue();
  }

  @Test
  void copyOfShouldCreateNewInstance() {
    ArrayHolder<String> original = ArrayHolder.valueOf("test");
    ArrayHolder<String> copy = ArrayHolder.copyOf(original);

    assertThat(copy).isNotSameAs(original);
    assertThat(copy.array).containsExactly("test");

    original.clear();
    assertThat(copy.array).containsExactly("test");
  }

  @Test
  void toStringShouldHandleEmptyAndNonEmpty() {
    ArrayHolder<Integer> holder = new ArrayHolder<>();
    assertThat(holder.toString()).isEqualTo("[]");

    holder.addAll(1, 2, 3);
    assertThat(holder.toString()).isEqualTo("[1, 2, 3]");
  }

  @Test
  void equalsAndHashCodeShouldWorkCorrectly() {
    ArrayHolder<String> holder1 = ArrayHolder.valueOf("test");
    ArrayHolder<String> holder2 = ArrayHolder.valueOf("test");
    ArrayHolder<String> holder3 = ArrayHolder.valueOf("different");

    assertThat(holder1)
            .isEqualTo(holder2)
            .hasSameHashCodeAs(holder2)
            .isNotEqualTo(holder3);
  }

  @Test
  void addAllShouldNotModifyWhenNullCollectionGiven() {
    ArrayHolder<String> holder = ArrayHolder.valueOf("test");
    holder.addAll((Collection) null);
    assertThat(holder.array).containsExactly("test");
  }

  @Test
  void mapShouldTransformPresentValue() {
    ArrayHolder<String> holder = ArrayHolder.valueOf("test");
    Integer result = holder.map(arr -> arr.length);
    assertThat(result).isEqualTo(1);
  }

  @Test
  void mapShouldReturnNullForEmptyHolder() {
    ArrayHolder<String> holder = new ArrayHolder<>();
    Integer result = holder.map(arr -> arr.length);
    assertThat(result).isNull();
  }

  @Test
  void mapWithSupplierShouldUseSupplierForEmptyHolder() {
    ArrayHolder<String> holder = new ArrayHolder<>();
    Integer result = holder.map(arr -> arr.length, () -> 42);
    assertThat(result).isEqualTo(42);
  }

  @Test
  void ifPresentShouldExecuteActionForNonEmptyHolder() {
    ArrayHolder<String> holder = ArrayHolder.valueOf("test");
    List<String> result = new ArrayList<>();
    holder.ifPresent(arr -> result.add(arr[0]));
    assertThat(result).containsExactly("test");
  }

  @Test
  void ifPresentShouldNotExecuteActionForEmptyHolder() {
    ArrayHolder<String> holder = new ArrayHolder<>();
    List<String> result = new ArrayList<>();
    holder.ifPresent(arr -> result.add(arr[0]));
    assertThat(result).isEmpty();
  }

  @Test
  void orElseThrowShouldReturnValueWhenPresent() {
    ArrayHolder<String> holder = ArrayHolder.valueOf("test");
    assertThat(holder.orElseThrow()).containsExactly("test");
  }

  @Test
  void orElseThrowShouldThrowWhenEmpty() {
    ArrayHolder<String> holder = new ArrayHolder<>();
    assertThatThrownBy(holder::orElseThrow)
            .isInstanceOf(NoSuchElementException.class)
            .hasMessage("No value present");
  }

  @Test
  void orElseThrowWithSupplierShouldThrowCustomException() {
    ArrayHolder<String> holder = new ArrayHolder<>();
    assertThatThrownBy(() -> holder.orElseThrow(IllegalArgumentException::new))
            .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void forClassShouldCreateHolderWithElementClass() {
    ArrayHolder<String> holder = ArrayHolder.forClass(String.class);
    holder.add("test");
    assertThat(holder.array).containsExactly("test");
  }

  @Test
  void forGeneratorShouldCreateHolderWithArrayGenerator() {
    ArrayHolder<String> holder = ArrayHolder.forGenerator(String[]::new);
    holder.add("test");
    assertThat(holder.array).containsExactly("test");
  }

  @Test
  void sortShouldHandleNullOrEmptyArray() {
    ArrayHolder<String> holder = new ArrayHolder<>();
    holder.sort(String::compareTo);
    assertThat(holder.array).isNull();

    holder.set(new String[0]);
    holder.sort(String::compareTo);
    assertThat(holder.array).isEmpty();
  }

  @Test
  void valueOfWithListShouldCreateArrayHolder() {
    List<String> input = List.of("a", "b", "c");
    ArrayHolder<String> holder = ArrayHolder.valueOf(input);
    assertThat(holder.array).containsExactly("a", "b", "c");
  }

  @Test
  void ifPresentOrElseShouldHandleBothCases() {
    ArrayHolder<String> holder = ArrayHolder.valueOf("test");
    List<String> result = new ArrayList<>();
    holder.ifPresentOrElse(
            arr -> result.add(arr[0]),
            () -> result.add("empty")
    );
    assertThat(result).containsExactly("test");

    holder.clear();
    holder.ifPresentOrElse(
            arr -> result.add(arr[0]),
            () -> result.add("empty")
    );
    assertThat(result).containsExactly("test", "empty");
  }

  @Test
  void setWithNullOrEmptyCollectionShouldClearArray() {
    ArrayHolder<String> holder = ArrayHolder.valueOf("test");
    holder.set((Collection<String>) null);
    assertThat(holder.array).isNull();

    holder = ArrayHolder.valueOf("test");
    holder.set(List.of());
    assertThat(holder.array).isNull();
  }

  @Test
  void setWithCollectionShouldDetermineElementClass() {
    ArrayHolder<String> holder = new ArrayHolder<>();
    holder.set(List.of("test"));
    assertThat(holder.array).isInstanceOf(String[].class);
  }

  @Test
  void iteratorShouldWorkWithEmptyHolder() {
    ArrayHolder<String> holder = new ArrayHolder<>();
    Iterator<String> iterator = holder.iterator();
    assertThat(iterator.hasNext()).isFalse();
  }

  @Test
  void getRequiredWithCustomMessageShouldThrowWithMessage() {
    ArrayHolder<String> holder = new ArrayHolder<>();
    String message = "Custom error message";
    assertThatThrownBy(() -> holder.getRequired(message))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage(message);
  }

  @Test
  void streamShouldHandleNullArray() {
    ArrayHolder<String> holder = new ArrayHolder<>();
    assertThat(holder.stream()).isEmpty();
  }

  @Test
  void orElseShouldReturnDefaultForEmptyArray() {
    ArrayHolder<String> holder = ArrayHolder.valueOf(new String[0]);
    String[] defaultArray = { "default" };
    assertThat(holder.orElse(defaultArray)).isEqualTo(defaultArray);
  }

  @Test
  void orElseGetShouldReturnDefaultForNullArray() {
    ArrayHolder<String> holder = new ArrayHolder<>();
    assertThat(holder.orElseGet(() -> new String[] { "default" })).containsExactly("default");
  }

  @Test
  void iteratorShouldHandleNullArray() {
    ArrayHolder<String> holder = new ArrayHolder<>();
    Iterator<String> iterator = holder.iterator();
    assertThat(iterator).isExhausted();
  }

  @Test
  void mapWithNullSupplierShouldReturnNullForEmptyHolder() {
    ArrayHolder<String> holder = new ArrayHolder<>();
    assertThat((Object) (holder.map(arr -> arr.length, null))).isNull();
  }

  @Test
  void setCollectionShouldUseArrayGenerator() {
    ArrayHolder<String> holder = ArrayHolder.forGenerator(String[]::new);
    holder.set(List.of("test"));
    assertThat(holder.array).isInstanceOf(String[].class);
  }

  @Test
  void copyOfShouldHandleNullArray() {
    ArrayHolder<String> original = new ArrayHolder<>();
    ArrayHolder<String> copy = ArrayHolder.copyOf(original);
    assertThat(copy.array).isNull();
  }

  @Test
  void addAtIndexShouldHandleEmptyHolder() {
    ArrayHolder<String> holder = new ArrayHolder<>();
    holder.add(0, "test");
    assertThat(holder.array).containsExactly("test");
  }

  @Test
  void setAtIndexShouldThrowForInvalidIndex() {
    ArrayHolder<String> holder = ArrayHolder.valueOf("test");
    assertThatThrownBy(() -> holder.set(1, "invalid"))
            .isInstanceOf(IndexOutOfBoundsException.class);
  }

  @Test
  void shouldPreserveElementOrderWhenAddingMultipleTimes() {
    ArrayHolder<String> holder = new ArrayHolder<>();
    holder.add("first");
    holder.add("second");
    holder.add("third");
    assertThat(holder.array).containsExactly("first", "second", "third");
  }

  @Test
  void forClassShouldHandleNullElementClass() {
    ArrayHolder<Object> holder = ArrayHolder.forClass(null);
    holder.add("test");
    assertThat(holder.array).isInstanceOf(String[].class);
  }

  @Test
  void getRequiredShouldReturnArrayWhenPresent() {
    ArrayHolder<String> holder = ArrayHolder.valueOf("test");
    assertThat(holder.getRequired()).containsExactly("test");
  }

  @Test
  void addAllWithNullArrayShouldNotModifyHolder() {
    ArrayHolder<String> holder = ArrayHolder.valueOf("test");
    holder.addAll((String[]) null);
    assertThat(holder.array).containsExactly("test");
  }

  @Test
  void iteratorShouldSupportRemoveOperation() {
    ArrayHolder<String> holder = ArrayHolder.valueOf("a", "b", "c");
    Iterator<String> iterator = holder.iterator();
    iterator.next();
    assertThatThrownBy(iterator::remove)
            .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void addShouldThrowWhenNullElementProvided() {
    ArrayHolder<String> holder = new ArrayHolder<>();
    assertThatThrownBy(() -> holder.add(null))
            .isInstanceOf(NullPointerException.class);
  }

  @Test
  void setAtIndexShouldWorkWithEmptyHolder() {
    ArrayHolder<String> holder = new ArrayHolder<>();
    assertThatThrownBy(() -> holder.set(0, "test"))
            .isInstanceOf(IndexOutOfBoundsException.class);
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
