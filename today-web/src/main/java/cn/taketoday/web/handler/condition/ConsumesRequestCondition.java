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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.InvalidMediaTypeException;
import cn.taketoday.http.MediaType;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.StringUtils;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.annotation.RequestMapping;

/**
 * A logical disjunction (' || ') request condition to match a request's
 * 'Content-Type' header to a list of media type expressions. Two kinds of
 * media type expressions are supported, which are described in
 * {@link RequestMapping#consumes()} and {@link RequestMapping#headers()}
 * where the header name is 'Content-Type'. Regardless of which syntax is
 * used, the semantics are the same.
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public final class ConsumesRequestCondition extends AbstractRequestCondition<ConsumesRequestCondition> {

  private static final ConsumesRequestCondition EMPTY_CONDITION = new ConsumesRequestCondition();

  @Nullable
  private final ArrayList<MediaTypeExpression> expressions;

  private boolean bodyRequired = true;

  @Nullable
  private Set<MediaType> consumableMediaTypes;

  /**
   * Creates a new instance from 0 or more "consumes" expressions.
   *
   * @param consumes expressions with the syntax described in
   * {@link RequestMapping#consumes()}; if 0 expressions are provided,
   * the condition will match to every request
   */
  public ConsumesRequestCondition(String... consumes) {
    this(consumes, null);
  }

  /**
   * Creates a new instance with "consumes" and "header" expressions.
   * "Header" expressions where the header name is not 'Content-Type' or have
   * no header value defined are ignored. If 0 expressions are provided in
   * total, the condition will match to every request
   *
   * @param consumes as described in {@link RequestMapping#consumes()}
   * @param headers as described in {@link RequestMapping#headers()}
   */
  public ConsumesRequestCondition(String[] consumes, @Nullable String[] headers) {
    var expressions = MediaTypeExpression.parse(HttpHeaders.CONTENT_TYPE, consumes, headers);
    if (expressions != null) {
      Collections.sort(expressions);
    }
    this.expressions = expressions;
  }

  /**
   * Private constructor for internal when creating matching conditions.
   * Note the expressions List is neither sorted nor deep copied.
   */
  private ConsumesRequestCondition(@Nullable ArrayList<MediaTypeExpression> expressions) {
    this.expressions = expressions;
  }

  /**
   * Return the contained MediaType expressions.
   */
  public Set<MediaTypeExpression> getExpressions() {
    return expressions == null ? Collections.emptySet() : new LinkedHashSet<>(expressions);
  }

  /**
   * Returns the media types for this condition excluding negated expressions.
   */
  public Set<MediaType> getConsumableMediaTypes() {
    Set<MediaType> consumableMediaTypes = this.consumableMediaTypes;
    if (consumableMediaTypes == null) {
      consumableMediaTypes = MediaTypeExpression.filterNotNegated(expressions);
      this.consumableMediaTypes = consumableMediaTypes;
    }
    return consumableMediaTypes;
  }

  /**
   * Whether the condition has any media type expressions.
   */
  @Override
  public boolean isEmpty() {
    return expressions == null;
  }

  @Override
  protected Collection<MediaTypeExpression> getContent() {
    return expressions == null ? Collections.emptySet() : expressions;
  }

  @Override
  protected String getToStringInfix() {
    return " || ";
  }

  /**
   * Whether this condition should expect requests to have a body.
   * <p>By default this is set to {@code true} in which case it is assumed a
   * request body is required and this condition matches to the "Content-Type"
   * header or falls back on "Content-Type: application/octet-stream".
   * <p>If set to {@code false}, and the request does not have a body, then this
   * condition matches automatically, i.e. without checking expressions.
   *
   * @param bodyRequired whether requests are expected to have a body
   */
  public void setBodyRequired(boolean bodyRequired) {
    this.bodyRequired = bodyRequired;
  }

  /**
   * Return the setting for {@link #setBodyRequired(boolean)}.
   */
  public boolean isBodyRequired() {
    return bodyRequired;
  }

  /**
   * Returns the "other" instance if it has any expressions; returns "this"
   * instance otherwise. Practically that means a method-level "consumes"
   * overrides a type-level "consumes" condition.
   */
  @Override
  public ConsumesRequestCondition combine(ConsumesRequestCondition other) {
    return other.expressions != null ? other : this;
  }

  /**
   * Checks if any of the contained media type expressions match the given
   * request 'Content-Type' header and returns an instance that is guaranteed
   * to contain matching expressions only. The match is performed via
   * {@link MediaType#includes(MediaType)}.
   *
   * @param request the current request
   * @return the same instance if the condition contains no expressions;
   * or a new condition with matching expressions only;
   * or {@code null} if no expressions match
   */
  @Override
  @Nullable
  public ConsumesRequestCondition getMatchingCondition(RequestContext request) {
    if (request.isPreFlightRequest()) {
      return EMPTY_CONDITION;
    }
    if (isEmpty()) {
      return this;
    }
    if (!hasBody(request) && !bodyRequired) {
      return EMPTY_CONDITION;
    }

    // Common media types are cached at the level of MimeTypeUtils

    MediaType contentType;
    try {
      String contentType1 = request.getContentType();
      contentType = StringUtils.isNotEmpty(contentType1)
                    ? MediaType.parseMediaType(contentType1)
                    : MediaType.APPLICATION_OCTET_STREAM;
    }
    catch (InvalidMediaTypeException ex) {
      return null;
    }

    ArrayList<MediaTypeExpression> result = getMatchingExpressions(contentType);
    if (result != null) {
      return new ConsumesRequestCondition(result);
    }
    return null;
  }

  private static boolean hasBody(RequestContext request) {
    String transferEncoding = request.requestHeaders().getFirst(HttpHeaders.TRANSFER_ENCODING);
    return StringUtils.hasText(transferEncoding) || (request.getContentLength() > 0L);
  }

  @Nullable
  private ArrayList<MediaTypeExpression> getMatchingExpressions(MediaType contentType) {
    ArrayList<MediaTypeExpression> expressions = this.expressions;
    if (expressions == null) {
      return null;
    }

    ArrayList<MediaTypeExpression> result = null;
    for (MediaTypeExpression expression : expressions) {
      if (expression.matchContentType(contentType)) {
        if (result == null) {
          result = new ArrayList<>(expressions.size());
        }
        result.add(expression);
      }
    }
    return result;
  }

  /**
   * Returns:
   * <ul>
   * <li>0 if the two conditions have the same number of expressions
   * <li>Less than 0 if "this" has more or more specific media type expressions
   * <li>Greater than 0 if "other" has more or more specific media type expressions
   * </ul>
   * <p>It is assumed that both instances have been obtained via
   * {@link #getMatchingCondition(RequestContext)} and each instance contains
   * the matching consumable media type expression only or is otherwise empty.
   */
  @Override
  public int compareTo(ConsumesRequestCondition other, RequestContext request) {
    ArrayList<MediaTypeExpression> expressions = this.expressions;
    ArrayList<MediaTypeExpression> otherExpressions = other.expressions;
    if (expressions == null && otherExpressions == null) {
      return 0;
    }
    else if (expressions == null) {
      return 1;
    }
    else if (otherExpressions == null) {
      return -1;
    }
    else {
      return expressions.get(0).compareTo(otherExpressions.get(0));
    }
  }

}
