/*
 * Copyright 2017 - 2024 the original author or authors.
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
package cn.taketoday.util;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Properties;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import static cn.taketoday.util.CollectionUtils.createApproximateCollection;
import static cn.taketoday.util.CollectionUtils.createApproximateMap;
import static cn.taketoday.util.CollectionUtils.createCollection;
import static cn.taketoday.util.CollectionUtils.createMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * @author TODAY <br>
 * 2020-01-19 19:58
 */
public class CollectionUtilsTests {

  private static class INNER {

  }

  @Test
  void isCollection() throws ClassNotFoundException {

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
  void createApproximateCollectionIsNotTypeSafeForEnumSet() {
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
  void createCollectionIsNotTypeSafeForEnumSet() {
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
  void createApproximateMapIsNotTypeSafeForEnumMap() {
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
  void createMapIsNotTypeSafeForEnumMap() {
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
  void createMapIsNotTypeSafeForDefaultMultiValueMap() {
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
  void createApproximateCollectionFromEmptyHashSet() {
    Collection<String> set = createApproximateCollection(new HashSet<String>(), 2);
    assertThat(set).isEmpty();
  }

  @Test
  void createApproximateCollectionFromNonEmptyHashSet() {
    HashSet<String> hashSet = new HashSet<>();
    hashSet.add("foo");
    Collection<String> set = createApproximateCollection(hashSet, 2);
    assertThat(set).isEmpty();
  }

  @Test
  void createApproximateCollectionFromEmptyEnumSet() {
    Collection<Color> colors = createApproximateCollection(EnumSet.noneOf(Color.class), 2);
    assertThat(colors).isEmpty();
  }

  @Test
  void createApproximateCollectionFromNonEmptyEnumSet() {
    Collection<Color> colors = createApproximateCollection(EnumSet.of(Color.BLUE), 2);
    assertThat(colors).isEmpty();
  }

  @Test
  void createApproximateMapFromEmptyHashMap() {
    Map<String, String> map = createApproximateMap(new HashMap<String, String>(), 2);
    assertThat(map).isEmpty();
  }

  @Test
  void createApproximateMapFromNonEmptyHashMap() {
    Map<String, String> hashMap = new HashMap<>();
    hashMap.put("foo", "bar");
    Map<String, String> map = createApproximateMap(hashMap, 2);
    assertThat(map).isEmpty();
  }

  @Test
  void createApproximateMapFromEmptyEnumMap() {
    Map<Color, String> colors = createApproximateMap(new EnumMap<Color, String>(Color.class), 2);
    assertThat(colors).isEmpty();
  }

  @Test
  void createApproximateMapFromNonEmptyEnumMap() {
    EnumMap<Color, String> enumMap = new EnumMap<>(Color.class);
    enumMap.put(Color.BLUE, "blue");
    Map<Color, String> colors = createApproximateMap(enumMap, 2);
    assertThat(colors).isEmpty();
  }

  @Test
  void createsCollectionsCorrectly() {
    // interfaces
    testCollection(List.class, ArrayList.class);
    testCollection(Set.class, LinkedHashSet.class);
    testCollection(Collection.class, LinkedHashSet.class);
    // on JDK 21: testCollection(SequencedSet.class, LinkedHashSet.class);
    // on JDK 21: testCollection(SequencedCollection.class, LinkedHashSet.class);
    testCollection(SortedSet.class, TreeSet.class);
    testCollection(NavigableSet.class, TreeSet.class);

    // concrete types
    testCollection(ArrayList.class, ArrayList.class);
    testCollection(HashSet.class, LinkedHashSet.class);
    testCollection(LinkedHashSet.class, LinkedHashSet.class);
    testCollection(TreeSet.class, TreeSet.class);
  }

  private void testCollection(Class<?> collectionType, Class<?> resultType) {
    assertThat(CollectionUtils.isApproximableCollectionType(collectionType)).isTrue();
    assertThat(createCollection(collectionType, 0)).isInstanceOf(resultType);
    assertThat(createCollection(collectionType, String.class, 0)).isInstanceOf(resultType);
  }

  @Test
  void createsEnumSet() {
    assertThat(createCollection(EnumSet.class, Color.class, 0)).isInstanceOf(EnumSet.class);
  }

  @Test
    // SPR-17619
  void createsEnumSetSubclass() {
    EnumSet<Color> enumSet = EnumSet.noneOf(Color.class);
    assertThat(createCollection(enumSet.getClass(), Color.class, 0)).isInstanceOf(enumSet.getClass());
  }

  @Test
  void rejectsInvalidElementTypeForEnumSet() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> createCollection(EnumSet.class, Object.class, 0));
  }

  @Test
  void rejectsNullElementTypeForEnumSet() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> createCollection(EnumSet.class, null, 0));
  }

  @Test
  void rejectsNullCollectionType() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> createCollection(null, Object.class, 0));
  }

