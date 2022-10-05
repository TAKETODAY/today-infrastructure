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

import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.web.RequestContext;

/**
 * Supports "name=value" style expressions as described in:
 * {@link cn.taketoday.web.annotation.RequestMapping#params()} and
 * {@link cn.taketoday.web.annotation.RequestMapping#headers()}.
 *
 * @param <T> the value type
 * @author Rossen Stoyanchev
 * @author Arjen Poutsma
 * @since 4.0
 */
abstract class AbstractNameValueExpression<T> implements NameValueExpression<T> {

  public final String name;

  @Nullable
  public final T value;

  public final boolean isNegated;

  AbstractNameValueExpression(String expression) {
    int separator = expression.indexOf('=');
    if (separator == -1) {
      this.isNegated = expression.startsWith("!");
      this.name = isNegated ? expression.substring(1) : expression;
      this.value = null;
    }
    else {
      this.isNegated = (separator > 0) && (expression.charAt(separator - 1) == '!');
      this.name = isNegated ? expression.substring(0, separator - 1) : expression.substring(0, separator);
      this.value = parseValue(expression.substring(separator + 1));
    }
  }

  @Override
  public String getName() {
    return this.name;
  }

  @Override
  @Nullable
  public T getValue() {
    return this.value;
  }

  @Override
  public boolean isNegated() {
    return this.isNegated;
  }

  public final boolean match(RequestContext request) {
    boolean isMatch;
    if (this.value != null) {
      isMatch = matchValue(request);
    }
    else {
      isMatch = matchName(request);
    }
    return this.isNegated != isMatch;
  }

  protected abstract boolean isCaseSensitiveName();

  protected abstract T parseValue(String valueExpression);

  protected abstract boolean matchName(RequestContext request);

  protected abstract boolean matchValue(RequestContext request);

  @Override
  public boolean equals(@Nullable Object other) {
    if (this == other) {
      return true;
    }
    if (other == null || getClass() != other.getClass()) {
      return false;
    }
    AbstractNameValueExpression<?> that = (AbstractNameValueExpression<?>) other;
    return ((isCaseSensitiveName() ? this.name.equals(that.name) : this.name.equalsIgnoreCase(that.name))
            && ObjectUtils.nullSafeEquals(this.value, that.value) && this.isNegated == that.isNegated);
  }

  @Override
  public int hashCode() {
    int result = (isCaseSensitiveName() ? this.name.hashCode() : this.name.toLowerCase().hashCode());
    result = 31 * result + (this.value != null ? this.value.hashCode() : 0);
    result = 31 * result + (this.isNegated ? 1 : 0);
    return result;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    if (this.value != null) {
      builder.append(this.name);
      if (this.isNegated) {
        builder.append('!');
      }
      builder.append('=');
      builder.append(this.value);
    }
    else {
      if (this.isNegated) {
        builder.append('!');
      }
      builder.append(this.name);
    }
    return builder.toString();
  }

}
