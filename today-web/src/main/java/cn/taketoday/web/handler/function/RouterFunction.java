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

package cn.taketoday.web.handler.function;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import cn.taketoday.lang.Assert;

/**
 * Represents a function that routes to a {@linkplain HandlerFunction handler function}.
 *
 * @param <T> the type of the {@linkplain HandlerFunction handler function} to route to
 * @author Arjen Poutsma
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see RouterFunctions
 * @since 4.0
 */
@FunctionalInterface
public interface RouterFunction<T extends ServerResponse> {

  /**
   * Return the {@linkplain HandlerFunction handler function} that matches the given request.
   *
   * @param request the request to route
   * @return an {@code Optional} describing the {@code HandlerFunction} that matches this request,
   * or an empty {@code Optional} if there is no match
   */
  Optional<HandlerFunction<T>> route(ServerRequest request);

  /**
   * Return a composed routing function that first invokes this function,
   * and then invokes the {@code other} function (of the same response type {@code T})
   * if this route had {@linkplain Optional#empty() no result}.
   *
   * @param other the function of type {@code T} to apply when this function has no result
   * @return a composed function that first routes with this function and then the
   * {@code other} function if this function has no result
   * @see #andOther(RouterFunction)
   */
  default RouterFunction<T> and(RouterFunction<T> other) {
    return new RouterFunctions.SameComposedRouterFunction<>(this, other);
  }

  /**
   * Return a composed routing function that first invokes this function,
   * and then invokes the {@code other} function (of a different response type) if this route had
   * {@linkplain Optional#empty() no result}.
   *
   * @param other the function to apply when this function has no result
   * @return a composed function that first routes with this function and then the
   * {@code other} function if this function has no result
   * @see #and(RouterFunction)
   */
  default RouterFunction<?> andOther(RouterFunction<?> other) {
    return new RouterFunctions.DifferentComposedRouterFunction(this, other);
  }

  /**
   * Return a composed routing function that routes to the given handler function if this
   * route does not match and the given request predicate applies. This method is a convenient
   * combination of {@link #and(RouterFunction)} and
   * {@link RouterFunctions#route(RequestPredicate, HandlerFunction)}.
   *
   * @param predicate the predicate to test if this route does not match
   * @param handlerFunction the handler function to route to if this route does not match and
   * the predicate applies
   * @return a composed function that route to {@code handlerFunction} if this route does not
   * match and if {@code predicate} applies
   */
  default RouterFunction<T> andRoute(RequestPredicate predicate, HandlerFunction<T> handlerFunction) {
    return and(RouterFunctions.route(predicate, handlerFunction));
  }

  /**
   * Return a composed routing function that routes to the given router function if this
   * route does not match and the given request predicate applies. This method is a convenient
   * combination of {@link #and(RouterFunction)} and
   * {@link RouterFunctions#nest(RequestPredicate, RouterFunction)}.
   *
   * @param predicate the predicate to test if this route does not match
   * @param routerFunction the router function to route to if this route does not match and
   * the predicate applies
   * @return a composed function that route to {@code routerFunction} if this route does not
   * match and if {@code predicate} applies
   */
  default RouterFunction<T> andNest(RequestPredicate predicate, RouterFunction<T> routerFunction) {
    return and(RouterFunctions.nest(predicate, routerFunction));
  }

  /**
   * Filter all {@linkplain HandlerFunction handler functions} routed by this function with the given
   * {@linkplain HandlerFilterFunction filter function}.
   *
   * @param <S> the filter return type
   * @param filterFunction the filter to apply
   * @return the filtered routing function
   */
  default <S extends ServerResponse> RouterFunction<S> filter(HandlerFilterFunction<T, S> filterFunction) {
    return new RouterFunctions.FilteredRouterFunction<>(this, filterFunction);
  }

  /**
   * Accept the given visitor. Default implementation calls
   * {@link RouterFunctions.Visitor#unknown(RouterFunction)}; composed {@code RouterFunction}
   * implementations are expected to call {@code accept} for all components that make up this
   * router function.
   *
   * @param visitor the visitor to accept
   */
  default void accept(RouterFunctions.Visitor visitor) {
    visitor.unknown(this);
  }

  /**
   * Return a new routing function with the given attribute.
   *
   * @param name the attribute name
   * @param value the attribute value
   * @return a function that has the specified attributes
   */
  default RouterFunction<T> withAttribute(String name, Object value) {
    Assert.hasLength(name, "Name must not be empty");
    Assert.notNull(value, "Value is required");

    var attributes = new LinkedHashMap<String, Object>();
    attributes.put(name, value);
    return new RouterFunctions.AttributesRouterFunction<>(this, attributes);
  }

  /**
   * Return a new routing function with attributes manipulated with the given consumer.
   * <p>The map provided to the consumer is "live", so that the consumer can be used
   * to {@linkplain Map#put(Object, Object) overwrite} existing attributes,
   * {@linkplain Map#remove(Object) remove} attributes, or use any of the other
   * {@link Map} methods.
   *
   * @param attributesConsumer a function that consumes the attributes map
   * @return this builder
   */
  default RouterFunction<T> withAttributes(Consumer<Map<String, Object>> attributesConsumer) {
    Assert.notNull(attributesConsumer, "AttributesConsumer is required");

    Map<String, Object> attributes = new LinkedHashMap<>();
    attributesConsumer.accept(attributes);
    return new RouterFunctions.AttributesRouterFunction<>(this, attributes);
  }

}
