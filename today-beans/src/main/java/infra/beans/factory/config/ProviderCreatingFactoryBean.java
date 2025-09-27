/*
 * Copyright 2017 - 2025 the original author or authors.
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
