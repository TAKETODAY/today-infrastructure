/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.core;

import org.jspecify.annotations.Nullable;

import java.lang.reflect.Executable;
import java.lang.reflect.Method;

import infra.lang.Constant;

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
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2021/9/10 22:33
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
  public String @Nullable [] getParameterNames(Executable executable) {
    if (executable.getParameterCount() == 0) {
      return Constant.EMPTY_STRING_ARRAY;
    }
    if (executable instanceof Method) {
      executable = BridgeMethodResolver.findBridgedMethod((Method) executable);
    }
    return doGet(executable);
  }

  protected String @Nullable [] doGet(Executable executable) {
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
  public static String @Nullable [] findParameterNames(Executable executable) {
    return getSharedInstance().getParameterNames(executable);
  }

}
