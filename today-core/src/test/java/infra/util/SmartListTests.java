/*
 * Copyright 2017 - 2026 the TODAY authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package infra.util;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import infra.lang.Constant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/9/4 21:20
 */
class SmartListTests {

  @Test
  public void testEmpty() {
    assertThat(new SmartList<Integer>()).isEmpty();
  }

  @Test
  public void testOneElement() {
    List<Integer> l = new SmartList<>();
    l.add(1);
    assertThat(l).hasSize(1);
    assertThat(l.get(0)).isEqualTo(1);

    assertThat(l.indexOf(1)).isEqualTo(0);
    assertThat(l.indexOf(2)).isEqualTo(-1);
    assertThat(l.contains(1)).isTrue();
    assertThat(l.contains(2)).isFalse();
  }

  @Test
  public void testTwoElement() {
    List<Integer> l = new SmartList<>();
    l.add(1);
    l.add(2);
    assertThat(l).hasSize(2);
    assertThat(l.get(0)).isEqualTo(1);
    assertThat(l.get(1)).isEqualTo(2);

    assertThat(l.indexOf(1)).isEqualTo(0);
    assertThat(l.indexOf(2)).isEqualTo(1);
    assertThat(l.contains(1)).isTrue();
    assertThat(l.contains(2)).isTrue();
    assertThat(l.indexOf(42)).isEqualTo(-1);
    assertThat(l.contains(42)).isFalse();
  }

  @Test
  public void testThreeElement() {
    List<Integer> l = new SmartList<>();
    l.add(1);
    l.add(2);
    l.add(3);
    assertThat(l).hasSize(3);
    assertThat(l.get(0)).isEqualTo(1);
    assertThat(l.get(1)).isEqualTo(2);
    assertThat(l.get(2)).isEqualTo(3);
  }

  @Test
  public void testFourElement() {
    SmartList<Integer> l = new SmartList<>();
    int modCount = 0;
    assertThat(l.getModificationCount()).isEqualTo(modCount);
    l.add(1);
    assertThat(l.getModificationCount()).isEqualTo(++modCount);
    l.add(2);
    assertThat(l.getModificationCount()).isEqualTo(++modCount);
    l.add(3);
    assertThat(l.getModificationCount()).isEqualTo(++modCount);
    l.add(4);
    assertThat(l.getModificationCount()).isEqualTo(++modCount);
    assertThat(l).hasSize(4);
    assertThat(l.get(0)).isEqualTo(1);
    assertThat(l.get(1)).isEqualTo(2);
    assertThat(l.get(2)).isEqualTo(3);
    assertThat(l.get(3)).isEqualTo(4);
    assertThat(l.getModificationCount()).isEqualTo(modCount);

    l.remove(2);
    assertThat(l).hasSize(3);
    assertThat(l.getModificationCount()).isEqualTo(++modCount);
    assertThat(l.toString()).isEqualTo("[1, 2, 4]");

    l.set(2, 3);
    assertThat(l).hasSize(3);
    assertThat(l.getModificationCount()).isEqualTo(modCount);
    assertThat(l.toString()).isEqualTo("[1, 2, 3]");

    l.clear();
    assertThat(l).isEmpty();
    assertThat(l.getModificationCount()).isEqualTo(++modCount);
    assertThat(l.toString()).isEqualTo("[]");

    assertThatThrownBy(() -> l.set(1, 3))
            .isInstanceOf(IndexOutOfBoundsException.class);

  }

  @Test
  public void testFourElement2() {
    SmartList<Integer> l = new SmartList<>();
    int modCount = 0;

    l.clear();
    assertThat(l).isEmpty();
    assertThat(l.getModificationCount()).isEqualTo(++modCount);
    assertThat(l.toString()).isEqualTo("[]");

    Iterator<Integer> iterator = l.iterator();
    assertThat(iterator).isSameAs(Collections.emptyIterator());
    assertThat(iterator.hasNext()).isFalse();

    l.add(-2);
    iterator = l.iterator();
    assertThat(iterator).isNotSameAs(Collections.emptyIterator());
    assertThat(iterator.hasNext()).isTrue();
    assertThat(iterator.next()).isEqualTo(-2);
    assertThat(iterator.hasNext()).isFalse();

    assertThatThrownBy(() -> l.get(1)).isInstanceOf(IndexOutOfBoundsException.class);
  }

