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

package cn.taketoday.aop.config;

import org.w3c.dom.Node;

import java.util.List;

import cn.taketoday.aop.framework.ProxyFactoryBean;
import cn.taketoday.beans.factory.config.BeanDefinition;
import cn.taketoday.beans.factory.config.BeanDefinitionHolder;
import cn.taketoday.beans.factory.support.AbstractBeanDefinition;
import cn.taketoday.beans.factory.support.BeanDefinitionReaderUtils;
import cn.taketoday.beans.factory.support.BeanDefinitionRegistry;
import cn.taketoday.beans.factory.support.ManagedList;
import cn.taketoday.beans.factory.support.RootBeanDefinition;
import cn.taketoday.beans.factory.xml.BeanDefinitionDecorator;
import cn.taketoday.beans.factory.xml.ParserContext;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.NonNull;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.StringUtils;

/**
 * Base implementation for
 * {@link cn.taketoday.beans.factory.xml.BeanDefinitionDecorator BeanDefinitionDecorators}
 * wishing to add an {@link org.aopalliance.intercept.MethodInterceptor interceptor}
 * to the resulting bean.
 *
 * <p>This base class controls the creation of the {@link ProxyFactoryBean} bean definition
 * and wraps the original as an inner-bean definition for the {@code target} property
 * of {@link ProxyFactoryBean}.
 *
 * <p>Chaining is correctly handled, ensuring that only one {@link ProxyFactoryBean} definition
 * is created. If a previous {@link cn.taketoday.beans.factory.xml.BeanDefinitionDecorator}
 * already created the {@link ProxyFactoryBean} then the
 * interceptor is simply added to the existing definition.
 *
 * <p>Subclasses have only to create the {@code BeanDefinition} to the interceptor that
 * they wish to add.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see org.aopalliance.intercept.MethodInterceptor
 * @since 4.0 2022/3/7 19:29
 */
public abstract class AbstractInterceptorDrivenBeanDefinitionDecorator implements BeanDefinitionDecorator {

  @Override
  public final BeanDefinitionHolder decorate(@NonNull Node node, BeanDefinitionHolder definitionHolder, ParserContext parserContext) {
    BeanDefinitionRegistry registry = parserContext.getRegistry();

    // get the root bean name - will be the name of the generated proxy factory bean
    String existingBeanName = definitionHolder.getBeanName();
    BeanDefinition targetDefinition = definitionHolder.getBeanDefinition();
    BeanDefinitionHolder targetHolder = new BeanDefinitionHolder(targetDefinition, existingBeanName + ".TARGET");

    // delegate to subclass for interceptor definition
    BeanDefinition interceptorDefinition = createInterceptorDefinition(node);

    // generate name and register the interceptor
    String interceptorName = existingBeanName + '.' + getInterceptorNameSuffix(interceptorDefinition);
    BeanDefinitionReaderUtils.registerBeanDefinition(
            new BeanDefinitionHolder(interceptorDefinition, interceptorName), registry);

    BeanDefinitionHolder result = definitionHolder;

    if (!isProxyFactoryBeanDefinition(targetDefinition)) {
      // create the proxy definition
      RootBeanDefinition proxyDefinition = new RootBeanDefinition();
      // create proxy factory bean definition
      proxyDefinition.setBeanClass(ProxyFactoryBean.class);
      proxyDefinition.setScope(targetDefinition.getScope());
      proxyDefinition.setLazyInit(targetDefinition.isLazyInit());
      // set the target
      proxyDefinition.setDecoratedDefinition(targetHolder);
      proxyDefinition.getPropertyValues().add("target", targetHolder);
      // create the interceptor names list
      proxyDefinition.getPropertyValues().add("interceptorNames", new ManagedList<String>());
      // copy autowire settings from original bean definition.
      proxyDefinition.setAutowireCandidate(targetDefinition.isAutowireCandidate());
      proxyDefinition.setPrimary(targetDefinition.isPrimary());
      if (targetDefinition instanceof AbstractBeanDefinition) {
        proxyDefinition.copyQualifiersFrom((AbstractBeanDefinition) targetDefinition);
      }
      // wrap it in a BeanDefinitionHolder with bean name
      result = new BeanDefinitionHolder(proxyDefinition, existingBeanName);
    }

    addInterceptorNameToList(interceptorName, result.getBeanDefinition());
    return result;
  }

  @SuppressWarnings("unchecked")
  private void addInterceptorNameToList(String interceptorName, BeanDefinition beanDefinition) {
    List<String> list = (List<String>) beanDefinition.getPropertyValues().getPropertyValue("interceptorNames");
    Assert.state(list != null, "Missing 'interceptorNames' property");
    list.add(interceptorName);
  }

  private boolean isProxyFactoryBeanDefinition(BeanDefinition existingDefinition) {
    return ProxyFactoryBean.class.getName().equals(existingDefinition.getBeanClassName());
  }

  protected String getInterceptorNameSuffix(BeanDefinition interceptorDefinition) {
    String beanClassName = interceptorDefinition.getBeanClassName();
    return StringUtils.isNotEmpty(beanClassName) ?
           StringUtils.uncapitalize(ClassUtils.getShortName(beanClassName)) : "";
  }

  /**
   * Subclasses should implement this method to return the {@code BeanDefinition}
   * for the interceptor they wish to apply to the bean being decorated.
   */
  protected abstract BeanDefinition createInterceptorDefinition(Node node);

}
