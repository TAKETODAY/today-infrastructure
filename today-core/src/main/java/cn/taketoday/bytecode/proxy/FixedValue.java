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
package cn.taketoday.bytecode.proxy;

/**
 * {@link Enhancer} callback that simply returns the value to return from the
 * proxied method. No information about what method is being called is available
 * to the callback, and the type of the returned object must be compatible with
 * the return type of the proxied method. This makes this callback primarily
 * useful for forcing a particular method (through the use of a
 * {@link CallbackFilter} to return a fixed value with little overhead.
 */
@FunctionalInterface
public interface FixedValue extends Callback {
  /**
   * Return the object which the original method invocation should return. This
   * method is called for <b>every</b> method invocation.
   *
   * @return an object matching the type of the return value for every method this
   * callback is mapped to
   */
  Object loadObject() throws Exception;
}
