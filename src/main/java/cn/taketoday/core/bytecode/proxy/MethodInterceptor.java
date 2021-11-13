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
 */package cn.taketoday.core.bytecode.proxy;

import java.lang.reflect.Method;

/**
 * General-purpose {@link Enhancer} callback which provides for "around advice".
 *
 * @author Juozas Baliuka <a href="mailto:baliuka@mwm.lt">baliuka@mwm.lt</a>
 * @author TODAY
 */
@FunctionalInterface
public interface MethodInterceptor extends Callback {

  /**
   * All generated proxied methods call this method instead of the original
   * method. The original method may either be invoked by normal reflection using
   * the Method object, or by using the MethodProxy (faster).
   *
   * @param obj "this", the enhanced object
   * @param method intercepted Method
   * @param args argument array; primitive types are wrapped
   * @param proxy used to invoke super (non-intercepted method); may be called as
   * many times as needed
   * @return any value compatible with the signature of the proxied method. Method
   * returning void will ignore this value.
   * @throws Throwable any exception may be thrown; if so, super method will not be
   * invoked
   * @see MethodProxy
   */
  Object intercept(Object obj, Method method, Object[] args, MethodProxy proxy) throws Throwable;

}