  @Test
  void createsMapsCorrectly() {
    // interfaces
    testMap(Map.class, LinkedHashMap.class);
    // on JDK 21: testMap(SequencedMap.class, LinkedHashMap.class);
    testMap(SortedMap.class, TreeMap.class);
    testMap(NavigableMap.class, TreeMap.class);
    testMap(MultiValueMap.class, LinkedMultiValueMap.class);

    // concrete types
    testMap(HashMap.class, HashMap.class);
    testMap(LinkedHashMap.class, LinkedHashMap.class);
    testMap(TreeMap.class, TreeMap.class);
    testMap(LinkedMultiValueMap.class, LinkedMultiValueMap.class);
  }

  private void testMap(Class<?> mapType, Class<?> resultType) {
    assertThat(CollectionUtils.isApproximableMapType(mapType)).isTrue();
    assertThat(createMap(mapType, 0)).isInstanceOf(resultType);
    assertThat(createMap(mapType, String.class, 0)).isInstanceOf(resultType);
  }

  @Test
  void createsEnumMap() {
    assertThat(createMap(EnumMap.class, Color.class, 0)).isInstanceOf(EnumMap.class);
  }

  @Test
  void rejectsInvalidKeyTypeForEnumMap() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> createMap(EnumMap.class, Object.class, 0));
  }

  @Test
  void rejectsNullKeyTypeForEnumMap() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> createMap(EnumMap.class, null, 0));
  }

  @Test
  void rejectsNullMapType() {
    assertThatIllegalArgumentException().isThrownBy(() ->
            createMap(null, Object.class, 0));
  }

  enum Color {
    RED, BLUE;
  }

  @Test
  void isEmpty() {
    assertThat(CollectionUtils.isEmpty((Set<Object>) null)).isTrue();
    assertThat(CollectionUtils.isEmpty((Map<String, String>) null)).isTrue();
    assertThat(CollectionUtils.isEmpty(new HashMap<String, String>())).isTrue();
    assertThat(CollectionUtils.isEmpty(new HashSet<>())).isTrue();

    List<Object> list = new ArrayList<>();
    list.add(new Object());
    assertThat(CollectionUtils.isEmpty(list)).isFalse();

    Map<String, String> map = new HashMap<>();
    map.put("foo", "bar");
    assertThat(CollectionUtils.isEmpty(map)).isFalse();
  }

  @Test
  void mergeArrayIntoCollection() {
    Object[] arr = new Object[] { "value1", "value2" };
    List<Comparable<?>> list = new ArrayList<>();
    list.add("value3");

    CollectionUtils.mergeArrayIntoCollection(arr, list);
    assertThat(list.get(0)).isEqualTo("value3");
    assertThat(list.get(1)).isEqualTo("value1");
    assertThat(list.get(2)).isEqualTo("value2");
  }

  @Test
  void mergePrimitiveArrayIntoCollection() {
    int[] arr = new int[] { 1, 2 };
    List<Comparable<?>> list = new ArrayList<>();
    list.add(Integer.valueOf(3));

    CollectionUtils.mergeArrayIntoCollection(arr, list);
    assertThat(list.get(0)).isEqualTo(Integer.valueOf(3));
    assertThat(list.get(1)).isEqualTo(Integer.valueOf(1));
    assertThat(list.get(2)).isEqualTo(Integer.valueOf(2));
  }

  @Test
  void mergePropertiesIntoMap() {
    Properties defaults = new Properties();
    defaults.setProperty("prop1", "value1");
    Properties props = new Properties(defaults);
    props.setProperty("prop2", "value2");
    props.put("prop3", Integer.valueOf(3));

    Map<String, Object> map = new HashMap<>();
    map.put("prop4", "value4");

    CollectionUtils.mergePropertiesIntoMap(props, map);
    assertThat(map.get("prop1")).isEqualTo("value1");
    assertThat(map.get("prop2")).isEqualTo("value2");
    assertThat(map.get("prop3")).isEqualTo(Integer.valueOf(3));
    assertThat(map.get("prop4")).isEqualTo("value4");
  }

  @Test
  void contains() {
    assertThat(CollectionUtils.contains((Iterator<String>) null, "myElement")).isFalse();
    assertThat(CollectionUtils.contains((Enumeration<String>) null, "myElement")).isFalse();
    assertThat(CollectionUtils.contains(new ArrayList<String>().iterator(), "myElement")).isFalse();
    assertThat(CollectionUtils.contains(new Hashtable<String, Object>().keys(), "myElement")).isFalse();
    assertThat(CollectionUtils.contains(Collections.emptyIterator(), "myElement")).isFalse();

    List<String> list = new ArrayList<>();
    list.add("myElement");
    assertThat(CollectionUtils.contains(list.iterator(), "myElement")).isTrue();

    Hashtable<String, String> ht = new Hashtable<>();
    ht.put("myElement", "myValue");
    assertThat(CollectionUtils.contains(ht.keys(), "myElement")).isTrue();

    //  contains(@Nullable Object[] array, Object element) {

    Object[] array = list.toArray();
    assertThat(ObjectUtils.containsElement(array, "myElement")).isTrue();
    assertThat(ObjectUtils.containsElement(array, "myElements")).isFalse();
    assertThat(ObjectUtils.containsElement((Object[]) null, "myElements")).isFalse();

  }

  @Test
  void containsAny() throws Exception {
    List<String> source = new ArrayList<>();
    source.add("abc");
    source.add("def");
    source.add("ghi");

    List<String> candidates = new ArrayList<>();
    candidates.add("xyz");
    candidates.add("def");
    candidates.add("abc");

    assertThat(CollectionUtils.containsAny(source, candidates)).isTrue();
    candidates.remove("def");
    assertThat(CollectionUtils.containsAny(source, candidates)).isTrue();
    candidates.remove("abc");
    assertThat(CollectionUtils.containsAny(source, candidates)).isFalse();
  }

  @Test
  void containsInstanceWithNullCollection() throws Exception {
    assertThat(CollectionUtils.containsInstance(null, this)).as("Must return false if supplied Collection argument is null").isFalse();
  }

  @Test
  void containsInstanceWithInstancesThatAreEqualButDistinct() throws Exception {
    List<Instance> list = new ArrayList<>();
    list.add(new Instance("fiona"));
    assertThat(CollectionUtils.containsInstance(list, new Instance("fiona"))).as("Must return false if instance is not in the supplied Collection argument").isFalse();
  }

  @Test
  void containsInstanceWithSameInstance() throws Exception {
    List<Instance> list = new ArrayList<>();
    list.add(new Instance("apple"));
    Instance instance = new Instance("fiona");
    list.add(instance);
    assertThat(CollectionUtils.containsInstance(list, instance)).as("Must return true if instance is in the supplied Collection argument").isTrue();
  }

  @Test
  void containsInstanceWithNullInstance() throws Exception {
    List<Instance> list = new ArrayList<>();
    list.add(new Instance("apple"));
    list.add(new Instance("fiona"));
    assertThat(CollectionUtils.containsInstance(list, null)).as("Must return false if null instance is supplied").isFalse();
  }

  @Test
  void findFirstMatch() throws Exception {
    List<String> source = new ArrayList<>();
    source.add("abc");
    source.add("def");
    source.add("ghi");

    List<String> candidates = new ArrayList<>();
    candidates.add("xyz");
    candidates.add("def");
    candidates.add("abc");

    assertThat(CollectionUtils.findFirstMatch(source, candidates)).isEqualTo("def");
    assertThat(CollectionUtils.findFirstMatch(null, candidates)).isNull();
  }

  @Test
  void hasUniqueObject() {
    List<String> list = new ArrayList<>();
    list.add("myElement");
    list.add("myOtherElement");
    assertThat(CollectionUtils.hasUniqueObject(list)).isFalse();

    list = new ArrayList<>();
    list.add("myElement");
    assertThat(CollectionUtils.hasUniqueObject(list)).isTrue();

    list = new ArrayList<>();
    list.add("myElement");
    list.add(null);
    assertThat(CollectionUtils.hasUniqueObject(list)).isFalse();

    list = new ArrayList<>();
    list.add(null);
    list.add("myElement");
    assertThat(CollectionUtils.hasUniqueObject(list)).isFalse();

    list = new ArrayList<>();
    list.add(null);
    list.add(null);
    assertThat(CollectionUtils.hasUniqueObject(list)).isTrue();

    list = new ArrayList<>();
    list.add(null);
    assertThat(CollectionUtils.hasUniqueObject(list)).isTrue();

    list = new ArrayList<>();
    assertThat(CollectionUtils.hasUniqueObject(list)).isFalse();
  }

  @Test
  void getElement() {
    List<String> list = new ArrayList<>();
    list.add("myElement");
    list.add("myOtherElement");

    assertThat(CollectionUtils.getElement(list, 0)).isNotNull().isNotEmpty().isEqualTo("myElement");
    assertThat(CollectionUtils.getElement(list, 1)).isNotNull().isNotEmpty().isEqualTo("myOtherElement");
    assertThat(CollectionUtils.getElement(list, -1)).isNull();
    assertThat(CollectionUtils.getElement(list, 10)).isNull();
  }

  @Test
  void firstElement() {
    List<String> list = new ArrayList<>();
    list.add("myElement");
    list.add("myOtherElement");
    assertThat(CollectionUtils.firstElement(list)).isNotNull().isNotEmpty().isEqualTo("myElement");
    assertThat(CollectionUtils.firstElement((Collection<String>) list)).isNotNull().isNotEmpty().isEqualTo("myElement");

    //
    assertThat(CollectionUtils.firstElement((List<?>) null)).isNull();
    assertThat(CollectionUtils.firstElement((Collection<?>) null)).isNull();

    TreeSet<String> objects = new TreeSet<>();

    objects.add("myElement");
    objects.add("myOtherElement");
    assertThat(CollectionUtils.firstElement(objects)).isNotNull().isNotEmpty().isEqualTo("myElement");

    // lastElement

    assertThat(CollectionUtils.lastElement((List<?>) null)).isNull();
    assertThat(CollectionUtils.lastElement(list)).isNotNull().isNotEmpty().isEqualTo("myOtherElement");
  }

  @Test
  void conversionOfEmptyMap() {
    MultiValueMap<String, String> asMultiValueMap = MultiValueMap.forAdaption((k) -> new ArrayList<>(1));
    assertThat(asMultiValueMap.isEmpty()).isTrue();
    assertThat(asMultiValueMap).isEmpty();
  }

  @Test
  void conversionOfNonEmptyMap() {
    Map<String, List<String>> wrapped = new HashMap<>();
    wrapped.put("key", Arrays.asList("first", "second"));
    MultiValueMap<String, String> asMultiValueMap = MultiValueMap.forAdaption(
            wrapped, k -> new ArrayList<>(1));
    assertThat(asMultiValueMap).containsAllEntriesOf(wrapped);
  }

  @Test
  void changesValueByReference() {
    Map<String, List<String>> wrapped = new HashMap<>();
    MultiValueMap<String, String> asMultiValueMap = MultiValueMap.forSmartListAdaption(wrapped);
    assertThat(asMultiValueMap).doesNotContainKeys("key");
    wrapped.put("key", new ArrayList<>());
    assertThat(asMultiValueMap).containsKey("key");
  }

  private static final class Instance {

    private final String name;

    public Instance(String name) {
      this.name = name;
    }

    @Override
    public boolean equals(Object rhs) {
      if (this == rhs) {
        return true;
      }
      if (rhs == null || this.getClass() != rhs.getClass()) {
        return false;
      }
      Instance instance = (Instance) rhs;
      return this.name.equals(instance.name);
    }

    @Override
    public int hashCode() {
      return this.name.hashCode();
    }
  }

}
