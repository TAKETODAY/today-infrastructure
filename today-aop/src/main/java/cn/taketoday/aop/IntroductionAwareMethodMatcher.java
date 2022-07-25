/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

package cn.taketoday.aop;

import java.lang.reflect.Method;

/**
 * A specialized type of {@link MethodMatcher} that takes into account introductions
 * when matching methods. If there are no introductions on the target class,
 * a method matcher may be able to optimize matching more effectively for example.
 *
 * @author Adrian Colyer
 * @author TODAY 2021/2/1 18:30
 * @since 3.0
 */
public interface IntroductionAwareMethodMatcher extends MethodMatcher {

  /**
   * Perform static checking whether the given method matches. This may be invoked
   * instead of the 2-arg {@link #matches(java.lang.reflect.Method, Class)} method
   * if the caller supports the extended IntroductionAwareMethodMatcher interface.
   *
   * @param method the candidate method
   * @param targetClass the target class
   * @param hasIntroductions {@code true} if the object on whose behalf we are
   * asking is the subject on one or more introductions; {@code false} otherwise
   * @return whether or not this method matches statically
   */
  boolean matches(Method method, Class<?> targetClass, boolean hasIntroductions);

}
