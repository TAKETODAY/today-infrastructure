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

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

import cn.taketoday.lang.Assert;

/**
 * Represents a function that filters a {@linkplain HandlerFunction handler function}.
 *
 * @param <T> the type of the {@linkplain HandlerFunction handler function} to filter
 * @param <R> the type of the response of the function
 * @author Arjen Poutsma
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see RouterFunction#filter(HandlerFilterFunction)
 * @since 4.0
 */
@FunctionalInterface
public interface HandlerFilterFunction<T extends ServerResponse, R extends ServerResponse> {

  /**
   * Apply this filter to the given handler function. The given
   * {@linkplain HandlerFunction handler function} represents the next entity in the chain,
   * and can be {@linkplain HandlerFunction#handle(ServerRequest) invoked} in order to
   * proceed to this entity, or not invoked to block the chain.
   *
   * @param request the request
   * @param next the next handler or filter function in the chain
   * @return the filtered response
   */
  R filter(ServerRequest request, HandlerFunction<T> next) throws Exception;

  /**
   * Return a composed filter function that first applies this filter, and then applies the
   * {@code after} filter.
   *
   * @param after the filter to apply after this filter is applied
   * @return a composed filter that first applies this function and then applies the
   * {@code after} function
   */
  default HandlerFilterFunction<T, R> andThen(HandlerFilterFunction<T, T> after) {
    Assert.notNull(after, "HandlerFilterFunction is required");
    return (request, next) -> {
      HandlerFunction<T> nextHandler = handlerRequest -> after.filter(handlerRequest, next);
      return filter(request, nextHandler);
    };
  }

  /**
   * Apply this filter to the given handler function, resulting in a filtered handler function.
   *
   * @param handler the handler function to filter
   * @return the filtered handler function
   */
  default HandlerFunction<R> apply(HandlerFunction<T> handler) {
    Assert.notNull(handler, "HandlerFunction is required");
    return request -> this.filter(request, handler);
  }

  /**
   * Adapt the given request processor function to a filter function that only operates
   * on the {@code ServerRequest}.
   *
   * @param requestProcessor the request processor
   * @return the filter adaptation of the request processor
   */
  static <T extends ServerResponse> HandlerFilterFunction<T, T>
  ofRequestProcessor(Function<ServerRequest, ServerRequest> requestProcessor) {

    Assert.notNull(requestProcessor, "Function is required");
    return (request, next) -> next.handle(requestProcessor.apply(request));
  }

  /**
   * Adapt the given response processor function to a filter function that only operates
   * on the {@code ServerResponse}.
   *
   * @param responseProcessor the response processor
   * @return the filter adaptation of the request processor
   */
  static <T extends ServerResponse, R extends ServerResponse> HandlerFilterFunction<T, R>
  ofResponseProcessor(BiFunction<ServerRequest, T, R> responseProcessor) {

    Assert.notNull(responseProcessor, "Function is required");
    return (request, next) -> responseProcessor.apply(request, next.handle(request));
  }

  /**
   * Adapt the given predicate and response provider function to a filter function that returns
   * a {@code ServerResponse} on a given exception.
   *
   * @param predicate the predicate to match an exception
   * @param errorHandler the response provider
   * @return the filter adaption of the error handler
   */
  static <T extends ServerResponse> HandlerFilterFunction<T, T>
  ofErrorHandler(Predicate<Throwable> predicate, BiFunction<Throwable, ServerRequest, T> errorHandler) {

    Assert.notNull(predicate, "Predicate is required");
    Assert.notNull(errorHandler, "ErrorHandler is required");

    return (request, next) -> {
      try {
        T t = next.handle(request);
        if (t instanceof ErrorHandlingServerResponse) {
          ((ErrorHandlingServerResponse) t).addErrorHandler(predicate, errorHandler);
        }
        return t;
      }
      catch (Throwable throwable) {
        if (predicate.test(throwable)) {
          return errorHandler.apply(throwable, request);
        }
        else {
          throw throwable;
        }
      }
    };
  }

}
