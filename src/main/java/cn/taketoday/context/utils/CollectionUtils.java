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
package cn.taketoday.context.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
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

import cn.taketoday.context.Constant;

/**
 * Factory for collections that is aware of common Java and Spring collection
 * types.
 *
 * <p>
 * Mainly for internal use within the framework.
 * </p>
 * <p>
 * From Spring
 * </p>
 *
 * @author TODAY 2019-12-29 23:39
 */
public abstract class CollectionUtils {

  private static final HashSet<Class<?>> approximableMapTypes = new HashSet<>();
  private static final HashSet<Class<?>> approximableCollectionTypes = new HashSet<>();

  static {
    // Standard collection interfaces
    approximableCollectionTypes.add(Collection.class);
    approximableCollectionTypes.add(List.class);
    approximableCollectionTypes.add(Set.class);
    approximableCollectionTypes.add(SortedSet.class);
    approximableCollectionTypes.add(NavigableSet.class);
    approximableMapTypes.add(Map.class);
    approximableMapTypes.add(SortedMap.class);
    approximableMapTypes.add(NavigableMap.class);

    // Common concrete collection classes
    approximableCollectionTypes.add(ArrayList.class);
    approximableCollectionTypes.add(LinkedList.class);
    approximableCollectionTypes.add(HashSet.class);
    approximableCollectionTypes.add(LinkedHashSet.class);
    approximableCollectionTypes.add(TreeSet.class);
    approximableCollectionTypes.add(EnumSet.class);
    approximableMapTypes.add(HashMap.class);
    approximableMapTypes.add(LinkedHashMap.class);
    approximableMapTypes.add(TreeMap.class);
    approximableMapTypes.add(EnumMap.class);
  }

  /**
   * Return {@code true} if the supplied Collection is {@code null} or empty.
   * Otherwise, return {@code false}.
   *
   * @param collection
   *         the Collection to check
   *
   * @return whether the given Collection is empty
   */
  public static boolean isEmpty(Collection<?> collection) {
    return (collection == null || collection.isEmpty());
  }

  /**
   * Return {@code true} if the supplied Map is {@code null} or empty. Otherwise,
   * return {@code false}.
   *
   * @param map
   *         the Map to check
   *
   * @return whether the given Map is empty
   */
  public static boolean isEmpty(Map<?, ?> map) {
    return (map == null || map.isEmpty());
  }

  public static boolean isCollection(Class<?> cls) {
    return Collection.class.isAssignableFrom(cls);
  }

  /**
   * Create a hash set
   *
   * @param elements
   *         Elements instance
   */
  @SafeVarargs
  public static <E> Set<E> newHashSet(E... elements) {
    final HashSet<E> ret = new HashSet<>();
    Collections.addAll(ret, elements);
    return ret;
  }

  // CollectionFactory

  /**
   * Determine whether the given collection type is an <em>approximable</em> type,
   * i.e. a type that {@link #createApproximateCollection} can approximate.
   *
   * @param collectionType
   *         the collection type to check
   *
   * @return {@code true} if the type is <em>approximable</em>
   *
   * @since 3.0
   */
  public static boolean isApproximableCollectionType(Class<?> collectionType) {
    return (collectionType != null && approximableCollectionTypes.contains(collectionType));
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
   * @param collection
   *         the original collection object, potentially {@code null}
   * @param capacity
   *         the initial capacity
   *
   * @return a new, empty collection instance
   *
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
    if (collection instanceof LinkedList) {
      return new LinkedList<>();
    }
    else if (collection instanceof List) {
      return new ArrayList<>(capacity);
    }
    else if (collection instanceof EnumSet) {
      // Cast is necessary for compilation in Eclipse 4.4.1.
      Collection<E> enumSet = (Collection<E>) EnumSet.copyOf((EnumSet) collection);
      enumSet.clear();
      return enumSet;
    }
    else if (collection instanceof SortedSet) {
      return new TreeSet<>(((SortedSet<E>) collection).comparator());
    }
    else {
      return new LinkedHashSet<>(capacity);
    }
  }

  /**
   * Create the most appropriate collection for the given collection type.
   * <p>
   * Delegates to {@link #createCollection(Class, Class, int)} with a {@code null}
   * element type, and {@link Constant#DEFAULT_CAPACITY}.
   *
   * @param collectionType
   *         the desired type of the target collection (never {@code null})
   *
   * @return a new collection instance
   *
   * @throws IllegalArgumentException
   *         if the supplied {@code collectionType} is {@code null} or of type
   *         {@link EnumSet}
   * @since 3.0
   */
  public static <E> Collection<E> createCollection(Class<?> collectionType) {
    return createCollection(collectionType, null, Constant.DEFAULT_CAPACITY);
  }

