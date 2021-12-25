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

package cn.taketoday.context.expression;

import java.lang.reflect.Method;
import java.util.Arrays;

import cn.taketoday.core.ParameterNameDiscoverer;
import cn.taketoday.expression.StandardExpressionContext;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ObjectUtils;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2021/12/25 17:39
 */
public class MethodBasedEvaluationContext extends StandardExpressionContext {

  private final Method method;
  private final Object[] arguments;
  private boolean argumentsLoaded = false;
  private final ParameterNameDiscoverer parameterNameDiscoverer;

  public MethodBasedEvaluationContext(
          Object rootObject, Method method, Object[] arguments,
          ParameterNameDiscoverer parameterNameDiscoverer) {
    this.method = method;
    this.arguments = arguments;
    this.parameterNameDiscoverer = parameterNameDiscoverer;
    addResolver(new RootObjectExpressionResolver(rootObject));
  }

  @Override
  @Nullable
  public Object getBean(String name) {
    Object variable = super.getBean(name);
    if (variable != null) {
      return variable;
    }
    if (!this.argumentsLoaded) {
      lazyLoadArguments();
      this.argumentsLoaded = true;
      variable = super.getBean(name);
    }
    return variable;
  }

  /**
   * Load the param information only when needed.
   */
  protected void lazyLoadArguments() {
    // Shortcut if no args need to be loaded
    if (ObjectUtils.isEmpty(this.arguments)) {
      return;
    }

    // Expose indexed variables as well as parameter names (if discoverable)
    String[] paramNames = this.parameterNameDiscoverer.getParameterNames(this.method);
    int paramCount = paramNames != null ? paramNames.length : this.method.getParameterCount();
    int argsCount = this.arguments.length;

    for (int i = 0; i < paramCount; i++) {
      Object value = null;
      if (argsCount > paramCount && i == paramCount - 1) {
        // Expose remaining arguments as vararg array for last parameter
        value = Arrays.copyOfRange(this.arguments, i, argsCount);
      }
      else if (argsCount > i) {
        // Actual argument found - otherwise left as null
        value = this.arguments[i];
      }
      setVariable("a" + i, value);
      setVariable("p" + i, value);
      if (paramNames != null && paramNames[i] != null) {
        setVariable(paramNames[i], value);
      }
    }
  }

}