  @SuppressWarnings({ "CollectionAddedToSelf" })
  @Test
  public void testFourElement3() {
    SmartList<Integer> l = new SmartList<>();
    l.clear();
    l.add(-2);
    assertThatThrownBy(() -> l.addAll(l))
            .isInstanceOf(ConcurrentModificationException.class);

    assertThat(l).hasSize(2);
    assertThat(l.toString()).isEqualTo("[-2, -2]");
    assertThatThrownBy(() -> l.addAll(l))
            .isInstanceOf(ConcurrentModificationException.class);
  }

  @Test
  public void testAddIndexedNegativeIndex() {
    assertThatThrownBy(() -> new SmartList<Integer>().add(-1, 1))
            .isInstanceOf(IndexOutOfBoundsException.class);
  }

  @Test
  public void testAddIndexedWrongIndex() {
    assertThatThrownBy(() -> new SmartList<>(1).add(3, 1))
            .isInstanceOf(IndexOutOfBoundsException.class);
  }

  @Test
  public void testAddIndexedEmptyWrongIndex() {
    assertThatThrownBy(() -> new SmartList<>().add(1, 1))
            .isInstanceOf(IndexOutOfBoundsException.class);
  }

  @Test
  public void testAddIndexedEmpty() {
    SmartList<Integer> l = new SmartList<>();
    int modCount = 0;
    l.add(0, 1);
    assertThat(l.getModificationCount()).isEqualTo(++modCount);
    assertThat(l).hasSize(1);
    assertThat(l.get(0)).isEqualTo(1);
  }

  @Test
  public void testAddIndexedOneElement() {
    SmartList<Integer> l = new SmartList<>(0);
    assertThat(l).hasSize(1);

    int modCount = l.getModificationCount();
    l.add(0, 42);
    assertThat(l.getModificationCount()).isEqualTo(++modCount);
    assertThat(l).hasSize(2);
    assertThat(l.get(0)).isEqualTo(42);
    assertThat(l.get(1)).isEqualTo(0);
  }

  @Test
  public void testAddIndexedOverOneElement() {
    SmartList<Integer> l = new SmartList<>(0);
    assertThat(l).hasSize(1);

    int modCount = l.getModificationCount();
    l.add(1, 42);
    assertThat(l.getModificationCount()).isEqualTo(++modCount);
    assertThat(l).hasSize(2);
    assertThat(l.get(0)).isEqualTo(0);
    assertThat(l.get(1)).isEqualTo(42);
  }

  @Test
  public void testAddIndexedOverTwoElements() {
    SmartList<Integer> l = new SmartList<>(0, 1);
    assertThat(l).hasSize(2);

    int modCount = l.getModificationCount();
    l.add(1, 42);
    assertThat(l.getModificationCount()).isEqualTo(++modCount);
    assertThat(l).hasSize(3);
    assertThat(l.get(0)).isEqualTo(0);
    assertThat(l.get(1)).isEqualTo(42);
    assertThat(l.get(2)).isEqualTo(1);
  }

  @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
  @Test
  public void testEmptyToArray() {
    SmartList<Integer> l = new SmartList<>();
    assertThat(new Integer[] {}).isEqualTo(l.toArray());
    assertThat(new Integer[] {}).isEqualTo(l.toArray(new Integer[] {}));
  }

  @Test
  public void testSingleToArray() {
    assertThat(new SmartList<>("foo").toArray(Constant.EMPTY_STRING_ARRAY)).containsExactly("foo");
  }

  @Test
  public void testToArray() {
    SmartList<Integer> l = new SmartList<>(0, 1);
    assertThat(l.toArray()).isEqualTo(new Object[] { 0, 1 });
    assertThat(l.toArray()).isEqualTo(new Integer[] { 0, 1 });
    assertThat(l.toArray(new Integer[0])).isEqualTo(new Integer[] { 0, 1 });

    assertThat(l.toArray(new Integer[4])).containsExactly(0, 1, null, null);

    l.remove(1);

    checkForEach(l);

    assertThat(l.toArray(new Integer[4])).containsExactly(0, null, null, null);
    assertThat(l.toArray()).containsExactly(0);
  }

