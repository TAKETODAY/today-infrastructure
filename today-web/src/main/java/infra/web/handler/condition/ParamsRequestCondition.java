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

package infra.web.handler.condition;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import infra.lang.Nullable;
import infra.util.ObjectUtils;
import infra.web.RequestContext;
import infra.web.annotation.RequestMapping;

/**
 * A logical conjunction ({@code ' && '}) request condition that matches a request against
 * a set parameter expressions with syntax defined in {@link RequestMapping#params()}.
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public final class ParamsRequestCondition extends AbstractRequestCondition<ParamsRequestCondition> {

  private final Set<ParamExpression> expressions;

  /**
   * Create a new instance from the given param expressions.
   *
   * @param params expressions with syntax defined in {@link RequestMapping#params()};
   * if 0, the condition will match to every request.
   */
  public ParamsRequestCondition(String... params) {
    this.expressions = parseExpressions(params);
  }

  private static Set<ParamExpression> parseExpressions(String... params) {
    if (ObjectUtils.isEmpty(params)) {
      return Collections.emptySet();
    }

    LinkedHashSet<ParamExpression> expressions = new LinkedHashSet<>(params.length);
    for (String param : params) {
      expressions.add(new ParamExpression(param));
    }
    return expressions;
  }

  private ParamsRequestCondition(Set<ParamExpression> conditions) {
    this.expressions = conditions;
  }

  /**
   * Return the contained request parameter expressions.
   */
  public Set<NameValueExpression> getExpressions() {
    return new LinkedHashSet<>(this.expressions);
  }

  @Override
  protected Collection<ParamExpression> getContent() {
    return this.expressions;
  }

  @Override
  protected String getToStringInfix() {
    return " && ";
  }

  /**
   * Returns a new instance with the union of the param expressions
   * from "this" and the "other" instance.
   */
  @Override
  public ParamsRequestCondition combine(ParamsRequestCondition other) {
    if (other.isEmpty()) {
      return this;
    }
    else if (isEmpty()) {
      return other;
    }
    LinkedHashSet<ParamExpression> set = new LinkedHashSet<>(this.expressions);
    set.addAll(other.expressions);
    return new ParamsRequestCondition(set);
  }

  /**
   * Returns "this" instance if the request matches all param expressions;
   * or {@code null} otherwise.
   */
  @Override
  @Nullable
  public ParamsRequestCondition getMatchingCondition(RequestContext request) {
    for (ParamExpression expression : this.expressions) {
      if (!expression.match(request)) {
        return null;
      }
    }
    return this;
  }

  /**
   * Compare to another condition based on parameter expressions. A condition
   * is considered to be a more specific match, if it has:
   * <ol>
   * <li>A greater number of expressions.
   * <li>A greater number of non-negated expressions with a concrete value.
   * </ol>
   * <p>It is assumed that both instances have been obtained via
   * {@link #getMatchingCondition(RequestContext)} and each instance
   * contains the matching parameter expressions only or is otherwise empty.
   */
  @Override
  public int compareTo(ParamsRequestCondition other, RequestContext request) {
    int result = other.expressions.size() - this.expressions.size();
    if (result != 0) {
      return result;
    }
    return getValueMatchCount(other.expressions) - getValueMatchCount(this.expressions);
  }

  private static int getValueMatchCount(Set<ParamExpression> expressions) {
    int count = 0;
    for (ParamExpression e : expressions) {
      if (e.value != null && !e.negated) {
        count++;
      }
    }
    return count;
  }

  /**
   * Parses and matches a single param expression to a request.
   */
  static class ParamExpression extends NameValueExpression {

    final HashSet<String> namesToMatch;

    ParamExpression(String expression) {
      this(expression, new HashSet<>());
    }

    ParamExpression(String expression, HashSet<String> namesToMatch) {
      super(expression);
      this.namesToMatch = namesToMatch;
      this.namesToMatch.add(name);
    }

    @Override
    protected boolean isCaseSensitiveName() {
      return true;
    }

    @Override
    protected boolean matchName(RequestContext request) {
      var parameters = request.getParameters();
      for (String current : namesToMatch) {
        if (parameters.get(current) != null) {
          return true;
        }
      }
      return parameters.containsKey(this.name);
    }

    @Override
    protected boolean matchValue(RequestContext request) {
      return ObjectUtils.nullSafeEquals(this.value, request.getParameter(this.name));
    }
  }

}
