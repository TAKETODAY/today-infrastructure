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
import java.util.function.Supplier;

import cn.taketoday.beans.BeansException;
import cn.taketoday.beans.factory.config.AbstractFactoryBean;
import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.beans.factory.BeanFactoryUtils;
import cn.taketoday.beans.factory.FactoryBean;
import cn.taketoday.beans.factory.config.ServiceLocatorFactoryBean;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;

/**
 * A {@link FactoryBean} implementation that returns a value which is an
 * {@link Supplier} that in turn returns a bean sourced from a {@link BeanFactory}.
 *
 * <p>As such, this may be used to avoid having a client object directly calling
 * {@link BeanFactory#getBean(String)} to get
 * a (typically prototype) bean from a {@link BeanFactory}, which would be a
 * violation of the inversion of control principle. Instead, with the use
 * of this class, the client object can be fed an {@link Supplier} instance as a
 * property which directly returns only the one target bean (again, which is
 * typically a prototype bean).
 *
 * <p>A sample config in an XML-based {@link BeanFactory} might look as follows:
 *
 * <pre class="code">&lt;beans&gt;
 *
 *   &lt;!-- Prototype bean since we have state --&gt;
 *   &lt;bean id="myService" class="a.b.c.MyService" scope="prototype"/&gt;
 *
 *   &lt;bean id="myServiceFactory"
 *       class="cn.taketoday.beans.factory.config.SupplierFactoryCreatingFactoryBean"&gt;
 *     &lt;property name="targetBeanName"&gt;&lt;idref local="myService"/&gt;&lt;/property&gt;
 *   &lt;/bean&gt;
 *
 *   &lt;bean id="clientBean" class="a.b.c.MyClientBean"&gt;
 *     &lt;property name="myServiceFactory" ref="myServiceFactory"/&gt;
 *   &lt;/bean&gt;
 *
 * &lt;/beans&gt;</pre>
 *
 * <p>The attendant {@code MyClientBean} class implementation might look
 * something like this:
 *
 * <pre class="code">package a.b.c;
 *
 * import java.util.function.Supplier;
 *
 * public class MyClientBean {
 *
 *   private ObjectFactory&lt;MyService&gt; myServiceFactory;
 *
 *   public void setMyServiceFactory(ObjectFactory&lt;MyService&gt; myServiceFactory) {
 *     this.myServiceFactory = myServiceFactory;
 *   }
 *
 *   public void someBusinessMethod() {
 *     // get a 'fresh', brand new MyService instance
 *     MyService service = this.myServiceFactory.getObject();
 *     // use the service object to effect the business logic...
 *   }
 * }</pre>
 *
 * <p>An alternate approach to this application of an object creation pattern
 * would be to use the {@link ServiceLocatorFactoryBean}
 * to source (prototype) beans. The {@link ServiceLocatorFactoryBean} approach
 * has the advantage of the fact that one doesn't have to depend on any
 * interface such as {@link java.util.function.Supplier},
 * but has the disadvantage of requiring runtime class generation. Please do
 * consult the {@link ServiceLocatorFactoryBean ServiceLocatorFactoryBean JavaDoc}
 * for a fuller discussion of this issue.
 *
 * @author Colin Sampaleanu
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see Supplier
 * @see ServiceLocatorFactoryBean
 * @since 4.0 2021/11/30 14:24
 */
public class SupplierFactoryCreatingFactoryBean extends AbstractFactoryBean<Supplier<Object>> {

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
    return Supplier.class;
  }

  @Override
  protected Supplier<Object> createBeanInstance() {
    BeanFactory beanFactory = getBeanFactory();
    Assert.state(beanFactory != null, "No BeanFactory available");
    Assert.state(this.targetBeanName != null, "No target bean name specified");
    return new TargetBeanObjectFactory(beanFactory, this.targetBeanName);
  }

  /**
   * Independent inner class - for serialization purposes.
   */
  private record TargetBeanObjectFactory(BeanFactory beanFactory, String targetBeanName)
          implements Supplier<Object>, Serializable {

    @Override
    public Object get() throws BeansException {
      return BeanFactoryUtils.requiredBean(beanFactory, targetBeanName);
    }
  }

}
