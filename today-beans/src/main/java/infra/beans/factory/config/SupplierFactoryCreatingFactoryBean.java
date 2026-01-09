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
import java.util.function.Supplier;

import infra.beans.BeansException;
import infra.beans.factory.BeanFactory;
import infra.beans.factory.FactoryBean;
import infra.lang.Assert;

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
 *       class="infra.beans.factory.config.SupplierFactoryCreatingFactoryBean"&gt;
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
    @SuppressWarnings("NullAway")
    public Object get() throws BeansException {
      return beanFactory.getBean(targetBeanName);
    }
  }

}
