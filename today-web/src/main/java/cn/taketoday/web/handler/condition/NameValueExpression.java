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

import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.annotation.RequestMapping;

/**
 * A contract for {@code "name!=value"} style expression used to specify request
 * parameters and request header conditions in {@code @RequestMapping}.
 * <p>
 * Supports "name=value" style expressions as described in:
 * {@link cn.taketoday.web.annotation.RequestMapping#params()} and
 * {@link cn.taketoday.web.annotation.RequestMapping#headers()}.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see RequestMapping#params()
 * @see RequestMapping#headers()
 * @since 4.0
 */
public abstract class NameValueExpression {

  public final String name;

  @Nullable
  public final String value;

  // is negated !
  public final boolean negated;

  NameValueExpression(String expression) {
    int separator = expression.indexOf('=');
    if (separator == -1) {
      this.negated = expression.charAt(0) == '!';
      this.name = negated ? expression.substring(1) : expression;
      this.value = null;
    }
    else {
      this.negated = (separator > 0) && (expression.charAt(separator - 1) == '!');
      this.name = negated ? expression.substring(0, separator - 1) : expression.substring(0, separator);
      this.value = expression.substring(separator + 1);
    }
  }

  public final boolean match(RequestContext request) {
    boolean isMatch;
    if (this.value != null) {
      isMatch = matchValue(request);
    }
    else {
      isMatch = matchName(request);
    }
    return this.negated != isMatch;
  }

  protected abstract boolean isCaseSensitiveName();

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
    NameValueExpression that = (NameValueExpression) other;
    return ((isCaseSensitiveName() ? this.name.equals(that.name) : this.name.equalsIgnoreCase(that.name))
            && ObjectUtils.nullSafeEquals(this.value, that.value) && this.negated == that.negated);
  }

  @Override
  public int hashCode() {
    int result = (isCaseSensitiveName() ? this.name.hashCode() : this.name.toLowerCase().hashCode());
    result = 31 * result + (this.value != null ? this.value.hashCode() : 0);
    result = 31 * result + (this.negated ? 1 : 0);
    return result;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    if (this.value != null) {
      builder.append(this.name);
      if (this.negated) {
        builder.append('!');
      }
      builder.append('=');
      builder.append(this.value);
    }
    else {
      if (this.negated) {
        builder.append('!');
      }
      builder.append(this.name);
    }
    return builder.toString();
  }

}
