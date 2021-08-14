/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
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
package cn.taketoday.core.utils;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import cn.taketoday.core.DefaultMultiValueMap;
import cn.taketoday.core.MultiValueMap;

import static cn.taketoday.core.utils.CollectionUtils.createApproximateCollection;
import static cn.taketoday.core.utils.CollectionUtils.createApproximateMap;
import static cn.taketoday.core.utils.CollectionUtils.createCollection;
import static cn.taketoday.core.utils.CollectionUtils.createMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * @author TODAY <br>
 * 2020-01-19 19:58
 */
public class CollectionUtilsTest {

  private static class INNER {

  }

  @Test
  public void isCollection() throws ClassNotFoundException {

    assert CollectionUtils.isCollection(List.class);
    assert CollectionUtils.isCollection(Set.class);
    assert !CollectionUtils.isCollection(INNER.class);
    assert CollectionUtils.isCollection(ArrayList.class);
  }

  /**
   * The test demonstrates that the generics-based API for
   * {@link CollectionUtils#createApproximateCollection(Object, int)}
   * is not type-safe.
   * <p>Specifically, the parameterized type {@code E} is not bound to
   * the type of elements contained in the {@code collection} argument
   * passed to {@code createApproximateCollection()}. Thus casting the
   * value returned by {@link EnumSet#copyOf(EnumSet)} to
   * {@code (Collection<E>)} cannot guarantee that the returned collection
   * actually contains elements of type {@code E}.
   */
  @Test
  public void createApproximateCollectionIsNotTypeSafeForEnumSet() {
    Collection<Integer> ints = createApproximateCollection(EnumSet.of(Color.BLUE), 3);

    // Use a try-catch block to ensure that the exception is thrown as a result of the
    // next line and not as a result of the previous line.

    // Note that ints is of type Collection<Integer>, but the collection returned
    // by createApproximateCollection() is of type Collection<Color>. Thus, 42
    // cannot be cast to a Color.

    assertThatExceptionOfType(ClassCastException.class).isThrownBy(() ->
                                                                           ints.add(42));
  }

  @Test
  public void createCollectionIsNotTypeSafeForEnumSet() {
    Collection<Integer> ints = createCollection(EnumSet.class, Color.class, 3);

    // Use a try-catch block to ensure that the exception is thrown as a result of the
    // next line and not as a result of the previous line.

    // Note that ints is of type Collection<Integer>, but the collection returned
    // by createCollection() is of type Collection<Color>. Thus, 42 cannot be cast
    // to a Color.

    assertThatExceptionOfType(ClassCastException.class).isThrownBy(() ->
                                                                           ints.add(42));
  }

  /**
   * The test demonstrates that the generics-based API for
   * {@link CollectionUtils#createApproximateMap(Object, int)}
   * is not type-safe.
   * <p>The reasoning is similar that described in
   * {@link #createApproximateCollectionIsNotTypeSafeForEnumSet}.
   */
  @Test
  public void createApproximateMapIsNotTypeSafeForEnumMap() {
    EnumMap<Color, Integer> enumMap = new EnumMap<>(Color.class);
    enumMap.put(Color.RED, 1);
    enumMap.put(Color.BLUE, 2);
    Map<String, Integer> map = createApproximateMap(enumMap, 3);

    // Use a try-catch block to ensure that the exception is thrown as a result of the
    // next line and not as a result of the previous line.

    // Note that the 'map' key must be of type String, but the keys in the map
    // returned by createApproximateMap() are of type Color. Thus "foo" cannot be
    // cast to a Color.

    assertThatExceptionOfType(ClassCastException.class).isThrownBy(() ->
                                                                           map.put("foo", 1));
  }

  @Test
  public void createMapIsNotTypeSafeForEnumMap() {
    Map<String, Integer> map = createMap(EnumMap.class, Color.class, 3);

    // Use a try-catch block to ensure that the exception is thrown as a result of the
    // next line and not as a result of the previous line.

    // Note that the 'map' key must be of type String, but the keys in the map
    // returned by createMap() are of type Color. Thus "foo" cannot be cast to a
    // Color.

    assertThatExceptionOfType(ClassCastException.class).isThrownBy(() ->
                                                                           map.put("foo", 1));
  }

  @Test
  public void createMapIsNotTypeSafeForLinkedMultiValueMap() {
    Map<String, Integer> map = createMap(MultiValueMap.class, null, 3);

    // Use a try-catch block to ensure that the exception is thrown as a result of the
    // next line and not as a result of the previous line.

    // Note: 'map' values must be of type Integer, but the values in the map
    // returned by createMap() are of type java.util.List. Thus 1 cannot be
    // cast to a List.

    assertThatExceptionOfType(ClassCastException.class).isThrownBy(() ->
                                                                           map.put("foo", 1));
  }

  @Test
  public void createApproximateCollectionFromEmptyHashSet() {
    Collection<String> set = createApproximateCollection(new HashSet<String>(), 2);
    Assertions.assertThat(set).isEmpty();
  }

  @Test
  public void createApproximateCollectionFromNonEmptyHashSet() {
    HashSet<String> hashSet = new HashSet<>();
    hashSet.add("foo");
    Collection<String> set = createApproximateCollection(hashSet, 2);
    assertThat(set).isEmpty();
  }

  @Test
  public void createApproximateCollectionFromEmptyEnumSet() {
    Collection<Color> colors = createApproximateCollection(EnumSet.noneOf(Color.class), 2);
    assertThat(colors).isEmpty();
  }

