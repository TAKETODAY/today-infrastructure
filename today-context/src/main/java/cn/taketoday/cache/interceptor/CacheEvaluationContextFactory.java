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

package cn.taketoday.cache.interceptor;

import java.lang.reflect.Method;
import java.util.function.Supplier;

import cn.taketoday.core.DefaultParameterNameDiscoverer;
import cn.taketoday.core.ParameterNameDiscoverer;
import cn.taketoday.expression.spel.support.StandardEvaluationContext;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.function.SingletonSupplier;

/**
 * A factory for {@link CacheEvaluationContext} that makes sure that internal
 * delegates are reused.
 *
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class CacheEvaluationContextFactory {

  private final StandardEvaluationContext originalContext;

  @Nullable
  private Supplier<ParameterNameDiscoverer> parameterNameDiscoverer;

  CacheEvaluationContextFactory(StandardEvaluationContext originalContext) {
    this.originalContext = originalContext;
  }

  public void setParameterNameDiscoverer(Supplier<ParameterNameDiscoverer> parameterNameDiscoverer) {
    this.parameterNameDiscoverer = parameterNameDiscoverer;
  }

  public ParameterNameDiscoverer getParameterNameDiscoverer() {
    if (this.parameterNameDiscoverer == null) {
      this.parameterNameDiscoverer = SingletonSupplier.valueOf(new DefaultParameterNameDiscoverer());
    }
    return this.parameterNameDiscoverer.get();
  }

  /**
   * Creates a {@link CacheEvaluationContext} for the specified operation.
   *
   * @param rootObject the {@code root} object to use for the context
   * @param targetMethod the target cache {@link Method}
   * @param args the arguments of the method invocation
   * @return a context suitable for this cache operation
   */
  public CacheEvaluationContext forOperation(CacheExpressionRootObject rootObject,
          Method targetMethod, Object[] args) {

    CacheEvaluationContext evaluationContext = new CacheEvaluationContext(
            rootObject, targetMethod, args, getParameterNameDiscoverer());
    this.originalContext.applyDelegatesTo(evaluationContext);
    return evaluationContext;
  }

}
