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

package cn.taketoday.test.context.transaction;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import cn.taketoday.beans.BeansException;
import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.beans.factory.annotation.BeanFactoryAnnotationUtils;
import cn.taketoday.core.annotation.AnnotatedElementUtils;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.test.context.support.AbstractTestExecutionListener;
import cn.taketoday.test.annotation.Commit;
import cn.taketoday.test.annotation.Rollback;
import cn.taketoday.test.context.TestContext;
import cn.taketoday.test.context.TestContextAnnotationUtils;
import cn.taketoday.transaction.PlatformTransactionManager;
import cn.taketoday.transaction.TransactionDefinition;
import cn.taketoday.transaction.TransactionStatus;
import cn.taketoday.transaction.annotation.AnnotationTransactionAttributeSource;
import cn.taketoday.transaction.annotation.Transactional;
import cn.taketoday.transaction.interceptor.TransactionAttribute;
import cn.taketoday.transaction.interceptor.TransactionAttributeSource;
import cn.taketoday.util.ReflectionUtils;
import cn.taketoday.util.ReflectionUtils.MethodFilter;
import cn.taketoday.util.StringUtils;

/**
 * {@code TestExecutionListener} that provides support for executing tests
 * within <em>test-managed transactions</em> by honoring Framework's
 * {@link Transactional @Transactional} annotation.
 *
 * <h3>Test-managed Transactions</h3>
 * <p><em>Test-managed transactions</em> are transactions that are managed
 * declaratively via this listener or programmatically via
 * {@link TestTransaction}. Such transactions should not be confused with
 * <em>Spring-managed transactions</em> (i.e., those managed directly
 * by Spring within the {@code ApplicationContext} loaded for tests) or
 * <em>application-managed transactions</em> (i.e., those managed
 * programmatically within application code that is invoked via tests).
 * Spring-managed and application-managed transactions will typically
 * participate in test-managed transactions; however, caution should be
 * taken if Spring-managed or application-managed transactions are
 * configured with any propagation type other than
 * {@link cn.taketoday.transaction.annotation.Propagation#REQUIRED REQUIRED}
 * or {@link cn.taketoday.transaction.annotation.Propagation#SUPPORTS SUPPORTS}.
 *
 * <h3>Enabling and Disabling Transactions</h3>
 * <p>Annotating a test method with {@code @Transactional} causes the test
 * to be run within a transaction that will, by default, be automatically
 * <em>rolled back</em> after completion of the test. If a test class is
 * annotated with {@code @Transactional}, each test method within that class
 * hierarchy will be run within a transaction. Test methods that are
 * <em>not</em> annotated with {@code @Transactional} (at the class or method
 * level) will not be run within a transaction. Furthermore, tests that
 * <em>are</em> annotated with {@code @Transactional} but have the
 * {@link Transactional#propagation propagation} type set to
 * {@link cn.taketoday.transaction.annotation.Propagation#NOT_SUPPORTED NOT_SUPPORTED}
 * or {@link cn.taketoday.transaction.annotation.Propagation#NEVER NEVER}
 * will not be run within a transaction.
 *
 * <h3>Declarative Rollback and Commit Behavior</h3>
 * <p>By default, test transactions will be automatically <em>rolled back</em>
 * after completion of the test; however, transactional commit and rollback
 * behavior can be configured declaratively via the {@link Commit @Commit}
 * and {@link Rollback @Rollback} annotations at the class level and at the
 * method level.
 *
 * <h3>Programmatic Transaction Management</h3>
 * <p>As of Spring Framework 4.1, it is possible to interact with test-managed
 * transactions programmatically via the static methods in {@link TestTransaction}.
 * {@code TestTransaction} may be used within <em>test</em> methods,
 * <em>before</em> methods, and <em>after</em> methods.
 *
 * <h3>Executing Code outside of a Transaction</h3>
 * <p>When executing transactional tests, it is sometimes useful to be able to
 * execute certain <em>set up</em> or <em>tear down</em> code outside of a
 * transaction. {@code TransactionalTestExecutionListener} provides such
 * support for methods annotated with {@link BeforeTransaction @BeforeTransaction}
 * or {@link AfterTransaction @AfterTransaction}. As of Spring Framework 4.3,
 * {@code @BeforeTransaction} and {@code @AfterTransaction} may also be declared
 * on Java 8 based interface default methods.
 *
 * <h3>Configuring a Transaction Manager</h3>
 * <p>{@code TransactionalTestExecutionListener} expects a
 * {@link PlatformTransactionManager} bean to be defined in the Spring
 * {@code ApplicationContext} for the test. In case there are multiple
 * instances of {@code PlatformTransactionManager} within the test's
 * {@code ApplicationContext}, a <em>qualifier</em> may be declared via
 * {@link Transactional @Transactional} (e.g., {@code @Transactional("myTxMgr")}
 * or {@code @Transactional(transactionManager = "myTxMgr")}, or
 * {@link cn.taketoday.transaction.annotation.TransactionManagementConfigurer
 * TransactionManagementConfigurer} can be implemented by an
 * {@link cn.taketoday.context.annotation.Configuration @Configuration}
 * class. See {@link TestContextTransactionUtils#retrieveTransactionManager}
 * for details on the algorithm used to look up a transaction manager in
 * the test's {@code ApplicationContext}.
 *
 * <h3>{@code @Transactional} Attribute Support</h3>
 * <table border="1">
 * <tr><th>Attribute</th><th>Supported for test-managed transactions</th></tr>
 * <tr><td>{@link Transactional#value value} and {@link Transactional#transactionManager transactionManager}</td><td>yes</td></tr>
 * <tr><td>{@link Transactional#propagation propagation}</td>
 * <td>only {@link cn.taketoday.transaction.annotation.Propagation#NOT_SUPPORTED NOT_SUPPORTED}
 * and {@link cn.taketoday.transaction.annotation.Propagation#NEVER NEVER} are supported</td></tr>
 * <tr><td>{@link Transactional#isolation isolation}</td><td>no</td></tr>
 * <tr><td>{@link Transactional#timeout timeout}</td><td>no</td></tr>
 * <tr><td>{@link Transactional#readOnly readOnly}</td><td>no</td></tr>
 * <tr><td>{@link Transactional#rollbackFor rollbackFor} and {@link Transactional#rollbackForClassName rollbackForClassName}</td>
 * <td>no: use {@link TestTransaction#flagForRollback()} instead</td></tr>
 * <tr><td>{@link Transactional#noRollbackFor noRollbackFor} and {@link Transactional#noRollbackForClassName noRollbackForClassName}</td>
 * <td>no: use {@link TestTransaction#flagForCommit()} instead</td></tr>
 * </table>
 *
 * @author Sam Brannen
 * @author Juergen Hoeller
 * @see cn.taketoday.transaction.annotation.TransactionManagementConfigurer
 * @see cn.taketoday.transaction.annotation.Transactional
 * @see Commit
 * @see Rollback
 * @see BeforeTransaction
 * @see AfterTransaction
 * @see TestTransaction
 * @since 2.5
 */