  @Test
  public void createApproximateCollectionFromNonEmptyEnumSet() {
    Collection<Color> colors = createApproximateCollection(EnumSet.of(Color.BLUE), 2);
    assertThat(colors).isEmpty();
  }

  @Test
  public void createApproximateMapFromEmptyHashMap() {
    Map<String, String> map = createApproximateMap(new HashMap<String, String>(), 2);
    assertThat(map).isEmpty();
  }

  @Test
  public void createApproximateMapFromNonEmptyHashMap() {
    Map<String, String> hashMap = new HashMap<>();
    hashMap.put("foo", "bar");
    Map<String, String> map = createApproximateMap(hashMap, 2);
    assertThat(map).isEmpty();
  }

  @Test
  public void createApproximateMapFromEmptyEnumMap() {
    Map<Color, String> colors = createApproximateMap(new EnumMap<Color, String>(Color.class), 2);
    assertThat(colors).isEmpty();
  }

  @Test
  public void createApproximateMapFromNonEmptyEnumMap() {
    EnumMap<Color, String> enumMap = new EnumMap<>(Color.class);
    enumMap.put(Color.BLUE, "blue");
    Map<Color, String> colors = createApproximateMap(enumMap, 2);
    assertThat(colors).isEmpty();
  }

  @Test
  public void createsCollectionsCorrectly() {
    // interfaces
    assertThat(createCollection(List.class, 0)).isInstanceOf(ArrayList.class);
    assertThat(createCollection(Set.class, 0)).isInstanceOf(LinkedHashSet.class);
    assertThat(createCollection(Collection.class, 0)).isInstanceOf(LinkedHashSet.class);
    assertThat(createCollection(SortedSet.class, 0)).isInstanceOf(TreeSet.class);
    assertThat(createCollection(NavigableSet.class, 0)).isInstanceOf(TreeSet.class);

    assertThat(createCollection(List.class, String.class, 0)).isInstanceOf(ArrayList.class);
    assertThat(createCollection(Set.class, String.class, 0)).isInstanceOf(LinkedHashSet.class);
    assertThat(createCollection(Collection.class, String.class, 0)).isInstanceOf(LinkedHashSet.class);
    assertThat(createCollection(SortedSet.class, String.class, 0)).isInstanceOf(TreeSet.class);
    assertThat(createCollection(NavigableSet.class, String.class, 0)).isInstanceOf(TreeSet.class);

    // concrete types
    assertThat(createCollection(HashSet.class, 0)).isInstanceOf(HashSet.class);
    assertThat(createCollection(HashSet.class, String.class, 0)).isInstanceOf(HashSet.class);
  }

  @Test
  public void createsEnumSet() {
    assertThat(createCollection(EnumSet.class, Color.class, 0)).isInstanceOf(EnumSet.class);
  }

  @Test  // SPR-17619
  public void createsEnumSetSubclass() {
    EnumSet<Color> enumSet = EnumSet.noneOf(Color.class);
    assertThat(createCollection(enumSet.getClass(), Color.class, 0)).isInstanceOf(enumSet.getClass());
  }

  @Test
  public void rejectsInvalidElementTypeForEnumSet() {
    assertThatIllegalArgumentException().isThrownBy(() ->
                                                            createCollection(EnumSet.class, Object.class, 0));
  }

  @Test
  public void rejectsNullElementTypeForEnumSet() {
    assertThatIllegalArgumentException().isThrownBy(() ->
                                                            createCollection(EnumSet.class, null, 0));
  }

  @Test
  public void rejectsNullCollectionType() {
    assertThatIllegalArgumentException().isThrownBy(() ->
                                                            createCollection(null, Object.class, 0));
  }

  @Test
  public void createsMapsCorrectly() {
    // interfaces
    assertThat(createMap(Map.class, 0)).isInstanceOf(LinkedHashMap.class);
    assertThat(createMap(SortedMap.class, 0)).isInstanceOf(TreeMap.class);
    assertThat(createMap(NavigableMap.class, 0)).isInstanceOf(TreeMap.class);
    assertThat(createMap(MultiValueMap.class, 0)).isInstanceOf(DefaultMultiValueMap.class);

    assertThat(createMap(Map.class, String.class, 0)).isInstanceOf(LinkedHashMap.class);
    assertThat(createMap(SortedMap.class, String.class, 0)).isInstanceOf(TreeMap.class);
    assertThat(createMap(NavigableMap.class, String.class, 0)).isInstanceOf(TreeMap.class);
    assertThat(createMap(MultiValueMap.class, String.class, 0)).isInstanceOf(DefaultMultiValueMap.class);

    // concrete types
    assertThat(createMap(HashMap.class, 0)).isInstanceOf(HashMap.class);

    assertThat(createMap(HashMap.class, String.class, 0)).isInstanceOf(HashMap.class);
  }

  @Test
  public void createsEnumMap() {
    assertThat(createMap(EnumMap.class, Color.class, 0)).isInstanceOf(EnumMap.class);
  }

  @Test
  public void rejectsInvalidKeyTypeForEnumMap() {
    assertThatIllegalArgumentException().isThrownBy(() ->
                                                            createMap(EnumMap.class, Object.class, 0));
  }

  @Test
  public void rejectsNullKeyTypeForEnumMap() {
    assertThatIllegalArgumentException().isThrownBy(() ->
                                                            createMap(EnumMap.class, null, 0));
  }

  @Test
  public void rejectsNullMapType() {
    assertThatIllegalArgumentException().isThrownBy(() ->
                                                            createMap(null, Object.class, 0));
  }

  enum Color {
    RED, BLUE;
  }
}
