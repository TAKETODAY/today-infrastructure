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

import java.io.Serial;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import cn.taketoday.core.ArraySizeTrimmer;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Constant;
import cn.taketoday.lang.NonNull;
import cn.taketoday.lang.Nullable;

/**
 * @author TODAY 2019-12-29 23:39
 */
public abstract class CollectionUtils {

  /**
   * Default load factor for {@link HashMap}/{@link LinkedHashMap} variants.
   *
   * @see #newHashMap(int)
   * @see #newLinkedHashMap(int)
   */
  static final float DEFAULT_LOAD_FACTOR = 0.75f;

  private static final Set<Class<?>> approximableCollectionTypes = Set.of(
          // Standard collection interfaces
          Collection.class,
          List.class,
          Set.class,
          SortedSet.class,
          NavigableSet.class,
          // Common concrete collection classes
          ArrayList.class,
          LinkedList.class,
          HashSet.class,
          LinkedHashSet.class,
          TreeSet.class,
          EnumSet.class
  );

  private static final Set<Class<?>> approximableMapTypes = Set.of(
          // Standard map interfaces
          Map.class,
          MultiValueMap.class,
          SortedMap.class,
          NavigableMap.class,
          // Common concrete map classes
          HashMap.class,
          LinkedHashMap.class,
          LinkedMultiValueMap.class,
          TreeMap.class,
          EnumMap.class
  );

  /**
   * Return {@code true} if the supplied Collection is {@code null} or empty.
   * Otherwise, return {@code false}.
   *
   * @param collection the Collection to check
   * @return whether the given Collection is empty
   */
  public static boolean isEmpty(@Nullable Collection<?> collection) {
    return collection == null || collection.isEmpty();
  }

  /**
   * @since 4.0
   */
  public static boolean isNotEmpty(@Nullable Collection<?> collection) {
    return collection != null && !collection.isEmpty();
  }

  /**
   * Return {@code true} if the supplied Collection is not {@code null} and empty.
   * Otherwise, return {@code false}.
   *
   * @param holder the ArrayHolder to check
   * @return whether the given ArrayHolder is not empty
   * @since 4.0
   */
  public static boolean isNotEmpty(@Nullable ArrayHolder<?> holder) {
    return holder != null && !holder.isEmpty();
  }

  /**
   * Return {@code true} if the supplied Collection is {@code null} or empty.
   * Otherwise, return {@code false}.
   *
   * @param holder the ArrayHolder to check
   * @return whether the given ArrayHolder is empty
   * @since 4.0
   */
  public static boolean isEmpty(@Nullable ArrayHolder<?> holder) {
    return holder == null || holder.isEmpty();
  }

  /**
   * Return {@code true} if the supplied Map is {@code null} or empty. Otherwise,
   * return {@code false}.
   *
   * @param map the Map to check
   * @return whether the given Map is empty
   */
  public static boolean isEmpty(@Nullable Map<?, ?> map) {
    return map == null || map.isEmpty();
  }

  /**
   * @since 4.0
   */
  public static boolean isNotEmpty(@Nullable Map<?, ?> map) {
    return !isEmpty(map);
  }

  public static boolean isCollection(Class<?> cls) {
    return Collection.class.isAssignableFrom(cls);
  }

  /**
   * Create a hash set
   *
   * @param elements Elements instance
   */
  @SafeVarargs
  public static <E> HashSet<E> newHashSet(@Nullable E... elements) {
    if (ObjectUtils.isNotEmpty(elements)) {
      HashSet<E> ret = new HashSet<>(Math.max((int) (elements.length / DEFAULT_LOAD_FACTOR) + 1, 16));
      addAll(ret, elements);
      return ret;
    }
    else {
      return new HashSet<>();
    }
  }

  /**
   * Instantiate a new {@link LinkedHashSet} with an initial elements
   * that can accommodate the specified number of elements without
   * any immediate resize/rehash operations to be expected.
   *
   * @param elements the expected number of elements (with a corresponding
   * capacity to be derived so that no resize/rehash operations are needed)
   * @see #newHashMap(int)
   * @since 4.0
   */
  @SafeVarargs
  public static <E> LinkedHashSet<E> newLinkedHashSet(@Nullable E... elements) {
    if (ObjectUtils.isNotEmpty(elements)) {
      LinkedHashSet<E> ret = new LinkedHashSet<>(Math.max((int) (elements.length / DEFAULT_LOAD_FACTOR) + 1, 16));
      addAll(ret, elements);
      return ret;
    }
    else {
      return new LinkedHashSet<>();
    }
  }

  /**
   * @param elements elements array
   * @param <E> Element type
   * @return ArrayLost of input elements
   * @since 4.0
   */
  @NonNull
  @SafeVarargs
  public static <E> ArrayList<E> newArrayList(@Nullable E... elements) {
    if (ObjectUtils.isNotEmpty(elements)) {
      final ArrayList<E> ret = new ArrayList<>(elements.length);
      addAll(ret, elements);
      return ret;
    }
    else {
      return new ArrayList<>();
    }
  }

  // CollectionFactory

  /**
   * Determine whether the given collection type is an <em>approximable</em> type,
   * i.e. a type that {@link #createApproximateCollection} can approximate.
   *
   * @param collectionType the collection type to check
   * @return {@code true} if the type is <em>approximable</em>
   * @since 3.0
   */
  public static boolean isApproximableCollectionType(@Nullable Class<?> collectionType) {
    return collectionType != null &&
            (approximableCollectionTypes.contains(collectionType)
                    || collectionType.getName().equals("java.util.SequencedSet")
                    || collectionType.getName().equals("java.util.SequencedCollection"));
  }