public class TransactionalTestExecutionListener extends AbstractTestExecutionListener {

  private static final Log logger = LogFactory.getLog(TransactionalTestExecutionListener.class);

  // Do not require @Transactional test methods to be public.
  @SuppressWarnings("serial")
  protected final TransactionAttributeSource attributeSource = new AnnotationTransactionAttributeSource(false) {

    @Override
    protected TransactionAttribute findTransactionAttribute(Class<?> clazz) {
      // @Transactional present in inheritance hierarchy?
      TransactionAttribute result = super.findTransactionAttribute(clazz);
      if (result != null) {
        return result;
      }
      // @Transactional present in enclosing class hierarchy?
      return findTransactionAttributeInEnclosingClassHierarchy(clazz);
    }

    @Nullable
    private TransactionAttribute findTransactionAttributeInEnclosingClassHierarchy(Class<?> clazz) {
      if (TestContextAnnotationUtils.searchEnclosingClass(clazz)) {
        return findTransactionAttribute(clazz.getEnclosingClass());
      }
      return null;
    }
  };

  /**
   * Returns {@code 4000}.
   */
  @Override
  public final int getOrder() {
    return 4000;
  }

  /**
   * If the test method of the supplied {@linkplain TestContext test context}
   * is configured to run within a transaction, this method will run
   * {@link BeforeTransaction @BeforeTransaction} methods and start a new
   * transaction.
   * <p>Note that if a {@code @BeforeTransaction} method fails, any remaining
   * {@code @BeforeTransaction} methods will not be invoked, and a transaction
   * will not be started.
   *
   * @see Transactional @Transactional
   * @see #getTransactionManager(TestContext, String)
   */
  @Override
  public void beforeTestMethod(final TestContext testContext) throws Exception {
    Method testMethod = testContext.getTestMethod();
    Class<?> testClass = testContext.getTestClass();
    Assert.notNull(testMethod, "Test method of supplied TestContext must not be null");

    TransactionContext txContext = TransactionContextHolder.removeCurrentTransactionContext();
    Assert.state(txContext == null, "Cannot start new transaction without ending existing transaction");

    PlatformTransactionManager tm = null;
    TransactionAttribute transactionAttribute = this.attributeSource.getTransactionAttribute(testMethod, testClass);

    if (transactionAttribute != null) {
      transactionAttribute = TestContextTransactionUtils.createDelegatingTransactionAttribute(testContext,
              transactionAttribute);

      if (logger.isDebugEnabled()) {
        logger.debug("Explicit transaction definition [" + transactionAttribute +
                "] found for test context " + testContext);
      }

      if (transactionAttribute.getPropagationBehavior() == TransactionDefinition.PROPAGATION_NOT_SUPPORTED ||
              transactionAttribute.getPropagationBehavior() == TransactionDefinition.PROPAGATION_NEVER) {
        return;
      }

      tm = getTransactionManager(testContext, transactionAttribute.getQualifier());
      Assert.state(tm != null,
              () -> "Failed to retrieve PlatformTransactionManager for @Transactional test: " + testContext);
    }

    if (tm != null) {
      txContext = new TransactionContext(testContext, tm, transactionAttribute, isRollback(testContext));
      runBeforeTransactionMethods(testContext);
      txContext.startTransaction();
      TransactionContextHolder.setCurrentTransactionContext(txContext);
    }
  }

