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

package cn.taketoday.cache.interceptor;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

import cn.taketoday.context.expression.MethodBasedEvaluationContext;
import cn.taketoday.core.ParameterNameDiscoverer;
import cn.taketoday.lang.Nullable;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2021/12/26 00:00
 */
final class CacheEvaluationContext extends MethodBasedEvaluationContext {

  private final Set<String> unavailableVariables = new HashSet<>(1);

  public CacheEvaluationContext(
          Object rootObject, Method method, Object[] arguments, ParameterNameDiscoverer parameterNameDiscoverer) {
    super(rootObject, method, arguments, parameterNameDiscoverer);
  }

  /**
   * Add the specified variable name as unavailable for that context.
   * Any expression trying to access this variable should lead to an exception.
   * <p>This permits the validation of expressions that could potentially a
   * variable even when such variable isn't available yet. Any expression
   * trying to use that variable should therefore fail to evaluate.
   */
  public void addUnavailableVariable(String name) {
    this.unavailableVariables.add(name);
  }

  /**
   * Load the param information only when needed.
   */
  @Nullable
  @Override
  public Object getBean(String name) {
    if (this.unavailableVariables.contains(name)) {
      throw new VariableNotAvailableException(name);
    }
    return super.getBean(name);
  }

}