  /**
   * Create the most approximate collection for the given collection.
   * <p>
   * <strong>Warning</strong>: Since the parameterized type {@code E} is not bound
   * to the type of elements contained in the supplied {@code collection}, type
   * safety cannot be guaranteed if the supplied {@code collection} is an
   * {@link EnumSet}. In such scenarios, the caller is responsible for ensuring
   * that the element type for the supplied {@code collection} is an enum type
   * matching type {@code E}. As an alternative, the caller may wish to treat the
   * return value as a raw collection or collection of {@link Object}.
   *
   * @param collection the original collection object, potentially {@code null}
   * @param capacity the initial capacity
   * @return a new, empty collection instance
   * @see #isApproximableCollectionType
   * @see java.util.LinkedList
   * @see java.util.ArrayList
   * @see java.util.EnumSet
   * @see java.util.TreeSet
   * @see java.util.LinkedHashSet
   * @since 3.0
   */
  @SuppressWarnings({ "rawtypes", "unchecked", "cast" })
  public static <E> Collection<E> createApproximateCollection(Object collection, int capacity) {
    if (collection instanceof EnumSet enumSet) {
      Collection<E> copy = EnumSet.copyOf(enumSet);
      copy.clear();
      return copy;
    }
    else if (collection instanceof SortedSet sortedSet) {
      return new TreeSet<>(sortedSet.comparator());
    }
    else if (collection instanceof LinkedList) {
      return new LinkedList<>();
    }
    else if (collection instanceof List) {
      return new ArrayList<>(capacity);
    }
    else {
      return new LinkedHashSet<>(capacity);
    }
  }

  /**
   * Create the most appropriate collection for the given collection type.
   * <p>
   * Delegates to {@link #createCollection(Class, Class, int)} with a {@code null}
   * element type, and {@link Constant#ZERO}.
   *
   * @param collectionType the desired type of the target collection (never {@code null})
   * @return a new collection instance
   * @throws IllegalArgumentException if the supplied {@code collectionType} is {@code null} or of type
   * {@link EnumSet}
   * @since 3.0
   */
  public static <E> Collection<E> createCollection(Class<?> collectionType) {
    return createCollection(collectionType, null, Constant.ZERO);
  }

  /**
   * Create the most appropriate collection for the given collection type.
   * <p>
   * Delegates to {@link #createCollection(Class, Class, int)} with a {@code null}
   * element type.
   *
   * @param collectionType the desired type of the target collection (never {@code null})
   * @param capacity the initial capacity
   * @return a new collection instance
   * @throws IllegalArgumentException if the supplied {@code collectionType} is {@code null} or of type
   * {@link EnumSet}
   * @since 3.0
   */
  public static <E> Collection<E> createCollection(Class<?> collectionType, int capacity) {
    return createCollection(collectionType, null, capacity);
  }

  /**
   * Create the most appropriate collection for the given collection type.
   * <p>
   * <strong>Warning</strong>: Since the parameterized type {@code E} is not bound
   * to the supplied {@code elementType}, type safety cannot be guaranteed if the
   * desired {@code collectionType} is {@link EnumSet}. In such scenarios, the
   * caller is responsible for ensuring that the supplied {@code elementType} is
   * an enum type matching type {@code E}. As an alternative, the caller may wish
   * to treat the return value as a raw collection or collection of
   * {@link Object}.
   *
   * @param collectionType the desired type of the target collection (never {@code null})
   * @param elementType the collection's element type, or {@code null} if unknown (note:
   * only relevant for {@link EnumSet} creation)
   * @param capacity the initial capacity
   * @return a new collection instance
   * @throws IllegalArgumentException if the supplied {@code collectionType} is {@code null}; or if the
   * desired {@code collectionType} is {@link EnumSet} and the supplied
   * {@code elementType} is not a subtype of {@link Enum}
   * @see java.util.LinkedHashSet
   * @see java.util.ArrayList
   * @see java.util.TreeSet
   * @see java.util.EnumSet
   * @since 3.0
   */
  @SuppressWarnings({ "unchecked", "cast" })
  public static <E> Collection<E> createCollection(Class<?> collectionType, @Nullable Class<?> elementType, int capacity) {
    Assert.notNull(collectionType, "Collection type is required");
    if (LinkedHashSet.class == collectionType
            || HashSet.class == collectionType
            || Set.class == collectionType
            || Collection.class == collectionType
            || collectionType.getName().equals("java.util.SequencedSet")
            || collectionType.getName().equals("java.util.SequencedCollection")) {
      return new LinkedHashSet<>(capacity);
    }
    else if (ArrayList.class == collectionType || List.class == collectionType) {
      return new ArrayList<>(capacity);
    }
    else if (LinkedList.class == collectionType) {
      return new LinkedList<>();
    }
    else if (TreeSet.class == collectionType
            || NavigableSet.class == collectionType
            || SortedSet.class == collectionType) {
      return new TreeSet<>();
    }
    else if (EnumSet.class.isAssignableFrom(collectionType)) {
      Assert.notNull(elementType, "Cannot create EnumSet for unknown element type");
      return EnumSet.noneOf(asEnumType(elementType));
    }
    else {
      if (collectionType.isInterface() || !Collection.class.isAssignableFrom(collectionType)) {
        throw new IllegalArgumentException("Unsupported Collection type: " + collectionType.getName());
      }
      try {
        return (Collection<E>) ReflectionUtils.accessibleConstructor(collectionType).newInstance();
      }
      catch (Throwable ex) {
        throw new IllegalArgumentException(
                "Could not instantiate Collection type: " + collectionType.getName(), ex);
      }
    }
  }

