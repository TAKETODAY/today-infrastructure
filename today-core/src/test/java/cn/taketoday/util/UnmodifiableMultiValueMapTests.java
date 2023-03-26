/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
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

import org.assertj.core.api.Assertions;
import org.assertj.core.api.ThrowableTypeAssert;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
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
    UnmodifiableMultiValueMap<String, String> map = new UnmodifiableMultiValueMap<>(new DefaultMultiValueMap<>());

    assertThatUnsupportedOperationException().isThrownBy(() -> map.put("foo", List.of("bar")));
    assertThatUnsupportedOperationException().isThrownBy(() -> map.putIfAbsent("foo", List.of("bar")));
    assertThatUnsupportedOperationException().isThrownBy(() -> map.putAll(Map.of("foo", List.of("bar"))));
    assertThatUnsupportedOperationException().isThrownBy(() -> map.remove("foo"));
    assertThatUnsupportedOperationException().isThrownBy(() -> map.add("foo", "bar"));
    assertThatUnsupportedOperationException().isThrownBy(() -> map.addAll("foo", List.of("bar")));
    assertThatUnsupportedOperationException().isThrownBy(() -> map.addAll(new DefaultMultiValueMap<>()));
    assertThatUnsupportedOperationException().isThrownBy(() -> map.addIfAbsent("foo", "baz"));
    assertThatUnsupportedOperationException().isThrownBy(() -> map.set("foo", "baz"));
    assertThatUnsupportedOperationException().isThrownBy(() -> map.setAll(Map.of("foo", "baz")));
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
    Set<Map.Entry<String, List<String>>> set = new UnmodifiableMultiValueMap<String, String>(new DefaultMultiValueMap<>()).entrySet();

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
            new UnmodifiableMultiValueMap<String, String>(new DefaultMultiValueMap<>()).values();

    assertThatUnsupportedOperationException().isThrownBy(() -> values.add(List.of("foo")));
    assertThatUnsupportedOperationException().isThrownBy(() -> values.remove(List.of("foo")));
    assertThatUnsupportedOperationException().isThrownBy(() -> values.addAll(List.of(List.of("foo"))));
    assertThatUnsupportedOperationException().isThrownBy(() -> values.removeAll(List.of(List.of("foo"))));
    assertThatUnsupportedOperationException().isThrownBy(() -> values.retainAll(List.of(List.of("foo"))));
    assertThatUnsupportedOperationException().isThrownBy(() -> values.removeIf(s -> true));
    assertThatUnsupportedOperationException().isThrownBy(() -> values.clear());
  }

  private static ThrowableTypeAssert<UnsupportedOperationException> assertThatUnsupportedOperationException() {
    return Assertions.assertThatExceptionOfType(UnsupportedOperationException.class);
  }

}
