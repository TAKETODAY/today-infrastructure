/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.core;

import java.util.function.Supplier;

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;

/**
 * Describes the semantics of a reactive type including boolean checks for
 * {@link #isMultiValue()}, {@link #isNoValue()}, and {@link #supportsEmpty()}.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public final class ReactiveTypeDescriptor {

  private final Class<?> reactiveType;

  private final boolean multiValue;

  private final boolean noValue;

  @Nullable
  private final Supplier<?> emptySupplier;

  private final boolean deferred;

  private ReactiveTypeDescriptor(Class<?> reactiveType, boolean multiValue, boolean noValue,
          @Nullable Supplier<?> emptySupplier) {

    this(reactiveType, multiValue, noValue, emptySupplier, true);
  }

  private ReactiveTypeDescriptor(Class<?> reactiveType, boolean multiValue, boolean noValue,
          @Nullable Supplier<?> emptySupplier, boolean deferred) {

    Assert.notNull(reactiveType, "'reactiveType' must not be null");
    this.reactiveType = reactiveType;
    this.multiValue = multiValue;
    this.noValue = noValue;
    this.emptySupplier = emptySupplier;
    this.deferred = deferred;
  }

  /**
   * Return the reactive type for this descriptor.
   */
  public Class<?> getReactiveType() {
    return this.reactiveType;
  }

  /**
   * Return {@code true} if the reactive type can produce more than 1 value
   * can be produced and is therefore a good fit to adapt to {@code Flux}.
   * A {@code false} return value implies the reactive type can produce 1
   * value at most and is therefore a good fit to adapt to {@code Mono}.
   */
  public boolean isMultiValue() {
    return this.multiValue;
  }

  /**
   * Return {@code true} if the reactive type does not produce any values and
   * only provides completion and error signals.
   */
  public boolean isNoValue() {
    return this.noValue;
  }

  /**
   * Return {@code true} if the reactive type can complete with no values.
   */
  public boolean supportsEmpty() {
    return (this.emptySupplier != null);
  }

  /**
   * Return an empty-value instance for the underlying reactive or async type.
   * <p>Use of this type implies {@link #supportsEmpty()} is {@code true}.
   */
  public Object getEmptyValue() {
    Assert.state(this.emptySupplier != null, "Empty values not supported");
    return this.emptySupplier.get();
  }

  /**
   * Whether the underlying operation is deferred and needs to be started
   * explicitly, e.g. via subscribing (or similar), or whether it is triggered
   * without the consumer having any control.
   */
  public boolean isDeferred() {
    return this.deferred;
  }

  @Override
  public boolean equals(@Nullable Object other) {
    if (this == other) {
      return true;
    }
    if (other == null || getClass() != other.getClass()) {
      return false;
    }
    return this.reactiveType.equals(((ReactiveTypeDescriptor) other).reactiveType);
  }

  @Override
  public int hashCode() {
    return this.reactiveType.hashCode();
  }

  /**
   * Descriptor for a reactive type that can produce 0..N values.
   *
   * @param type the reactive type
   * @param emptySupplier a supplier of an empty-value instance of the reactive type
   */
  public static ReactiveTypeDescriptor multiValue(Class<?> type, Supplier<?> emptySupplier) {
    return new ReactiveTypeDescriptor(type, true, false, emptySupplier);
  }

  /**
   * Descriptor for a reactive type that can produce 0..1 values.
   *
   * @param type the reactive type
   * @param emptySupplier a supplier of an empty-value instance of the reactive type
   */
  public static ReactiveTypeDescriptor singleOptionalValue(Class<?> type, Supplier<?> emptySupplier) {
    return new ReactiveTypeDescriptor(type, false, false, emptySupplier);
  }

  /**
   * Descriptor for a reactive type that must produce 1 value to complete.
   *
   * @param type the reactive type
   */
  public static ReactiveTypeDescriptor singleRequiredValue(Class<?> type) {
    return new ReactiveTypeDescriptor(type, false, false, null);
  }

  /**
   * Descriptor for a reactive type that does not produce any values.
   *
   * @param type the reactive type
   * @param emptySupplier a supplier of an empty-value instance of the reactive type
   */
  public static ReactiveTypeDescriptor noValue(Class<?> type, Supplier<?> emptySupplier) {
    return new ReactiveTypeDescriptor(type, false, true, emptySupplier);
  }

  /**
   * The same as {@link #singleOptionalValue(Class, Supplier)} but for a
   * non-deferred, async type such as {@link java.util.concurrent.CompletableFuture}.
   *
   * @param type the reactive type
   * @param emptySupplier a supplier of an empty-value instance of the reactive type
   */
  public static ReactiveTypeDescriptor nonDeferredAsyncValue(Class<?> type, Supplier<?> emptySupplier) {
    return new ReactiveTypeDescriptor(type, false, false, emptySupplier, false);
  }

}
