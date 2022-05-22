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

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import cn.taketoday.http.MediaType;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.util.MimeType;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.web.HttpMediaTypeException;
import cn.taketoday.web.HttpMediaTypeNotAcceptableException;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.accept.ContentNegotiationManager;
import cn.taketoday.web.annotation.RequestMapping;
import cn.taketoday.web.handler.condition.HeadersRequestCondition.HeaderExpression;

/**
 * A logical disjunction (' || ') request condition to match a request's 'Accept' header
 * to a list of media type expressions. Two kinds of media type expressions are
 * supported, which are described in {@link RequestMapping#produces()} and
 * {@link RequestMapping#headers()} where the header name is 'Accept'.
 * Regardless of which syntax is used, the semantics are the same.
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public final class ProducesRequestCondition extends AbstractRequestCondition<ProducesRequestCondition> {

  private static final ContentNegotiationManager DEFAULT_CONTENT_NEGOTIATION_MANAGER =
          new ContentNegotiationManager();

  private static final ProducesRequestCondition EMPTY_CONDITION = new ProducesRequestCondition();

  private static final List<ProduceMediaTypeExpression> MEDIA_TYPE_ALL_LIST =
          Collections.singletonList(new ProduceMediaTypeExpression(MediaType.ALL_VALUE));

  private static final String MEDIA_TYPES_ATTRIBUTE = ProducesRequestCondition.class.getName() + ".MEDIA_TYPES";

  private final List<ProduceMediaTypeExpression> expressions;

  private final ContentNegotiationManager contentNegotiationManager;

  /**
   * Creates a new instance from "produces" expressions. If 0 expressions
   * are provided in total, this condition will match to any request.
   *
   * @param produces expressions with syntax defined by {@link RequestMapping#produces()}
   */
  public ProducesRequestCondition(String... produces) {
    this(produces, null, null);
  }

  /**
   * Creates a new instance with "produces" and "header" expressions. "Header"
   * expressions where the header name is not 'Accept' or have no header value
   * defined are ignored. If 0 expressions are provided in total, this condition
   * will match to any request.
   *
   * @param produces expressions with syntax defined by {@link RequestMapping#produces()}
   * @param headers expressions with syntax defined by {@link RequestMapping#headers()}
   */
  public ProducesRequestCondition(String[] produces, @Nullable String[] headers) {
    this(produces, headers, null);
  }

  /**
   * Same as {@link #ProducesRequestCondition(String[], String[])} but also
   * accepting a {@link ContentNegotiationManager}.
   *
   * @param produces expressions with syntax defined by {@link RequestMapping#produces()}
   * @param headers expressions with syntax defined by {@link RequestMapping#headers()}
   * @param manager used to determine requested media types
   */
  public ProducesRequestCondition(String[] produces, @Nullable String[] headers,
          @Nullable ContentNegotiationManager manager) {

    this.expressions = parseExpressions(produces, headers);
    if (this.expressions.size() > 1) {
      Collections.sort(this.expressions);
    }
    this.contentNegotiationManager = manager != null ? manager : DEFAULT_CONTENT_NEGOTIATION_MANAGER;
  }

  private List<ProduceMediaTypeExpression> parseExpressions(String[] produces, @Nullable String[] headers) {
    Set<ProduceMediaTypeExpression> result = null;
    if (ObjectUtils.isNotEmpty(headers)) {
      for (String header : headers) {
        HeaderExpression expr = new HeaderExpression(header);
        if ("Accept".equalsIgnoreCase(expr.name) && expr.value != null) {
          for (MediaType mediaType : MediaType.parseMediaTypes(expr.value)) {
            result = (result != null ? result : new LinkedHashSet<>());
            result.add(new ProduceMediaTypeExpression(mediaType, expr.isNegated));
          }
        }
      }
    }
    if (ObjectUtils.isNotEmpty(produces)) {
      for (String produce : produces) {
        result = (result != null ? result : new LinkedHashSet<>());
        result.add(new ProduceMediaTypeExpression(produce));
      }
    }
    return (result != null ? new ArrayList<>(result) : Collections.emptyList());
  }

  /**
   * Private constructor for internal use to create matching conditions.
   * Note the expressions List is neither sorted nor deep copied.
   */
  private ProducesRequestCondition(List<ProduceMediaTypeExpression> expressions, ProducesRequestCondition other) {
    this.expressions = expressions;
    this.contentNegotiationManager = other.contentNegotiationManager;
  }

  /**
   * Return the contained "produces" expressions.
   */
  public Set<MediaTypeExpression> getExpressions() {
    return new LinkedHashSet<>(this.expressions);
  }

  /**
   * Return the contained producible media types excluding negated expressions.
   */
  public Set<MediaType> getProducibleMediaTypes() {
    Set<MediaType> result = new LinkedHashSet<>();
    for (ProduceMediaTypeExpression expression : this.expressions) {
      if (!expression.isNegated()) {
        result.add(expression.getMediaType());
      }
    }
    return result;
  }

  /**
   * Whether the condition has any media type expressions.
   */
  @Override
  public boolean isEmpty() {
    return this.expressions.isEmpty();
  }

  @Override
  protected List<ProduceMediaTypeExpression> getContent() {
    return this.expressions;
  }

  @Override
  protected String getToStringInfix() {
    return " || ";
  }

  /**
   * Returns the "other" instance if it has any expressions; returns "this"
   * instance otherwise. Practically that means a method-level "produces"
   * overrides a type-level "produces" condition.
   */
  @Override
  public ProducesRequestCondition combine(ProducesRequestCondition other) {
    return (!other.expressions.isEmpty() ? other : this);
  }

  /**
   * Checks if any of the contained media type expressions match the given
   * request 'Content-Type' header and returns an instance that is guaranteed
   * to contain matching expressions only. The match is performed via
   * {@link MediaType#isCompatibleWith(MediaType)}.
   *
   * @param request the current request
   * @return the same instance if there are no expressions;
   * or a new condition with matching expressions;
   * or {@code null} if no expressions match.
   */
  @Override
  @Nullable
  public ProducesRequestCondition getMatchingCondition(RequestContext request) {
    if (request.isPreFlightRequest()) {
      return EMPTY_CONDITION;
    }
    if (isEmpty()) {
      return this;
    }
    List<MediaType> acceptedMediaTypes;
    try {
      acceptedMediaTypes = getAcceptedMediaTypes(request);
    }
    catch (HttpMediaTypeException ex) {
      return null;
    }
    List<ProduceMediaTypeExpression> result = getMatchingExpressions(acceptedMediaTypes);
    if (CollectionUtils.isNotEmpty(result)) {
      return new ProducesRequestCondition(result, this);
    }
    else if (MediaType.ALL.isPresentIn(acceptedMediaTypes)) {
      return EMPTY_CONDITION;
    }
    else {
      return null;
    }
  }

  @Nullable
  private List<ProduceMediaTypeExpression> getMatchingExpressions(List<MediaType> acceptedMediaTypes) {
    List<ProduceMediaTypeExpression> result = null;
    for (ProduceMediaTypeExpression expression : this.expressions) {
      if (expression.match(acceptedMediaTypes)) {
        result = result != null ? result : new ArrayList<>();
        result.add(expression);
      }
    }
    return result;
  }

  /**
   * Compares this and another "produces" condition as follows:
   * <ol>
   * <li>Sort 'Accept' header media types by quality value via
   * {@link cn.taketoday.util.MimeTypeUtils#sortBySpecificity(List)}
   * and iterate the list.
   * <li>Get the first index of matching media types in each "produces"
   * condition first matching with {@link MediaType#equals(Object)} and
   * then with {@link MediaType#includes(MediaType)}.
   * <li>If a lower index is found, the condition at that index wins.
   * <li>If both indexes are equal, the media types at the index are
   * compared further with {@link MediaType#isMoreSpecific(MimeType)}.
   * </ol>
   * <p>It is assumed that both instances have been obtained via
   * {@link #getMatchingCondition(RequestContext)} and each instance
   * contains the matching producible media type expression only or
   * is otherwise empty.
   */
  @Override
  public int compareTo(ProducesRequestCondition other, RequestContext request) {
    try {
      List<MediaType> acceptedMediaTypes = getAcceptedMediaTypes(request);
      for (MediaType acceptedMediaType : acceptedMediaTypes) {
        int thisIndex = this.indexOfEqualMediaType(acceptedMediaType);
        int otherIndex = other.indexOfEqualMediaType(acceptedMediaType);
        int result = compareMatchingMediaTypes(this, thisIndex, other, otherIndex);
        if (result != 0) {
          return result;
        }
        thisIndex = this.indexOfIncludedMediaType(acceptedMediaType);
        otherIndex = other.indexOfIncludedMediaType(acceptedMediaType);
        result = compareMatchingMediaTypes(this, thisIndex, other, otherIndex);
        if (result != 0) {
          return result;
        }
      }
      return 0;
    }
    catch (HttpMediaTypeNotAcceptableException ex) {
      // should never happen
      throw new IllegalStateException("Cannot compare without having any requested media types", ex);
    }
  }

  @SuppressWarnings("unchecked")
  private List<MediaType> getAcceptedMediaTypes(RequestContext request)
          throws HttpMediaTypeNotAcceptableException {

    List<MediaType> result = (List<MediaType>) request.getAttribute(MEDIA_TYPES_ATTRIBUTE);
    if (result == null) {
      result = this.contentNegotiationManager.resolveMediaTypes(request);
      request.setAttribute(MEDIA_TYPES_ATTRIBUTE, result);
    }
    return result;
  }

  private int indexOfEqualMediaType(MediaType mediaType) {
    for (int i = 0; i < getExpressionsToCompare().size(); i++) {
      MediaType currentMediaType = getExpressionsToCompare().get(i).getMediaType();
      if (mediaType.getType().equalsIgnoreCase(currentMediaType.getType()) &&
              mediaType.getSubtype().equalsIgnoreCase(currentMediaType.getSubtype())) {
        return i;
      }
    }
    return -1;
  }

  private int indexOfIncludedMediaType(MediaType mediaType) {
    for (int i = 0; i < getExpressionsToCompare().size(); i++) {
      if (mediaType.includes(getExpressionsToCompare().get(i).getMediaType())) {
        return i;
      }
    }
    return -1;
  }

  private int compareMatchingMediaTypes(ProducesRequestCondition condition1, int index1,
          ProducesRequestCondition condition2, int index2) {

    int result = 0;
    if (index1 != index2) {
      result = index2 - index1;
    }
    else if (index1 != -1) {
      ProduceMediaTypeExpression expr1 = condition1.getExpressionsToCompare().get(index1);
      ProduceMediaTypeExpression expr2 = condition2.getExpressionsToCompare().get(index2);
      result = expr1.compareTo(expr2);
      result = (result != 0) ? result : expr1.getMediaType().compareTo(expr2.getMediaType());
    }
    return result;
  }

  /**
   * Return the contained "produces" expressions or if that's empty, a list
   * with a {@value MediaType#ALL_VALUE} expression.
   */
  private List<ProduceMediaTypeExpression> getExpressionsToCompare() {
    return (this.expressions.isEmpty() ? MEDIA_TYPE_ALL_LIST : this.expressions);
  }

  /**
   * Use this to clear {@link #MEDIA_TYPES_ATTRIBUTE} that contains the parsed,
   * requested media types.
   *
   * @param request the current request
   */
  public static void clearMediaTypesAttribute(RequestContext request) {
    request.removeAttribute(MEDIA_TYPES_ATTRIBUTE);
  }

  /**
   * Parses and matches a single media type expression to a request's 'Accept' header.
   */
  static class ProduceMediaTypeExpression extends AbstractMediaTypeExpression {

    ProduceMediaTypeExpression(MediaType mediaType, boolean negated) {
      super(mediaType, negated);
    }

    ProduceMediaTypeExpression(String expression) {
      super(expression);
    }

    public final boolean match(List<MediaType> acceptedMediaTypes) {
      boolean match = matchMediaType(acceptedMediaTypes);
      return !isNegated() == match;
    }

    private boolean matchMediaType(List<MediaType> acceptedMediaTypes) {
      for (MediaType acceptedMediaType : acceptedMediaTypes) {
        if (getMediaType().isCompatibleWith(acceptedMediaType) && matchParameters(acceptedMediaType)) {
          return true;
        }
      }
      return false;
    }

  }

}
