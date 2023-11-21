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

package cn.taketoday.util.comparator;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/1/28 10:41
 */

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import cn.taketoday.lang.Assert;

/**
 * Comparator capable of sorting exceptions based on their depth from the thrown exception type.
 *
 * @author Juergen Hoeller
 * @author Arjen Poutsma
 * @since 4.0
 */
public class ExceptionDepthComparator implements Comparator<Class<? extends Throwable>> {

  private final Class<? extends Throwable> targetException;

  /**
   * Create a new ExceptionDepthComparator for the given exception.
   *
   * @param exception the target exception to compare to when sorting by depth
   */
  public ExceptionDepthComparator(Throwable exception) {
    Assert.notNull(exception, "Target exception is required");
    this.targetException = exception.getClass();
  }

  /**
   * Create a new ExceptionDepthComparator for the given exception type.
   *
   * @param exceptionType the target exception type to compare to when sorting by depth
   */
  public ExceptionDepthComparator(Class<? extends Throwable> exceptionType) {
    Assert.notNull(exceptionType, "Target exception type is required");
    this.targetException = exceptionType;
  }

  @Override
  public int compare(Class<? extends Throwable> o1, Class<? extends Throwable> o2) {
    int depth1 = getDepth(o1, this.targetException, 0);
    int depth2 = getDepth(o2, this.targetException, 0);
    return (depth1 - depth2);
  }

  private int getDepth(Class<?> declaredException, Class<?> exceptionToMatch, int depth) {
    if (exceptionToMatch.equals(declaredException)) {
      // Found it!
      return depth;
    }
    // If we've gone as far as we can go and haven't found it...
    if (exceptionToMatch == Throwable.class) {
      return Integer.MAX_VALUE;
    }
    return getDepth(declaredException, exceptionToMatch.getSuperclass(), depth + 1);
  }

  /**
   * Obtain the closest match from the given exception types for the given target exception.
   *
   * @param exceptionTypes the collection of exception types
   * @param targetException the target exception to find a match for
   * @return the closest matching exception type from the given collection
   */
  public static Class<? extends Throwable> findClosestMatch(
          Collection<Class<? extends Throwable>> exceptionTypes, Throwable targetException) {

    Assert.notEmpty(exceptionTypes, "Exception types must not be empty");
    if (exceptionTypes.size() == 1) {
      return exceptionTypes.iterator().next();
    }
    List<Class<? extends Throwable>> handledExceptions = new ArrayList<>(exceptionTypes);
    handledExceptions.sort(new ExceptionDepthComparator(targetException));
    return handledExceptions.get(0);
  }

}
