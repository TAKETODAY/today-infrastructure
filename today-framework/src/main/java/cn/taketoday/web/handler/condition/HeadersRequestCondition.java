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
 * @since 4.0
 */
public final class HeadersRequestCondition extends AbstractRequestCondition<HeadersRequestCondition> {

  private static final HeadersRequestCondition PRE_FLIGHT_MATCH = new HeadersRequestCondition();

  private final Set<HeaderExpression> expressions;

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

  private static Set<HeaderExpression> parseExpressions(String... headers) {
    Set<HeaderExpression> result = null;
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
    return result != null ? result : Collections.emptySet();
  }

  private HeadersRequestCondition(Set<HeaderExpression> conditions) {
    this.expressions = conditions;
  }

  /**
   * Return the contained request header expressions.
   */
  public Set<NameValueExpression<String>> getExpressions() {
    return new LinkedHashSet<>(this.expressions);
  }

  @Override
  protected Collection<HeaderExpression> getContent() {
    return this.expressions;
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
    if (isEmpty() && other.isEmpty()) {
      return this;
    }
    else if (other.isEmpty()) {
      return this;
    }
    else if (isEmpty()) {
      return other;
    }
    LinkedHashSet<HeaderExpression> set = new LinkedHashSet<>(this.expressions);
    set.addAll(other.expressions);
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
    for (HeaderExpression expression : this.expressions) {
      if (!expression.match(request)) {
        return null;
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
    int result = other.expressions.size() - this.expressions.size();
    if (result != 0) {
      return result;
    }
    return (int) (getValueMatchCount(other.expressions) - getValueMatchCount(this.expressions));
  }

  private long getValueMatchCount(Set<HeaderExpression> expressions) {
    long count = 0;
    for (HeaderExpression e : expressions) {
      if (e.getValue() != null && !e.isNegated()) {
        count++;
      }
    }
    return count;
  }

  /**
   * Parses and matches a single header expression to a request.
   */
  static class HeaderExpression extends AbstractNameValueExpression<String> {

    HeaderExpression(String expression) {
      super(expression);
    }

    @Override
    protected boolean isCaseSensitiveName() {
      return false;
    }

    @Override
    protected String parseValue(String valueExpression) {
      return valueExpression;
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