  @Test
  public void testNullIndexOf() {
    List<Integer> l = new SmartList<>();
    l.add(null);
    l.add(null);

    assertThat(l.indexOf(null)).isEqualTo(0);
    assertThat(l.contains(null)).isTrue();
    assertThat(l.indexOf(42)).isEqualTo(-1);
    assertThat(l.contains(42)).isFalse();
  }

  @Test
  public void testEqualsSelf() {
    List<Integer> list = new SmartList<>();

    assertThat(list).isEqualTo(list);
  }

  @SuppressWarnings({ "EqualsBetweenInconvertibleTypes", "MismatchedQueryAndUpdateOfCollection" })
  @Test
  public void testEqualsNonListCollection() {
    List<Integer> list = new SmartList<>();

    assertThat(list.equals(new HashSet<>())).isFalse();
  }

  @Test
  public void testEqualsEmptyList() {
    List<Integer> list = new SmartList<>();

    assertThat(list).isEqualTo(new SmartList<>());
    assertThat(list).isEqualTo(new ArrayList<>());
    assertThat(list).isEqualTo(new LinkedList<>());
    assertThat(list).isEqualTo(Collections.emptyList());
    assertThat(list).isEqualTo(List.of());
    assertThat(list).isEqualTo(Collections.emptyList());

    assertThat(list).isNotEqualTo(new SmartList<>(1));
    assertThat(list).isNotEqualTo(new ArrayList<>(Collections.singletonList(1)));
    assertThat(list).isNotEqualTo(new LinkedList<>(Collections.singletonList(1)));
    assertThat(list).isNotEqualTo(List.of(1));
    assertThat(list).isNotEqualTo(Collections.singletonList(1));
  }

  @Test
  public void testEqualsListWithSingleNullableElement() {
    List<Integer> list = new SmartList<>((Integer) null);

    assertThat(list).isEqualTo(new SmartList<>((Integer) null));
    assertThat(list).isEqualTo(new ArrayList<>(Collections.singletonList(null)));
    assertThat(list).isEqualTo(new LinkedList<>(Collections.singletonList(null)));
    assertThat(list).isEqualTo(Arrays.asList(new Integer[] { null }));
    assertThat(list).isEqualTo(Collections.singletonList(null));

    assertThat(list).isNotEqualTo(new SmartList<>());
    assertThat(list).isNotEqualTo(new ArrayList<>());
    assertThat(list).isNotEqualTo(new LinkedList<>());
    assertThat(list).isNotEqualTo(Collections.emptyList());
    assertThat(list).isNotEqualTo(List.of());
    assertThat(list).isNotEqualTo(Collections.emptyList());
  }

  @Test
  public void testEqualsListWithSingleNonNullElement() {
    List<Integer> list = new SmartList<>(1);

    checkForEach(list);

    assertThat(list).isEqualTo(new SmartList<>(1));
    assertThat(list).isEqualTo(new ArrayList<>(List.of(1)));
    assertThat(list).isEqualTo(new LinkedList<>(List.of(1)));
    assertThat(list).isEqualTo(List.of(1));
    assertThat(list).isEqualTo(Collections.singletonList(1));

    assertThat(list).isNotEqualTo(new SmartList<>());
    assertThat(list).isNotEqualTo(new ArrayList<>());
    assertThat(list).isNotEqualTo(new LinkedList<>());
    assertThat(list).isNotEqualTo(Collections.emptyList());
    assertThat(list).isNotEqualTo(List.of());
    assertThat(list).isNotEqualTo(Collections.emptyList());
  }

  @Test
  public void testEqualsListWithMultipleElements() {
    List<Integer> list = new SmartList<>(1, null, 3);

    checkForEach(list);

    assertThat(list).isEqualTo(new SmartList<>(1, null, 3));
    assertThat(list).isEqualTo(new ArrayList<>(Arrays.asList(1, null, 3)));
    assertThat(list).isEqualTo(new LinkedList<>(Arrays.asList(1, null, 3)));
    assertThat(list).isEqualTo(Arrays.asList(1, null, 3));

    assertThat(list).isNotEqualTo(new SmartList<>(Integer.valueOf(1), Integer.valueOf(2), Integer.valueOf(3)));
    assertThat(list).isNotEqualTo(new ArrayList<>(Arrays.asList(Integer.valueOf(1), Integer.valueOf(2), Integer.valueOf(3))));
    assertThat(list).isNotEqualTo(new LinkedList<>(Arrays.asList(Integer.valueOf(1), Integer.valueOf(2), Integer.valueOf(3))));
    assertThat(list).isNotEqualTo(Arrays.asList(Integer.valueOf(1), Integer.valueOf(2), Integer.valueOf(3)));
  }

