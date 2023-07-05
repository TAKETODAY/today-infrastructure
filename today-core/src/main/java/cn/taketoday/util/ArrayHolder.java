/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.RandomAccess;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Supplier;
import java.util.stream.Stream;

import cn.taketoday.core.annotation.AnnotationAwareOrderComparator;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;

/**
 * It is used for the scenario of changing less and reading more frequently.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/1/12 15:28
 */
public final class ArrayHolder<E> implements Supplier<E[]>, Iterable<E>, RandomAccess {
  private E[] array;

  @Nullable
  private final Class<E> elementClass;

  @Nullable
  private final IntFunction<E[]> arrayGenerator;

  public ArrayHolder() {
    this(null, null);
  }

  public ArrayHolder(@Nullable Class<E> elementClass, @Nullable IntFunction<E[]> arrayGenerator) {
    this.elementClass = elementClass;
    this.arrayGenerator = arrayGenerator;
  }

  @SafeVarargs
  public final void set(@Nullable E... array) {
    this.array = array;
  }

  @SuppressWarnings("unchecked")
  public void sort() {
    sort((Comparator<E>) AnnotationAwareOrderComparator.INSTANCE);
  }

  public void sort(Comparator<E> comparator) {
    if (array != null && array.length > 1) {
      Arrays.sort(array, comparator);
    }
  }

  /**
   * @param element element to add
   * @throws NullPointerException element is null
   */
  @SuppressWarnings("unchecked")
  public void add(E element) {
    E[] array = this.array;
    if (array == null) {
      array = (E[]) Array.newInstance(element.getClass(), 1);
      array[0] = element;
    }
    else {
      Class<E> elementClass = this.elementClass;
      if (elementClass == null) {
        E first = array[0];
        elementClass = (Class<E>) first.getClass();
      }
      int length = array.length;
      array = (E[]) Array.newInstance(elementClass, length + 1);
      array[length] = element;
    }
    this.array = array;
  }

  @SafeVarargs
  public final void add(@Nullable E... array) {
    if (ObjectUtils.isNotEmpty(array)) {
      ArrayList<E> objects = new ArrayList<>(array.length + size());
      CollectionUtils.addAll(objects, this.array);
      CollectionUtils.addAll(objects, array);
      set(objects);
    }
  }

  public void set(int index, E element) {
    ArrayList<E> objects = new ArrayList<>(size());
    CollectionUtils.addAll(objects, array);
    objects.set(index, element);
    set(objects);
  }

  public void add(int index, E element) {
    ArrayList<E> objects = new ArrayList<>(size() + 1);
    CollectionUtils.addAll(objects, array);
    objects.add(index, element);
    set(objects);
  }

  /**
   * add list at end of the array
   *
   * @param list list to add
   * @throws NullPointerException input list is null
   */
  public void addAll(@Nullable Collection<E> list) {
    if (CollectionUtils.isNotEmpty(list)) {
      ArrayList<E> objects = new ArrayList<>(list.size());
      CollectionUtils.addAll(objects, this.array);
      CollectionUtils.addAll(objects, list);
      set(objects);
    }
  }

  @SuppressWarnings("unchecked")
  public void set(@Nullable Collection<E> list) {
    if (CollectionUtils.isEmpty(list)) {
      this.array = null;
    }
    else {
      if (arrayGenerator != null) {
        set(list.toArray(arrayGenerator.apply(list.size())));
      }
      else {
        Class<E> elementClass = this.elementClass;
        if (elementClass == null) {
          E firstElement = CollectionUtils.firstElement(list);
          Assert.state(firstElement != null, "list is empty");
          elementClass = (Class<E>) firstElement.getClass();
        }
        set(list.toArray((E[]) Array.newInstance(elementClass, list.size())));
      }
    }
  }

  @Override
  @Nullable
  public E[] get() {
    return array;
  }

  @Override
  public Iterator<E> iterator() {
    if (isPresent()) {
      return new ArrayIterator<>(array);
    }
    return Collections.emptyIterator();
  }

  /**
   * If array is present, performs the given action with the value,
   * otherwise does nothing.
   *
   * @param action the action to be performed, if array is present
   * @throws NullPointerException if value is present and the given action is
   * {@code null}
   * @see #isPresent()
   */
  public void ifPresent(Consumer<E[]> action) {
    if (isPresent()) {
      action.accept(array);
    }
  }

  /**
   * If array is present, performs the given action with the value,
   * otherwise performs the given empty-based action.
   *
   * @param action the action to be performed, if array is present
   * @param emptyAction the empty-based action to be performed, if no value is
   * present
   * @throws NullPointerException if array is present and the given action
   * is {@code null}, or no value is present and the given empty-based
   * action is {@code null}.
   * @see #isPresent()
   */
  public void ifPresentOrElse(Consumer<E[]> action, Runnable emptyAction) {
    if (isPresent()) {
      action.accept(array);
    }
    else {
      emptyAction.run();
    }
  }

  /**
   * @param mapper the mapping function to apply to a value, if present
   * @param <U> The type of the value returned from the mapping function
   * @return an {@code U} describing the result of applying a mapping
   * function to the value of this {@code array}, if a value is
   * present, otherwise {@code null}
   * @throws NullPointerException if the mapping function is {@code null}
   */
  @Nullable
  public <U> U map(Function<? super E[], ? extends U> mapper) {
    return map(mapper, null);
  }

