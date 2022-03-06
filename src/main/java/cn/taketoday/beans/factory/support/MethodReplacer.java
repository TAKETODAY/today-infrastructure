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

package cn.taketoday.beans.factory.support;

import java.lang.reflect.Method;

/**
 * Interface to be implemented by classes that can reimplement any method on an
 * IoC-managed object: the <b>Method Injection</b> form of Dependency Injection.
 *
 * <p>Such methods may be (but need not be) abstract, in which case the
 * container will create a concrete subclass to instantiate.
 *
 * @author Rod Johnson
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/3/6 22:39
 */
public interface MethodReplacer {

  /**
   * Reimplement the given method.
   *
   * @param obj the instance we're reimplementing the method for
   * @param method the method to reimplement
   * @param args arguments to the method
   * @return return value for the method
   */
  Object reimplement(Object obj, Method method, Object[] args) throws Throwable;

}