  @Test
  void listSupportsBulkAddAllOperation() {
    SmartList<String> list = new SmartList<>();
    list.addAll(Arrays.asList("a", "b", "c"));
    assertThat(list).containsExactly("a", "b", "c");
  }

  @Test
  void listSupportsRemoveAllOperation() {
    SmartList<String> list = new SmartList<>("a", "b", "c", "b");
    list.removeAll(Collections.singleton("b"));
    assertThat(list).containsExactly("a", "c");
  }

  @Test
  void listSupportsRetainAllOperation() {
    SmartList<String> list = new SmartList<>("a", "b", "c", "b");
    list.retainAll(Arrays.asList("b", "c"));
    assertThat(list).containsExactly("b", "c", "b");
  }

  @Test
  void listCanBeCleared() {
    SmartList<String> list = new SmartList<>("a", "b", "c");
    list.clear();
    assertThat(list).isEmpty();
    assertThat(list.size()).isZero();
  }

  @Test
  void listCanBeSorted() {
    SmartList<String> list = new SmartList<>("c", "a", "b");
    list.sort(String::compareTo);
    assertThat(list).containsExactly("a", "b", "c");
  }

  @Test
  void trimToSizeReducesCapacity() {
    SmartList<String> list = new SmartList<>("a", "b", "c");
    list.remove(2);
    list.trimToSize();
    assertThat(list).containsExactly("a", "b");
  }

  @Test
  void toArrayConvertsToArray() {
    SmartList<String> list = new SmartList<>("a", "b", "c");
    String[] array = list.toArray(new String[0]);
    assertThat(array).containsExactly("a", "b", "c");
  }

  @Test
  void toArrayWithLargerArrayFillsNullAtEnd() {
    SmartList<String> list = new SmartList<>("a", "b");
    String[] array = list.toArray(new String[3]);
    assertThat(array).containsExactly("a", "b", null);
  }

  @Test
  void equalsWithDifferentListImplementations() {
    SmartList<String> smartList = new SmartList<>("a", "b");
    List<String> arrayList = new ArrayList<>(Arrays.asList("a", "b"));
    List<String> linkedList = new LinkedList<>(Arrays.asList("a", "b"));

    assertThat(smartList).isEqualTo(arrayList);
    assertThat(smartList).isEqualTo(linkedList);
  }

  @Test
  void addAllAtSpecificIndex() {
    SmartList<String> list = new SmartList<>("a", "b");
    list.addAll(1, Arrays.asList("x", "y"));
    assertThat(list).containsExactly("a", "x", "y", "b");
  }

  @Test
  void subListReturnsViewOfPortion() {
    SmartList<String> list = new SmartList<>("a", "b", "c", "d");
    List<String> subList = list.subList(1, 3);
    assertThat(subList).containsExactly("b", "c");
  }

  @Test
  void lastIndexOfFindsLastOccurrence() {
    SmartList<String> list = new SmartList<>("a", "b", "a", "c");
    assertThat(list.lastIndexOf("a")).isEqualTo(2);
  }

  @Test
  void containsAllChecksForAllElements() {
    SmartList<String> list = new SmartList<>("a", "b", "c");
    assertThat(list.containsAll(Arrays.asList("a", "c"))).isTrue();
    assertThat(list.containsAll(Arrays.asList("a", "d"))).isFalse();
  }

  @Test
  void removeAllWithEmptyCollectionDoesNotModifyList() {
    SmartList<String> list = new SmartList<>("a", "b", "c");
    list.removeAll(Collections.emptyList());
    assertThat(list).containsExactly("a", "b", "c");
  }

  @Test
  void removeAllWithNonExistentElementsDoesNotModifyList() {
    SmartList<String> list = new SmartList<>("a", "b", "c");
    list.removeAll(Arrays.asList("x", "y", "z"));
    assertThat(list).containsExactly("a", "b", "c");
  }

