/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.web.handler.condition;

import org.jspecify.annotations.Nullable;

import java.util.Locale;

import infra.util.ObjectUtils;
import infra.web.RequestContext;
import infra.web.annotation.RequestMapping;

/**
 * A contract for {@code "name!=value"} style expression used to specify request
 * parameters and request header conditions in {@code @RequestMapping}.
 * <p>
 * Supports "name=value" style expressions as described in:
 * {@link infra.web.annotation.RequestMapping#params()} and
 * {@link infra.web.annotation.RequestMapping#headers()}.
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
    int result = (isCaseSensitiveName() ? this.name.hashCode() : this.name.toLowerCase(Locale.ROOT).hashCode());
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
