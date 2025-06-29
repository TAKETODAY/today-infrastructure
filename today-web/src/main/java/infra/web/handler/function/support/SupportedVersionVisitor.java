/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.web.handler.function.support;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import infra.core.io.Resource;
import infra.http.HttpMethod;
import infra.web.accept.DefaultApiVersionStrategy;
import infra.web.handler.function.HandlerFunction;
import infra.web.handler.function.RequestPredicate;
import infra.web.handler.function.RequestPredicates;
import infra.web.handler.function.RouterFunction;
import infra.web.handler.function.RouterFunctions;
import infra.web.handler.function.ServerRequest;

/**
 * {@link RequestPredicates.Visitor} that discovers versions used in routes in
 * order to add them to the list of supported versions.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0
 */
final class SupportedVersionVisitor implements RouterFunctions.Visitor, RequestPredicates.Visitor {

  private final DefaultApiVersionStrategy versionStrategy;

  SupportedVersionVisitor(DefaultApiVersionStrategy versionStrategy) {
    this.versionStrategy = versionStrategy;
  }

  // RouterFunctions.Visitor

  @Override
  public void startNested(RequestPredicate predicate) {
    predicate.accept(this);
  }

  @Override
  public void endNested(RequestPredicate predicate) {
  }

  @Override
  public void route(RequestPredicate predicate, HandlerFunction<?> handlerFunction) {
    predicate.accept(this);
  }

  @Override
  public void resources(Function<ServerRequest, Optional<Resource>> lookupFunction) {
  }

  @Override
  public void attributes(Map<String, Object> attributes) {
  }

  @Override
  public void unknown(RouterFunction<?> routerFunction) {
  }

  // RequestPredicates.Visitor

  @Override
  public void method(Set<HttpMethod> methods) {
  }

  @Override
  public void path(String pattern) {
  }

  @SuppressWarnings("removal")
  @Override
  public void pathExtension(String extension) {
  }

  @Override
  public void header(String name, String value) {
  }

  @Override
  public void param(String name, String value) {

  }

  @Override
  public void version(String version) {
    this.versionStrategy.addMappedVersion(
            (version.endsWith("+") ? version.substring(0, version.length() - 1) : version));
  }

  @Override
  public void startAnd() {
  }

  @Override
  public void and() {
  }

  @Override
  public void endAnd() {
  }

  @Override
  public void startOr() {
  }

  @Override
  public void or() {
  }

  @Override
  public void endOr() {
  }

  @Override
  public void startNegate() {
  }

  @Override
  public void endNegate() {
  }

  @Override
  public void unknown(RequestPredicate predicate) {
  }

}