  @Test
  void forEachRemainingBehavesCorrectlyWithIterator() {
    SmartList<String> list = new SmartList<>("a", "b", "c");
    List<String> collected = new ArrayList<>();
    Iterator<String> iterator = list.iterator();
    iterator.next(); // Skip first element
    iterator.forEachRemaining(collected::add);
    assertThat(collected).containsExactly("b", "c");
  }

  @Test
  void iteratorThrowsConcurrentModificationException() {
    SmartList<String> list = new SmartList<>("a", "b", "c");
    Iterator<String> iterator = list.iterator();
    list.add("d");
    assertThatThrownBy(iterator::next)
            .isInstanceOf(ConcurrentModificationException.class);
  }

  @Test
  void subListModificationsAreReflectedInMainList() {
    SmartList<String> list = new SmartList<>("a", "b", "c", "d");
    List<String> subList = list.subList(1, 3);
    subList.remove("b");
    assertThat(list).containsExactly("a", "c", "d");
  }

  @Test
  void addAllAtIndexWithEmptyCollectionDoesNotModify() {
    SmartList<String> list = new SmartList<>("a", "b");
    list.addAll(1, Collections.emptyList());
    assertThat(list).containsExactly("a", "b");
  }

  @Test
  void setAllowsReplacingNullValues() {
    SmartList<String> list = new SmartList<>("a", null, "c");
    list.set(1, "b");
    assertThat(list).containsExactly("a", "b", "c");
  }

  @Test
  void sortWithNullComparatorUsesNaturalOrdering() {
    SmartList<String> list = new SmartList<>("c", "a", "b");
    list.sort(null);
    assertThat(list).containsExactly("a", "b", "c");
  }

  @Test
  void addAllPreservesOrderOfAddedElements() {
    SmartList<String> list = new SmartList<>("a");
    List<String> toAdd = Arrays.asList("b", "c", "d");
    list.addAll(toAdd);
    assertThat(list).containsExactly("a", "b", "c", "d");
  }

  @Test
  void hashCodeChangesWithModifications() {
    SmartList<String> list = new SmartList<>("a", "b");
    int initialHashCode = list.hashCode();
    list.add("c");
    assertThat(list.hashCode()).isNotEqualTo(initialHashCode);
  }

  @Test
  void listMaintainsInsertionOrderWithMultipleAddRemoveOperations() {
    SmartList<String> list = new SmartList<>();
    list.add("a");
    list.add("b");
    list.add(1, "c");
    list.remove(0);
    list.add(0, "d");
    assertThat(list).containsExactly("d", "c", "b");
  }

  @Test
  void toStringReturnsCorrectRepresentation() {
    SmartList<String> emptyList = new SmartList<>();
    assertThat(emptyList.toString()).isEqualTo("[]");

    SmartList<String> singleItemList = new SmartList<>("test");
    assertThat(singleItemList.toString()).isEqualTo("test");

    SmartList<String> multiItemList = new SmartList<>("a", "b", "c");
    assertThat(multiItemList.toString()).isEqualTo("[a, b, c]");
  }

  @Test
  void addAllAtIndexPreservesExistingElements() {
    SmartList<String> list = new SmartList<>("a", "b", "c");
    list.addAll(1, List.of("x", "y"));
    assertThat(list).containsExactly("a", "x", "y", "b", "c");
  }

  @Test
  void removeOperationShiftsElementsCorrectly() {
    SmartList<String> list = new SmartList<>("a", "b", "c", "d");
    String removed = list.remove(1);
    assertThat(removed).isEqualTo("b");
    assertThat(list).containsExactly("a", "c", "d");
  }

  @Test
  void forEachHandlesAllListSizes() {
    List<String> collected = new ArrayList<>();

    SmartList<String> emptyList = new SmartList<>();
    emptyList.forEach(collected::add);
    assertThat(collected).isEmpty();

    SmartList<String> singleList = new SmartList<>("a");
    singleList.forEach(collected::add);
    assertThat(collected).containsExactly("a");

    collected.clear();
    SmartList<String> multiList = new SmartList<>("a", "b", "c");
    multiList.forEach(collected::add);
    assertThat(collected).containsExactly("a", "b", "c");
  }

  @Test
  void constructorHandlesNullElements() {
    SmartList<String> list = new SmartList<>(null, "a", null);
    assertThat(list).containsExactly(null, "a", null);
  }

