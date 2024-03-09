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

import java.util.Optional;
import java.util.function.Function;

import cn.taketoday.core.io.Resource;
import cn.taketoday.lang.Assert;

/**
 * Lookup function used by {@link RouterFunctions#resource(RequestPredicate, Resource)} and
 * {@link RouterFunctions#resource(RequestPredicate, Resource, java.util.function.BiConsumer)}.
 *
 * @author Sebastien Deleuze
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class PredicateResourceLookupFunction implements Function<ServerRequest, Optional<Resource>> {

  private final RequestPredicate predicate;

  private final Resource resource;

  public PredicateResourceLookupFunction(RequestPredicate predicate, Resource resource) {
    Assert.notNull(predicate, "'predicate' must not be null");
    Assert.notNull(resource, "'resource' must not be null");
    this.predicate = predicate;
    this.resource = resource;
  }

  @Override
  public Optional<Resource> apply(ServerRequest serverRequest) {
    return this.predicate.test(serverRequest) ? Optional.of(this.resource) : Optional.empty();
  }

}
