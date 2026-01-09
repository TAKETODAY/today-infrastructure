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

package infra.beans.factory.config;

import org.jspecify.annotations.Nullable;

import java.io.Serializable;

import infra.beans.BeansException;
import infra.beans.factory.BeanFactory;
import infra.beans.factory.FactoryBean;
import infra.lang.Assert;
import jakarta.inject.Provider;

/**
 * A {@link FactoryBean} implementation that returns a value which is a
 * JSR-330 {@link jakarta.inject.Provider} that in turn returns a bean
 * sourced from a {@link BeanFactory}.
 *
 * <p>This is basically a JSR-330 compliant variant of  good old
 * {@link SupplierFactoryCreatingFactoryBean}. It can be used for traditional
 * external dependency injection configuration that targets a property or
 * constructor argument of type {@code jakarta.inject.Provider}, as an
 * alternative to JSR-330's {@code @Inject} annotation-driven approach.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see jakarta.inject.Provider
 * @see SupplierFactoryCreatingFactoryBean
 * @since 4.0 2021/11/30 14:22
 */
public class ProviderCreatingFactoryBean extends AbstractFactoryBean<Provider<Object>> {

  @Nullable
  private String targetBeanName;

  /**
   * Set the name of the target bean.
   * <p>The target does not <i>have</i> to be a non-singleton bean, but realistically
   * always will be (because if the target bean were a singleton, then said singleton
   * bean could simply be injected straight into the dependent object, thus obviating
   * the need for the extra level of indirection afforded by this factory approach).
   */
  public void setTargetBeanName(@Nullable String targetBeanName) {
    this.targetBeanName = targetBeanName;
  }

  @Override
  public void afterPropertiesSet() throws Exception {
    Assert.hasText(this.targetBeanName, "Property 'targetBeanName' is required");
    super.afterPropertiesSet();
  }

  @Override
  public Class<?> getObjectType() {
    return Provider.class;
  }

  @Override
  protected Provider<Object> createBeanInstance() {
    BeanFactory beanFactory = getBeanFactory();
    Assert.state(beanFactory != null, "No BeanFactory available");
    Assert.state(this.targetBeanName != null, "No target bean name specified");
    return new TargetBeanProvider(beanFactory, this.targetBeanName);
  }

  /**
   * Independent inner class - for serialization purposes.
   */
  private record TargetBeanProvider(BeanFactory beanFactory, String targetBeanName)
          implements Provider<Object>, Serializable {

    @SuppressWarnings("NullAway")
    @Override
    public Object get() throws BeansException {
      return this.beanFactory.getBean(this.targetBeanName);
    }

  }

}