  @Test
  void equalsHandlesDifferentImplementations() {
    SmartList<String> list1 = new SmartList<>("a", "b");
    List<String> list2 = Arrays.asList("a", "b");
    List<String> list3 = new ArrayList<>(Arrays.asList("a", "b"));

    assertThat(list1).isEqualTo(list2);
    assertThat(list1).isEqualTo(list3);
  }

  @Test
  void sortHandlesNullElements() {
    SmartList<String> list = new SmartList<>("c", null, "a", null, "b");
    list.sort(Comparator.nullsFirst(String::compareTo));
    assertThat(list).containsExactly(null, null, "a", "b", "c");
  }

  @Test
  void constructorFromCollectionHandlesAllSizes() {
    Collection<String> empty = Collections.emptyList();
    assertThat(new SmartList<>(empty)).isEmpty();

    Collection<String> single = Collections.singleton("a");
    assertThat(new SmartList<>(single)).containsExactly("a");

    Collection<String> multiple = Arrays.asList("a", "b", "c");
    assertThat(new SmartList<>(multiple)).containsExactly("a", "b", "c");
  }

  @Test
  void indexOfWithNullValuesInMiddle() {
    SmartList<String> list = new SmartList<>("a", null, "b", null, "c");
    assertThat(list.indexOf(null)).isEqualTo(1);
  }

  @Test
  void listHandlesModificationsDuringIteration() {
    SmartList<String> list = new SmartList<>("a", "b", "c");
    Iterator<String> iterator = list.iterator();
    iterator.next();
    iterator.remove();
    assertThat(list).containsExactly("b", "c");
  }

  @Test
  void retainAllWithEmptyCollectionClearsTheList() {
    SmartList<String> list = new SmartList<>("a", "b", "c");
    list.retainAll(Collections.emptyList());
    assertThat(list).isEmpty();
  }

  @Test
  void addAllWithNullCollectionThrowsException() {
    SmartList<String> list = new SmartList<>();
    Collection<String> nullCollection = null;
    assertThatThrownBy(() -> list.addAll(nullCollection))
            .isInstanceOf(NullPointerException.class);
  }

  @Test
  void listStaysConsistentAfterFailedOperation() {
    SmartList<String> list = new SmartList<>("a", "b");
    Collection<String> nullCollection = null;

    try {
      list.addAll(nullCollection);
    }
    catch (NullPointerException ignored) { }

    assertThat(list).containsExactly("a", "b");
  }

  @Test
  void subListModificationsFailAfterMainListModification() {
    SmartList<String> list = new SmartList<>("a", "b", "c");
    List<String> subList = list.subList(1, 2);
    list.add("d");
    assertThatThrownBy(() -> subList.get(0))
            .isInstanceOf(ConcurrentModificationException.class);
  }

  @Test
  void constructorWithArrayMakesDefensiveCopy() {
    String[] array = { "a", "b" };
    SmartList<String> list = new SmartList<>(array);
    array[0] = "c";
    assertThat(list).containsExactly("a", "b");
  }

  @Test
  void subListBoundsAreChecked() {
    SmartList<String> list = new SmartList<>("a", "b", "c");
    assertThatThrownBy(() -> list.subList(-1, 2))
            .isInstanceOf(IndexOutOfBoundsException.class);
    assertThatThrownBy(() -> list.subList(0, 4))
            .isInstanceOf(IndexOutOfBoundsException.class);
    assertThatThrownBy(() -> list.subList(2, 1))
            .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void toArrayWithSmallerTypedArrayCreatesNew() {
    SmartList<String> list = new SmartList<>("a", "b", "c");
    String[] small = new String[1];
    String[] result = list.toArray(small);
    assertThat(result).isNotSameAs(small);
    assertThat(result).containsExactly("a", "b", "c");
  }

  @Test
  void addAllAtIndexWithNullElementsIsAllowed() {
    SmartList<String> list = new SmartList<>("a", "b");
    list.addAll(1, Arrays.asList(null, "c", null));
    assertThat(list).containsExactly("a", null, "c", null, "b");
  }

  private static <T> void checkForEach(List<T> list) {
    List<T> checkList = new ArrayList<>();
    //noinspection UseBulkOperation
    list.forEach(checkList::add);
    assertThat(list).isEqualTo(checkList);
  }
}