  /**
   * @param mapper the mapping function to apply to a value, if present
   * @param emptySupplier the supplier to be used if no value is present
   * @param <U> The type of the value returned from the mapping function
   * @return an {@code U} describing the result of applying a mapping
   * function to the value of this {@code array}, if a value is
   * present, otherwise {@code null}
   * @throws NullPointerException if the mapping function is {@code null}
   */
  @Nullable
  public <U> U map(Function<? super E[], ? extends U> mapper, @Nullable Supplier<U> emptySupplier) {
    if (isEmpty()) {
      if (emptySupplier != null) {
        return emptySupplier.get();
      }
      return null;
    }
    else {
      return mapper.apply(array);
    }
  }

  /**
   * If array is present, returns a sequential {@link Stream} containing
   * only that value, otherwise returns an empty {@code Stream}.
   * <p>
   * This method can be used to transform a {@code Stream} of optional
   * elements to a {@code Stream} of present value elements:
   * <pre>{@code
   *     Stream<Optional<T>> os = ..
   *     Stream<T> s = os.flatMap(ArrayHolder::stream)
   * }</pre>
   *
   * @return the optional value as a {@code Stream}
   * @see #isPresent()
   */
  public Stream<E> stream() {
    if (isEmpty()) {
      return Stream.empty();
    }
    else {
      return Arrays.stream(array);
    }
  }

  /**
   * If array is present, returns the value, otherwise returns
   * {@code other}.
   *
   * @param other the value to be returned, if no value is present.
   * May be {@code null}.
   * @return the value, if present, otherwise {@code other}
   * @see #isPresent()
   */
  public E[] orElse(E[] other) {
    return isPresent() ? array : other;
  }

  /**
   * If array is present, returns the value, otherwise returns the result
   * produced by the supplying function.
   *
   * @param supplier the supplying function that produces array to be returned
   * @return the value, if present, otherwise the result produced by the
   * supplying function
   * @throws NullPointerException if no value is present and the supplying
   * function is {@code null}
   * @see #isPresent()
   */
  public E[] orElseGet(Supplier<E[]> supplier) {
    return isPresent() ? array : supplier.get();
  }

  /**
   * If array is present, returns the value, otherwise throws
   * {@code NoSuchElementException}.
   *
   * @return the non-{@code null} value described by this {@code Optional}
   * @throws NoSuchElementException if no value is present
   * @see #isPresent()
   */
  public E[] orElseThrow() {
    if (isEmpty()) {
      throw new NoSuchElementException("No value present");
    }
    return array;
  }

  public Optional<E[]> getOptional() {
    return Optional.ofNullable(array);
  }

  public E[] getRequired() {
    return getRequired("Array is not available");
  }

  /**
   * If array is {@code null}, returns the value, otherwise throws
   * {@code IllegalStateException}
   *
   * @throws IllegalStateException if array is {@code null}
   */
  public E[] getRequired(String message) {
    if (array == null) {
      throw new IllegalStateException(message);
    }
    return array;
  }

  /**
   * If array is present, returns the value, otherwise throws an exception
   * produced by the exception supplying function.
   * <p>
   * A method reference to the exception constructor with an empty argument
   * list can be used as the supplier. For example,
   * {@code IllegalStateException::new}
   *
   * @param <X> Type of the exception to be thrown
   * @param exceptionSupplier the supplying function that produces an
   * exception to be thrown
   * @return the value, if present
   * @throws X if no value is present
   * @throws NullPointerException if no value is present and the exception
   * supplying function is {@code null}
   * @see #isPresent()
   */
  public <X extends Throwable> E[] orElseThrow(Supplier<? extends X> exceptionSupplier) throws X {
    if (isPresent()) {
      return array;
    }
    else {
      throw exceptionSupplier.get();
    }
  }

  /**
   * If array is non-null and non-empty , returns {@code true}, otherwise {@code false}.
   *
   * @return {@code true} if array is present, otherwise {@code false}
   */
  public boolean isPresent() {
    final E[] array = this.array;
    return array != null && array.length != 0;
  }

  /**
   * If array is null or empty , returns {@code true}, otherwise
   * {@code false}.
   *
   * @return {@code true} if array is not present, otherwise {@code false}
   */
  public boolean isEmpty() {
    final E[] array = this.array;
    return array == null || array.length == 0;
  }

  public int size() {
    final E[] array = this.array;
    return array == null ? 0 : array.length;
  }

  /**
   * set array to {@code null}
   */
  public void clear() {
    this.array = null;
  }

  @Override
  public String toString() {
    return isPresent()
           ? Arrays.toString(array)
           : "[]";
  }

  @Override
  public boolean equals(Object o) {
    return this == o || (o instanceof ArrayHolder<?> that && Arrays.equals(array, that.array));
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(array);
  }

  // static factory method

  @SafeVarargs
  public static <E> ArrayHolder<E> valueOf(E... array) {
    ArrayHolder<E> holder = new ArrayHolder<>();
    holder.set(array);
    return holder;
  }

  public static <E> ArrayHolder<E> valueOf(List<E> list) {
    ArrayHolder<E> holder = new ArrayHolder<>();
    holder.set(list);
    return holder;
  }

  public static <E> ArrayHolder<E> copyOf(ArrayHolder<E> holder) {
    ArrayHolder<E> arrayHolder = new ArrayHolder<>();
    if (ObjectUtils.isNotEmpty(holder.array)) {
      arrayHolder.set(Arrays.copyOf(holder.array, holder.array.length));
    }
    return arrayHolder;
  }

  /**
   * @see #set(Collection)
   */
  public static <E> ArrayHolder<E> forClass(@Nullable Class<E> elementClass) {
    return new ArrayHolder<>(elementClass, null);
  }

  /**
   * @see #set(Collection)
   */
  public static <E> ArrayHolder<E> forGenerator(@Nullable IntFunction<E[]> arrayGenerator) {
    return new ArrayHolder<>(null, arrayGenerator);
  }

}