  /**
   * Determine whether the given map type is an <em>approximable</em> type, i.e. a
   * type that {@link #createApproximateMap} can approximate.
   *
   * @param mapType the map type to check
   * @return {@code true} if the type is <em>approximable</em>
   * @since 3.0
   */
  public static boolean isApproximableMapType(@Nullable Class<?> mapType) {
    return mapType != null &&
            (approximableMapTypes.contains(mapType)
                    || mapType.getName().equals("java.util.SequencedMap"));
  }

  /**
   * Create the most approximate map for the given map.
   * <p>
   * <strong>Warning</strong>: Since the parameterized type {@code K} is not bound
   * to the type of keys contained in the supplied {@code map}, type safety cannot
   * be guaranteed if the supplied {@code map} is an {@link EnumMap}. In such
   * scenarios, the caller is responsible for ensuring that the key type in the
   * supplied {@code map} is an enum type matching type {@code K}. As an
   * alternative, the caller may wish to treat the return value as a raw map or
   * map keyed by {@link Object}.
   * <p>
   * use default capacity {@link Constant#ZERO}.
   *
   * @param map the original map object, potentially {@code null}
   * @return a new, empty map instance
   * @see #isApproximableMapType
   * @see java.util.EnumMap
   * @see java.util.TreeMap
   * @see java.util.LinkedHashMap
   * @since 3.0
   */
  public static <K, V> Map<K, V> createApproximateMap(@Nullable Object map) {
    return createApproximateMap(map, Constant.ZERO);
  }

  /**
   * Create the most approximate map for the given map.
   * <p>
   * <strong>Warning</strong>: Since the parameterized type {@code K} is not bound
   * to the type of keys contained in the supplied {@code map}, type safety cannot
   * be guaranteed if the supplied {@code map} is an {@link EnumMap}. In such
   * scenarios, the caller is responsible for ensuring that the key type in the
   * supplied {@code map} is an enum type matching type {@code K}. As an
   * alternative, the caller may wish to treat the return value as a raw map or
   * map keyed by {@link Object}.
   *
   * @param map the original map object, potentially {@code null}
   * @param capacity the initial capacity
   * @return a new, empty map instance
   * @see #isApproximableMapType
   * @see java.util.EnumMap
   * @see java.util.TreeMap
   * @see java.util.LinkedHashMap
   * @since 3.0
   */
  @SuppressWarnings({ "rawtypes", "unchecked" })
  public static <K, V> Map<K, V> createApproximateMap(@Nullable Object map, int capacity) {
    if (map instanceof EnumMap enumMap) {
      EnumMap copy = new EnumMap(enumMap);
      copy.clear();
      return copy;
    }
    else if (map instanceof SortedMap sortedMap) {
      return new TreeMap<>(sortedMap.comparator());
    }
    else if (map instanceof MultiValueMap) {
      return new LinkedMultiValueMap(capacity);
    }
    else {
      return new LinkedHashMap<>(capacity);
    }
  }

  /**
   * Create the most appropriate map for the given map type.
   * <p>
   * Delegates to {@link #createMap(Class, Class, int)} with a {@code null} key
   * type, and default capacity {@link Constant#ZERO}.
   *
   * @param mapType the desired type of the target map
   * @return a new map instance
   * @throws IllegalArgumentException if the supplied {@code mapType} is {@code null} or of type
   * {@link EnumMap}
   * @since 3.0
   */
  public static <K, V> Map<K, V> createMap(@NonNull Class<?> mapType) {
    return createMap(mapType, null, Constant.ZERO);
  }

  /**
   * Create the most appropriate map for the given map type.
   * <p>
   * Delegates to {@link #createMap(Class, Class, int)} with a {@code null} key
   * type.
   *
   * @param mapType the desired type of the target map
   * @param capacity the initial capacity
   * @return a new map instance
   * @throws IllegalArgumentException if the supplied {@code mapType} is {@code null} or of type
   * {@link EnumMap}
   * @since 3.0
   */
  public static <K, V> Map<K, V> createMap(Class<?> mapType, int capacity) {
    return createMap(mapType, null, capacity);
  }

  /**
   * Create the most appropriate map for the given map type.
   * <p>
   * <strong>Warning</strong>: Since the parameterized type {@code K} is not bound
   * to the supplied {@code keyType}, type safety cannot be guaranteed if the
   * desired {@code mapType} is {@link EnumMap}. In such scenarios, the caller is
   * responsible for ensuring that the {@code keyType} is an enum type matching
   * type {@code K}. As an alternative, the caller may wish to treat the return
   * value as a raw map or map keyed by {@link Object}. Similarly, type safety
   * cannot be enforced if the desired {@code mapType} is {@link MultiValueMap}.
   *
   * @param mapType the desired type of the target map (never {@code null})
   * @param keyType the map's key type, or {@code null} if unknown (note: only relevant
   * for {@link EnumMap} creation)
   * @param capacity the initial capacity
   * @return a new map instance
   * @throws IllegalArgumentException if the supplied {@code mapType} is {@code null}; or if the desired
   * {@code mapType} is {@link EnumMap} and the supplied {@code keyType}
   * is not a subtype of {@link Enum}
   * @see java.util.LinkedHashMap
   * @see java.util.TreeMap
   * @see LinkedMultiValueMap
   * @see java.util.EnumMap
   * @since 3.0
   */
  @SuppressWarnings({ "rawtypes", "unchecked" })
  public static <K, V> Map<K, V> createMap(Class<?> mapType, @Nullable Class<?> keyType, int capacity) {
    Assert.notNull(mapType, "Map type is required");
    if (HashMap.class == mapType) {
      return new HashMap<>(capacity);
    }
    else if (LinkedHashMap.class == mapType
            || Map.class == mapType
            || mapType.getName().equals("java.util.SequencedMap")) {
      return new LinkedHashMap<>(capacity);
    }
    else if (LinkedMultiValueMap.class == mapType || MultiValueMap.class == mapType) {
      return new LinkedMultiValueMap();
    }
    else if (TreeMap.class == mapType || SortedMap.class == mapType || NavigableMap.class == mapType) {
      return new TreeMap<>();
    }
    else if (EnumMap.class == mapType) {
      Assert.notNull(keyType, "Cannot create EnumMap for unknown key type");
      return new EnumMap(asEnumType(keyType));
    }
    else {
      if (mapType.isInterface() || !Map.class.isAssignableFrom(mapType)) {
        throw new IllegalArgumentException("Unsupported Map type: " + mapType.getName());
      }
      try {
        return (Map<K, V>) ReflectionUtils.accessibleConstructor(mapType).newInstance();
      }
      catch (Throwable ex) {
        throw new IllegalArgumentException("Could not instantiate Map type: " + mapType.getName(), ex);
      }
    }
  }

