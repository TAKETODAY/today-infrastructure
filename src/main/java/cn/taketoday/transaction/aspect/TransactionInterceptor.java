/**
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2020 All Rights Reserved.
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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cn.taketoday.transaction.aspect;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import cn.taketoday.aop.support.annotation.Advice;
import cn.taketoday.aop.support.annotation.Aspect;
import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.beans.factory.NoSuchBeanDefinitionException;
import cn.taketoday.beans.factory.ObjectSupplier;
import cn.taketoday.lang.Autowired;
import cn.taketoday.core.AnnotationAttributes;
import cn.taketoday.lang.Assert;
import cn.taketoday.core.ConfigurationException;
import cn.taketoday.core.NamedThreadLocal;
import cn.taketoday.core.Order;
import cn.taketoday.core.Ordered;
import cn.taketoday.core.annotation.AnnotationUtils;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.transaction.DefaultTransactionDefinition;
import cn.taketoday.transaction.NoTransactionException;
import cn.taketoday.transaction.TransactionDefinition;
import cn.taketoday.transaction.TransactionManager;
import cn.taketoday.transaction.TransactionStatus;
import cn.taketoday.transaction.TransactionSystemException;
import cn.taketoday.transaction.Transactional;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.StringUtils;

/**
 * @author TODAY <br>
 * 2018-11-12 21:09
 */
@Aspect
@Advice(Transactional.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TransactionInterceptor implements MethodInterceptor {

  private static final Logger log = LoggerFactory.getLogger(TransactionInterceptor.class);

  private final BeanFactory beanFactory;
  private final ObjectSupplier<TransactionManager> transactionManager; // lazy load

  private static final Map<Object, TransactionDefinition> DEF_CACHE = new HashMap<>(1024);
  private static final ThreadLocal<TransactionStatus> TRANSACTION
          = new NamedThreadLocal<>("Current Transaction Status");

  @Autowired
  public TransactionInterceptor(
          ObjectSupplier<TransactionManager> transactionManager, BeanFactory beanFactory) {
    this.beanFactory = beanFactory;
    this.transactionManager = transactionManager;
  }

  /**
   * Return the transaction status of the current method invocation. Mainly
   * intended for code that wants to set the current transaction rollback-only but
   * not throw an application exception.
   *
   * @throws NoTransactionException
   *         if the transaction info cannot be found, because the method was
   *         invoked outside an AOP invocation context
   */
  public static TransactionStatus currentTransactionStatus() throws NoTransactionException {
    TransactionStatus metaData = TRANSACTION.get();
    if (metaData == null) {
      throw new NoTransactionException("No transaction aspect-managed TransactionStatus in scope");
    }
    return metaData;
  }

  @Override
  public Object invoke(final MethodInvocation invocation) throws Throwable {

    final TransactionDefinition def = obtainDefinition(invocation.getMethod());
    final TransactionManager tm = obtainTransactionManager(def);
    final TransactionStatus status = tm.getTransaction(def);
    final TransactionStatus old = bindToThread(status);

    try {
      final Object ret = invocation.proceed();
      if (status != null) {
        tm.commit(status);
      }
      return ret;
    }
    catch (final Throwable e) {
      completeTransactionAfterThrowing(def, status, tm, e);
      throw e;
    }
    finally {
      if (old == null) {
        TRANSACTION.remove();
      }
      else {
        restoreThreadLocalStatus(old);
      }
    }
  }

  static TransactionStatus bindToThread(TransactionStatus status) {

    final ThreadLocal<TransactionStatus> local = TRANSACTION;
    TransactionStatus old = local.get();
    local.set(status);
    return old;
  }

  static void restoreThreadLocalStatus(TransactionStatus old) {
    TRANSACTION.set(old);
  }

  /**
   * Handle a throwable, completing the transaction. We may commit or roll back,
   * depending on the configuration.
   *
   * @param ex
   *         throwable encountered
   */
  static void completeTransactionAfterThrowing(final TransactionDefinition def,
                                               final TransactionStatus transactionStatus,
                                               final TransactionManager tm, final Throwable ex) {
    if (transactionStatus != null) {
      if (log.isTraceEnabled()) {
        log.trace("Completing transaction for [{}] after exception: [{}] ", def.getName(), ex, ex);
      }
      if (def.rollbackOn(ex)) {
        try {
          tm.rollback(transactionStatus);
        }
        catch (TransactionSystemException ex2) {
          log.error("Application exception overridden by rollback exception", ex);
          ex2.initApplicationException(ex);
          throw ex2;
        }
        catch (RuntimeException | Error ex2) {
          log.error("Application exception overridden by rollback exception", ex);
          throw ex2;
        }
      }
      else {
        try {
          // We don't roll back on this exception.
          // Will still roll back if TransactionStatus.isRollbackOnly() is true.
          tm.commit(transactionStatus);
        }
        catch (TransactionSystemException ex2) {
          log.error("Application exception overridden by commit exception", ex);
          ex2.initApplicationException(ex);
          throw ex2;
        }
        catch (RuntimeException | Error ex2) {
          log.error("Application exception overridden by commit exception", ex);
          throw ex2;
        }
      }
    }
  }

  static TransactionDefinition obtainDefinition(final Method method) {
    TransactionDefinition ret = DEF_CACHE.get(method);
    if (ret == null) {
      ret = getTransaction(method);
      DEF_CACHE.put(method, ret);
      if (log.isTraceEnabled()) {
        log.trace("Adding transactional method '{}' with attribute: {}", ret.getName(), ret);
      }
    }
    return ret;
  }

  static TransactionDefinition getTransaction(Method method) {
    AnnotationAttributes attributes = AnnotationUtils.getAttributes(Transactional.class, method);
    if (attributes == null) {
      attributes = AnnotationUtils.getAttributes(Transactional.class, method.getDeclaringClass());
    }
    if (attributes == null) {
      throw new ConfigurationException(
              "'cn.taketoday.transaction.Transactional' must present on: ["
                      + method + "] or on its class");
    }

    return new DefaultTransactionDefinition(attributes).setName(ClassUtils.getQualifiedMethodName(method));
  }

  protected TransactionManager obtainTransactionManager(TransactionDefinition definition) {

    if (definition != null && beanFactory != null) {
      final String qualifier = definition.getQualifier();
      if (StringUtils.isNotEmpty(qualifier)) {
        return obtainQualifiedTransactionManager(beanFactory, qualifier);
      }
    }

    TransactionManager ret = transactionManager.getIfAvailable();
    Assert.state(ret != null, "No TransactionManager.");
    return ret;
  }

  static TransactionManager obtainQualifiedTransactionManager(BeanFactory beanFactory, String qualifier) {
    final TransactionManager ret = beanFactory.getBean(qualifier, TransactionManager.class);
    if (ret == null) {
      throw new NoSuchBeanDefinitionException(qualifier, TransactionManager.class);
    }
    return ret;
  }

  // -----------------------

  public final BeanFactory getBeanFactory() {
    return beanFactory;
  }

  public TransactionManager getTransactionManager() {
    return transactionManager.get();
  }
}
