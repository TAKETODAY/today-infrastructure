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

package infra.web.handler.function;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import infra.core.io.Resource;
import infra.web.util.pattern.PathPatternParser;

/**
 * Implementation of {@link RouterFunctions.Visitor} that changes the
 * {@link PathPatternParser} on path-related request predicates
 * (i.e. {@code RequestPredicates.PathPatternPredicate}.
 *
 * @author Arjen Poutsma
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class ChangePathPatternParserVisitor implements RouterFunctions.Visitor {

  private final PathPatternParser parser;

  public ChangePathPatternParserVisitor(PathPatternParser parser) {
    this.parser = parser;
  }

  @Override
  public void startNested(RequestPredicate predicate) {
    changeParser(predicate);
  }

  @Override
  public void endNested(RequestPredicate predicate) {
  }

  @Override
  public void route(RequestPredicate predicate, HandlerFunction<?> handlerFunction) {
    changeParser(predicate);
  }

  @Override
  public void unknown(RouterFunction<?> routerFunction) {
  }

  @Override
  public void attributes(Map<String, Object> attributes) {
  }

  @Override
  public void resources(Function<ServerRequest, Optional<Resource>> lookupFunction) {
  }

  private void changeParser(RequestPredicate predicate) {
    if (predicate instanceof Target target) {
      target.changeParser(this.parser);
    }
  }

  /**
   * Interface implemented by predicates that can change the parser.
   */
  public interface Target {

    void changeParser(PathPatternParser parser);
  }
}
