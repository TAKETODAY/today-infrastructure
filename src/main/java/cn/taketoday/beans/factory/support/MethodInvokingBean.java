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

package cn.taketoday.beans.factory.support;

import java.lang.reflect.InvocationTargetException;

import cn.taketoday.beans.TypeConverter;
import cn.taketoday.beans.factory.BeanClassLoaderAware;
import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.beans.factory.BeanFactoryAware;
import cn.taketoday.beans.factory.InitializingBean;
import cn.taketoday.beans.support.ArgumentConvertingMethodInvoker;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.ReflectiveMethodInvoker;

/**
 * Simple method invoker bean: just invoking a target method, not expecting a result
 * to expose to the container (in contrast to {@link MethodInvokingFactoryBean}).
 *
 * <p>This invoker supports any kind of target method. A static method may be specified
 * by setting the {@link #setTargetMethod targetMethod} property to a String representing
 * the static method name, with {@link #setTargetClass targetClass} specifying the Class
 * that the static method is defined on. Alternatively, a target instance method may be
 * specified, by setting the {@link #setTargetObject targetObject} property as the target
 * object, and the {@link #setTargetMethod targetMethod} property as the name of the
 * method to call on that target object. Arguments for the method invocation may be
 * specified by setting the {@link #setArguments arguments} property.
 *
 * <p>This class depends on {@link #afterPropertiesSet()} being called once
 * all properties have been set, as per the InitializingBean contract.
 *
 * <p>An example (in an XML based bean factory definition) of a bean definition
 * which uses this class to call a static initialization method:
 *
 * <pre class="code">
 * &lt;bean id="myObject" class="cn.taketoday.beans.factory.support.MethodInvokingBean"&gt;
 *   &lt;property name="staticMethod" value="com.whatever.MyClass.init"/&gt;
 * &lt;/bean&gt;</pre>
 *
 * <p>An example of calling an instance method to start some server bean:
 *
 * <pre class="code">
 * &lt;bean id="myStarter" class="cn.taketoday.beans.factory.support.MethodInvokingBean"&gt;
 *   &lt;property name="targetObject" ref="myServer"/&gt;
 *   &lt;property name="targetMethod" value="start"/&gt;
 * &lt;/bean&gt;</pre>
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see MethodInvokingFactoryBean
 * @see ReflectiveMethodInvoker
 * @since 4.0 2022/2/18 11:13
 */
public class MethodInvokingBean extends ArgumentConvertingMethodInvoker
        implements BeanClassLoaderAware, BeanFactoryAware, InitializingBean {

  @Nullable
  private ClassLoader beanClassLoader = ClassUtils.getDefaultClassLoader();

  @Nullable
  private ConfigurableBeanFactory beanFactory;

  @Override
  public void setBeanClassLoader(ClassLoader classLoader) {
    this.beanClassLoader = classLoader;
  }

  @Override
  protected Class<?> resolveClassName(String className) throws ClassNotFoundException {
    return ClassUtils.forName(className, this.beanClassLoader);
  }

  @Override
  public void setBeanFactory(BeanFactory beanFactory) {
    if (beanFactory instanceof ConfigurableBeanFactory) {
      this.beanFactory = (ConfigurableBeanFactory) beanFactory;
    }
  }

  /**
   * Obtain the TypeConverter from the BeanFactory that this bean runs in,
   * if possible.
   *
   * @see ConfigurableBeanFactory#getTypeConverter()
   */
  @Override
  protected TypeConverter getDefaultTypeConverter() {
    if (this.beanFactory != null) {
      return this.beanFactory.getTypeConverter();
    }
    else {
      return super.getDefaultTypeConverter();
    }
  }

  @Override
  public void afterPropertiesSet() throws Exception {
    prepare();
    invokeWithTargetException();
  }

  /**
   * Perform the invocation and convert InvocationTargetException
   * into the underlying target exception.
   */
  @Nullable
  protected Object invokeWithTargetException() throws Exception {
    try {
      return invoke();
    }
    catch (InvocationTargetException ex) {
      if (ex.getTargetException() instanceof Exception) {
        throw (Exception) ex.getTargetException();
      }
      if (ex.getTargetException() instanceof Error) {
        throw (Error) ex.getTargetException();
      }
      throw ex;
    }
  }

}