  /**
   * Create a variant of {@link java.util.Properties} that automatically adapts
   * non-String values to String representations in
   * {@link Properties#getProperty}.
   * <p>
   * In addition, the returned {@code Properties} instance sorts properties
   * alphanumerically based on their keys.
   *
   * @return a new {@code Properties} instance
   * @see #createSortedProperties(boolean)
   * @see #createSortedProperties(Properties, boolean)
   * @since 3.0
   */
  public static Properties createStringAdaptingProperties() {
    final class StringAdaptingProperties extends SortedProperties {
      @Serial
      private static final long serialVersionUID = 1L;

      StringAdaptingProperties() {
        super(false);
      }

      @Nullable
      @Override
      public String getProperty(String key) {
        Object value = get(key);
        return (value != null ? value.toString() : null);
      }
    }
    return new StringAdaptingProperties();
  }

  /**
   * Create a variant of {@link java.util.Properties} that sorts properties
   * alphanumerically based on their keys.
   * <p>
   * This can be useful when storing the {@link Properties} instance in a
   * properties file, since it allows such files to be generated in a repeatable
   * manner with consistent ordering of properties. Comments in generated
   * properties files can also be optionally omitted.
   *
   * @param omitComments {@code true} if comments should be omitted when storing properties
   * in a file
   * @return a new {@code Properties} instance
   * @see #createStringAdaptingProperties()
   * @see #createSortedProperties(Properties, boolean)
   * @since 3.0
   */
  public static Properties createSortedProperties(boolean omitComments) {
    return new SortedProperties(omitComments);
  }

  /**
   * Create a variant of {@link java.util.Properties} that sorts properties
   * alphanumerically based on their keys.
   * <p>
   * This can be useful when storing the {@code Properties} instance in a
   * properties file, since it allows such files to be generated in a repeatable
   * manner with consistent ordering of properties. Comments in generated
   * properties files can also be optionally omitted.
   * <p>
   * The returned {@code Properties} instance will be populated with properties
   * from the supplied {@code properties} object, but default properties from the
   * supplied {@code properties} object will not be copied.
   *
   * @param properties the {@code Properties} object from which to copy the initial
   * properties
   * @param omitComments {@code true} if comments should be omitted when storing properties
   * in a file
   * @return a new {@code Properties} instance
   * @see #createStringAdaptingProperties()
   * @see #createSortedProperties(boolean)
   * @since 3.0
   */
  public static Properties createSortedProperties(Properties properties, boolean omitComments) {
    return new SortedProperties(properties, omitComments);
  }

  /**
   * Cast the given type to a subtype of {@link Enum}.
   *
   * @param enumType the enum type, never {@code null}
   * @return the given type as subtype of {@link Enum}
   * @throws IllegalArgumentException if the given type is not a subtype of {@link Enum}
   * @since 3.0
   */
  @SuppressWarnings("rawtypes")
  private static Class<? extends Enum> asEnumType(Class<?> enumType) {
    Assert.notNull(enumType, "Enum type is required");
    if (!Enum.class.isAssignableFrom(enumType)) {
      throw new IllegalArgumentException("Supplied type is not an enum: " + enumType.getName());
    }
    return enumType.asSubclass(Enum.class);
  }

  /**
   * Replaces the element at the specified position in this list with the
   * specified element (optional operation).
   *
   * @param list target list
   * @param index new element's index
   * @param element element object
   * @throws IndexOutOfBoundsException if the index is out of range {@code (index < 0 || index >= size())}
   * @throws NullPointerException if the specified element is null and this list does not permit null
   * elements
   * @see List#set(int, Object)
   * @since 3.0
   */
  @SuppressWarnings({ "unchecked", "rawtypes" })
  public static void setValue(final List list, final int index, final Object element) {
    final int size = list.size();
    if (index >= size && index < Integer.MAX_VALUE) {
      for (int i = size; i < index; i++) {
        list.add(null);
      }
      list.add(element);
    }
    else {
      list.set(index, element);
    }
  }

  /**
   * Returns the element at the specified position in this list.
   * <p>list can be {@code null}, then returns {@code null}
   *
   * @param index index of the element to return
   * @return the element at the specified position in this list
   * @see List#get(int)
   * @since 4.0
   */
  @Nullable
  public static <T> T getElement(@Nullable final List<T> list, final int index) {
    if (list != null && index >= 0 && index < list.size()) {
      return list.get(index);
    }
    return null;
  }

