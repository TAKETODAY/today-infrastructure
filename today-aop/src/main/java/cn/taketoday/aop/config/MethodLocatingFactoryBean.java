/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
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

import java.lang.reflect.Method;

import cn.taketoday.beans.BeanUtils;
import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.beans.factory.BeanFactoryAware;
import cn.taketoday.beans.factory.FactoryBean;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.StringUtils;

/**
 * {@link FactoryBean} implementation that locates a {@link Method} on a specified bean.
 *
 * @author Rob Harrop
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/3/7 21:27
 */
public class MethodLocatingFactoryBean implements FactoryBean<Method>, BeanFactoryAware {

  @Nullable
  private String targetBeanName;

  @Nullable
  private String methodName;

  @Nullable
  private Method method;

  /**
   * Set the name of the bean to locate the {@link Method} on.
   * <p>This property is required.
   *
   * @param targetBeanName the name of the bean to locate the {@link Method} on
   */
  public void setTargetBeanName(String targetBeanName) {
    this.targetBeanName = targetBeanName;
  }

  /**
   * Set the name of the {@link Method} to locate.
   * <p>This property is required.
   *
   * @param methodName the name of the {@link Method} to locate
   */
  public void setMethodName(String methodName) {
    this.methodName = methodName;
  }

  @Override
  public void setBeanFactory(BeanFactory beanFactory) {
    if (StringUtils.isBlank(this.targetBeanName)) {
      throw new IllegalArgumentException("Property 'targetBeanName' is required");
    }
    if (StringUtils.isBlank(this.methodName)) {
      throw new IllegalArgumentException("Property 'methodName' is required");
    }

    Class<?> beanClass = beanFactory.getType(this.targetBeanName);
    if (beanClass == null) {
      throw new IllegalArgumentException("Can't determine type of bean with name '" + this.targetBeanName + "'");
    }
    this.method = BeanUtils.resolveSignature(this.methodName, beanClass);

    if (this.method == null) {
      throw new IllegalArgumentException("Unable to locate method [" + this.methodName +
              "] on bean [" + this.targetBeanName + "]");
    }
  }

  @Override
  @Nullable
  public Method getObject() throws Exception {
    return this.method;
  }

  @Override
  public Class<Method> getObjectType() {
    return Method.class;
  }

  @Override
  public boolean isSingleton() {
    return true;
  }

}