  /**
   * Create the most appropriate collection for the given collection type.
   * <p>
   * Delegates to {@link #createCollection(Class, Class, int)} with a {@code null}
   * element type.
   *
   * @param collectionType
   *         the desired type of the target collection (never {@code null})
   * @param capacity
   *         the initial capacity
   *
   * @return a new collection instance
   *
   * @throws IllegalArgumentException
   *         if the supplied {@code collectionType} is {@code null} or of type
   *         {@link EnumSet}
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
   * @param collectionType
   *         the desired type of the target collection (never {@code null})
   * @param elementType
   *         the collection's element type, or {@code null} if unknown (note:
   *         only relevant for {@link EnumSet} creation)
   * @param capacity
   *         the initial capacity
   *
   * @return a new collection instance
   *
   * @throws IllegalArgumentException
   *         if the supplied {@code collectionType} is {@code null}; or if the
   *         desired {@code collectionType} is {@link EnumSet} and the supplied
   *         {@code elementType} is not a subtype of {@link Enum}
   * @see java.util.LinkedHashSet
   * @see java.util.ArrayList
   * @see java.util.TreeSet
   * @see java.util.EnumSet
   * @since 3.0
   */
  @SuppressWarnings({ "unchecked", "cast" })
  public static <E> Collection<E> createCollection(Class<?> collectionType, Class<?> elementType, int capacity) {
    Assert.notNull(collectionType, "Collection type must not be null");
    if (collectionType.isInterface()) {
      if (Set.class == collectionType || Collection.class == collectionType) {
        return new LinkedHashSet<>(capacity);
      }
      else if (List.class == collectionType) {
        return new ArrayList<>(capacity);
      }
      else if (SortedSet.class == collectionType || NavigableSet.class == collectionType) {
        return new TreeSet<>();
      }
      else {
        throw new IllegalArgumentException("Unsupported Collection interface: " + collectionType.getName());
      }
    }
    else if (EnumSet.class.isAssignableFrom(collectionType)) {
      Assert.notNull(elementType, "Cannot create EnumSet for unknown element type");
      // Cast is necessary for compilation in Eclipse 4.4.1.
      return (Collection<E>) EnumSet.noneOf(asEnumType(elementType));
    }
    else {
      if (!Collection.class.isAssignableFrom(collectionType)) {
        throw new IllegalArgumentException("Unsupported Collection type: " + collectionType.getName());
      }
      try {
        return (Collection<E>) ReflectionUtils.accessibleConstructor(collectionType).newInstance();
      }
      catch (Throwable ex) {
        throw new IllegalArgumentException(
                "Could not instantiate Collection type: " + collectionType.getName(),
                ex);
      }
    }
  }

