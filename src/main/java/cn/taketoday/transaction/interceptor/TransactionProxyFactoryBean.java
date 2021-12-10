/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
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

package cn.taketoday.transaction.interceptor;

import java.util.Properties;

import cn.taketoday.aop.Pointcut;
import cn.taketoday.aop.proxy.AbstractSingletonProxyFactoryBean;
import cn.taketoday.aop.proxy.ProxyFactory;
import cn.taketoday.aop.support.DefaultPointcutAdvisor;
import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.beans.factory.BeanFactoryAware;
import cn.taketoday.beans.factory.FactoryBean;
import cn.taketoday.lang.Nullable;
import cn.taketoday.transaction.PlatformTransactionManager;

/**
 * Proxy factory bean for simplified declarative transaction handling.
 * This is a convenient alternative to a standard AOP
 * {@link cn.taketoday.aop.proxy.ProxyFactoryBean}
 * with a separate {@link TransactionInterceptor} definition.
 *
 * <p><strong>HISTORICAL NOTE:</strong> This class was originally designed to cover the
 * typical case of declarative transaction demarcation: namely, wrapping a singleton
 * target object with a transactional proxy, proxying all the interfaces that the target
 * implements. However, in Spring versions 2.0 and beyond, the functionality provided here
 * is superseded by the more convenient {@code tx:} XML namespace. See the
 * <a href="https://docs.spring.io/spring/docs/current/spring-framework-reference/data-access.html#transaction-declarative">declarative transaction management</a>
 * section of the Spring reference documentation to understand modern options for managing
 * transactions in Spring applications. For these reasons, <strong>users should favor
 * the {@code tx:} XML namespace as well as
 * the @{@link cn.taketoday.transaction.annotation.Transactional Transactional}
 * and @{@link cn.taketoday.transaction.annotation.EnableTransactionManagement
 * EnableTransactionManagement} annotations.</strong>
 *
 * <p>There are three main properties that need to be specified:
 * <ul>
 * <li>"transactionManager": the {@link PlatformTransactionManager} implementation to use
 * (for example, a {@link cn.taketoday.transaction.jta.JtaTransactionManager} instance)
 * <li>"target": the target object that a transactional proxy should be created for
 * <li>"transactionAttributes": the transaction attributes (for example, propagation
 * behavior and "readOnly" flag) per target method name (or method name pattern)
 * </ul>
 *
 * <p>If the "transactionManager" property is not set explicitly and this {@link FactoryBean}
 * is running in a {@link BeanFactory}, a single matching bean of type
 * {@link PlatformTransactionManager} will be fetched from the {@link BeanFactory}.
 *
 * <p>In contrast to {@link TransactionInterceptor}, the transaction attributes are
 * specified as properties, with method names as keys and transaction attribute
 * descriptors as values. Method names are always applied to the target class.
 *
 * <p>Internally, a {@link TransactionInterceptor} instance is used, but the user of this
 * class does not have to care. Optionally, a method pointcut can be specified
 * to cause conditional invocation of the underlying {@link TransactionInterceptor}.
 *
 * <p>The "preInterceptors" and "postInterceptors" properties can be set to add
 * additional interceptors to the mix, like
 * {@link cn.taketoday.aop.support.interceptor.PerformanceMonitorInterceptor}.
 *
 * <p><b>HINT:</b> This class is often used with parent / child bean definitions.
 * Typically, you will define the transaction manager and default transaction
 * attributes (for method name patterns) in an abstract parent bean definition,
 * deriving concrete child bean definitions for specific target objects.
 * This reduces the per-bean definition effort to a minimum.
 *
 * <pre class="code">
 * &lt;bean id="baseTransactionProxy" class="cn.taketoday.transaction.interceptor.TransactionProxyFactoryBean"
 *     abstract="true"&gt;
 *   &lt;property name="transactionManager" ref="transactionManager"/&gt;
 *   &lt;property name="transactionAttributes"&gt;
 *     &lt;props&gt;
 *       &lt;prop key="insert*"&gt;PROPAGATION_REQUIRED&lt;/prop&gt;
 *       &lt;prop key="update*"&gt;PROPAGATION_REQUIRED&lt;/prop&gt;
 *       &lt;prop key="*"&gt;PROPAGATION_REQUIRED,readOnly&lt;/prop&gt;
 *     &lt;/props&gt;
 *   &lt;/property&gt;
 * &lt;/bean&gt;
 *
 * &lt;bean id="myProxy" parent="baseTransactionProxy"&gt;
 *   &lt;property name="target" ref="myTarget"/&gt;
 * &lt;/bean&gt;
 *
 * &lt;bean id="yourProxy" parent="baseTransactionProxy"&gt;
 *   &lt;property name="target" ref="yourTarget"/&gt;
 * &lt;/bean&gt;</pre>
 *
 * @author Juergen Hoeller
 * @author Dmitriy Kopylenko
 * @author Rod Johnson
 * @author Chris Beams
 * @see #setTransactionManager
 * @see #setTarget
 * @see #setTransactionAttributes
 * @see TransactionInterceptor
 * @see cn.taketoday.aop.proxy.ProxyFactoryBean
 * @since 21.08.2003
 */
