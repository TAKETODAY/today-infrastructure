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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import infra.http.HttpHeaders;
import infra.http.MediaType;
import infra.lang.Nullable;
import infra.lang.Unmodifiable;
import infra.util.MimeType;
import infra.util.MimeTypeUtils;
import infra.web.HttpMediaTypeException;
import infra.web.HttpMediaTypeNotAcceptableException;
import infra.web.RequestContext;
import infra.web.accept.ContentNegotiationManager;
import infra.web.annotation.RequestMapping;

/**
 * A logical disjunction (' || ') request condition to match a request's 'Accept' header
 * to a list of media type expressions. Two kinds of media type expressions are
 * supported, which are described in {@link RequestMapping#produces()} and
 * {@link RequestMapping#headers()} where the header name is 'Accept'.
 * Regardless of which syntax is used, the semantics are the same.
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public final class ProducesRequestCondition extends AbstractRequestCondition<ProducesRequestCondition> {

  private static final String MEDIA_TYPES_ATTRIBUTE = ProducesRequestCondition.class.getName() + ".MEDIA_TYPES";

  private static final ContentNegotiationManager DEFAULT_CONTENT_NEGOTIATION_MANAGER = new ContentNegotiationManager();

  private static final ProducesRequestCondition EMPTY_CONDITION = new ProducesRequestCondition();

  private static final List<MediaTypeExpression> MEDIA_TYPE_ALL_LIST =
          Collections.singletonList(new MediaTypeExpression(MediaType.ALL_VALUE));

  @Nullable
  private final ArrayList<MediaTypeExpression> expressions;

  private final ContentNegotiationManager contentNegotiationManager;

  @Nullable
  @Unmodifiable
  private Collection<MediaType> producibleMediaTypes;

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
  public ProducesRequestCondition(String[] produces, @Nullable String[] headers, @Nullable ContentNegotiationManager manager) {
    var expressions = MediaTypeExpression.parse(HttpHeaders.ACCEPT, produces, headers);
    if (expressions != null) {
      Collections.sort(expressions);
    }
    this.expressions = expressions;
    this.contentNegotiationManager = manager != null ? manager : DEFAULT_CONTENT_NEGOTIATION_MANAGER;
  }

  /**
   * Private constructor for internal use to create matching conditions.
   * Note the expressions List is neither sorted nor deep copied.
   */
  private ProducesRequestCondition(ArrayList<MediaTypeExpression> expressions, ProducesRequestCondition other) {
    this.expressions = expressions;
    this.contentNegotiationManager = other.contentNegotiationManager;
    this.producibleMediaTypes = other.getProducibleMediaTypes();
  }

  /**
   * Return the contained "produces" expressions.
   */
  @Unmodifiable
  public Collection<MediaTypeExpression> getExpressions() {
    return expressions == null ? Collections.emptySet() : Collections.unmodifiableList(expressions);
  }

  /**
   * Return the contained producible media types excluding negated expressions.
   */
  @Unmodifiable
  public Collection<MediaType> getProducibleMediaTypes() {
    Collection<MediaType> producibleMediaTypes = this.producibleMediaTypes;
    if (producibleMediaTypes == null) {
      producibleMediaTypes = MediaTypeExpression.filterNotNegated(expressions);
      this.producibleMediaTypes = producibleMediaTypes;
    }
    return producibleMediaTypes;
  }

  /**
   * Whether the condition has any media type expressions.
   */
  @Override
  public boolean isEmpty() {
    return expressions == null;
  }

  @Override
  protected List<MediaTypeExpression> getContent() {
    return expressions == null ? Collections.emptyList() : expressions;
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
    return other.expressions != null ? other : this;
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
    if (expressions == null) {
      return this;
    }
    List<MediaType> acceptedMediaTypes;
    try {
      acceptedMediaTypes = getAcceptedMediaTypes(request);
    }
    catch (HttpMediaTypeException ex) {
      return null;
    }
    ArrayList<MediaTypeExpression> result = getMatchingExpressions(acceptedMediaTypes);
    if (result != null) {
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
  private ArrayList<MediaTypeExpression> getMatchingExpressions(List<MediaType> acceptedMediaTypes) {
    ArrayList<MediaTypeExpression> expressions = this.expressions;
    if (expressions == null) {
      return null;
    }
    ArrayList<MediaTypeExpression> result = null;
    for (MediaTypeExpression expression : expressions) {
      if (expression.matchAccept(acceptedMediaTypes)) {
        if (result == null) {
          result = new ArrayList<>(expressions.size());
        }
        result.add(expression);
      }
    }
    return result; // null or not empty list
  }

  /**
   * Compares this and another "produces" condition as follows:
   * <ol>
   * <li>Sort 'Accept' header media types by quality value via
   * {@link MimeTypeUtils#sortBySpecificity(List)}
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
  private List<MediaType> getAcceptedMediaTypes(RequestContext request) throws HttpMediaTypeNotAcceptableException {
    List<MediaType> result = (List<MediaType>) request.getAttribute(MEDIA_TYPES_ATTRIBUTE);
    if (result == null) {
      result = contentNegotiationManager.resolveMediaTypes(request);
      request.setAttribute(MEDIA_TYPES_ATTRIBUTE, result);
    }
    return result;
  }

  private int indexOfEqualMediaType(MediaType mediaType) {
    List<MediaTypeExpression> expressionsToCompare = getExpressionsToCompare();
    for (int i = 0; i < expressionsToCompare.size(); i++) {
      MediaType currentMediaType = expressionsToCompare.get(i).mediaType;
      if (mediaType.getType().equalsIgnoreCase(currentMediaType.getType())
              && mediaType.getSubtype().equalsIgnoreCase(currentMediaType.getSubtype())) {
        return i;
      }
    }
    return -1;
  }

  private int indexOfIncludedMediaType(MediaType mediaType) {
    List<MediaTypeExpression> expressionsToCompare = getExpressionsToCompare();
    for (int i = 0; i < expressionsToCompare.size(); i++) {
      if (mediaType.includes(expressionsToCompare.get(i).mediaType)) {
        return i;
      }
    }
    return -1;
  }

  private int compareMatchingMediaTypes(ProducesRequestCondition condition1,
          int index1, ProducesRequestCondition condition2, int index2) {

    int result = 0;
    if (index1 != index2) {
      result = index2 - index1;
    }
    else if (index1 != -1) {
      MediaTypeExpression expr1 = condition1.getExpressionsToCompare().get(index1);
      MediaTypeExpression expr2 = condition2.getExpressionsToCompare().get(index2);
      result = expr1.compareTo(expr2);
      if (result == 0) {
        result = expr1.mediaType.compareTo(expr2.mediaType);
      }
    }
    return result;
  }

  /**
   * Return the contained "produces" expressions or if that's empty, a list
   * with a {@value MediaType#ALL_VALUE} expression.
   */
  private List<MediaTypeExpression> getExpressionsToCompare() {
    return expressions == null ? MEDIA_TYPE_ALL_LIST : expressions;
  }

}