  /**
   * Returns the element at the specified position in this list.
   * <p>list can be {@code null}, then returns {@code null}
   *
   * @param index index of the element to return
   * @return the element at the specified position in this list
   * @see List#get(int)
   * @since 4.0
   */
  @Nullable
  public static <T> T getElement(@Nullable final T[] array, final int index) {
    if (array != null && index >= 0 && index < array.length) {
      return array[index];
    }
    return null;
  }

  /**
   * Adds all of the specified elements to the specified collection.
   * Elements to be added may be specified individually or as an array.
   * The behavior of this convenience method is identical to that of
   * <tt>c.addAll(Arrays.asList(elements))</tt>, but this method is likely
   * to run significantly faster under most implementations.
   *
   * <p>When elements are specified individually, this method provides a
   * convenient way to add a few elements to an existing collection:
   * <pre>
   *     CollectionUtils.addAll(flavors, "Peaches 'n Plutonium", "Rocky Racoon");
   *     CollectionUtils.addAll(flavors, null); // add nothing element can be null
   * </pre>
   *
   * @param c the collection into which <tt>elements</tt> are to be inserted
   * @param elements the elements to insert into <tt>c</tt>
   * @throws UnsupportedOperationException if <tt>c</tt> does not support
   * the <tt>add</tt> operation
   * @throws NullPointerException if <tt>elements</tt> contains one or more
   * null values and <tt>c</tt> does not permit null elements, or
   * if <tt>c</tt> or <tt>elements</tt> are <tt>null</tt>
   * @throws IllegalArgumentException if some property of a value in
   * <tt>elements</tt> prevents it from being added to <tt>c</tt>
   * @see Collection#add(Object)
   * @since 4.0
   */
  @SuppressWarnings({ "rawtypes", "unchecked" })
  public static void addAll(Collection c, @Nullable Object[] elements) {
    if (elements != null) {
      for (Object element : elements) {
        c.add(element);
      }
    }
  }

  /**
   * Adds all of the specified elements to the specified collection.
   * Elements to be added may be specified individually or as Enumeration.
   *
   * @param c the collection into which <tt>elements</tt> are to be inserted
   * @param values the elements to insert into <tt>c</tt>
   * @throws UnsupportedOperationException if <tt>c</tt> does not support
   * the <tt>add</tt> operation
   * @throws NullPointerException if <tt>elements</tt> contains one or more
   * null values and <tt>c</tt> does not permit null elements, or
   * if <tt>c</tt> or <tt>elements</tt> are <tt>null</tt>
   * @throws IllegalArgumentException if some property of a value in
   * <tt>elements</tt> prevents it from being added to <tt>c</tt>
   * @see Collection#add(Object)
   * @since 4.0
   */
  @SuppressWarnings({ "rawtypes", "unchecked" })
  public static void addAll(Collection c, @Nullable Enumeration values) {
    if (values != null) {
      while (values.hasMoreElements()) {
        c.add(values.nextElement());
      }
    }
  }

  /**
   * Adds all of the specified elements to the specified collection.
   * Elements to be added may be specified individually or as an array.
   * The behavior of this convenience method is identical to that of
   * <tt>c.addAll(Arrays.asList(elements))</tt>, but this method is likely
   * to run significantly faster under most implementations.
   *
   * <p>When elements are specified individually, this method provides a
   * convenient way to add a few elements to an existing collection:
   * <pre>
   *     CollectionUtils.addAll(flavors, list);
   *     CollectionUtils.addAll(flavors, null); // add nothing element can be null
   * </pre>
   *
   * @param c the collection into which <tt>elements</tt> are to be inserted
   * @param elements the elements to insert into <tt>c</tt>
   * @throws UnsupportedOperationException if <tt>c</tt> does not support
   * the <tt>add</tt> operation
   * @throws NullPointerException if <tt>elements</tt> contains one or more
   * null values and <tt>c</tt> does not permit null elements, or
   * if <tt>c</tt> or <tt>elements</tt> are <tt>null</tt>
   * @throws IllegalArgumentException if some property of a value in
   * <tt>elements</tt> prevents it from being added to <tt>c</tt>
   * @see Collection#addAll(Collection)
   * @since 4.0
   */
  @SuppressWarnings({ "rawtypes", "unchecked" })
  public static void addAll(Collection c, @Nullable Collection elements) {
    if (elements != null) {
      c.addAll(elements);
    }
  }

  /**
   * @since 4.0
   */
  @SuppressWarnings({ "rawtypes", "unchecked" })
  public static void addAll(ArrayHolder c, @Nullable Object[] elements) {
    if (elements != null) {
      c.add(elements);
    }
  }

  /**
   * @since 4.0
   */
  @SuppressWarnings({ "rawtypes", "unchecked" })
  public static void addAll(ArrayHolder c, @Nullable Collection elements) {
    if (elements != null) {
      c.addAll(elements);
    }
  }

  /**
   * Copies all of the mappings from the specified map to target map
   * (optional operation).  The effect of this call is equivalent to that
   * of calling {@link Map#put(Object, Object) put(k, v)} on this map once
   * for each mapping from key <tt>k</tt> to value <tt>v</tt> in the
   * specified map.  The behavior of this operation is undefined if the
   * specified map is modified while the operation is in progress.
   *
   * @param mappings mappings to be stored in target map
   * @throws UnsupportedOperationException if the <tt>putAll</tt> operation
   * is not supported by this map
   * @throws ClassCastException if the class of a key or value in the
   * specified map prevents it from being stored in this map
   * @throws NullPointerException if the specified map is null, or if
   * this map does not permit null keys or values, and the
   * specified map contains null keys or values
   * @throws IllegalArgumentException if some property of a key or value in
   * the specified map prevents it from being stored in this map
   */
  @SuppressWarnings({ "rawtypes", "unchecked" })
  public static void putAll(Map target, @Nullable Map mappings) {
    if (mappings != null) {
      target.putAll(mappings);
    }
  }

