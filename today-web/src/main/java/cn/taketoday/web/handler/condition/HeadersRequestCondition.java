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

package cn.taketoday.web.handler.condition;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import cn.taketoday.http.HttpHeaders;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.annotation.RequestMapping;

/**
 * A logical conjunction ({@code ' && '}) request condition that matches a request against
 * a set of header expressions with syntax defined in {@link RequestMapping#headers()}.
 *
 * <p>Expressions passed to the constructor with header names 'Accept' or
 * 'Content-Type' are ignored. See {@link ConsumesRequestCondition} and
 * {@link ProducesRequestCondition} for those.
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public final class HeadersRequestCondition extends AbstractRequestCondition<HeadersRequestCondition> {

  private static final HeadersRequestCondition PRE_FLIGHT_MATCH = new HeadersRequestCondition();

  @Nullable
  private final LinkedHashSet<HeaderExpression> expressions;

  /**
   * Create a new instance from the given header expressions. Expressions with
   * header names 'Accept' or 'Content-Type' are ignored. See {@link ConsumesRequestCondition}
   * and {@link ProducesRequestCondition} for those.
   *
   * @param headers media type expressions with syntax defined in {@link RequestMapping#headers()};
   * if 0, the condition will match to every request
   */
  public HeadersRequestCondition(String... headers) {
    this.expressions = parseExpressions(headers);
  }

  @Nullable
  private static LinkedHashSet<HeaderExpression> parseExpressions(String... headers) {
    LinkedHashSet<HeaderExpression> result = null;
    if (ObjectUtils.isNotEmpty(headers)) {
      for (String header : headers) {
        HeaderExpression expr = new HeaderExpression(header);
        if (HttpHeaders.ACCEPT.equalsIgnoreCase(expr.name)
                || HttpHeaders.CONTENT_TYPE.equalsIgnoreCase(expr.name)) {
          continue;
        }
        if (result == null) {
          result = new LinkedHashSet<>(headers.length);
        }
        result.add(expr);
      }
    }
    return result;
  }

  private HeadersRequestCondition(LinkedHashSet<HeaderExpression> conditions) {
    this.expressions = conditions;
  }

  /**
   * Return the contained request header expressions.
   */
  public Set<NameValueExpression> getExpressions() {
    return expressions == null ? Collections.emptySet() : new LinkedHashSet<>(this.expressions);
  }

  @Override
  protected Collection<HeaderExpression> getContent() {
    return expressions == null ? Collections.emptySet() : expressions;
  }

  @Override
  public boolean isEmpty() {
    return expressions == null;
  }

  @Override
  protected String getToStringInfix() {
    return " && ";
  }

  /**
   * Returns a new instance with the union of the header expressions
   * from "this" and the "other" instance.
   */
  @Override
  public HeadersRequestCondition combine(HeadersRequestCondition other) {
    LinkedHashSet<HeaderExpression> expressions = this.expressions;
    LinkedHashSet<HeaderExpression> otherExpressions = other.expressions;
    if (expressions == null && otherExpressions == null) {
      return this;
    }
    else if (otherExpressions == null) {
      return this;
    }
    else if (expressions == null) {
      return other;
    }
    LinkedHashSet<HeaderExpression> set = new LinkedHashSet<>(expressions);
    set.addAll(otherExpressions);
    return new HeadersRequestCondition(set);
  }

  /**
   * Returns "this" instance if the request matches all expressions;
   * or {@code null} otherwise.
   */
  @Override
  @Nullable
  public HeadersRequestCondition getMatchingCondition(RequestContext request) {
    if (request.isPreFlightRequest()) {
      return PRE_FLIGHT_MATCH;
    }
    LinkedHashSet<HeaderExpression> expressions = this.expressions;
    if (expressions != null) {
      for (HeaderExpression expression : expressions) {
        if (!expression.match(request)) {
          return null;
        }
      }
    }
    return this;
  }

  /**
   * Compare to another condition based on header expressions. A condition
   * is considered to be a more specific match, if it has:
   * <ol>
   * <li>A greater number of expressions.
   * <li>A greater number of non-negated expressions with a concrete value.
   * </ol>
   * <p>It is assumed that both instances have been obtained via
   * {@link #getMatchingCondition(RequestContext)} and each instance
   * contains the matching header expression only or is otherwise empty.
   */
  @Override
  public int compareTo(HeadersRequestCondition other, RequestContext request) {
    LinkedHashSet<HeaderExpression> expressions = this.expressions;
    LinkedHashSet<HeaderExpression> otherExpressions = other.expressions;
    int result = (otherExpressions == null ? 0 : otherExpressions.size()) - (expressions == null ? 0 : expressions.size());
    if (result != 0) {
      return result;
    }
    return getValueMatchCount(otherExpressions) - getValueMatchCount(expressions);
  }

  private int getValueMatchCount(@Nullable LinkedHashSet<HeaderExpression> expressions) {
    if (expressions == null) {
      return 0;
    }
    int count = 0;
    for (HeaderExpression e : expressions) {
      if (e.value != null && !e.negated) {
        count++;
      }
    }
    return count;
  }

  /**
   * Parses and matches a single header expression to a request.
   */
  static class HeaderExpression extends NameValueExpression {

    HeaderExpression(String expression) {
      super(expression);
    }

    @Override
    protected boolean isCaseSensitiveName() {
      return false;
    }

    @Override
    protected boolean matchName(RequestContext request) {
      return request.requestHeaders().getFirst(this.name) != null;
    }

    @Override
    protected boolean matchValue(RequestContext request) {
      return ObjectUtils.nullSafeEquals(this.value, request.requestHeaders().getFirst(this.name));
    }
  }

}
