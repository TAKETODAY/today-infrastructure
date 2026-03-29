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

package infra.test.context.bean.override.convention;

import org.jspecify.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Objects;

import infra.beans.factory.config.BeanDefinition;
import infra.core.ResolvableType;
import infra.core.style.ToStringBuilder;
import infra.test.context.bean.override.BeanOverrideHandler;
import infra.test.context.bean.override.BeanOverrideStrategy;
import infra.util.ReflectionUtils;

/**
 * {@link BeanOverrideHandler} implementation for {@link TestBean @TestBean}.
 *
 * @author Simon Baslé
 * @author Stephane Nicoll
 * @author Sam Brannen
 * @since 5.0
 */
final class TestBeanOverrideHandler extends BeanOverrideHandler {

  private final Method factoryMethod;

  TestBeanOverrideHandler(Field field, ResolvableType beanType, @Nullable String beanName,
          String contextName, BeanOverrideStrategy strategy, Method factoryMethod) {

    super(field, beanType, beanName, contextName, strategy);
    this.factoryMethod = factoryMethod;
  }

  @Override
  protected Object createOverrideInstance(String beanName, @Nullable BeanDefinition existingBeanDefinition,
          @Nullable Object existingBeanInstance) {

    try {
      ReflectionUtils.makeAccessible(this.factoryMethod);
      return this.factoryMethod.invoke(null);
    }
    catch (Throwable ex) {
      throw new IllegalStateException(
              "Failed to invoke @TestBean factory method: " + this.factoryMethod, ex);
    }
  }

  @Override
  public boolean equals(@Nullable Object other) {
    if (this == other) {
      return true;
    }
    if (other == null || getClass() != other.getClass()) {
      return false;
    }
    if (!super.equals(other)) {
      return false;
    }
    TestBeanOverrideHandler that = (TestBeanOverrideHandler) other;
    return Objects.equals(this.factoryMethod, that.factoryMethod);
  }

  @Override
  public int hashCode() {
    return this.factoryMethod.hashCode() * 29 + super.hashCode();
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this)
            .append("field", getField())
            .append("beanType", getBeanType())
            .append("beanName", getBeanName())
            .append("contextName", getContextName())
            .append("strategy", getStrategy())
            .append("factoryMethod", this.factoryMethod)
            .toString();
  }

}
