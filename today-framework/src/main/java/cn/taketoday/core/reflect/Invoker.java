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
package cn.taketoday.core.reflect;

/**
 * @author TODAY 2019-10-18 22:35
 */
@FunctionalInterface
public interface Invoker {

  /**
   * Invokes the underlying method represented by this {@code Invoker}
   * object, on the specified object with the specified parameters.
   * Individual parameters are automatically unwrapped to match
   * primitive formal parameters, and both primitive and reference
   * parameters are subject to method invocation conversions as
   * necessary.
   *
   * <p>If the underlying method is static, then the specified {@code obj}
   * argument is ignored. It may be null.
   *
   * <p>If the number of formal parameters required by the underlying method is
   * 0, the supplied {@code args} array may be of length 0 or null.
   *
   * <p>If the underlying method is static, the class that declared
   * the method is initialized if it has not already been initialized.
   *
   * <p>If the method completes normally, the value it returns is
   * returned to the caller of invoke; if the value has a primitive
   * type, it is first appropriately wrapped in an object. However,
   * if the value has the type of array of a primitive type, the
   * elements of the array are <i>not</i> wrapped in objects; in
   * other words, an array of primitive type is returned.  If the
   * underlying method return type is void, the invocation returns
   * null.
   *
   * @param obj the object the underlying method is invoked from
   * @param args the arguments used for the method call
   * @return the result of dispatching the method represented by
   * this object on {@code obj} with parameters
   * {@code args}
   * @throws NullPointerException if the specified object is null and the method is an instance method.
   * @throws ExceptionInInitializerError if the initialization provoked by this method fails.
   */
  Object invoke(Object obj, Object[] args);
}
