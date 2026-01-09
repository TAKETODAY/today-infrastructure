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

package infra.context.expression;

import org.jspecify.annotations.Nullable;

import infra.beans.factory.config.BeanExpressionContext;
import infra.expression.AccessException;
import infra.expression.EvaluationContext;
import infra.expression.PropertyAccessor;
import infra.expression.TypedValue;
import infra.lang.Assert;

/**
 * EL property accessor that knows how to traverse the beans and contextual objects
 * of a {@link BeanExpressionContext}.
 *
 * @author Juergen Hoeller
 * @author Andy Clement
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/3/11 17:31
 */
public class BeanExpressionContextAccessor implements PropertyAccessor {

  @Override
  public boolean canRead(EvaluationContext context, @Nullable Object target, String name) throws AccessException {
    return (target instanceof BeanExpressionContext && ((BeanExpressionContext) target).containsObject(name));
  }

  @Override
  public TypedValue read(EvaluationContext context, @Nullable Object target, String name) throws AccessException {
    Assert.state(target instanceof BeanExpressionContext, "Target must be of type BeanExpressionContext");
    return new TypedValue(((BeanExpressionContext) target).getObject(name));
  }

  @Override
  public boolean canWrite(EvaluationContext context, @Nullable Object target, String name) throws AccessException {
    return false;
  }

  @Override
  public void write(EvaluationContext context, @Nullable Object target, String name, @Nullable Object newValue)
          throws AccessException {

    throw new AccessException("Beans in a BeanFactory are read-only");
  }

  @Override
  public Class<?>[] getSpecificTargetClasses() {
    return new Class<?>[] { BeanExpressionContext.class };
  }

}