  /**
   * @since 4.0
   */
  public static <T> void iterate(@Nullable Enumeration<T> enumeration, Consumer<T> consumer) {
    if (enumeration != null) {
      while (enumeration.hasMoreElements()) {
        consumer.accept(enumeration.nextElement());
      }
    }
  }

  /**
   * @since 4.0
   */
  public static <T> void iterate(@Nullable Iterator<T> iterator, Consumer<T> consumer) {
    if (iterator != null) {
      while (iterator.hasNext()) {
        consumer.accept(iterator.next());
      }
    }
  }

  /**
   * Trims the capacity of <tt>ArrayList</tt> instance to be the
   * list's current size.  An application can use this operation to minimize
   * the storage of an <tt>ArrayList</tt> instance.
   *
   * @see ArrayList#trimToSize()
   * @since 4.0
   */
  public static void trimToSize(@Nullable Object list) {
    if (list instanceof ArrayList) {
      ((ArrayList<?>) list).trimToSize();
    }
    else if (list instanceof ArraySizeTrimmer sizeTrimmer) {
      sizeTrimmer.trimToSize();
    }
  }

  /**
   * reverse source map key-value to target map value-key
   *
   * @since 4.0
   */
  public static <K, V> void reverse(Map<K, V> source, Map<V, K> target) {
    for (Map.Entry<K, V> entry : source.entrySet()) {
      target.put(entry.getValue(), entry.getKey());
    }
  }

  /**
   * remove the elements of this collection that match the given predicate.
   *
   * @param predicate a predicate to apply to each element to determine if it
   * should be removed
   * @see Predicate#negate()
   * @see Collection#removeIf(Predicate)
   * @since 4.0
   */
  public static <T> void filter(Collection<T> collection, Predicate<? super T> predicate) {
    collection.removeIf(predicate.negate());
  }

  /**
   * remove the elements of this collection that is {@code null}.
   *
   * @see Collection#removeIf(Predicate)
   * @since 4.0
   */
  public static <T> void removeNullElements(Collection<T> collection) {
    filter(collection, Objects::nonNull);
  }

  /**
   * transform T to R
   *
   * @param c collection
   * @param transformer transformer
   * @param <T> value type
   * @param <R> transformed value type
   * @since 4.0
   */
  public static <T, R> List<R> transform(Collection<? extends T> c, final Function<T, R> transformer) {
    final ArrayList<R> result = new ArrayList<>(c.size());
    for (final T obj : c) {
      result.add(transformer.apply(obj));
    }
    return result;
  }

  public static <K, V> MultiValueMap<K, V> buckets(V[] c, Function<V, K> transformer) {
    LinkedMultiValueMap<K, V> buckets = MultiValueMap.forLinkedHashMap();
    for (final V value : c) {
      final K key = transformer.apply(value);
      buckets.add(key, value);
    }
    return buckets;
  }

  public static <K, V> MultiValueMap<K, V> buckets(Iterable<V> c, Function<V, K> transformer) {
    LinkedMultiValueMap<K, V> buckets = MultiValueMap.forLinkedHashMap();
    for (final V value : c) {
      final K key = transformer.apply(value);
      buckets.add(key, value);
    }
    return buckets;
  }

  /**
   * Retrieve the first element of the given Iterable, using {@link SortedSet#first()}
   * or otherwise using the iterator.
   *
   * @param iterable the iterable to check (may be {@code null} or empty)
   * @return the first element, or {@code null} if none
   * @see SortedSet
   * @see java.util.Queue
   * @see LinkedHashMap#keySet()
   * @see java.util.LinkedHashSet
   * @since 4.0
   */
  @Nullable
  public static <T> T firstElement(@Nullable Iterable<T> iterable) {
    if (iterable == null
            || iterable instanceof Collection && ((Collection<T>) iterable).isEmpty()) {
      return null;
    }
    if (iterable instanceof SortedSet) {
      return ((SortedSet<T>) iterable).first();
    }

    Iterator<T> it = iterable.iterator();
    T first = null;
    if (it.hasNext()) {
      first = it.next();
    }
    return first;
  }

  /**
   * Retrieve the first element of the given List, accessing the zero index.
   *
   * @param list the List to check (may be {@code null} or empty)
   * @return the first element, or {@code null} if none
   * @since 4.0
   */
  @Nullable
  public static <T> T firstElement(@Nullable List<T> list) {
    return getElement(list, 0);
  }

  /**
   * Retrieve the first element of the given Array, accessing the zero index.
   *
   * @param array the array to check (may be {@code null} or empty)
   * @return the first element, or {@code null} if none
   * @since 4.0
   */
  @Nullable
  public static <T> T firstElement(@Nullable final T[] array) {
    return getElement(array, 0);
  }

  /**
   * Retrieve the last element of the given List, accessing the highest index.
   *
   * @param list the List to check (may be {@code null} or empty)
   * @return the last element, or {@code null} if none
   * @since 4.0
   */
  @Nullable
  public static <T> T lastElement(@Nullable List<T> list) {
    if (isEmpty(list)) {
      return null;
    }
    return list.get(list.size() - 1);
  }

  /**
   * Retrieve the last element of the given array, accessing the highest index.
   *
   * @param array the array to check (may be {@code null} or empty)
   * @return the last element, or {@code null} if none
   * @since 4.0
   */
  @Nullable
  public static <T> T lastElement(@Nullable final T[] array) {
    if (array == null || array.length == 0) {
      return null;
    }
    return array[array.length - 1];
  }

