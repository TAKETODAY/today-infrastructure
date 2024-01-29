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
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cn.taketoday.http.MediaType;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.StringUtils;
import cn.taketoday.web.annotation.RequestMapping;

/**
 * A contract for media type expressions (e.g. "text/plain", "!text/plain") as
 * defined in the {@code @RequestMapping} annotation for "consumes" and
 * "produces" conditions.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see RequestMapping#consumes()
 * @see RequestMapping#produces()
 * @since 4.0
 */
final class MediaTypeExpression implements Comparable<MediaTypeExpression> {

  public final MediaType mediaType;

  public final boolean isNegated;

  MediaTypeExpression(String expression) {
    if (expression.charAt(0) == '!') {
      this.isNegated = true;
      expression = expression.substring(1);
    }
    else {
      this.isNegated = false;
    }
    this.mediaType = MediaType.parseMediaType(expression);
  }

  MediaTypeExpression(MediaType mediaType, boolean negated) {
    this.mediaType = mediaType;
    this.isNegated = negated;
  }

  /**
   * matches a single media type expression to a request's 'Content-Type' header.
   */
  public boolean matchContentType(MediaType contentType) {
    boolean match = mediaType.includes(contentType) && matchParameters(contentType);
    return !isNegated == match;
  }

  /**
   * matches a single media type expression to a request's 'Accept' header.
   */
  public boolean matchAccept(List<MediaType> acceptedMediaTypes) {
    boolean match = matchMediaType(acceptedMediaTypes);
    return !isNegated == match;
  }

  private boolean matchMediaType(List<MediaType> acceptedMediaTypes) {
    for (MediaType acceptedMediaType : acceptedMediaTypes) {
      if (mediaType.isCompatibleWith(acceptedMediaType) && matchParameters(acceptedMediaType)) {
        return true;
      }
    }
    return false;
  }

  private boolean matchParameters(MediaType contentType) {
    for (Map.Entry<String, String> entry : mediaType.getParameters().entrySet()) {
      if (StringUtils.hasText(entry.getValue())) {
        String value = contentType.getParameter(entry.getKey());
        if (StringUtils.hasText(value) && !entry.getValue().equalsIgnoreCase(value)) {
          return false;
        }
      }
    }
    return true;
  }

  @Override
  public int compareTo(MediaTypeExpression other) {
    MediaType mediaType1 = mediaType;
    MediaType mediaType2 = other.mediaType;
    if (mediaType1.isMoreSpecific(mediaType2)) {
      return -1;
    }
    else if (mediaType1.isLessSpecific(mediaType2)) {
      return 1;
    }
    else {
      return 0;
    }
  }

  @Override
  public boolean equals(@Nullable Object other) {
    if (this == other) {
      return true;
    }
    if (other == null || getClass() != other.getClass()) {
      return false;
    }
    MediaTypeExpression otherExpr = (MediaTypeExpression) other;
    return mediaType.equals(otherExpr.mediaType) && isNegated == otherExpr.isNegated;
  }

  @Override
  public int hashCode() {
    return mediaType.hashCode();
  }

  @Override
  public String toString() {
    if (isNegated) {
      return '!' + mediaType.toString();
    }
    return mediaType.toString();
  }

  // static

  @Nullable
  static ArrayList<MediaTypeExpression> parse(String exprHeader, String[] expressions, @Nullable String[] headers) {
    LinkedHashSet<MediaTypeExpression> result = null;
    if (ObjectUtils.isNotEmpty(headers)) {
      for (String header : headers) {
        HeadersRequestCondition.HeaderExpression expr = new HeadersRequestCondition.HeaderExpression(header);
        if (exprHeader.equalsIgnoreCase(expr.name) && expr.value != null) {
          if (result == null) {
            result = new LinkedHashSet<>();
          }
          for (MediaType mediaType : MediaType.parseMediaTypes(expr.value)) {
            result.add(new MediaTypeExpression(mediaType, expr.negated));
          }
        }
      }
    }
    if (ObjectUtils.isNotEmpty(expressions)) {
      for (String produce : expressions) {
        if (result == null) {
          result = new LinkedHashSet<>();
        }
        result.add(new MediaTypeExpression(produce));
      }
    }
    return CollectionUtils.isNotEmpty(result) ? new ArrayList<>(result) : null;
  }

  static Set<MediaType> filterNotNegated(@Nullable ArrayList<MediaTypeExpression> expressions) {
    if (expressions == null || expressions.isEmpty()) {
      return Collections.emptySet();
    }
    LinkedHashSet<MediaType> result = new LinkedHashSet<>();
    for (MediaTypeExpression expression : expressions) {
      if (!expression.isNegated) {
        result.add(expression.mediaType);
      }
    }
    return result;
  }

}