  /**
   * If a transaction is currently active for the supplied
   * {@linkplain TestContext test context}, this method will end the transaction
   * and run {@link AfterTransaction @AfterTransaction} methods.
   * <p>{@code @AfterTransaction} methods are guaranteed to be invoked even if
   * an error occurs while ending the transaction.
   */
  @Override
  public void afterTestMethod(TestContext testContext) throws Exception {
    Method testMethod = testContext.getTestMethod();
    Assert.notNull(testMethod, "The test method of the supplied TestContext must not be null");

    TransactionContext txContext = TransactionContextHolder.removeCurrentTransactionContext();
    // If there was (or perhaps still is) a transaction...
    if (txContext != null) {
      TransactionStatus transactionStatus = txContext.getTransactionStatus();
      try {
        // If the transaction is still active...
        if (transactionStatus != null && !transactionStatus.isCompleted()) {
          txContext.endTransaction();
        }
      }
      finally {
        runAfterTransactionMethods(testContext);
      }
    }
  }

  /**
   * Run all {@link BeforeTransaction @BeforeTransaction} methods for the
   * specified {@linkplain TestContext test context}. If one of the methods
   * fails, however, the caught exception will be rethrown in a wrapped
   * {@link RuntimeException}, and the remaining methods will <strong>not</strong>
   * be given a chance to execute.
   *
   * @param testContext the current test context
   */
  protected void runBeforeTransactionMethods(TestContext testContext) throws Exception {
    try {
      List<Method> methods = getAnnotatedMethods(testContext.getTestClass(), BeforeTransaction.class);
      Collections.reverse(methods);
      for (Method method : methods) {
        if (logger.isDebugEnabled()) {
          logger.debug("Executing @BeforeTransaction method [" + method + "] for test context " + testContext);
        }
        ReflectionUtils.makeAccessible(method);
        method.invoke(testContext.getTestInstance());
      }
    }
    catch (InvocationTargetException ex) {
      if (logger.isErrorEnabled()) {
        logger.error("Exception encountered while executing @BeforeTransaction methods for test context " +
                testContext + ".", ex.getTargetException());
      }
      ReflectionUtils.rethrowException(ex.getTargetException());
    }
  }

