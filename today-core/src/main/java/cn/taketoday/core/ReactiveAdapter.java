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

package cn.taketoday.core;

import org.reactivestreams.Publisher;

import java.util.function.Function;

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;

/**
 * Adapter for a Reactive Streams {@link Publisher} to and from an async/reactive
 * type such as {@code CompletableFuture}, RxJava {@code Observable}, and others.
 *
 * <p>An adapter is typically obtained via {@link ReactiveAdapterRegistry}.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class ReactiveAdapter {

  private final ReactiveTypeDescriptor descriptor;
  private final Function<Object, Publisher<?>> toPublisherFunction;
  private final Function<Publisher<?>, Object> fromPublisherFunction;

  /**
   * Constructor for an adapter with functions to convert the target reactive
   * or async type to and from a Reactive Streams Publisher.
   *
   * @param descriptor the reactive type descriptor
   * @param toPublisherFunction adapter to a Publisher
   * @param fromPublisherFunction adapter from a Publisher
   */
  public ReactiveAdapter(ReactiveTypeDescriptor descriptor,
          Function<Object, Publisher<?>> toPublisherFunction,
          Function<Publisher<?>, Object> fromPublisherFunction) {

    Assert.notNull(descriptor, "'descriptor' is required");
    Assert.notNull(toPublisherFunction, "'toPublisherFunction' is required");
    Assert.notNull(fromPublisherFunction, "'fromPublisherFunction' is required");

    this.descriptor = descriptor;
    this.toPublisherFunction = toPublisherFunction;
    this.fromPublisherFunction = fromPublisherFunction;
  }

  /**
   * Return the descriptor of the reactive type for the adapter.
   */
  public ReactiveTypeDescriptor getDescriptor() {
    return this.descriptor;
  }

  /**
   * Shortcut for {@code getDescriptor().getReactiveType()}.
   */
  public Class<?> getReactiveType() {
    return getDescriptor().getReactiveType();
  }

  /**
   * Shortcut for {@code getDescriptor().isMultiValue()}.
   */
  public boolean isMultiValue() {
    return getDescriptor().isMultiValue();
  }

  /**
   * Shortcut for {@code getDescriptor().isNoValue()}.
   */
  public boolean isNoValue() {
    return getDescriptor().isNoValue();
  }

  /**
   * Shortcut for {@code getDescriptor().supportsEmpty()}.
   */
  public boolean supportsEmpty() {
    return getDescriptor().supportsEmpty();
  }

  /**
   * Adapt the given instance to a Reactive Streams {@code Publisher}.
   *
   * @param source the source object to adapt from; if the given object is
   * {@code null}, {@link ReactiveTypeDescriptor#getEmptyValue()} is used.
   * @return the Publisher representing the adaptation
   */
  @SuppressWarnings("unchecked")
  public <T> Publisher<T> toPublisher(@Nullable Object source) {
    if (source == null) {
      source = getDescriptor().getEmptyValue();
    }
    return (Publisher<T>) this.toPublisherFunction.apply(source);
  }

  /**
   * Adapt from the given Reactive Streams Publisher.
   *
   * @param publisher the publisher to adapt from
   * @return the reactive type instance representing the adapted publisher
   */
  public Object fromPublisher(Publisher<?> publisher) {
    return this.fromPublisherFunction.apply(publisher);
  }

}
