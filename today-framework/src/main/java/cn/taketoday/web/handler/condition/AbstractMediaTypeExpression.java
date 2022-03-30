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

import cn.taketoday.http.MediaType;
import cn.taketoday.lang.Nullable;
import cn.taketoday.web.annotation.RequestMapping;

/**
 * Supports media type expressions as described in:
 * {@link RequestMapping#consumes()} and {@link RequestMapping#produces()}.
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @since 4.0
 */
abstract class AbstractMediaTypeExpression implements MediaTypeExpression, Comparable<AbstractMediaTypeExpression> {

  private final MediaType mediaType;

  private final boolean isNegated;

  AbstractMediaTypeExpression(String expression) {
    if (expression.startsWith("!")) {
      this.isNegated = true;
      expression = expression.substring(1);
    }
    else {
      this.isNegated = false;
    }
    this.mediaType = MediaType.parseMediaType(expression);
  }

  AbstractMediaTypeExpression(MediaType mediaType, boolean negated) {
    this.mediaType = mediaType;
    this.isNegated = negated;
  }

  @Override
  public MediaType getMediaType() {
    return this.mediaType;
  }

  @Override
  public boolean isNegated() {
    return this.isNegated;
  }

  @Override
  public int compareTo(AbstractMediaTypeExpression other) {
    MediaType mediaType1 = this.getMediaType();
    MediaType mediaType2 = other.getMediaType();
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
    AbstractMediaTypeExpression otherExpr = (AbstractMediaTypeExpression) other;
    return (this.mediaType.equals(otherExpr.mediaType) && this.isNegated == otherExpr.isNegated);
  }

  @Override
  public int hashCode() {
    return this.mediaType.hashCode();
  }

  @Override
  public String toString() {
    if (this.isNegated) {
      return '!' + this.mediaType.toString();
    }
    return this.mediaType.toString();
  }

}