@SuppressWarnings("serial")
public class TransactionProxyFactoryBean extends AbstractSingletonProxyFactoryBean
        implements BeanFactoryAware {

  private final TransactionInterceptor transactionInterceptor = new TransactionInterceptor();

  @Nullable
  private Pointcut pointcut;

  /**
   * Set the default transaction manager. This will perform actual
   * transaction management: This class is just a way of invoking it.
   *
   * @see TransactionInterceptor#setTransactionManager
   */
  public void setTransactionManager(PlatformTransactionManager transactionManager) {
    this.transactionInterceptor.setTransactionManager(transactionManager);
  }

  /**
   * Set properties with method names as keys and transaction attribute
   * descriptors (parsed via TransactionAttributeEditor) as values:
   * e.g. key = "myMethod", value = "PROPAGATION_REQUIRED,readOnly".
   * <p>Note: Method names are always applied to the target class,
   * no matter if defined in an interface or the class itself.
   * <p>Internally, a NameMatchTransactionAttributeSource will be
   * created from the given properties.
   *
   * @see #setTransactionAttributeSource
   * @see TransactionInterceptor#setTransactionAttributes
   * @see TransactionAttributeEditor
   * @see NameMatchTransactionAttributeSource
   */
  public void setTransactionAttributes(Properties transactionAttributes) {
    this.transactionInterceptor.setTransactionAttributes(transactionAttributes);
  }

  /**
   * Set the transaction attribute source which is used to find transaction
   * attributes. If specifying a String property value, a PropertyEditor
   * will create a MethodMapTransactionAttributeSource from the value.
   *
   * @see #setTransactionAttributes
   * @see TransactionInterceptor#setTransactionAttributeSource
   * @see TransactionAttributeSourceEditor
   * @see MethodMapTransactionAttributeSource
   * @see NameMatchTransactionAttributeSource
   * @see cn.taketoday.transaction.annotation.AnnotationTransactionAttributeSource
   */
  public void setTransactionAttributeSource(TransactionAttributeSource transactionAttributeSource) {
    this.transactionInterceptor.setTransactionAttributeSource(transactionAttributeSource);
  }

  /**
   * Set a pointcut, i.e a bean that can cause conditional invocation
   * of the TransactionInterceptor depending on method and attributes passed.
   * Note: Additional interceptors are always invoked.
   *
   * @see #setPreInterceptors
   * @see #setPostInterceptors
   */
  public void setPointcut(Pointcut pointcut) {
    this.pointcut = pointcut;
  }

  /**
   * This callback is optional: If running in a BeanFactory and no transaction
   * manager has been set explicitly, a single matching bean of type
   * {@link PlatformTransactionManager} will be fetched from the BeanFactory.
   *
   * @see cn.taketoday.beans.factory.BeanFactory#getBean(Class)
   * @see PlatformTransactionManager
   */
  @Override
  public void setBeanFactory(BeanFactory beanFactory) {
    this.transactionInterceptor.setBeanFactory(beanFactory);
  }

  /**
   * Creates an advisor for this FactoryBean's TransactionInterceptor.
   */
  @Override
  protected Object createMainInterceptor() {
    this.transactionInterceptor.afterPropertiesSet();
    if (this.pointcut != null) {
      return new DefaultPointcutAdvisor(this.pointcut, this.transactionInterceptor);
    }
    else {
      // Rely on default pointcut.
      return new TransactionAttributeSourceAdvisor(this.transactionInterceptor);
    }
  }

  /**
   * As of 4.2, this method adds {@link TransactionalProxy} to the set of
   * proxy interfaces in order to avoid re-processing of transaction metadata.
   */
  @Override
  protected void postProcessProxyFactory(ProxyFactory proxyFactory) {
    proxyFactory.addInterface(TransactionalProxy.class);
  }

}