  /**
   * Run all {@link AfterTransaction @AfterTransaction} methods for the
   * specified {@linkplain TestContext test context}. If one of the methods
   * fails, the caught exception will be logged as an error, and the remaining
   * methods will be given a chance to execute. After all methods have
   * executed, the first caught exception, if any, will be rethrown.
   *
   * @param testContext the current test context
   */
  protected void runAfterTransactionMethods(TestContext testContext) throws Exception {
    Throwable afterTransactionException = null;

    List<Method> methods = getAnnotatedMethods(testContext.getTestClass(), AfterTransaction.class);
    for (Method method : methods) {
      try {
        if (logger.isDebugEnabled()) {
          logger.debug("Executing @AfterTransaction method [" + method + "] for test context " + testContext);
        }
        ReflectionUtils.makeAccessible(method);
        method.invoke(testContext.getTestInstance());
      }
      catch (InvocationTargetException ex) {
        Throwable targetException = ex.getTargetException();
        if (afterTransactionException == null) {
          afterTransactionException = targetException;
        }
        logger.error("Exception encountered while executing @AfterTransaction method [" + method +
                "] for test context " + testContext, targetException);
      }
      catch (Exception ex) {
        if (afterTransactionException == null) {
          afterTransactionException = ex;
        }
        logger.error("Exception encountered while executing @AfterTransaction method [" + method +
                "] for test context " + testContext, ex);
      }
    }

    if (afterTransactionException != null) {
      ReflectionUtils.rethrowException(afterTransactionException);
    }
  }

  /**
   * Get the {@linkplain PlatformTransactionManager transaction manager} to use
   * for the supplied {@linkplain TestContext test context} and {@code qualifier}.
   * <p>Delegates to {@link #getTransactionManager(TestContext)} if the
   * supplied {@code qualifier} is {@code null} or empty.
   *
   * @param testContext the test context for which the transaction manager
   * should be retrieved
   * @param qualifier the qualifier for selecting between multiple bean matches;
   * may be {@code null} or empty
   * @return the transaction manager to use, or {@code null} if not found
   * @throws BeansException if an error occurs while retrieving the transaction manager
   * @see #getTransactionManager(TestContext)
   */
  @Nullable
  protected PlatformTransactionManager getTransactionManager(TestContext testContext, @Nullable String qualifier) {
    // Look up by type and qualifier from @Transactional
    if (StringUtils.hasText(qualifier)) {
      try {
        // Use autowire-capable factory in order to support extended qualifier matching
        // (only exposed on the internal BeanFactory, not on the ApplicationContext).
        BeanFactory bf = testContext.getApplicationContext().getAutowireCapableBeanFactory();

        return BeanFactoryAnnotationUtils.qualifiedBeanOfType(bf, PlatformTransactionManager.class, qualifier);
      }
      catch (RuntimeException ex) {
        if (logger.isWarnEnabled()) {
          logger.warn(String.format(
                  "Caught exception while retrieving transaction manager with qualifier '%s' for test context %s",
                  qualifier, testContext), ex);
        }
        throw ex;
      }
    }

    // else
    return getTransactionManager(testContext);
  }

