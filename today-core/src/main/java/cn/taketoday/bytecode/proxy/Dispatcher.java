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

package cn.taketoday.bytecode.proxy;

import cn.taketoday.lang.Nullable;

/**
 * Dispatching {@link Enhancer} callback. This is identical to the
 * {@link LazyLoader} interface but needs to be separate so that
 * <code>Enhancer</code> knows which type of code to generate.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 */
@FunctionalInterface
public interface Dispatcher extends Callback {

  /**
   * Return the object which the original method invocation should be dispatched.
   * This method is called for <b>every</b> method invocation.
   *
   * @return an object that can invoke the method
   */
  @Nullable
  Object loadObject() throws Exception;
}
