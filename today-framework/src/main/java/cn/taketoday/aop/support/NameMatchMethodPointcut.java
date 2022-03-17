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

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.ArrayList;

import cn.taketoday.core.ArraySizeTrimmer;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.util.StringUtils;

/**
 * Pointcut bean for simple method name matches, as an alternative to regexp patterns.
 *
 * <p>Does not handle overloaded methods: all methods with a given name will be eligible.
 *
 * @author Juergen Hoeller
 * @author Rod Johnson
 * @author Rob Harrop
 * @author TODAY 2021/2/3 23:50
 * @see #isMatch
 * @since 3.0
 */
@SuppressWarnings("serial")
public class NameMatchMethodPointcut
        extends StaticMethodMatcherPointcut implements Serializable, ArraySizeTrimmer {

  private final ArrayList<String> mappedNames = new ArrayList<>();

  /**
   * Convenience method when we have only a single method name to match.
   * Use either this method or {@code setMappedNames}, not both.
   *
   * @see #setMappedNames
   */
  public void setMappedName(@Nullable String mappedName) {
    setMappedNames(mappedName);
  }

  /**
   * Set the method names defining methods to match.
   * Matching will be the union of all these; if any match,
   * the pointcut matches.
   *
   * @see ArrayList#clear()
   */
  public void setMappedNames(@Nullable String... mappedNames) {
    this.mappedNames.clear();
    CollectionUtils.addAll(this.mappedNames, mappedNames);
    trimToSize();
  }

  /**
   * Add another eligible method name, in addition to those already named.
   * Like the set methods, this method is for use when configuring proxies,
   * before a proxy is used.
   * <p><b>NB:</b> This method does not work after the proxy is in
   * use, as advice chains will be cached.
   *
   * @param name the name of the additional method that will match
   * @return this pointcut to allow for multiple additions in one line
   */
  public NameMatchMethodPointcut addMethodName(String name) {
    this.mappedNames.add(name);
    return this;
  }

  @Override
  public boolean matches(Method method, Class<?> targetClass) {
    final String methodName = method.getName();
    for (String mappedName : this.mappedNames) {
      if (mappedName.equals(methodName) || isMatch(methodName, mappedName)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Return if the given method name matches the mapped name.
   * <p>The default implementation checks for "xxx*", "*xxx" and "*xxx*" matches,
   * as well as direct equality. Can be overridden in subclasses.
   *
   * @param methodName the method name of the class
   * @param mappedName the name in the descriptor
   * @return if the names match
   * @see StringUtils#simpleMatch(String, String)
   */
  protected boolean isMatch(String methodName, String mappedName) {
    return StringUtils.simpleMatch(mappedName, methodName);
  }

  @Override
  public boolean equals(Object other) {
    return (this == other || (other instanceof NameMatchMethodPointcut &&
            this.mappedNames.equals(((NameMatchMethodPointcut) other).mappedNames)));
  }

  @Override
  public int hashCode() {
    return this.mappedNames.hashCode();
  }

  @Override
  public String toString() {
    return getClass().getName() + ": " + this.mappedNames;
  }

  @Override
  public void trimToSize() {
    mappedNames.trimToSize();
  }
}
