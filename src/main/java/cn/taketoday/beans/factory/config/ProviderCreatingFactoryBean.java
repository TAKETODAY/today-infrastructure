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
package cn.taketoday.beans.factory.config;

import java.io.Serializable;

import cn.taketoday.beans.BeansException;
import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.beans.factory.FactoryBean;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
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

    @Override
    public Object get() throws BeansException {
      return this.beanFactory.getBean(this.targetBeanName);
    }

  }

}
