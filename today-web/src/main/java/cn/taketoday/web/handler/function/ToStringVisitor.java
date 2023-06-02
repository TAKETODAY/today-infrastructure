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

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import cn.taketoday.core.io.Resource;
import cn.taketoday.http.HttpMethod;

/**
 * Implementation of {@link RouterFunctions.Visitor} that creates a formatted
 * string representation of router functions.
 *
 * @author Arjen Poutsma
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class ToStringVisitor implements RouterFunctions.Visitor, RequestPredicates.Visitor {

  private final StringBuilder builder = new StringBuilder();

  private int indent = 0;

  // RouterFunctions.Visitor

  @Override
  public void startNested(RequestPredicate predicate) {
    indent();
    predicate.accept(this);
    this.builder.append(" => {\n");
    this.indent++;
  }

  @Override
  public void endNested(RequestPredicate predicate) {
    this.indent--;
    indent();
    this.builder.append("}\n");
  }

  @Override
  public void route(RequestPredicate predicate, HandlerFunction<?> handlerFunction) {
    indent();
    predicate.accept(this);
    this.builder.append(" -> ");
    this.builder.append(handlerFunction).append('\n');
  }

  @Override
  public void resources(Function<ServerRequest, Optional<Resource>> lookupFunction) {
    indent();
    this.builder.append(lookupFunction).append('\n');
  }

  @Override
  public void attributes(Map<String, Object> attributes) {
  }

  @Override
  public void unknown(RouterFunction<?> routerFunction) {
    indent();
    this.builder.append(routerFunction);
  }

  private void indent() {
    for (int i = 0; i < this.indent; i++) {
      this.builder.append(' ');
    }
  }

  // RequestPredicates.Visitor

  @Override
  public void method(Set<HttpMethod> methods) {
    if (methods.size() == 1) {
      this.builder.append(methods.iterator().next());
    }
    else {
      this.builder.append(methods);
    }
  }

  @Override
  public void path(String pattern) {
    this.builder.append(pattern);
  }

  @Override
  public void pathExtension(String extension) {
    this.builder.append(String.format("*.%s", extension));
  }

  @Override
  public void header(String name, String value) {
    this.builder.append(String.format("%s: %s", name, value));
  }

  @Override
  public void param(String name, String value) {
    this.builder.append(String.format("?%s == %s", name, value));
  }

  @Override
  public void startAnd() {
    this.builder.append('(');
  }

  @Override
  public void and() {
    this.builder.append(" && ");
  }

  @Override
  public void endAnd() {
    this.builder.append(')');
  }

  @Override
  public void startOr() {
    this.builder.append('(');
  }

  @Override
  public void or() {
    this.builder.append(" || ");

  }

  @Override
  public void endOr() {
    this.builder.append(')');
  }

  @Override
  public void startNegate() {
    this.builder.append("!(");
  }

  @Override
  public void endNegate() {
    this.builder.append(')');
  }

  @Override
  public void unknown(RequestPredicate predicate) {
    this.builder.append(predicate);
  }

  @Override
  public String toString() {
    String result = this.builder.toString();
    if (result.endsWith("\n")) {
      result = result.substring(0, result.length() - 1);
    }
    return result;
  }

}
