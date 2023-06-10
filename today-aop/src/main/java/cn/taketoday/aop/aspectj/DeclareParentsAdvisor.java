/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.aop.aspectj;

import org.aopalliance.aop.Advice;

import cn.taketoday.aop.ClassFilter;
import cn.taketoday.aop.IntroductionAdvisor;
import cn.taketoday.aop.IntroductionInterceptor;
import cn.taketoday.aop.support.ClassFilters;
import cn.taketoday.aop.support.DelegatePerTargetObjectIntroductionInterceptor;
import cn.taketoday.aop.support.DelegatingIntroductionInterceptor;

/**
 * Introduction advisor delegating to the given object.
 * Implements AspectJ annotation-style behavior for the DeclareParents annotation.
 *
 * @author Rod Johnson
 * @author Ramnivas Laddad
 * @since 4.0
 */
public class DeclareParentsAdvisor implements IntroductionAdvisor {

  private final Advice advice;

  private final Class<?> introducedInterface;

  private final ClassFilter typePatternClassFilter;

  /**
   * Create a new advisor for this DeclareParents field.
   *
   * @param interfaceType static field defining the introduction
   * @param typePattern type pattern the introduction is restricted to
   * @param defaultImpl the default implementation class
   */
  public DeclareParentsAdvisor(Class<?> interfaceType, String typePattern, Class<?> defaultImpl) {
    this(interfaceType, typePattern,
            new DelegatePerTargetObjectIntroductionInterceptor(defaultImpl, interfaceType));
  }

  /**
   * Create a new advisor for this DeclareParents field.
   *
   * @param interfaceType static field defining the introduction
   * @param typePattern type pattern the introduction is restricted to
   * @param delegateRef the delegate implementation object
   */
  public DeclareParentsAdvisor(Class<?> interfaceType, String typePattern, Object delegateRef) {
    this(interfaceType, typePattern, new DelegatingIntroductionInterceptor(delegateRef));
  }

  /**
   * Private constructor to share common code between impl-based delegate and reference-based delegate
   * (cannot use method such as init() to share common code, due the use of final fields).
   *
   * @param interfaceType static field defining the introduction
   * @param typePattern type pattern the introduction is restricted to
   * @param interceptor the delegation advice as {@link IntroductionInterceptor}
   */
  private DeclareParentsAdvisor(Class<?> interfaceType, String typePattern, IntroductionInterceptor interceptor) {
    this.advice = interceptor;
    this.introducedInterface = interfaceType;

    // Excludes methods implemented.
    ClassFilter typePatternFilter = new TypePatternClassFilter(typePattern);
    ClassFilter exclusion = (clazz -> !this.introducedInterface.isAssignableFrom(clazz));
    this.typePatternClassFilter = ClassFilters.intersection(typePatternFilter, exclusion);
  }

  @Override
  public ClassFilter getClassFilter() {
    return this.typePatternClassFilter;
  }

  @Override
  public void validateInterfaces() throws IllegalArgumentException {
    // Do nothing
  }

  @Override
  public Advice getAdvice() {
    return this.advice;
  }

  @Override
  public Class<?>[] getInterfaces() {
    return new Class<?>[] { this.introducedInterface };
  }

}
