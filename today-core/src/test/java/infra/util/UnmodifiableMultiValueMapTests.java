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

import org.assertj.core.api.Assertions;
import org.assertj.core.api.ThrowableTypeAssert;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Spliterator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang 2021/11/16 16:05</a>
 */
class UnmodifiableMultiValueMapTests {

  @Test
  @SuppressWarnings("unchecked")
  void delegation() {
    MultiValueMap<String, String> mock = mock(MultiValueMap.class);
    UnmodifiableMultiValueMap<String, String> map = new UnmodifiableMultiValueMap<>(mock);

    given(mock.size()).willReturn(1);
    assertThat(map.size()).isEqualTo(1);

    given(mock.isEmpty()).willReturn(false);
    assertThat(map.isEmpty()).isFalse();

    given(mock.containsKey("foo")).willReturn(true);
    assertThat(map.containsKey("foo")).isTrue();

    given(mock.containsValue(List.of("bar"))).willReturn(true);
    assertThat(map.containsValue(List.of("bar"))).isTrue();

    List<String> list = new ArrayList<>();
    list.add("bar");
    given(mock.get("foo")).willReturn(list);
    List<String> result = map.get("foo");
    assertThat(result).isNotNull().containsExactly("bar");
    assertThatUnsupportedOperationException().isThrownBy(() -> result.add("baz"));

    given(mock.getOrDefault("foo", List.of("bar"))).willReturn(List.of("baz"));
    assertThat(map.getOrDefault("foo", List.of("bar"))).containsExactly("baz");

    given(mock.toSingleValueMap()).willReturn(Map.of("foo", "bar"));
    assertThat(map.toSingleValueMap()).containsExactly(Assertions.entry("foo", "bar"));
  }

  @Test
  void unsupported() {
    UnmodifiableMultiValueMap<String, String> map = new UnmodifiableMultiValueMap<>(new MappingMultiValueMap<>());

    assertThatUnsupportedOperationException().isThrownBy(() -> map.put("foo", List.of("bar")));
    assertThatUnsupportedOperationException().isThrownBy(() -> map.putIfAbsent("foo", List.of("bar")));
    assertThatUnsupportedOperationException().isThrownBy(() -> map.putAll(Map.of("foo", List.of("bar"))));
    assertThatUnsupportedOperationException().isThrownBy(() -> map.remove("foo"));
    assertThatUnsupportedOperationException().isThrownBy(() -> map.add("foo", "bar"));
    assertThatUnsupportedOperationException().isThrownBy(() -> map.addAll("foo", List.of("bar")));
    assertThatUnsupportedOperationException().isThrownBy(() -> map.addAll(new MappingMultiValueMap<>()));
    assertThatUnsupportedOperationException().isThrownBy(() -> map.addIfAbsent("foo", "baz"));
    assertThatUnsupportedOperationException().isThrownBy(() -> map.setOrRemove("foo", "baz"));
    assertThatUnsupportedOperationException().isThrownBy(() -> map.setAll(Map.of("foo", List.of("baz"))));
    assertThatUnsupportedOperationException().isThrownBy(() -> map.replaceAll((s, strings) -> strings));
    assertThatUnsupportedOperationException().isThrownBy(() -> map.remove("foo", List.of("bar")));
    assertThatUnsupportedOperationException().isThrownBy(() -> map.replace("foo", List.of("bar")));
    assertThatUnsupportedOperationException().isThrownBy(() -> map.replace("foo", List.of("bar"), List.of("baz")));
    assertThatUnsupportedOperationException().isThrownBy(() -> map.computeIfAbsent("foo", s -> List.of("bar")));
    assertThatUnsupportedOperationException().isThrownBy(
            () -> map.computeIfPresent("foo", (s1, s2) -> List.of("bar")));
    assertThatUnsupportedOperationException().isThrownBy(() -> map.compute("foo", (s1, s2) -> List.of("bar")));
    assertThatUnsupportedOperationException().isThrownBy(() -> map.merge("foo", List.of("bar"), (s1, s2) -> s1));
    assertThatUnsupportedOperationException().isThrownBy(() -> map.clear());
  }