  /**
   * Retrieve the last element of the given Set, using {@link SortedSet#last()}
   * or otherwise iterating over all elements (assuming a linked set).
   *
   * @param set the Set to check (may be {@code null} or empty)
   * @return the last element, or {@code null} if none
   * @see SortedSet
   * @see LinkedHashMap#keySet()
   * @see java.util.LinkedHashSet
   * @since 4.0
   */
  @Nullable
  public static <T> T lastElement(@Nullable Set<T> set) {
    if (isEmpty(set)) {
      return null;
    }
    if (set instanceof SortedSet) {
      return ((SortedSet<T>) set).last();
    }

    // Full iteration necessary...
    Iterator<T> it = set.iterator();
    T last = null;
    while (it.hasNext()) {
      last = it.next();
    }
    return last;
  }

  /**
   * Marshal the elements from the given enumeration into an array of the given type.
   * Enumeration elements must be assignable to the type of the given array. The array
   * returned will be a different instance than the array given.
   *
   * @see List#toArray(Object[])
   * @since 4.0
   */
  public static <A, E extends A> A[] toArray(@Nullable Enumeration<E> enumeration, A[] array) {
    if (enumeration != null) {
      ArrayList<A> elements = new ArrayList<>();
      while (enumeration.hasMoreElements()) {
        elements.add(enumeration.nextElement());
      }
      return elements.toArray(array);
    }
    return array;
  }

  /**
   * Marshal the elements from the given iterator into an array of the given type.
   * Iterator elements must be assignable to the type of the given array. The array
   * returned will be a different instance than the array given.
   *
   * @see List#toArray(Object[])
   * @since 4.0
   */
  public static <A, E extends A> A[] toArray(@Nullable Iterator<E> iterator, A[] array) {
    if (iterator != null) {
      ArrayList<A> elements = new ArrayList<>();
      while (iterator.hasNext()) {
        elements.add(iterator.next());
      }
      return elements.toArray(array);
    }
    return array;
  }

  /**
   * Adapt an {@link Enumeration} to an {@link Iterator}.
   *
   * @param enumeration the original {@code Enumeration}
   * @return the adapted {@code Iterator}
   * @since 4.0
   */
  public static <E> Iterator<E> toIterator(@Nullable Enumeration<E> enumeration) {
    return enumeration != null ? enumeration.asIterator() : Collections.emptyIterator();
  }

  /**
   * @since 4.0
   */
  public static <E> Iterator<E> singletonIterator(final E e) {
    return new SingletonIterator<>(e);
  }

  /**
   * Instantiate a new {@link HashMap} with an initial capacity
   * that can accommodate the specified number of elements without
   * any immediate resize/rehash operations to be expected.
   * <p>This differs from the regular {@link HashMap} constructor
   * which takes an initial capacity relative to a load factor
   * but is effectively aligned with the JDK's
   * {@link java.util.concurrent.ConcurrentHashMap#ConcurrentHashMap(int)}.
   *
   * @param expectedSize the expected number of elements (with a corresponding
   * capacity to be derived so that no resize/rehash operations are needed)
   * @see #newLinkedHashMap(int)
   * @since 4.0
   */
  public static <K, V> HashMap<K, V> newHashMap(int expectedSize) {
    return new HashMap<>(computeMapInitialCapacity(expectedSize), DEFAULT_LOAD_FACTOR);
  }

  /**
   * Instantiate a new {@link LinkedHashMap} with an initial capacity
   * that can accommodate the specified number of elements without
   * any immediate resize/rehash operations to be expected.
   *
   * @param expectedSize the expected number of elements (with a corresponding
   * capacity to be derived so that no resize/rehash operations are needed)
   * @see #newHashMap(int)
   * @since 4.0
   */
  public static <K, V> LinkedHashMap<K, V> newLinkedHashMap(int expectedSize) {
    return new LinkedHashMap<>(computeMapInitialCapacity(expectedSize), DEFAULT_LOAD_FACTOR);
  }

  private static int computeMapInitialCapacity(int expectedSize) {
    return (int) Math.ceil(expectedSize / (double) DEFAULT_LOAD_FACTOR);
  }

  /**
   * Convert the supplied array into a List. A primitive array gets converted
   * into a List of the appropriate wrapper type.
   * <p><b>NOTE:</b> Generally prefer the standard {@link Arrays#asList} method.
   * This {@code arrayToList} method is just meant to deal with an incoming Object
   * value that might be an {@code Object[]} or a primitive array at runtime.
   * <p>A {@code null} source value will be converted to an empty List.
   *
   * @param source the (potentially primitive) array
   * @return the converted List result
   * @see ObjectUtils#toObjectArray(Object)
   * @see Arrays#asList(Object[])
   * @since 4.0
   */
  public static List<?> arrayToList(@Nullable Object source) {
    Object[] objectArray = ObjectUtils.toObjectArray(source);
    return newArrayList(objectArray);
  }

  /**
   * Merge the given array into the given Collection.
   *
   * @param array the array to merge (may be {@code null})
   * @param collection the target Collection to merge the array into
   * @since 4.0
   */
  @SuppressWarnings("unchecked")
  public static <E> void mergeArrayIntoCollection(@Nullable Object array, Collection<E> collection) {
    Object[] arr = ObjectUtils.toObjectArray(array);
    for (Object elem : arr) {
      collection.add((E) elem);
    }
  }

