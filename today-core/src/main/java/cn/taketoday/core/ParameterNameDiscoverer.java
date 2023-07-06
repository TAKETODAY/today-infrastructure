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

package cn.taketoday.core;

import java.lang.reflect.Executable;
import java.lang.reflect.Method;

import cn.taketoday.lang.Constant;
import cn.taketoday.lang.Nullable;

/**
 * abstract class to discover parameter names for methods and constructors.
 *
 * <p>Parameter name discovery is not always possible, but various strategies are
 * available to try, such as looking for debug information that may have been
 * emitted at compile time, and looking for argname annotation values optionally
 * accompanying AspectJ annotated methods.
 *
 * @author Rod Johnson
 * @author Adrian Colyer
 * @author TODAY 2021/9/10 22:33
 * @since 4.0
 */
public abstract class ParameterNameDiscoverer {

  /**
   * Return parameter names for an Executable(method or constructor), or {@code null}
   * if they cannot be determined.
   * <p>Individual entries in the array may be {@code null} if parameter names are only
   * available for some parameters of the given method but not for others. However,
   * it is recommended to use stub parameter names instead wherever feasible.
   *
   * @param executable the Executable(method or constructor) to find parameter names for
   * @return an array of parameter names if the names can be resolved,
   * or {@code null} if they cannot
   * @see Executable#getParameterCount()
   */
  @Nullable
  public String[] getParameterNames(@Nullable Executable executable) {
    if (executable == null) {
      return null;
    }
    if (executable.getParameterCount() == 0) {
      return Constant.EMPTY_STRING_ARRAY;
    }
    if (executable instanceof Method) {
      executable = BridgeMethodResolver.findBridgedMethod((Method) executable);
    }
    return doGet(executable);
  }

  @Nullable
  protected String[] doGet(Executable executable) {
    return null;
  }

  /**
   * shared instance
   */
  public static ParameterNameDiscoverer getSharedInstance() {
    return DefaultParameterNameDiscoverer.INSTANCE;
  }

  /**
   * Return parameter names for an Executable(method or constructor), or {@code null}
   * if they cannot be determined.
   * <p>Individual entries in the array may be {@code null} if parameter names are only
   * available for some parameters of the given method but not for others. However,
   * it is recommended to use stub parameter names instead wherever feasible.
   *
   * @param executable the Executable(method or constructor) to find parameter names for
   * @return an array of parameter names if the names can be resolved,
   * or {@code null} if they cannot
   * @see Executable#getParameterCount()
   * @see DefaultParameterNameDiscoverer
   * @see #getSharedInstance()
   */
  @Nullable
  public static String[] findParameterNames(@Nullable Executable executable) {
    return getSharedInstance().getParameterNames(executable);
  }

}
