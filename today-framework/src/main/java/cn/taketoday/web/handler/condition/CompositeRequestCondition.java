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
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.web.RequestContext;

/**
 * Implements the {@link RequestCondition} contract by delegating to multiple
 * {@code RequestCondition} types and using a logical conjunction ({@code ' && '}) to
 * ensure all conditions match a given request.
 *
 * <p>When {@code CompositeRequestCondition} instances are combined or compared
 * they are expected to (a) contain the same number of conditions and (b) that
 * conditions in the respective index are of the same type. It is acceptable to
 * provide {@code null} conditions or no conditions at all to the constructor.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class CompositeRequestCondition extends AbstractRequestCondition<CompositeRequestCondition> {

  private final RequestConditionHolder[] requestConditions;

  /**
   * Create an instance with 0 or more {@code RequestCondition} types. It is
   * important to create {@code CompositeRequestCondition} instances with the
   * same number of conditions so they may be compared and combined.
   * It is acceptable to provide {@code null} conditions.
   */
  public CompositeRequestCondition(RequestCondition<?>... requestConditions) {
    this.requestConditions = wrap(requestConditions);
  }

  private CompositeRequestCondition(RequestConditionHolder[] requestConditions) {
    this.requestConditions = requestConditions;
  }

  private RequestConditionHolder[] wrap(RequestCondition<?>... rawConditions) {
    RequestConditionHolder[] wrappedConditions = new RequestConditionHolder[rawConditions.length];
    for (int i = 0; i < rawConditions.length; i++) {
      wrappedConditions[i] = new RequestConditionHolder(rawConditions[i]);
    }
    return wrappedConditions;
  }

  /**
   * Whether this instance contains 0 conditions or not.
   */
  @Override
  public boolean isEmpty() {
    return ObjectUtils.isEmpty(this.requestConditions);
  }

  /**
   * Return the underlying conditions (possibly empty but never {@code null}).
   */
  public List<RequestCondition<?>> getConditions() {
    ArrayList<RequestCondition<?>> result = new ArrayList<>();
    for (RequestConditionHolder holder : requestConditions) {
      result.add(holder.getCondition());
    }
    return result;
  }

  @Override
  protected Collection<?> getContent() {
    return (!isEmpty() ? getConditions() : Collections.emptyList());
  }

  @Override
  protected String getToStringInfix() {
    return " && ";
  }

  private int getLength() {
    return this.requestConditions.length;
  }

  /**
   * If one instance is empty, return the other.
   * If both instances have conditions, combine the individual conditions
   * after ensuring they are of the same type and number.
   */
  @Override
  public CompositeRequestCondition combine(CompositeRequestCondition other) {
    if (isEmpty() && other.isEmpty()) {
      return this;
    }
    else if (other.isEmpty()) {
      return this;
    }
    else if (isEmpty()) {
      return other;
    }
    else {
      assertNumberOfConditions(other);
      int length = getLength();

      RequestConditionHolder[] otherConditions = other.requestConditions;
      RequestConditionHolder[] requestConditions = this.requestConditions;
      RequestConditionHolder[] combinedConditions = new RequestConditionHolder[length];
      for (int i = 0; i < length; i++) {
        combinedConditions[i] = requestConditions[i].combine(otherConditions[i]);
      }
      return new CompositeRequestCondition(combinedConditions);
    }
  }

  private void assertNumberOfConditions(CompositeRequestCondition other) {
    if (getLength() != other.getLength()) {
      throw new IllegalArgumentException(
              "Cannot combine CompositeRequestConditions with a different number of conditions. " +
                      ObjectUtils.nullSafeToString(requestConditions) + " and  " +
                      ObjectUtils.nullSafeToString(other.requestConditions));
    }
  }

  /**
   * Delegate to <em>all</em> contained conditions to match the request and return the
   * resulting "matching" condition instances.
   * <p>An empty {@code CompositeRequestCondition} matches to all requests.
   */
  @Override
  @Nullable
  public CompositeRequestCondition getMatchingCondition(RequestContext request) {
    if (isEmpty()) {
      return this;
    }
    int length = getLength();
    RequestConditionHolder[] requestConditions = this.requestConditions;
    RequestConditionHolder[] matchingConditions = new RequestConditionHolder[length];
    for (int i = 0; i < length; i++) {
      matchingConditions[i] = requestConditions[i].getMatchingCondition(request);
      if (matchingConditions[i] == null) {
        return null;
      }
    }
    return new CompositeRequestCondition(matchingConditions);
  }

  /**
   * If one instance is empty, the other "wins". If both instances have
   * conditions, compare them in the order in which they were provided.
   */
  @Override
  public int compareTo(CompositeRequestCondition other, RequestContext request) {
    if (isEmpty() && other.isEmpty()) {
      return 0;
    }
    else if (isEmpty()) {
      return 1;
    }
    else if (other.isEmpty()) {
      return -1;
    }
    else {
      assertNumberOfConditions(other);
      RequestConditionHolder[] otherConditions = other.requestConditions;
      RequestConditionHolder[] requestConditions = this.requestConditions;
      for (int i = 0; i < getLength(); i++) {
        int result = requestConditions[i].compareTo(otherConditions[i], request);
        if (result != 0) {
          return result;
        }
      }
      return 0;
    }
  }

}
