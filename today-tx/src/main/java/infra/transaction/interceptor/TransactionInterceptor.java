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

package infra.transaction.interceptor;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.io.Serializable;
import java.util.Properties;

import infra.aop.framework.ProxyFactory;
import infra.aop.framework.ProxyFactoryBean;
import infra.aop.support.AopUtils;
import infra.beans.factory.BeanFactory;
import infra.transaction.PlatformTransactionManager;
import infra.transaction.TransactionManager;

/**
 * AOP Alliance MethodInterceptor for declarative transaction
 * management using the common Framework transaction infrastructure
 * ({@link PlatformTransactionManager}/
 * {@link infra.transaction.ReactiveTransactionManager}).
 *
 * <p>Derives from the {@link TransactionAspectSupport} class which
 * contains the integration with Framework's underlying transaction API.
 * TransactionInterceptor simply calls the relevant superclass methods
 * such as {@link #invokeWithinTransaction} in the correct order.
 *
 * <p>TransactionInterceptors are thread-safe.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Sebastien Deleuze
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see TransactionProxyFactoryBean
 * @see ProxyFactoryBean
 * @see ProxyFactory
 */
@SuppressWarnings("serial")
public class TransactionInterceptor extends TransactionAspectSupport implements MethodInterceptor, Serializable {

  /**
   * Create a new TransactionInterceptor.
   * <p>Transaction manager and transaction attributes still need to be set.
   *
   * @see #setTransactionManager
   * @see #setTransactionAttributes(java.util.Properties)
   * @see #setTransactionAttributeSource(TransactionAttributeSource)
   */
  public TransactionInterceptor() { }

  /**
   * Create a new TransactionInterceptor.
   *
   * @param ptm the default transaction manager to perform the actual transaction management
   * @param tas the attribute source to be used to find transaction attributes
   * @see #setTransactionManager
   * @see #setTransactionAttributeSource
   */
  public TransactionInterceptor(TransactionManager ptm, TransactionAttributeSource tas) {
    setTransactionManager(ptm);
    setTransactionAttributeSource(tas);
  }

  /**
   * Create a new TransactionInterceptor.
   *
   * @param ptm the default transaction manager to perform the actual transaction management
   * @param tas the attribute source to be used to find transaction attributes
   * @see #setTransactionManager
   * @see #setTransactionAttributeSource
   */
  public TransactionInterceptor(PlatformTransactionManager ptm, TransactionAttributeSource tas) {
    setTransactionManager(ptm);
    setTransactionAttributeSource(tas);
  }

  /**
   * Create a new TransactionInterceptor.
   *
   * @param ptm the default transaction manager to perform the actual transaction management
   * @param attributes the transaction attributes in properties format
   * @see #setTransactionManager
   * @see #setTransactionAttributes(java.util.Properties)
   */
  public TransactionInterceptor(PlatformTransactionManager ptm, Properties attributes) {
    setTransactionManager(ptm);
    setTransactionAttributes(attributes);
  }

  @Override
  @Nullable
  public Object invoke(MethodInvocation invocation) throws Throwable {
    // Work out the target class: may be {@code null}.
    // The TransactionAttributeSource should be passed the target class
    // as well as the method, which may be from an interface.
    Class<?> targetClass = invocation.getThis() != null ? AopUtils.getTargetClass(invocation.getThis()) : null;

    // Adapt to TransactionAspectSupport's invokeWithinTransaction...
    return invokeWithinTransaction(invocation.getMethod(), targetClass, invocation::proceed);
  }

  //---------------------------------------------------------------------
  // Serialization support
  //---------------------------------------------------------------------

  @Serial
  private void writeObject(ObjectOutputStream oos) throws IOException {
    // Rely on default serialization, although this class itself doesn't carry state anyway...
    oos.defaultWriteObject();

    // Deserialize superclass fields.
    oos.writeObject(getTransactionManagerBeanName());
    oos.writeObject(getTransactionManager());
    oos.writeObject(getTransactionAttributeSource());
    oos.writeObject(getBeanFactory());
  }

  @Serial
  private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
    // Rely on default serialization, although this class itself doesn't carry state anyway...
    ois.defaultReadObject();

    // Serialize all relevant superclass fields.
    // Superclass can't implement Serializable because it also serves as base class
    // for AspectJ aspects (which are not allowed to implement Serializable)!
    setTransactionManagerBeanName((String) ois.readObject());
    setTransactionManager((PlatformTransactionManager) ois.readObject());
    setTransactionAttributeSource((TransactionAttributeSource) ois.readObject());
    setBeanFactory((BeanFactory) ois.readObject());
  }

}
