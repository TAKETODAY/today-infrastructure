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

package cn.taketoday.classify.util;

import java.lang.reflect.Method;

/**
 * Strategy interface for detecting a single Method on a Class.
 *
 * @author Mark Fisher
 */
public interface MethodResolver {

  /**
   * Find a single Method on the provided Object that matches this resolver's criteria.
   *
   * @param candidate the candidate Object whose Class should be searched for a Method
   * @return a single Method or <code>null</code> if no Method matching this resolver's
   * criteria can be found.
   * @throws IllegalArgumentException if more than one Method defined on the given
   * candidate's Class matches this resolver's criteria
   */
  Method findMethod(Object candidate) throws IllegalArgumentException;

  /**
   * Find a <em>single</em> Method on the given Class that matches this resolver's
   * criteria.
   *
   * @param clazz the Class instance on which to search for a Method
   * @return a single Method or <code>null</code> if no Method matching this resolver's
   * criteria can be found.
   * @throws IllegalArgumentException if more than one Method defined on the given Class
   * matches this resolver's criteria
   */
  Method findMethod(Class<?> clazz);

}
