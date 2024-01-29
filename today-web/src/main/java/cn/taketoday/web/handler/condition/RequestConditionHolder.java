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

import java.util.Collection;
import java.util.Collections;

import cn.taketoday.lang.Nullable;
import cn.taketoday.web.RequestContext;

/**
 * A holder for a {@link RequestCondition} useful when the type of the request
 * condition is not known ahead of time, e.g. custom condition. Since this
 * class is also an implementation of {@code RequestCondition}, effectively it
 * decorates the held request condition and allows it to be combined and compared
 * with other request conditions in a type and null safe way.
 *
 * <p>When two {@code RequestConditionHolder} instances are combined or compared
 * with each other, it is expected the conditions they hold are of the same type.
 * If they are not, a {@link ClassCastException} is raised.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public final class RequestConditionHolder extends AbstractRequestCondition<RequestConditionHolder> {

  @Nullable
  private final RequestCondition<Object> condition;

  /**
   * Create a new holder to wrap the given request condition.
   *
   * @param requestCondition the condition to hold, may be {@code null}
   */
  @SuppressWarnings("unchecked")
  public RequestConditionHolder(@Nullable RequestCondition<?> requestCondition) {
    this.condition = (RequestCondition<Object>) requestCondition;
  }

  /**
   * Return the held request condition, or {@code null} if not holding one.
   */
  @Nullable
  public RequestCondition<?> getCondition() {
    return this.condition;
  }

  @Override
  protected Collection<?> getContent() {
    return (this.condition != null ? Collections.singleton(this.condition) : Collections.emptyList());
  }

  @Override
  protected String getToStringInfix() {
    return " ";
  }

  /**
   * Combine the request conditions held by the two RequestConditionHolder
   * instances after making sure the conditions are of the same type.
   * Or if one holder is empty, the other holder is returned.
   */
  @Override
  public RequestConditionHolder combine(RequestConditionHolder other) {
    if (this.condition == null && other.condition == null) {
      return this;
    }
    else if (this.condition == null) {
      return other;
    }
    else if (other.condition == null) {
      return this;
    }
    else {
      assertEqualConditionTypes(this.condition, other.condition);
      RequestCondition<?> combined = (RequestCondition<?>) condition.combine(other.condition);
      return new RequestConditionHolder(combined);
    }
  }

  /**
   * Ensure the held request conditions are of the same type.
   */
  private void assertEqualConditionTypes(RequestCondition<?> thisCondition, RequestCondition<?> otherCondition) {
    Class<?> clazz = thisCondition.getClass();
    Class<?> otherClazz = otherCondition.getClass();
    if (!clazz.equals(otherClazz)) {
      throw new ClassCastException("Incompatible request conditions: " + clazz + " and " + otherClazz);
    }
  }

  /**
   * Get the matching condition for the held request condition wrap it in a
   * new RequestConditionHolder instance. Or otherwise if this is an empty
   * holder, return the same holder instance.
   */
  @Override
  @Nullable
  public RequestConditionHolder getMatchingCondition(RequestContext request) {
    if (this.condition == null) {
      return this;
    }
    RequestCondition<?> match = (RequestCondition<?>) this.condition.getMatchingCondition(request);
    return (match != null ? new RequestConditionHolder(match) : null);
  }

  /**
   * Compare the request conditions held by the two RequestConditionHolder
   * instances after making sure the conditions are of the same type.
   * Or if one holder is empty, the other holder is preferred.
   */
  @Override
  public int compareTo(RequestConditionHolder other, RequestContext request) {
    if (this.condition == null && other.condition == null) {
      return 0;
    }
    else if (this.condition == null) {
      return 1;
    }
    else if (other.condition == null) {
      return -1;
    }
    else {
      assertEqualConditionTypes(this.condition, other.condition);
      return this.condition.compareTo(other.condition, request);
    }
  }

}
