/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.web.handler.function;

import java.util.Optional;

/**
 * Represents a function that evaluates on a given {@link ServerRequest}.
 * Instances of this function that evaluate on common request properties
 * can be found in {@link RequestPredicates}.
 *
 * @author Arjen Poutsma
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see RequestPredicates
 * @see RouterFunctions#route(RequestPredicate, HandlerFunction)
 * @see RouterFunctions#nest(RequestPredicate, RouterFunction)
 * @since 4.0
 */
@FunctionalInterface
public interface RequestPredicate {

  /**
   * Evaluate this predicate on the given request.
   *
   * @param request the request to match against
   * @return {@code true} if the request matches the predicate; {@code false} otherwise
   */
  boolean test(ServerRequest request);

  /**
   * Return a composed request predicate that tests against both this predicate AND
   * the {@code other} predicate. When evaluating the composed predicate, if this
   * predicate is {@code false}, then the {@code other} predicate is not evaluated.
   *
   * @param other a predicate that will be logically-ANDed with this predicate
   * @return a predicate composed of this predicate AND the {@code other} predicate
   */
  default RequestPredicate and(RequestPredicate other) {
    return new RequestPredicates.AndRequestPredicate(this, other);
  }

  /**
   * Return a predicate that represents the logical negation of this predicate.
   *
   * @return a predicate that represents the logical negation of this predicate
   */
  default RequestPredicate negate() {
    return new RequestPredicates.NegateRequestPredicate(this);
  }

  /**
   * Return a composed request predicate that tests against both this predicate OR
   * the {@code other} predicate. When evaluating the composed predicate, if this
   * predicate is {@code true}, then the {@code other} predicate is not evaluated.
   *
   * @param other a predicate that will be logically-ORed with this predicate
   * @return a predicate composed of this predicate OR the {@code other} predicate
   */
  default RequestPredicate or(RequestPredicate other) {
    return new RequestPredicates.OrRequestPredicate(this, other);
  }

  /**
   * Transform the given request into a request used for a nested route. For instance,
   * a path-based predicate can return a {@code ServerRequest} with a path remaining
   * after a match.
   * <p>The default implementation returns an {@code Optional} wrapping the given request if
   * {@link #test(ServerRequest)} evaluates to {@code true}; or {@link Optional#empty()}
   * if it evaluates to {@code false}.
   *
   * @param request the request to be nested
   * @return the nested request
   * @see RouterFunctions#nest(RequestPredicate, RouterFunction)
   */
  default Optional<ServerRequest> nest(ServerRequest request) {
    return test(request) ? Optional.of(request) : Optional.empty();
  }

  /**
   * Accept the given visitor. Default implementation calls
   * {@link RequestPredicates.Visitor#unknown(RequestPredicate)}; composed {@code RequestPredicate}
   * implementations are expected to call {@code accept} for all components that make up this
   * request predicate.
   *
   * @param visitor the visitor to accept
   */
  default void accept(RequestPredicates.Visitor visitor) {
    visitor.unknown(this);
  }

}
