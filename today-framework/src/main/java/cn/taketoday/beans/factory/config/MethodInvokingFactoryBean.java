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

import cn.taketoday.beans.factory.FactoryBean;
import cn.taketoday.beans.factory.FactoryBeanNotInitializedException;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ReflectiveMethodInvoker;

/**
 * {@link FactoryBean} which returns a value which is the result of a static or instance
 * method invocation. For most use cases it is better to just use the container's
 * built-in factory method support for the same purpose, since that is smarter at
 * converting arguments. This factory bean is still useful though when you need to
 * call a method which doesn't return any value (for example, a static class method
 * to force some sort of initialization to happen). This use case is not supported
 * by factory methods, since a return value is needed to obtain the bean instance.
 *
 * <p>Note that as it is expected to be used mostly for accessing factory methods,
 * this factory by default operates in a <b>singleton</b> fashion. The first request
 * to {@link #getObject} by the owning bean factory will cause a method invocation,
 * whose return value will be cached for subsequent requests. An internal
 * {@link #setSingleton singleton} property may be set to "false", to cause this
 * factory to invoke the target method each time it is asked for an object.
 *
 * <p><b>NOTE: If your target method does not produce a result to expose, consider
 * {@link MethodInvokingBean} instead, which avoids the type determination and
 * lifecycle limitations that this {@link MethodInvokingFactoryBean} comes with.</b>
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
 * which uses this class to call a static factory method:
 *
 * <pre class="code">
 * &lt;bean id="myObject" class="cn.taketoday.beans.factory.config.MethodInvokingFactoryBean"&gt;
 *   &lt;property name="staticMethod" value="com.whatever.MyClassFactory.getInstance"/&gt;
 * &lt;/bean&gt;</pre>
 *
 * <p>An example of calling a static method then an instance method to get at a
 * Java system property. Somewhat verbose, but it works.
 *
 * <pre class="code">
 * &lt;bean id="sysProps" class="cn.taketoday.beans.factory.config.MethodInvokingFactoryBean"&gt;
 *   &lt;property name="targetClass" value="java.lang.System"/&gt;
 *   &lt;property name="targetMethod" value="getProperties"/&gt;
 * &lt;/bean&gt;
 *
 * &lt;bean id="javaVersion" class="cn.taketoday.beans.factory.config.MethodInvokingFactoryBean"&gt;
 *   &lt;property name="targetObject" ref="sysProps"/&gt;
 *   &lt;property name="targetMethod" value="getProperty"/&gt;
 *   &lt;property name="arguments" value="java.version"/&gt;
 * &lt;/bean&gt;</pre>
 *
 * @author Colin Sampaleanu
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see MethodInvokingBean
 * @see ReflectiveMethodInvoker
 * @since 4.0 2022/2/18 11:14
 */
public class MethodInvokingFactoryBean extends MethodInvokingBean implements FactoryBean<Object> {

  private boolean singleton = true;

  private boolean initialized = false;

  /** Method call result in the singleton case. */
  @Nullable
  private Object singletonObject;

  /**
   * Set if a singleton should be created, or a new object on each
   * {@link #getObject()} request otherwise. Default is "true".
   */
  public void setSingleton(boolean singleton) {
    this.singleton = singleton;
  }

  @Override
  public void afterPropertiesSet() throws Exception {
    prepare();
    if (this.singleton) {
      this.initialized = true;
      this.singletonObject = invokeWithTargetException();
    }
  }

  /**
   * Returns the same value each time if the singleton property is set
   * to "true", otherwise returns the value returned from invoking the
   * specified method on the fly.
   */
  @Override
  @Nullable
  public Object getObject() throws Exception {
    if (this.singleton) {
      if (!this.initialized) {
        throw new FactoryBeanNotInitializedException();
      }
      // Singleton: return shared object.
      return this.singletonObject;
    }
    else {
      // Prototype: new object on each call.
      return invokeWithTargetException();
    }
  }

  /**
   * Return the type of object that this FactoryBean creates,
   * or {@code null} if not known in advance.
   */
  @Override
  public Class<?> getObjectType() {
    if (!isPrepared()) {
      // Not fully initialized yet -> return null to indicate "not known yet".
      return null;
    }
    return getPreparedMethod().getReturnType();
  }

  @Override
  public boolean isSingleton() {
    return this.singleton;
  }

}
