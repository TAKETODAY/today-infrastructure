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

import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import cn.taketoday.core.io.Resource;
import cn.taketoday.lang.Nullable;

/**
 * @author Arjen Poutsma
 */
class AttributesTestVisitor implements RouterFunctions.Visitor {

  private Deque<Map<String, Object>> nestedAttributes = new LinkedList<>();

  @Nullable
  private Map<String, Object> attributes;

  private List<List<Map<String, Object>>> routerFunctionsAttributes = new LinkedList<>();

  private int visitCount;

  public List<List<Map<String, Object>>> routerFunctionsAttributes() {
    return this.routerFunctionsAttributes;
  }

  public int visitCount() {
    return this.visitCount;
  }

  @Override
  public void startNested(RequestPredicate predicate) {
    nestedAttributes.addFirst(attributes);
    attributes = null;
  }

  @Override
  public void endNested(RequestPredicate predicate) {
    attributes = nestedAttributes.removeFirst();
  }

  @Override
  public void route(RequestPredicate predicate, HandlerFunction<?> handlerFunction) {
    Stream<Map<String, Object>> current = Optional.ofNullable(attributes).stream();
    Stream<Map<String, Object>> nested = nestedAttributes.stream().filter(Objects::nonNull);
    routerFunctionsAttributes.add(Stream.concat(current, nested).collect(Collectors.toUnmodifiableList()));
    attributes = null;
  }

  @Override
  public void resources(Function<ServerRequest, Optional<Resource>> lookupFunction) {
  }

  @Override
  public void attributes(Map<String, Object> attributes) {
    this.attributes = attributes;
    this.visitCount++;
  }

  @Override
  public void unknown(RouterFunction<?> routerFunction) {

  }
}