  @Test
  @SuppressWarnings("unchecked")
  void entrySetDelegation() {
    MultiValueMap<String, String> mockMap = mock(MultiValueMap.class);
    Set<Map.Entry<String, List<String>>> mockSet = mock(Set.class);
    given(mockMap.entrySet()).willReturn(mockSet);
    Set<Map.Entry<String, List<String>>> set = new UnmodifiableMultiValueMap<>(mockMap).entrySet();

    given(mockSet.size()).willReturn(1);
    assertThat(set.size()).isEqualTo(1);

    given(mockSet.isEmpty()).willReturn(false);
    assertThat(set.isEmpty()).isFalse();

    Map.Entry<String, List<String>> mockedEntry = mock(Map.Entry.class);
    given(mockSet.contains(mockedEntry)).willReturn(true);
    assertThat(set.contains(mockedEntry)).isTrue();

    List<Map.Entry<String, List<String>>> mockEntries = List.of(mock(Map.Entry.class));
    given(mockSet.containsAll(mockEntries)).willReturn(true);
    assertThat(set.containsAll(mockEntries)).isTrue();

    Iterator<Map.Entry<String, List<String>>> mockIterator = mock(Iterator.class);
    given(mockSet.iterator()).willReturn(mockIterator);
    given(mockIterator.hasNext()).willReturn(false);
    assertThat(set.iterator()).isExhausted();
  }

  @Test
  @SuppressWarnings("unchecked")
  void entrySetUnsupported() {
    Set<Map.Entry<String, List<String>>> set = new UnmodifiableMultiValueMap<String, String>(new MappingMultiValueMap<>()).entrySet();

    assertThatUnsupportedOperationException().isThrownBy(() -> set.add(mock(Map.Entry.class)));
    assertThatUnsupportedOperationException().isThrownBy(() -> set.remove(mock(Map.Entry.class)));
    assertThatUnsupportedOperationException().isThrownBy(() -> set.removeIf(e -> true));
    assertThatUnsupportedOperationException().isThrownBy(() -> set.addAll(mock(List.class)));
    assertThatUnsupportedOperationException().isThrownBy(() -> set.retainAll(mock(List.class)));
    assertThatUnsupportedOperationException().isThrownBy(() -> set.removeAll(mock(List.class)));
    assertThatUnsupportedOperationException().isThrownBy(() -> set.clear());
  }

  @Test
  @SuppressWarnings("unchecked")
  void valuesDelegation() {
    MultiValueMap<String, String> mockMap = mock(MultiValueMap.class);
    Collection<List<String>> mockValues = mock(Collection.class);
    given(mockMap.values()).willReturn(mockValues);
    Collection<List<String>> values = new UnmodifiableMultiValueMap<>(mockMap).values();

    given(mockValues.size()).willReturn(1);
    assertThat(values.size()).isEqualTo(1);

    given(mockValues.isEmpty()).willReturn(false);
    assertThat(values.isEmpty()).isFalse();

    given(mockValues.contains(List.of("foo"))).willReturn(true);
    assertThat(mockValues.contains(List.of("foo"))).isTrue();

    given(mockValues.containsAll(List.of(List.of("foo")))).willReturn(true);
    assertThat(mockValues.containsAll(List.of(List.of("foo")))).isTrue();

    Iterator<List<String>> mockIterator = mock(Iterator.class);
    given(mockValues.iterator()).willReturn(mockIterator);
    given(mockIterator.hasNext()).willReturn(false);
    assertThat(values.iterator()).isExhausted();
  }