  /**
   * Get the {@linkplain PlatformTransactionManager transaction manager}
   * to use for the supplied {@linkplain TestContext test context}.
   * <p>The default implementation simply delegates to
   * {@link TestContextTransactionUtils#retrieveTransactionManager}.
   *
   * @param testContext the test context for which the transaction manager
   * should be retrieved
   * @return the transaction manager to use, or {@code null} if not found
   * @throws BeansException if an error occurs while retrieving an explicitly
   * named transaction manager
   * @throws IllegalStateException if more than one TransactionManagementConfigurer
   * exists in the ApplicationContext
   * @see #getTransactionManager(TestContext, String)
   */
  @Nullable
  protected PlatformTransactionManager getTransactionManager(TestContext testContext) {
    return TestContextTransactionUtils.retrieveTransactionManager(testContext, null);
  }

  /**
   * Determine whether or not to rollback transactions by default for the
   * supplied {@linkplain TestContext test context}.
   * <p>Supports {@link Rollback @Rollback} or {@link Commit @Commit} at the
   * class-level.
   *
   * @param testContext the test context for which the default rollback flag
   * should be retrieved
   * @return the <em>default rollback</em> flag for the supplied test context
   * @throws Exception if an error occurs while determining the default rollback flag
   */
  protected final boolean isDefaultRollback(TestContext testContext) throws Exception {
    Class<?> testClass = testContext.getTestClass();
    Rollback rollback = TestContextAnnotationUtils.findMergedAnnotation(testClass, Rollback.class);
    boolean rollbackPresent = (rollback != null);

    if (rollbackPresent) {
      boolean defaultRollback = rollback.value();
      if (logger.isDebugEnabled()) {
        logger.debug(String.format("Retrieved default @Rollback(%s) for test class [%s].",
                defaultRollback, testClass.getName()));
      }
      return defaultRollback;
    }

    // else
    return true;
  }

  /**
   * Determine whether or not to rollback transactions for the supplied
   * {@linkplain TestContext test context} by taking into consideration the
   * {@linkplain #isDefaultRollback(TestContext) default rollback} flag and a
   * possible method-level override via the {@link Rollback @Rollback}
   * annotation.
   *
   * @param testContext the test context for which the rollback flag
   * should be retrieved
   * @return the <em>rollback</em> flag for the supplied test context
   * @throws Exception if an error occurs while determining the rollback flag
   */
  protected final boolean isRollback(TestContext testContext) throws Exception {
    boolean rollback = isDefaultRollback(testContext);
    Rollback rollbackAnnotation =
            AnnotatedElementUtils.findMergedAnnotation(testContext.getTestMethod(), Rollback.class);
    if (rollbackAnnotation != null) {
      boolean rollbackOverride = rollbackAnnotation.value();
      if (logger.isDebugEnabled()) {
        logger.debug(String.format(
                "Method-level @Rollback(%s) overrides default rollback [%s] for test context %s.",
                rollbackOverride, rollback, testContext));
      }
      rollback = rollbackOverride;
    }
    else {
      if (logger.isDebugEnabled()) {
        logger.debug(String.format(
                "No method-level @Rollback override: using default rollback [%s] for test context %s.",
                rollback, testContext));
      }
    }
    return rollback;
  }

  /**
   * Get all methods in the supplied {@link Class class} and its superclasses
   * which are annotated with the supplied {@code annotationType} but
   * which are not <em>shadowed</em> by methods overridden in subclasses.
   * <p>Default methods on interfaces are also detected.
   *
   * @param clazz the class for which to retrieve the annotated methods
   * @param annotationType the annotation type for which to search
   * @return all annotated methods in the supplied class and its superclasses
   * as well as annotated interface default methods
   */
  private List<Method> getAnnotatedMethods(Class<?> clazz, Class<? extends Annotation> annotationType) {
    MethodFilter methodFilter = ReflectionUtils.USER_DECLARED_METHODS
            .and(method -> AnnotatedElementUtils.hasAnnotation(method, annotationType));
    return Arrays.asList(ReflectionUtils.getUniqueDeclaredMethods(clazz, methodFilter));
  }

}
