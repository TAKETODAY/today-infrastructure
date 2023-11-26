/*
 * Copyright 2017 - 2023 the original author or authors.
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
import cn.taketoday.expression.EvaluationContext;
import cn.taketoday.expression.spel.support.StandardEvaluationContext;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ObjectUtils;

/**
 * A method-based {@link EvaluationContext} that
 * provides explicit support for method-based invocations.
 *
 * <p>Expose the actual method arguments using the following aliases:
 * <ol>
 * <li>pX where X is the index of the argument (p0 for the first argument)</li>
 * <li>aX where X is the index of the argument (a1 for the second argument)</li>
 * <li>the name of the parameter as discovered by a configurable {@link ParameterNameDiscoverer}</li>
 * </ol>
 *
 * @author Stephane Nicoll
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2021/12/25 17:39
 */
public class MethodBasedEvaluationContext extends StandardEvaluationContext {

  private final Method method;

  private final Object[] arguments;

  private final ParameterNameDiscoverer parameterNameDiscoverer;

  private boolean argumentsLoaded = false;

  public MethodBasedEvaluationContext(Object rootObject, Method method,
          Object[] arguments, ParameterNameDiscoverer parameterNameDiscoverer) {
    super(rootObject);
    this.method = method;
    this.arguments = arguments;
    this.parameterNameDiscoverer = parameterNameDiscoverer;
  }

  public MethodBasedEvaluationContext(Object rootObject, Method method, Object[] arguments,
          ParameterNameDiscoverer parameterNameDiscoverer, StandardEvaluationContext shared) {
    super(rootObject, shared);
    this.method = method;
    this.arguments = arguments;
    this.parameterNameDiscoverer = parameterNameDiscoverer;
  }

  @Override
  @Nullable
  public Object lookupVariable(String name) {
    Object variable = super.lookupVariable(name);
    if (variable != null) {
      return variable;
    }
    if (!this.argumentsLoaded) {
      lazyLoadArguments();
      this.argumentsLoaded = true;
      variable = super.lookupVariable(name);
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
    int paramCount = (paramNames != null ? paramNames.length : this.method.getParameterCount());
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