  /**
   * Merge the given Properties instance into the given Map,
   * copying all properties (key-value pairs) over.
   * <p>Uses {@code Properties.propertyNames()} to even catch
   * default properties linked into the original Properties instance.
   *
   * @param props the Properties instance to merge (may be {@code null})
   * @param map the target Map to merge the properties into
   * @since 4.0
   */
  @SuppressWarnings("unchecked")
  public static <K, V> void mergePropertiesIntoMap(@Nullable Properties props, Map<K, V> map) {
    if (props != null) {
      for (Enumeration<?> en = props.propertyNames(); en.hasMoreElements(); ) {
        String key = (String) en.nextElement();
        Object value = props.get(key);
        if (value == null) {
          // Allow for defaults fallback or potentially overridden accessor...
          value = props.getProperty(key);
        }
        map.put((K) key, (V) value);
      }
    }
  }

  /**
   * Check whether the given Iterator contains the given element.
   *
   * @param iterator the Iterator to check
   * @param element the element to look for
   * @return {@code true} if found, {@code false} otherwise
   * @since 4.0
   */
  public static boolean contains(@Nullable Iterator<?> iterator, Object element) {
    if (iterator != null) {
      while (iterator.hasNext()) {
        Object candidate = iterator.next();
        if (ObjectUtils.nullSafeEquals(candidate, element)) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Check whether the given Enumeration contains the given element.
   *
   * @param enumeration the Enumeration to check
   * @param element the element to look for
   * @return {@code true} if found, {@code false} otherwise
   * @since 4.0
   */
  public static boolean contains(@Nullable Enumeration<?> enumeration, Object element) {
    if (enumeration != null) {
      while (enumeration.hasMoreElements()) {
        Object candidate = enumeration.nextElement();
        if (ObjectUtils.nullSafeEquals(candidate, element)) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Check whether the given Iterable contains the given element.
   *
   * @param iterable the Iterable to check
   * @param element the element to look for
   * @return {@code true} if found, {@code false} otherwise
   * @since 4.0
   */
  public static boolean contains(@Nullable Iterable<?> iterable, Object element) {
    if (iterable != null) {
      if (iterable instanceof Collection) {
        return ((Collection<?>) iterable).contains(element);
      }
      for (final Object candidate : iterable) {
        if (ObjectUtils.nullSafeEquals(candidate, element)) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Check whether the given Collection contains the given element instance.
   * <p>Enforces the given instance to be present, rather than returning
   * {@code true} for an equal element as well.
   *
   * @param collection the Collection to check
   * @param element the element to look for
   * @return {@code true} if found, {@code false} otherwise
   * @since 4.0
   */
  public static boolean containsInstance(@Nullable Collection<?> collection, Object element) {
    if (collection != null) {
      for (Object candidate : collection) {
        if (candidate == element) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Return {@code true} if any element in '{@code candidates}' is
   * contained in '{@code source}'; otherwise returns {@code false}.
   *
   * @param source the source Collection
   * @param candidates the candidates to search for
   * @return whether any of the candidates has been found
   * @since 4.0
   */
  public static boolean containsAny(Collection<?> source, Collection<?> candidates) {
    return findFirstMatch(source, candidates) != null;
  }

  /**
   * Return the first element in '{@code candidates}' that is contained in
   * '{@code source}'. If no element in '{@code candidates}' is present in
   * '{@code source}' returns {@code null}. Iteration order is
   * {@link Collection} implementation specific.
   *
   * @param source the source Collection
   * @param candidates the candidates to search for
   * @return the first present object, or {@code null} if not found
   * @since 4.0
   */
  @Nullable
  public static <E> E findFirstMatch(Collection<?> source, Collection<E> candidates) {
    if (isEmpty(source) || isEmpty(candidates)) {
      return null;
    }
    for (E candidate : candidates) {
      if (source.contains(candidate)) {
        return candidate;
      }
    }
    return null;
  }

  /**
   * Determine whether the given Collection only contains a single unique object.
   *
   * @param collection the Collection to check
   * @return {@code true} if the collection contains a single reference or
   * multiple references to the same instance, {@code false} otherwise
   * @since 4.0
   */
  public static boolean hasUniqueObject(Collection<?> collection) {
    if (isEmpty(collection)) {
      return false;
    }
    boolean hasCandidate = false;
    Object candidate = null;
    for (Object elem : collection) {
      if (!hasCandidate) {
        hasCandidate = true;
        candidate = elem;
      }
      else if (candidate != elem) {
        return false;
      }
    }
    return true;
  }

  /**
   * Find a single value of the given type in the given Collection.
   *
   * @param collection the Collection to search
   * @param type the type to look for
   * @return a value of the given type found if there is a clear match,
   * or {@code null} if none or more than one such value found
   * @since 4.0
   */
  @SuppressWarnings("unchecked")
  @Nullable
  public static <T> T findValueOfType(Collection<?> collection, @Nullable Class<T> type) {
    if (isEmpty(collection)) {
      return null;
    }
    T value = null;
    for (Object element : collection) {
      if (type == null || type.isInstance(element)) {
        if (value != null) {
          // More than one value found... no clear single value.
          return null;
        }
        value = (T) element;
      }
    }
    return value;
  }

  /**
   * Find a single value of one of the given types in the given Collection:
   * searching the Collection for a value of the first type, then
   * searching for a value of the second type, etc.
   *
   * @param collection the collection to search
   * @param types the types to look for, in prioritized order
   * @return a value of one of the given types found if there is a clear match,
   * or {@code null} if none or more than one such value found
   * @since 4.0
   */
  @Nullable
  public static Object findValueOfType(Collection<?> collection, Class<?>[] types) {
    if (isEmpty(collection) || ObjectUtils.isEmpty(types)) {
      return null;
    }
    for (Class<?> type : types) {
      Object value = findValueOfType(collection, type);
      if (value != null) {
        return value;
      }
    }
    return null;
  }

}
