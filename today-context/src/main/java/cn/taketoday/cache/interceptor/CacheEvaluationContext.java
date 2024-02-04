/*
 * Copyright 2017 - 2024 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package cn.taketoday.cache.interceptor;

import java.lang.reflect.Method;
import java.util.HashSet;

import cn.taketoday.context.expression.MethodBasedEvaluationContext;
import cn.taketoday.core.ParameterNameDiscoverer;
import cn.taketoday.expression.spel.support.StandardEvaluationContext;
import cn.taketoday.lang.Nullable;

/**
 * Cache-specific evaluation context that adds method parameters as SpEL
 * variables, in a lazy manner. The lazy nature avoids unnecessary
 * parsing of a class's byte code for parameter discovery.
 *
 * <p>Also defines a set of "unavailable variables" (i.e. variables that should
 * lead to an exception as soon as they are accessed). This can be useful
 * to verify a condition does not match even when not all potential variables
 * are present.
 *
 * <p>To limit the creation of objects, an ugly constructor is used
 * (rather than a dedicated 'closure'-like class for deferred execution).
 *
 * @author Costin Leau
 * @author Stephane Nicoll
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2021/12/26 00:00
 */
final class CacheEvaluationContext extends MethodBasedEvaluationContext {

  private final HashSet<String> unavailableVariables = new HashSet<>(1);

  CacheEvaluationContext(Object rootObject, Method method, Object[] arguments,
          ParameterNameDiscoverer parameterNameDiscoverer, StandardEvaluationContext shared) {

    super(rootObject, method, arguments, parameterNameDiscoverer, shared);
  }

  /**
   * Add the specified variable name as unavailable for this context.
   * <p>Any expression trying to access this variable should lead to an exception.
   * <p>This permits the validation of expressions that could potentially access
   * a variable even when such a variable isn't available yet. Any expression
   * trying to use that variable should therefore fail to evaluate.
   */
  public void addUnavailableVariable(String name) {
    this.unavailableVariables.add(name);
  }

  /**
   * Load the param information only when needed.
   */
  @Override
  @Nullable
  public Object lookupVariable(String name) {
    if (this.unavailableVariables.contains(name)) {
      throw new VariableNotAvailableException(name);
    }
    return super.lookupVariable(name);
  }

}