  /**
   * Determine whether the given map type is an <em>approximable</em> type, i.e. a
   * type that {@link #createApproximateMap} can approximate.
   *
   * @param mapType
   *         the map type to check
   *
   * @return {@code true} if the type is <em>approximable</em>
   *
   * @since 3.0
   */
  public static boolean isApproximableMapType(Class<?> mapType) {
    return (mapType != null && approximableMapTypes.contains(mapType));
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
   * use default capacity {@link Constant#DEFAULT_CAPACITY}.
   *
   * @param map
   *         the original map object, potentially {@code null}
   *
   * @return a new, empty map instance
   *
   * @see #isApproximableMapType
   * @see java.util.EnumMap
   * @see java.util.TreeMap
   * @see java.util.LinkedHashMap
   * @since 3.0
   */
  public static <K, V> Map<K, V> createApproximateMap(Object map) {
    return createApproximateMap(map, Constant.DEFAULT_CAPACITY);
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
   * @param map
   *         the original map object, potentially {@code null}
   * @param capacity
   *         the initial capacity
   *
   * @return a new, empty map instance
   *
   * @see #isApproximableMapType
   * @see java.util.EnumMap
   * @see java.util.TreeMap
   * @see java.util.LinkedHashMap
   * @since 3.0
   */
  @SuppressWarnings({ "rawtypes", "unchecked" })
  public static <K, V> Map<K, V> createApproximateMap(Object map, int capacity) {
    if (map instanceof EnumMap) {
      EnumMap enumMap = new EnumMap((EnumMap) map);
      enumMap.clear();
      return enumMap;
    }
    else if (map instanceof SortedMap) {
      return new TreeMap<>(((SortedMap<K, V>) map).comparator());
    }
    else {
      return new LinkedHashMap<>(capacity);
    }
  }

  /**
   * Create the most appropriate map for the given map type.
   * <p>
   * Delegates to {@link #createMap(Class, Class, int)} with a {@code null} key
   * type, and default capacity {@link Constant#DEFAULT_CAPACITY}.
   *
   * @param mapType
   *         the desired type of the target map
   *
   * @return a new map instance
   *
   * @throws IllegalArgumentException
   *         if the supplied {@code mapType} is {@code null} or of type
   *         {@link EnumMap}
   * @since 3.0
   */
  public static <K, V> Map<K, V> createMap(Class<?> mapType) {
    return createMap(mapType, null, Constant.DEFAULT_CAPACITY);
  }

  /**
   * Create the most appropriate map for the given map type.
   * <p>
   * Delegates to {@link #createMap(Class, Class, int)} with a {@code null} key
   * type.
   *
   * @param mapType
   *         the desired type of the target map
   * @param capacity
   *         the initial capacity
   *
   * @return a new map instance
   *
   * @throws IllegalArgumentException
   *         if the supplied {@code mapType} is {@code null} or of type
   *         {@link EnumMap}
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
   * @param mapType
   *         the desired type of the target map (never {@code null})
   * @param keyType
   *         the map's key type, or {@code null} if unknown (note: only relevant
   *         for {@link EnumMap} creation)
   * @param capacity
   *         the initial capacity
   *
   * @return a new map instance
   *
   * @throws IllegalArgumentException
   *         if the supplied {@code mapType} is {@code null}; or if the desired
   *         {@code mapType} is {@link EnumMap} and the supplied {@code keyType}
   *         is not a subtype of {@link Enum}
   * @see java.util.LinkedHashMap
   * @see java.util.TreeMap
   * @see DefaultMultiValueMap
   * @see java.util.EnumMap
   * @since 3.0
   */
  @SuppressWarnings({ "rawtypes", "unchecked" })
  public static <K, V> Map<K, V> createMap(Class<?> mapType, Class<?> keyType, int capacity) {
    Assert.notNull(mapType, "Map type must not be null");
    if (mapType.isInterface()) {
      if (Map.class == mapType) {
        return new LinkedHashMap<>(capacity);
      }
      else if (SortedMap.class == mapType || NavigableMap.class == mapType) {
        return new TreeMap<>();
      }
      else if (MultiValueMap.class == mapType) {
        return new DefaultMultiValueMap();
      }
      else {
        throw new IllegalArgumentException("Unsupported Map interface: " + mapType.getName());
      }
    }
    else if (EnumMap.class == mapType) {
      Assert.notNull(keyType, "Cannot create EnumMap for unknown key type");
      return new EnumMap(asEnumType(keyType));
    }
    else {
      if (!Map.class.isAssignableFrom(mapType)) {
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
   *
   * @see #createSortedProperties(boolean)
   * @see #createSortedProperties(Properties, boolean)
   * @since 3.0
   */
  public static Properties createStringAdaptingProperties() {
    final class SortedProperties0 extends SortedProperties {
      private static final long serialVersionUID = 1L;

      SortedProperties0() {
        super(false);
      }

      @Override
      public String getProperty(String key) {
        Object value = get(key);
        return (value != null ? value.toString() : null);
      }
    }
    return new SortedProperties0();
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
   * @param omitComments
   *         {@code true} if comments should be omitted when storing properties
   *         in a file
   *
   * @return a new {@code Properties} instance
   *
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
   * @param properties
   *         the {@code Properties} object from which to copy the initial
   *         properties
   * @param omitComments
   *         {@code true} if comments should be omitted when storing properties
   *         in a file
   *
   * @return a new {@code Properties} instance
   *
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
   * @param enumType
   *         the enum type, never {@code null}
   *
   * @return the given type as subtype of {@link Enum}
   *
   * @throws IllegalArgumentException
   *         if the given type is not a subtype of {@link Enum}
   * @since 3.0
   */
  @SuppressWarnings("rawtypes")
  private static Class<? extends Enum> asEnumType(Class<?> enumType) {
    Assert.notNull(enumType, "Enum type must not be null");
    if (!Enum.class.isAssignableFrom(enumType)) {
      throw new IllegalArgumentException("Supplied type is not an enum: " + enumType.getName());
    }
    return enumType.asSubclass(Enum.class);
  }

  /**
   * Replaces the element at the specified position in this list with the
   * specified element (optional operation).
   *
   * @param list
   *         target list
   * @param index
   *         new element's index
   * @param element
   *         element object
   * @param <E>
   *         Element type
   *
   * @throws IndexOutOfBoundsException
   *         if the index is out of range (index < 0 || index >= size())
   * @throws NullPointerException
   *         if the specified element is null and this list does not permit null
   *         elements
   * @since 3.0
   */
  public static <E> void setValue(final List<E> list, final int index, final E element) {
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
   * </pre>
   *
   * @param <T>
   *         the class of the elements to add and of the collection
   * @param c
   *         the collection into which <tt>elements</tt> are to be inserted
   * @param elements
   *         the elements to insert into <tt>c</tt>
   *
   * @return <tt>true</tt> if the collection changed as a result of the call
   *
   * @throws UnsupportedOperationException
   *         if <tt>c</tt> does not support
   *         the <tt>add</tt> operation
   * @throws NullPointerException
   *         if <tt>elements</tt> contains one or more
   *         null values and <tt>c</tt> does not permit null elements, or
   *         if <tt>c</tt> or <tt>elements</tt> are <tt>null</tt>
   * @throws IllegalArgumentException
   *         if some property of a value in
   *         <tt>elements</tt> prevents it from being added to <tt>c</tt>
   * @see Collection#addAll(Collection)
   * @since 4.0
   */
  @SuppressWarnings("all")
  public static void addAll(Collection c, Object[] elements) {
    for (Object element : elements) {
      c.add(element);
    }
  }

}