  @Test
  void valuesUnsupported() {
    Collection<List<String>> values =
            new UnmodifiableMultiValueMap<String, String>(new MappingMultiValueMap<>()).values();

    assertThatUnsupportedOperationException().isThrownBy(() -> values.add(List.of("foo")));
    assertThatUnsupportedOperationException().isThrownBy(() -> values.remove(List.of("foo")));
    assertThatUnsupportedOperationException().isThrownBy(() -> values.addAll(List.of(List.of("foo"))));
    assertThatUnsupportedOperationException().isThrownBy(() -> values.removeAll(List.of(List.of("foo"))));
    assertThatUnsupportedOperationException().isThrownBy(() -> values.retainAll(List.of(List.of("foo"))));
    assertThatUnsupportedOperationException().isThrownBy(() -> values.removeIf(s -> true));
    assertThatUnsupportedOperationException().isThrownBy(() -> values.clear());
  }

  @Test
  void constructorWithNullDelegateThrowsException() {
    assertThatIllegalArgumentException().isThrownBy(() -> new UnmodifiableMultiValueMap<>(null));
  }

  @Test
  void getReturnsUnmodifiableList() {
    MultiValueMap<String, String> delegate = new MappingMultiValueMap<>();
    delegate.add("key", "value1");
    delegate.add("key", "value2");

    UnmodifiableMultiValueMap<String, String> unmodifiable = new UnmodifiableMultiValueMap<>(delegate);
    List<String> result = unmodifiable.get("key");

    assertThat(result).isNotNull().containsExactly("value1", "value2");
    assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> result.add("value3"));
  }

  @Test
  void getReturnsNullForNonExistentKey() {
    MultiValueMap<String, String> delegate = new MappingMultiValueMap<>();
    UnmodifiableMultiValueMap<String, String> unmodifiable = new UnmodifiableMultiValueMap<>(delegate);

    assertThat(unmodifiable.get("nonexistent")).isNull();
  }

  @Test
  void getFirstReturnsCorrectValue() {
    MultiValueMap<String, String> delegate = new MappingMultiValueMap<>();
    delegate.add("key", "first");
    delegate.add("key", "second");

    UnmodifiableMultiValueMap<String, String> unmodifiable = new UnmodifiableMultiValueMap<>(delegate);
    assertThat(unmodifiable.getFirst("key")).isEqualTo("first");
  }

  @Test
  void getFirstReturnsNullForNonExistentKey() {
    MultiValueMap<String, String> delegate = new MappingMultiValueMap<>();
    UnmodifiableMultiValueMap<String, String> unmodifiable = new UnmodifiableMultiValueMap<>(delegate);

    assertThat(unmodifiable.getFirst("nonexistent")).isNull();
  }

  @Test
  void keySetIsUnmodifiable() {
    MultiValueMap<String, String> delegate = new MappingMultiValueMap<>();
    delegate.add("key1", "value1");
    delegate.add("key2", "value2");

    UnmodifiableMultiValueMap<String, String> unmodifiable = new UnmodifiableMultiValueMap<>(delegate);
    Set<String> keySet = unmodifiable.keySet();

    assertThat(keySet).containsExactlyInAnyOrder("key1", "key2");
    assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> keySet.add("key3"));
  }

  @Test
  void keySetIsCached() {
    MultiValueMap<String, String> delegate = new MappingMultiValueMap<>();
    UnmodifiableMultiValueMap<String, String> unmodifiable = new UnmodifiableMultiValueMap<>(delegate);

    Set<String> keySet1 = unmodifiable.keySet();
    Set<String> keySet2 = unmodifiable.keySet();

    assertThat(keySet1).isSameAs(keySet2);
  }

  @Test
  void valuesIsUnmodifiable() {
    MultiValueMap<String, String> delegate = new MappingMultiValueMap<>();
    delegate.add("key1", "value1");
    delegate.add("key2", "value2");

    UnmodifiableMultiValueMap<String, String> unmodifiable = new UnmodifiableMultiValueMap<>(delegate);
    Collection<List<String>> values = unmodifiable.values();

    assertThat(values).hasSize(2);
    assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> values.add(List.of("value3")));
  }

  @Test
  void valuesIsCached() {
    MultiValueMap<String, String> delegate = new MappingMultiValueMap<>();
    UnmodifiableMultiValueMap<String, String> unmodifiable = new UnmodifiableMultiValueMap<>(delegate);

    Collection<List<String>> values1 = unmodifiable.values();
    Collection<List<String>> values2 = unmodifiable.values();

    assertThat(values1).isSameAs(values2);
  }

  @Test
  void entrySetIteratorProvidesUnmodifiableEntries() {
    MultiValueMap<String, String> delegate = new MappingMultiValueMap<>();
    delegate.add("key", "value1");
    delegate.add("key", "value2");

    UnmodifiableMultiValueMap<String, String> unmodifiable = new UnmodifiableMultiValueMap<>(delegate);
    Set<Map.Entry<String, List<String>>> entrySet = unmodifiable.entrySet();

    Iterator<Map.Entry<String, List<String>>> iterator = entrySet.iterator();
    assertThat(iterator.hasNext()).isTrue();

    Map.Entry<String, List<String>> entry = iterator.next();
    assertThat(entry.getKey()).isEqualTo("key");
    assertThat(entry.getValue()).containsExactly("value1", "value2");

    assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> entry.setValue(List.of("newvalue")));
  }

  @Test
  void entrySetIsCached() {
    MultiValueMap<String, String> delegate = new MappingMultiValueMap<>();
    UnmodifiableMultiValueMap<String, String> unmodifiable = new UnmodifiableMultiValueMap<>(delegate);

    Set<Map.Entry<String, List<String>>> entrySet1 = unmodifiable.entrySet();
    Set<Map.Entry<String, List<String>>> entrySet2 = unmodifiable.entrySet();

    assertThat(entrySet1).isSameAs(entrySet2);
  }

  @Test
  void forEachIteratesOverEntries() {
    MultiValueMap<String, String> delegate = new MappingMultiValueMap<>();
    delegate.add("key1", "value1");
    delegate.add("key2", "value2");

    UnmodifiableMultiValueMap<String, String> unmodifiable = new UnmodifiableMultiValueMap<>(delegate);
    List<String> collectedKeys = new ArrayList<>();
    List<List<String>> collectedValues = new ArrayList<>();

    unmodifiable.forEach((key, values) -> {
      collectedKeys.add(key);
      collectedValues.add(values);
    });

    assertThat(collectedKeys).containsExactlyInAnyOrder("key1", "key2");
    assertThat(collectedValues).containsExactlyInAnyOrder(List.of("value1"), List.of("value2"));
  }

  @Test
  void hashCodeEqualsDelegate() {
    MultiValueMap<String, String> delegate = new MappingMultiValueMap<>();
    delegate.add("key", "value");

    UnmodifiableMultiValueMap<String, String> unmodifiable = new UnmodifiableMultiValueMap<>(delegate);

    assertThat(unmodifiable.hashCode()).isEqualTo(delegate.hashCode());
  }

  @Test
  void equalsReturnsTrueForSameInstance() {
    MultiValueMap<String, String> delegate = new MappingMultiValueMap<>();
    UnmodifiableMultiValueMap<String, String> unmodifiable = new UnmodifiableMultiValueMap<>(delegate);

    assertThat(unmodifiable.equals(unmodifiable)).isTrue();
  }

  @Test
  void equalsReturnsTrueForEqualDelegate() {
    MultiValueMap<String, String> delegate1 = new MappingMultiValueMap<>();
    delegate1.add("key", "value");
    MultiValueMap<String, String> delegate2 = new MappingMultiValueMap<>();
    delegate2.add("key", "value");

    UnmodifiableMultiValueMap<String, String> unmodifiable1 = new UnmodifiableMultiValueMap<>(delegate1);
    UnmodifiableMultiValueMap<String, String> unmodifiable2 = new UnmodifiableMultiValueMap<>(delegate2);

    assertThat(unmodifiable1.equals(unmodifiable2)).isTrue();
  }

  @Test
  void equalsReturnsFalseForDifferentDelegate() {
    MultiValueMap<String, String> delegate1 = new MappingMultiValueMap<>();
    delegate1.add("key1", "value1");
    MultiValueMap<String, String> delegate2 = new MappingMultiValueMap<>();
    delegate2.add("key2", "value2");

    UnmodifiableMultiValueMap<String, String> unmodifiable1 = new UnmodifiableMultiValueMap<>(delegate1);
    UnmodifiableMultiValueMap<String, String> unmodifiable2 = new UnmodifiableMultiValueMap<>(delegate2);

    assertThat(unmodifiable1.equals(unmodifiable2)).isFalse();
  }

  @Test
  void toStringReturnsDelegateToString() {
    MultiValueMap<String, String> delegate = new MappingMultiValueMap<>();
    delegate.add("key", "value");

    UnmodifiableMultiValueMap<String, String> unmodifiable = new UnmodifiableMultiValueMap<>(delegate);

    assertThat(unmodifiable.toString()).isEqualTo(delegate.toString());
  }

  @Test
  void asReadOnlyReturnsSelf() {
    MultiValueMap<String, String> delegate = new MappingMultiValueMap<>();
    UnmodifiableMultiValueMap<String, String> unmodifiable = new UnmodifiableMultiValueMap<>(delegate);

    assertThat(unmodifiable.asReadOnly()).isSameAs(unmodifiable);
  }

  @Test
  void asWritableReturnsDelegate() {
    MultiValueMap<String, String> delegate = new MappingMultiValueMap<>();
    UnmodifiableMultiValueMap<String, String> unmodifiable = new UnmodifiableMultiValueMap<>(delegate);

    assertThat(unmodifiable.asWritable()).isSameAs(delegate);
  }

  @Test
  void getOrDefaultReturnsUnmodifiableList() {
    MultiValueMap<String, String> delegate = new MappingMultiValueMap<>();
    delegate.add("key", "value1");
    delegate.add("key", "value2");

    UnmodifiableMultiValueMap<String, String> unmodifiable = new UnmodifiableMultiValueMap<>(delegate);
    List<String> result = unmodifiable.getOrDefault("key", List.of("default"));

    assertThat(result).isNotNull().containsExactly("value1", "value2");
    assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> result.add("value3"));
  }

  @Test
  void getOrDefaultReturnsDefaultValueWhenKeyNotPresent() {
    MultiValueMap<String, String> delegate = new MappingMultiValueMap<>();
    UnmodifiableMultiValueMap<String, String> unmodifiable = new UnmodifiableMultiValueMap<>(delegate);

    List<String> defaultValue = List.of("default");
    List<String> result = unmodifiable.getOrDefault("nonexistent", defaultValue);

    assertThat(result).isSameAs(defaultValue);
  }

  @Test
  void toSingleValueMapReturnsCorrectMap() {
    MultiValueMap<String, String> delegate = new MappingMultiValueMap<>();
    delegate.add("key1", "value1");
    delegate.add("key1", "value2");
    delegate.add("key2", "value3");

    UnmodifiableMultiValueMap<String, String> unmodifiable = new UnmodifiableMultiValueMap<>(delegate);
    Map<String, String> singleValueMap = unmodifiable.toSingleValueMap();

    assertThat(singleValueMap).containsExactly(Assertions.entry("key1", "value1"), Assertions.entry("key2", "value3"));
  }

  @Test
  void entrySetToArrayReturnsUnmodifiableEntries() {
    MultiValueMap<String, String> delegate = new MappingMultiValueMap<>();
    delegate.add("key", "value1");
    delegate.add("key", "value2");

    UnmodifiableMultiValueMap<String, String> unmodifiable = new UnmodifiableMultiValueMap<>(delegate);
    Set<Map.Entry<String, List<String>>> entrySet = unmodifiable.entrySet();

    Object[] array = entrySet.toArray();
    assertThat(array).hasSize(1);

    @SuppressWarnings("unchecked")
    Map.Entry<String, List<String>> entry = (Map.Entry<String, List<String>>) array[0];
    assertThat(entry.getKey()).isEqualTo("key");
    assertThat(entry.getValue()).containsExactly("value1", "value2");

    assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> entry.setValue(List.of("newvalue")));
  }

  @Test
  void entrySetToArrayWithTypedArrayReturnsUnmodifiableEntries() {
    MultiValueMap<String, String> delegate = new MappingMultiValueMap<>();
    delegate.add("key", "value1");
    delegate.add("key", "value2");

    UnmodifiableMultiValueMap<String, String> unmodifiable = new UnmodifiableMultiValueMap<>(delegate);
    Set<Map.Entry<String, List<String>>> entrySet = unmodifiable.entrySet();

    Map.Entry<String, List<String>>[] array = entrySet.toArray(new Map.Entry[0]);
    assertThat(array).hasSize(1);

    Map.Entry<String, List<String>> entry = array[0];
    assertThat(entry.getKey()).isEqualTo("key");
    assertThat(entry.getValue()).containsExactly("value1", "value2");

    assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> entry.setValue(List.of("newvalue")));
  }

  @Test
  void valuesToArrayReturnsUnmodifiableLists() {
    MultiValueMap<String, String> delegate = new MappingMultiValueMap<>();
    delegate.add("key1", "value1");
    delegate.add("key1", "value2");
    delegate.add("key2", "value3");

    UnmodifiableMultiValueMap<String, String> unmodifiable = new UnmodifiableMultiValueMap<>(delegate);
    Collection<List<String>> values = unmodifiable.values();

    Object[] array = values.toArray();
    assertThat(array).hasSize(2);

    for (Object obj : array) {
      @SuppressWarnings("unchecked")
      List<String> list = (List<String>) obj;
      assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> list.add("newvalue"));
    }
  }

  @Test
  void valuesToArrayWithTypedArrayReturnsUnmodifiableLists() {
    MultiValueMap<String, String> delegate = new MappingMultiValueMap<>();
    delegate.add("key1", "value1");
    delegate.add("key1", "value2");
    delegate.add("key2", "value3");

    UnmodifiableMultiValueMap<String, String> unmodifiable = new UnmodifiableMultiValueMap<>(delegate);
    Collection<List<String>> values = unmodifiable.values();

    List<String>[] array = values.toArray(new List[0]);
    assertThat(array).hasSize(2);

    for (List<String> list : array) {
      assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> list.add("newvalue"));
    }
  }

  @Test
  void entrySetSpliteratorWorksCorrectly() {
    MultiValueMap<String, String> delegate = new MappingMultiValueMap<>();
    delegate.add("key1", "value1");
    delegate.add("key2", "value2");

    UnmodifiableMultiValueMap<String, String> unmodifiable = new UnmodifiableMultiValueMap<>(delegate);
    Set<Map.Entry<String, List<String>>> entrySet = unmodifiable.entrySet();

    Spliterator<Map.Entry<String, List<String>>> spliterator = entrySet.spliterator();
    assertThat(spliterator).isNotNull();
    assertThat(spliterator.estimateSize()).isEqualTo(2);
  }

  @Test
  void valuesSpliteratorWorksCorrectly() {
    MultiValueMap<String, String> delegate = new MappingMultiValueMap<>();
    delegate.add("key1", "value1");
    delegate.add("key2", "value2");

    UnmodifiableMultiValueMap<String, String> unmodifiable = new UnmodifiableMultiValueMap<>(delegate);
    Collection<List<String>> values = unmodifiable.values();

    Spliterator<List<String>> spliterator = values.spliterator();
    assertThat(spliterator).isNotNull();
    assertThat(spliterator.estimateSize()).isEqualTo(2);
  }

  @Test
  void entrySetStreamWorksCorrectly() {
    MultiValueMap<String, String> delegate = new MappingMultiValueMap<>();
    delegate.add("key1", "value1");
    delegate.add("key2", "value2");

    UnmodifiableMultiValueMap<String, String> unmodifiable = new UnmodifiableMultiValueMap<>(delegate);
    Set<Map.Entry<String, List<String>>> entrySet = unmodifiable.entrySet();

    long count = entrySet.stream().count();
    assertThat(count).isEqualTo(2);
  }

  @Test
  void valuesStreamWorksCorrectly() {
    MultiValueMap<String, String> delegate = new MappingMultiValueMap<>();
    delegate.add("key1", "value1");
    delegate.add("key2", "value2");

    UnmodifiableMultiValueMap<String, String> unmodifiable = new UnmodifiableMultiValueMap<>(delegate);
    Collection<List<String>> values = unmodifiable.values();

    long count = values.stream().count();
    assertThat(count).isEqualTo(2);
  }

  @Test
  void entrySetParallelStreamWorksCorrectly() {
    MultiValueMap<String, String> delegate = new MappingMultiValueMap<>();
    delegate.add("key1", "value1");
    delegate.add("key2", "value2");

    UnmodifiableMultiValueMap<String, String> unmodifiable = new UnmodifiableMultiValueMap<>(delegate);
    Set<Map.Entry<String, List<String>>> entrySet = unmodifiable.entrySet();

    long count = entrySet.parallelStream().count();
    assertThat(count).isEqualTo(2);
  }

  @Test
  void valuesParallelStreamWorksCorrectly() {
    MultiValueMap<String, String> delegate = new MappingMultiValueMap<>();
    delegate.add("key1", "value1");
    delegate.add("key2", "value2");

    UnmodifiableMultiValueMap<String, String> unmodifiable = new UnmodifiableMultiValueMap<>(delegate);
    Collection<List<String>> values = unmodifiable.values();

    long count = values.parallelStream().count();
    assertThat(count).isEqualTo(2);
  }

  @Test
  void entrySetHashCodeEqualsDelegate() {
    MultiValueMap<String, String> delegate = new MappingMultiValueMap<>();
    delegate.add("key", "value");

    UnmodifiableMultiValueMap<String, String> unmodifiable = new UnmodifiableMultiValueMap<>(delegate);
    Set<Map.Entry<String, List<String>>> entrySet = unmodifiable.entrySet();

    assertThat(entrySet.hashCode()).isEqualTo(delegate.entrySet().hashCode());
  }

  @Test
  void valuesHashCodeEqualsDelegate() {
    MultiValueMap<String, String> delegate = new MappingMultiValueMap<>();
    delegate.add("key", "value");

    UnmodifiableMultiValueMap<String, String> unmodifiable = new UnmodifiableMultiValueMap<>(delegate);
    Collection<List<String>> values = unmodifiable.values();

    assertThat(values.hashCode()).isEqualTo(delegate.values().hashCode());
  }

  @Test
  void entrySetToStringEqualsDelegate() {
    MultiValueMap<String, String> delegate = new MappingMultiValueMap<>();
    delegate.add("key", "value");

    UnmodifiableMultiValueMap<String, String> unmodifiable = new UnmodifiableMultiValueMap<>(delegate);
    Set<Map.Entry<String, List<String>>> entrySet = unmodifiable.entrySet();

    assertThat(entrySet.toString()).isEqualTo(delegate.entrySet().toString());
  }

  @Test
  void valuesToStringEqualsDelegate() {
    MultiValueMap<String, String> delegate = new MappingMultiValueMap<>();
    delegate.add("key", "value");

    UnmodifiableMultiValueMap<String, String> unmodifiable = new UnmodifiableMultiValueMap<>(delegate);
    Collection<List<String>> values = unmodifiable.values();

    assertThat(values.toString()).isEqualTo(delegate.values().toString());
  }

  private static ThrowableTypeAssert<UnsupportedOperationException> assertThatUnsupportedOperationException() {
    return assertThatExceptionOfType(UnsupportedOperationException.class);
  }

}
