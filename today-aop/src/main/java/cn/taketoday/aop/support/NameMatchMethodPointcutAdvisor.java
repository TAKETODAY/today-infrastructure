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

package cn.taketoday.aop.support;

import org.aopalliance.aop.Advice;

import cn.taketoday.aop.ClassFilter;
import cn.taketoday.aop.Pointcut;

/**
 * Convenient class for name-match method pointcuts that hold an Advice,
 * making them an Advisor.
 *
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @author TODAY 2021/2/4 12:09
 * @see NameMatchMethodPointcut
 * @since 3.0
 */
@SuppressWarnings("serial")
public class NameMatchMethodPointcutAdvisor extends AbstractGenericPointcutAdvisor {
  private final NameMatchMethodPointcut pointcut = new NameMatchMethodPointcut();

  public NameMatchMethodPointcutAdvisor() { }

  public NameMatchMethodPointcutAdvisor(Advice advice) {
    setAdvice(advice);
  }

  /**
   * Set the {@link ClassFilter} to use for this pointcut.
   * Default is {@link ClassFilter#TRUE}.
   *
   * @see NameMatchMethodPointcut#setClassFilter
   */
  public void setClassFilter(ClassFilter classFilter) {
    this.pointcut.setClassFilter(classFilter);
  }

  /**
   * Convenience method when we have only a single method name to match.
   * Use either this method or {@code setMappedNames}, not both.
   *
   * @see #setMappedNames
   * @see NameMatchMethodPointcut#setMappedName
   */
  public void setMappedName(String mappedName) {
    this.pointcut.setMappedName(mappedName);
  }

  /**
   * Set the method names defining methods to match.
   * Matching will be the union of all these; if any match,
   * the pointcut matches.
   *
   * @see NameMatchMethodPointcut#setMappedNames
   */
  public void setMappedNames(String... mappedNames) {
    this.pointcut.setMappedNames(mappedNames);
  }

  /**
   * Add another eligible method name, in addition to those already named.
   * Like the set methods, this method is for use when configuring proxies,
   * before a proxy is used.
   *
   * @param name the name of the additional method that will match
   * @return this pointcut to allow for multiple additions in one line
   * @see NameMatchMethodPointcut#addMethodName
   */
  public NameMatchMethodPointcut addMethodName(String name) {
    return this.pointcut.addMethodName(name);
  }

  @Override
  public Pointcut getPointcut() {
    return this.pointcut;
  }

}
