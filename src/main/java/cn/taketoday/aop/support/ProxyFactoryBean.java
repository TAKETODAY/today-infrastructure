/*
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
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

package cn.taketoday.aop.support;

import java.util.Set;

import cn.taketoday.aop.Advisor;
import cn.taketoday.aop.TargetSource;
import cn.taketoday.aop.proxy.AopConfigException;
import cn.taketoday.aop.proxy.ProxyCreatorSupport;
import cn.taketoday.aop.proxy.ProxyFactory;
import cn.taketoday.aop.target.SingletonTargetSource;
import cn.taketoday.context.aware.BeanClassLoaderAware;
import cn.taketoday.context.aware.BeanFactoryAware;
import cn.taketoday.context.factory.BeanFactory;
import cn.taketoday.context.factory.FactoryBean;
import cn.taketoday.context.utils.ClassUtils;
import cn.taketoday.context.utils.ObjectUtils;

/**
 * @author TODAY 2021/2/20 21:23
 * @since 3.0
 */
public class ProxyFactoryBean
        extends ProxyCreatorSupport implements FactoryBean<Object>, BeanFactoryAware, BeanClassLoaderAware {
  private static final long serialVersionUID = 1L;

  private String targetName;
  private String[] interceptorNames;
  private BeanFactory beanFactory;
  private Set<Class<?>> proxyInterfaces;
  private ClassLoader classLoader = ClassUtils.getClassLoader();

  public ClassLoader getClassLoader() {
    return classLoader;
  }

  public void setClassLoader(ClassLoader classLoader) {
    this.classLoader = classLoader;
  }

  public String getTargetName() {
    return targetName;
  }

  public String[] getInterceptorNames() {
    return interceptorNames;
  }

  public Set<Class<?>> getProxyInterfaces() {
    return proxyInterfaces;
  }

  public void setTargetName(String targetName) {
    this.targetName = targetName;
  }

  public void setInterceptorNames(String... interceptorNames) {
    this.interceptorNames = interceptorNames;
  }

  public void setProxyInterfaces(Set<Class<?>> proxyInterfaces) {
    this.proxyInterfaces = proxyInterfaces;
  }

  public BeanFactory getBeanFactory() {
    return beanFactory;
  }

  @Override
  public Object getBean() {
    final BeanFactory beanFactory = getBeanFactory();

    final ProxyFactory proxyFactory = new ProxyFactory();
    proxyFactory.copyFrom(this);
    proxyFactory.setTargetSource(getTargetSource());

    if (ObjectUtils.isNotEmpty(interceptorNames)) {
      for (final String interceptorName : interceptorNames) {
        final Object interceptor = beanFactory.getBean(interceptorName);
        if (interceptor == null) {
          throw new AopConfigException(
                  "Cannot determine an Advice or Advisor by interceptorName '" + interceptorName + "' from beanFactory:" + beanFactory);
        }
        final Advisor wrap = AopUtils.wrap(interceptor);
        proxyFactory.addAdvisor(wrap);
      }
    }

    return proxyFactory.getProxy(classLoader);
  }

  public TargetSource getTargetSource() {
    final Object target = beanFactory.getBean(targetName);
    return (target instanceof TargetSource ? (TargetSource) target : new SingletonTargetSource(target));
  }

  @Override
  public Class<Object> getBeanClass() {
    return null;
  }

  @Override
  public void setBeanFactory(BeanFactory beanFactory) {
    this.beanFactory = beanFactory;
  }

  @Override
  public void setBeanClassLoader(ClassLoader classLoader) {
    this.classLoader = classLoader;
  }
}
