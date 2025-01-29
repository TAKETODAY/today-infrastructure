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

import java.io.Serial;
import java.lang.reflect.Method;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import infra.beans.factory.BeanFactory;
import infra.beans.factory.BeanFactoryAware;
import infra.beans.factory.InitializingBean;
import infra.beans.factory.NoSuchBeanDefinitionException;
import infra.beans.factory.annotation.BeanFactoryAnnotationUtils;
import infra.core.NamedThreadLocal;
import infra.core.ReactiveAdapter;
import infra.core.ReactiveAdapterRegistry;
import infra.core.ReactiveStreams;
import infra.lang.Assert;
import infra.lang.Nullable;
import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.transaction.NoTransactionException;
import infra.transaction.PlatformTransactionManager;
import infra.transaction.ReactiveTransaction;
import infra.transaction.ReactiveTransactionManager;
import infra.transaction.TransactionException;
import infra.transaction.TransactionManager;
import infra.transaction.TransactionStatus;
import infra.transaction.TransactionSystemException;
import infra.transaction.reactive.TransactionContextManager;
import infra.transaction.support.CallbackPreferringPlatformTransactionManager;
import infra.util.ClassUtils;
import infra.util.ConcurrentReferenceHashMap;
import infra.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Base class for transactional aspects, such as the {@link TransactionInterceptor}
 * or an AspectJ aspect.
 *
 * <p>This enables the underlying Framework transaction infrastructure to be used easily
 * to implement an aspect for any aspect system.
 *
 * <p>Subclasses are responsible for calling methods in this class in the correct order.
 *
 * <p>If no transaction name has been specified in the {@link TransactionAttribute},
 * the exposed name will be the {@code fully-qualified class name + "." + method name}
 * (by default).
 *
 * <p>Uses the <b>Strategy</b> design pattern. A {@link PlatformTransactionManager} or
 * {@link ReactiveTransactionManager} implementation will perform the actual transaction
 * management, and a {@link TransactionAttributeSource} (e.g. annotation-based) is used
 * for determining transaction definitions for a particular class or method.
 *
 * <p>A transaction aspect is serializable if its {@code TransactionManager} and
 * {@code TransactionAttributeSource} are serializable.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Stéphane Nicoll
 * @author Sam Brannen
 * @author Mark Paluch
 * @author Sebastien Deleuze
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see PlatformTransactionManager
 * @see ReactiveTransactionManager
 * @see #setTransactionManager
 * @see #setTransactionAttributes
 * @see #setTransactionAttributeSource
 * @since 4.0
 */
public abstract class TransactionAspectSupport implements BeanFactoryAware, InitializingBean {

  // NOTE: This class must not implement Serializable because it serves as base
  // class for AspectJ aspects (which are not allowed to implement Serializable)!

  /**
   * Key to use to store the default transaction manager.
   */
  private static final Object DEFAULT_TRANSACTION_MANAGER_KEY = new Object();

  @Nullable
  private static final ReactiveAdapterRegistry reactiveAdapterRegistry
          = ReactiveStreams.isPresent
          ? ReactiveAdapterRegistry.getSharedInstance()
          : null;

  /**
   * Holder to support the {@code currentTransactionStatus()} method,
   * and to support communication between different cooperating advices
   * (e.g. before and after advice) if the aspect involves more than a
   * single method (as will be the case for around advice).
   */
  private static final ThreadLocal<TransactionInfo> transactionInfoHolder =
          new NamedThreadLocal<>("Current aspect-driven transaction");

  /**
   * Subclasses can use this to return the current TransactionInfo.
   * Only subclasses that cannot handle all operations in one method,
   * such as an AspectJ aspect involving distinct before and after advice,
   * need to use this mechanism to get at the current TransactionInfo.
   * An around advice such as an AOP Alliance MethodInterceptor can hold a
   * reference to the TransactionInfo throughout the aspect method.
   * <p>A TransactionInfo will be returned even if no transaction was created.
   * The {@code TransactionInfo.hasTransaction()} method can be used to query this.
   * <p>To find out about specific transaction characteristics, consider using
   * TransactionSynchronizationManager's {@code isSynchronizationActive()}
   * and/or {@code isActualTransactionActive()} methods.
   *
   * @return the TransactionInfo bound to this thread, or {@code null} if none
   * @see TransactionInfo#hasTransaction()
   * @see infra.transaction.support.TransactionSynchronizationManager#isSynchronizationActive()
   * @see infra.transaction.support.TransactionSynchronizationManager#isActualTransactionActive()
   */
  @Nullable
  protected static TransactionInfo currentTransactionInfo() throws NoTransactionException {
    return transactionInfoHolder.get();
  }

  /**
   * Return the transaction status of the current method invocation.
   * Mainly intended for code that wants to set the current transaction
   * rollback-only but not throw an application exception.
   *
   * @throws NoTransactionException if the transaction info cannot be found,
   * because the method was invoked outside an AOP invocation context
   */
  public static TransactionStatus currentTransactionStatus() throws NoTransactionException {
    TransactionInfo info = currentTransactionInfo();
    if (info == null || info.transactionStatus == null) {
      throw new NoTransactionException("No transaction aspect-managed TransactionStatus in scope");
    }
    return info.transactionStatus;
  }

  protected final Logger logger = LoggerFactory.getLogger(getClass());

  @Nullable
  private String transactionManagerBeanName;

  @Nullable
  private TransactionManager transactionManager;

  @Nullable
  private TransactionAttributeSource transactionAttributeSource;

  @Nullable
  private BeanFactory beanFactory;

  private final ConcurrentReferenceHashMap<Object, TransactionManager> transactionManagerCache =
          new ConcurrentReferenceHashMap<>(4);

  private final ConcurrentReferenceHashMap<Method, ReactiveTransactionSupport> transactionSupportCache =
          new ConcurrentReferenceHashMap<>(1024);

  /**
   * Specify the name of the default transaction manager bean.
   * <p>This can either point to a traditional {@link PlatformTransactionManager} or a
   * {@link ReactiveTransactionManager} for reactive transaction management.
   */
  public void setTransactionManagerBeanName(@Nullable String transactionManagerBeanName) {
    this.transactionManagerBeanName = transactionManagerBeanName;
  }

  /**
   * Return the name of the default transaction manager bean.
   */
  @Nullable
  protected final String getTransactionManagerBeanName() {
    return this.transactionManagerBeanName;
  }

  /**
   * Specify the <em>default</em> transaction manager to use to drive transactions.
   * <p>This can either be a traditional {@link PlatformTransactionManager} or a
   * {@link ReactiveTransactionManager} for reactive transaction management.
   * <p>The default transaction manager will be used if a <em>qualifier</em>
   * has not been declared for a given transaction or if an explicit name for the
   * default transaction manager bean has not been specified.
   *
   * @see #setTransactionManagerBeanName
   */
  public void setTransactionManager(@Nullable TransactionManager transactionManager) {
    this.transactionManager = transactionManager;
  }

  /**
   * Return the default transaction manager, or {@code null} if unknown.
   * <p>This can either be a traditional {@link PlatformTransactionManager} or a
   * {@link ReactiveTransactionManager} for reactive transaction management.
   */
  @Nullable
  public TransactionManager getTransactionManager() {
    return this.transactionManager;
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
   * @see TransactionAttributeEditor
   * @see NameMatchTransactionAttributeSource
   */
  public void setTransactionAttributes(Properties transactionAttributes) {
    NameMatchTransactionAttributeSource tas = new NameMatchTransactionAttributeSource();
    tas.setProperties(transactionAttributes);
    this.transactionAttributeSource = tas;
  }

  /**
   * Set multiple transaction attribute sources which are used to find transaction
   * attributes. Will build a CompositeTransactionAttributeSource for the given sources.
   *
   * @see CompositeTransactionAttributeSource
   * @see MethodMapTransactionAttributeSource
   * @see NameMatchTransactionAttributeSource
   * @see infra.transaction.annotation.AnnotationTransactionAttributeSource
   */
  public void setTransactionAttributeSources(TransactionAttributeSource... transactionAttributeSources) {
    this.transactionAttributeSource = new CompositeTransactionAttributeSource(transactionAttributeSources);
  }

  /**
   * Set the transaction attribute source which is used to find transaction
   * attributes. If specifying a String property value, a PropertyEditor
   * will create a MethodMapTransactionAttributeSource from the value.
   *
   * @see TransactionAttributeSourceEditor
   * @see MethodMapTransactionAttributeSource
   * @see NameMatchTransactionAttributeSource
   * @see infra.transaction.annotation.AnnotationTransactionAttributeSource
   */
  public void setTransactionAttributeSource(@Nullable TransactionAttributeSource transactionAttributeSource) {
    this.transactionAttributeSource = transactionAttributeSource;
  }

  /**
   * Return the transaction attribute source.
   */
  @Nullable
  public TransactionAttributeSource getTransactionAttributeSource() {
    return this.transactionAttributeSource;
  }

  /**
   * Set the BeanFactory to use for retrieving {@code TransactionManager} beans.
   */
  @Override
  public void setBeanFactory(@Nullable BeanFactory beanFactory) {
    this.beanFactory = beanFactory;
  }

  /**
   * Return the BeanFactory to use for retrieving {@code TransactionManager} beans.
   */
  @Nullable
  protected final BeanFactory getBeanFactory() {
    return this.beanFactory;
  }

  /**
   * Check that required properties were set.
   */
  @Override
  public void afterPropertiesSet() {
    if (getTransactionManager() == null && this.beanFactory == null) {
      throw new IllegalStateException(
              "Set the 'transactionManager' property or make sure to run within a BeanFactory " +
                      "containing a TransactionManager bean!");
    }
    if (getTransactionAttributeSource() == null) {
      throw new IllegalStateException(
              "Either 'transactionAttributeSource' or 'transactionAttributes' is required: " +
                      "If there are no transactional methods, then don't use a transaction aspect.");
    }
  }

  /**
   * General delegate for around-advice-based subclasses, delegating to several other template
   * methods on this class. Able to handle {@link CallbackPreferringPlatformTransactionManager}
   * as well as regular {@link PlatformTransactionManager} implementations and
   * {@link ReactiveTransactionManager} implementations for reactive return types.
   *
   * @param method the Method being invoked
   * @param targetClass the target class that we're invoking the method on
   * @param invocation the callback to use for proceeding with the target invocation
   * @return the return value of the method, if any
   * @throws Throwable propagated from the target invocation
   */
  @Nullable
  protected Object invokeWithinTransaction(Method method, @Nullable Class<?> targetClass, final InvocationCallback invocation) throws Throwable {
    // If the transaction attribute is null, the method is non-transactional.
    TransactionAttributeSource tas = getTransactionAttributeSource();
    final TransactionAttribute txAttr = tas != null ? tas.getTransactionAttribute(method, targetClass)
            : null;

    final TransactionManager tm = determineTransactionManager(txAttr, targetClass);

    if (reactiveAdapterRegistry != null && tm instanceof ReactiveTransactionManager) {
      ReactiveTransactionSupport txSupport = transactionSupportCache.computeIfAbsent(method, key -> {
        Class<?> reactiveType = key.getReturnType();
        ReactiveAdapter adapter = reactiveAdapterRegistry.getAdapter(reactiveType);
        if (adapter == null) {
          throw new IllegalStateException("Cannot apply reactive transaction to non-reactive return type [" +
                  method.getReturnType() + "] with specified transaction manager: " + tm);
        }
        return new ReactiveTransactionSupport(adapter);
      });

      return txSupport.invokeWithinTransaction(
              method, targetClass, invocation, txAttr, (ReactiveTransactionManager) tm);
    }

    PlatformTransactionManager ptm = asPlatformTransactionManager(tm);
    final String joinpointIdentification = methodIdentification(method, targetClass, txAttr);

    if (txAttr == null || !(ptm instanceof CallbackPreferringPlatformTransactionManager)) {
      // Standard transaction demarcation with getTransaction and commit/rollback calls.
      TransactionInfo txInfo = createTransactionIfNecessary(ptm, txAttr, joinpointIdentification);

      Object retVal;
      try {
        // This is an around advice: Invoke the next interceptor in the chain.
        // This will normally result in a target object being invoked.
        retVal = invocation.proceedWithInvocation();
      }
      catch (Throwable ex) {
        // target invocation exception
        completeTransactionAfterThrowing(txInfo, ex);
        throw ex;
      }
      finally {
        cleanupTransactionInfo(txInfo);
      }

      if (retVal != null && txAttr != null) {
        if (txInfo.transactionStatus != null) {
          if (retVal instanceof Future<?> future && future.isDone()) {
            try {
              future.get();
            }
            catch (ExecutionException ex) {
              Throwable cause = ex.getCause();
              Assert.state(cause != null, "Cause is required");
              if (txAttr.rollbackOn(cause)) {
                txInfo.transactionStatus.setRollbackOnly();
              }
            }
            catch (InterruptedException ex) {
              Thread.currentThread().interrupt();
            }
          }
        }
      }

      commitTransactionAfterReturning(txInfo);
      return retVal;
    }
    else {
      Object result;
      ThrowableHolder throwableHolder = new ThrowableHolder();

      // It's a CallbackPreferringPlatformTransactionManager: pass a TransactionCallback in.
      try {
        result = ((CallbackPreferringPlatformTransactionManager) ptm).execute(txAttr, status -> {
          TransactionInfo txInfo = prepareTransactionInfo(ptm, txAttr, joinpointIdentification, status);
          try {
            return invocation.proceedWithInvocation();
          }
          catch (Throwable ex) {
            if (txAttr.rollbackOn(ex)) {
              // A RuntimeException: will lead to a rollback.
              if (ex instanceof RuntimeException) {
                throw (RuntimeException) ex;
              }
              else {
                throw new ThrowableHolderException(ex);
              }
            }
            else {
              // A normal return value: will lead to a commit.
              throwableHolder.throwable = ex;
              return null;
            }
          }
          finally {
            cleanupTransactionInfo(txInfo);
          }
        });
      }
      catch (ThrowableHolderException ex) {
        throw ex.getCause();
      }
      catch (TransactionSystemException ex2) {
        if (throwableHolder.throwable != null) {
          logger.error("Application exception overridden by commit exception", throwableHolder.throwable);
          ex2.initApplicationException(throwableHolder.throwable);
        }
        throw ex2;
      }
      catch (Throwable ex2) {
        if (throwableHolder.throwable != null) {
          logger.error("Application exception overridden by commit exception", throwableHolder.throwable);
        }
        throw ex2;
      }

      // Check result state: It might indicate a Throwable to rethrow.
      if (throwableHolder.throwable != null) {
        throw throwableHolder.throwable;
      }
      return result;
    }
  }

  /**
   * Clear the transaction manager cache.
   */
  protected void clearTransactionManagerCache() {
    this.transactionManagerCache.clear();
    this.beanFactory = null;
  }

  /**
   * Determine the specific transaction manager to use for the given transaction.
   *
   * @param txAttr the current transaction attribute
   * @param targetClass the target class that the attribute has been declared on
   */
  @Nullable
  protected TransactionManager determineTransactionManager(
          @Nullable TransactionAttribute txAttr, @Nullable Class<?> targetClass) {

    // Do not attempt to lookup tx manager if no tx attributes are set
    if (txAttr == null || this.beanFactory == null) {
      return getTransactionManager();
    }

    String qualifier = txAttr.getQualifier();
    if (StringUtils.hasText(qualifier)) {
      return determineQualifiedTransactionManager(this.beanFactory, qualifier);
    }
    else if (targetClass != null) {
      // Consider type-level qualifier annotations for transaction manager selection
      String typeQualifier = BeanFactoryAnnotationUtils.getQualifierValue(targetClass);
      if (StringUtils.hasText(typeQualifier)) {
        try {
          return determineQualifiedTransactionManager(this.beanFactory, typeQualifier);
        }
        catch (NoSuchBeanDefinitionException ex) {
          // Consider type qualifier as optional, proceed with regular resolution below.
        }
      }
    }

    if (StringUtils.hasText(this.transactionManagerBeanName)) {
      return determineQualifiedTransactionManager(this.beanFactory, this.transactionManagerBeanName);
    }
    else {
      TransactionManager defaultTransactionManager = getTransactionManager();
      if (defaultTransactionManager == null) {
        defaultTransactionManager = this.transactionManagerCache.get(DEFAULT_TRANSACTION_MANAGER_KEY);
        if (defaultTransactionManager == null) {
          defaultTransactionManager = this.beanFactory.getBean(TransactionManager.class);
          this.transactionManagerCache.putIfAbsent(
                  DEFAULT_TRANSACTION_MANAGER_KEY, defaultTransactionManager);
        }
      }
      return defaultTransactionManager;
    }
  }

  private TransactionManager determineQualifiedTransactionManager(BeanFactory beanFactory, String qualifier) {
    TransactionManager txManager = this.transactionManagerCache.get(qualifier);
    if (txManager == null) {
      txManager = BeanFactoryAnnotationUtils.qualifiedBeanOfType(
              beanFactory, TransactionManager.class, qualifier);
      this.transactionManagerCache.putIfAbsent(qualifier, txManager);
    }
    return txManager;
  }

  @Nullable
  private PlatformTransactionManager asPlatformTransactionManager(@Nullable Object transactionManager) {
    if (transactionManager == null) {
      return null;
    }
    if (transactionManager instanceof PlatformTransactionManager ptm) {
      return ptm;
    }
    else {
      throw new IllegalStateException(
              "Specified transaction manager is not a PlatformTransactionManager: " + transactionManager);
    }
  }

  private String methodIdentification(Method method, @Nullable Class<?> targetClass, @Nullable TransactionAttribute txAttr) {
    String methodIdentification = methodIdentification(method, targetClass);
    if (methodIdentification == null) {
      if (txAttr instanceof DefaultTransactionAttribute) {
        methodIdentification = ((DefaultTransactionAttribute) txAttr).getDescriptor();
      }
      if (methodIdentification == null) {
        methodIdentification = ClassUtils.getQualifiedMethodName(method, targetClass);
      }
    }
    return methodIdentification;
  }

  /**
   * Convenience method to return a String representation of this Method
   * for use in logging. Can be overridden in subclasses to provide a
   * different identifier for the given method.
   * <p>The default implementation returns {@code null}, indicating the
   * use of {@link DefaultTransactionAttribute#getDescriptor()} instead,
   * ending up as {@link ClassUtils#getQualifiedMethodName(Method, Class)}.
   *
   * @param method the method we're interested in
   * @param targetClass the class that the method is being invoked on
   * @return a String representation identifying this method
   * @see ClassUtils#getQualifiedMethodName
   */
  @Nullable
  protected String methodIdentification(Method method, @Nullable Class<?> targetClass) {
    return null;
  }

  /**
   * Create a transaction if necessary based on the given TransactionAttribute.
   * <p>Allows callers to perform custom TransactionAttribute lookups through
   * the TransactionAttributeSource.
   *
   * @param txAttr the TransactionAttribute (may be {@code null})
   * @param joinpointIdentification the fully qualified method name
   * (used for monitoring and logging purposes)
   * @return a TransactionInfo object, whether or not a transaction was created.
   * The {@code hasTransaction()} method on TransactionInfo can be used to
   * tell if there was a transaction created.
   * @see #getTransactionAttributeSource()
   */
  protected TransactionInfo createTransactionIfNecessary(@Nullable PlatformTransactionManager tm,
          @Nullable TransactionAttribute txAttr, final String joinpointIdentification) {

    // If no name specified, apply method identification as transaction name.
    if (txAttr != null && txAttr.getName() == null) {
      txAttr = new JoinPointTransactionAttribute(txAttr, joinpointIdentification);
    }

    TransactionStatus status = null;
    if (txAttr != null) {
      if (tm != null) {
        status = tm.getTransaction(txAttr);
      }
      else if (logger.isDebugEnabled()) {
        logger.debug("Skipping transactional joinpoint [{}] because no transaction manager has been configured",
                joinpointIdentification);
      }
    }
    return prepareTransactionInfo(tm, txAttr, joinpointIdentification, status);
  }

  /**
   * Prepare a TransactionInfo for the given attribute and status object.
   *
   * @param txAttr the TransactionAttribute (may be {@code null})
   * @param joinpointIdentification the fully qualified method name
   * (used for monitoring and logging purposes)
   * @param status the TransactionStatus for the current transaction
   * @return the prepared TransactionInfo object
   */
  protected TransactionInfo prepareTransactionInfo(@Nullable PlatformTransactionManager tm,
          @Nullable TransactionAttribute txAttr, String joinpointIdentification, @Nullable TransactionStatus status) {

    TransactionInfo txInfo = new TransactionInfo(tm, txAttr, joinpointIdentification);
    if (txAttr != null) {
      // We need a transaction for this method...
      if (logger.isDebugEnabled()) {
        logger.trace("Getting transaction for [{}]", txInfo.joinpointIdentification);
      }
      // The transaction manager will flag an error if an incompatible tx already exists.
      txInfo.newTransactionStatus(status);
    }
    else {
      // The TransactionInfo.hasTransaction() method will return false. We created it only
      // to preserve the integrity of the ThreadLocal stack maintained in this class.
      if (logger.isDebugEnabled()) {
        logger.trace("No need to create transaction for [{}]: This method is not transactional.",
                joinpointIdentification);
      }
    }

    // We always bind the TransactionInfo to the thread, even if we didn't create
    // a new transaction here. This guarantees that the TransactionInfo stack
    // will be managed correctly even if no transaction was created by this aspect.
    txInfo.bindToThread();
    return txInfo;
  }

  /**
   * Execute after successful completion of call, but not after an exception was handled.
   * Do nothing if we didn't create a transaction.
   *
   * @param txInfo information about the current transaction
   */
  protected void commitTransactionAfterReturning(@Nullable TransactionInfo txInfo) {
    if (txInfo != null && txInfo.hasTransaction()) {
      if (logger.isDebugEnabled()) {
        logger.trace("Completing transaction for [{}]", txInfo.joinpointIdentification);
      }
      txInfo.commit();
    }
  }

  /**
   * Handle a throwable, completing the transaction.
   * We may commit or roll back, depending on the configuration.
   *
   * @param txInfo information about the current transaction
   * @param ex throwable encountered
   */
  protected void completeTransactionAfterThrowing(@Nullable TransactionInfo txInfo, Throwable ex) {
    if (txInfo != null && txInfo.hasTransaction()) {
      if (logger.isDebugEnabled()) {
        logger.trace("Completing transaction for [{}] after exception: {}", txInfo.joinpointIdentification, ex);
      }
      if (txInfo.transactionAttribute != null && txInfo.transactionAttribute.rollbackOn(ex)) {
        try {
          txInfo.rollback();
        }
        catch (TransactionSystemException ex2) {
          logger.error("Application exception overridden by rollback exception", ex);
          ex2.initApplicationException(ex);
          throw ex2;
        }
        catch (RuntimeException | Error ex2) {
          logger.error("Application exception overridden by rollback exception", ex);
          throw ex2;
        }
      }
      else {
        // We don't roll back on this exception.
        // Will still roll back if TransactionStatus.isRollbackOnly() is true.
        try {
          txInfo.commit();
        }
        catch (TransactionSystemException ex2) {
          logger.error("Application exception overridden by commit exception", ex);
          ex2.initApplicationException(ex);
          throw ex2;
        }
        catch (RuntimeException | Error ex2) {
          logger.error("Application exception overridden by commit exception", ex);
          throw ex2;
        }
      }
    }
  }

  /**
   * Reset the TransactionInfo ThreadLocal.
   * <p>Call this in all cases: exception or normal return!
   *
   * @param txInfo information about the current transaction (may be {@code null})
   */
  protected void cleanupTransactionInfo(@Nullable TransactionInfo txInfo) {
    if (txInfo != null) {
      txInfo.restoreThreadLocalStatus();
    }
  }

  /**
   * Opaque object used to hold transaction information. Subclasses
   * must pass it back to methods on this class, but not see its internals.
   */
  protected static final class TransactionInfo {

    @Nullable
    public final PlatformTransactionManager transactionManager;

    @Nullable
    public final TransactionAttribute transactionAttribute;

    /**
     * a String representation of this joinpoint (usually a Method call)
     * for use in logging.
     */
    public final String joinpointIdentification;

    @Nullable
    public TransactionStatus transactionStatus;

    @Nullable
    public TransactionInfo oldTransactionInfo;

    public TransactionInfo(@Nullable PlatformTransactionManager transactionManager,
            @Nullable TransactionAttribute transactionAttribute, String joinpointIdentification) {

      this.transactionManager = transactionManager;
      this.transactionAttribute = transactionAttribute;
      this.joinpointIdentification = joinpointIdentification;
    }

    @Nullable
    public TransactionAttribute getTransactionAttribute() {
      return this.transactionAttribute;
    }

    public void newTransactionStatus(@Nullable TransactionStatus status) {
      this.transactionStatus = status;
    }

    public void commit() throws TransactionException {
      PlatformTransactionManager transactionManager = this.transactionManager;
      Assert.state(transactionManager != null, "No PlatformTransactionManager set");
      transactionManager.commit(transactionStatus);
    }

    public void rollback() throws TransactionException {
      PlatformTransactionManager transactionManager = this.transactionManager;
      Assert.state(transactionManager != null, "No PlatformTransactionManager set");
      transactionManager.rollback(transactionStatus);
    }

    /**
     * Return whether a transaction was created by this aspect,
     * or whether we just have a placeholder to keep ThreadLocal stack integrity.
     */
    public boolean hasTransaction() {
      return transactionStatus != null;
    }

    private void bindToThread() {
      // Expose current TransactionStatus, preserving any existing TransactionStatus
      // for restoration after this transaction is complete.
      this.oldTransactionInfo = transactionInfoHolder.get();
      transactionInfoHolder.set(this);
    }

    private void restoreThreadLocalStatus() {
      // Use stack to restore old transaction TransactionInfo.
      // Will be null if none was set.
      transactionInfoHolder.set(this.oldTransactionInfo);
    }

    @Override
    public String toString() {
      return (this.transactionAttribute != null ? this.transactionAttribute.toString() : "No transaction");
    }
  }

  /**
   * Simple callback interface for proceeding with the target invocation.
   * Concrete interceptors/aspects adapt this to their invocation mechanism.
   */
  @FunctionalInterface
  public interface InvocationCallback {

    @Nullable
    Object proceedWithInvocation() throws Throwable;
  }

  /**
   * Internal holder class for a Throwable in a callback transaction model.
   */
  private static class ThrowableHolder {

    @Nullable
    public Throwable throwable;
  }

  /**
   * Internal holder class for a Throwable, used as a RuntimeException to be
   * thrown from a TransactionCallback (and subsequently unwrapped again).
   */
  @SuppressWarnings("serial")
  private static class ThrowableHolderException extends RuntimeException {

    public ThrowableHolderException(Throwable throwable) {
      super(throwable);
    }

    @Override
    public String toString() {
      return getCause().toString();
    }
  }

  /**
   * Delegate for Reactor-based management of transactional methods with a
   * reactive return type.
   */
  private class ReactiveTransactionSupport {

    private final ReactiveAdapter adapter;

    public ReactiveTransactionSupport(ReactiveAdapter adapter) {
      this.adapter = adapter;
    }

    public Object invokeWithinTransaction(Method method, @Nullable Class<?> targetClass,
            InvocationCallback invocation, @Nullable TransactionAttribute txAttr, ReactiveTransactionManager rtm) {
      String joinpointIdentification = methodIdentification(method, targetClass, txAttr);
      if (Mono.class.isAssignableFrom(method.getReturnType())) {
        return TransactionContextManager.currentContext()
                .flatMap(context -> Mono.<Object, ReactiveTransactionInfo>usingWhen(
                                createTransactionIfNecessary(rtm, txAttr, joinpointIdentification),
                                tx -> {
                                  try {
                                    return (Mono<?>) invocation.proceedWithInvocation();
                                  }
                                  catch (Throwable ex) {
                                    return Mono.error(ex);
                                  }
                                },
                                this::commitTransactionAfterReturning,
                                this::completeTransactionAfterThrowing,
                                this::rollbackTransactionOnCancel)
                        .onErrorMap(this::unwrapIfResourceCleanupFailure))
                .contextWrite(TransactionContextManager.getOrCreateContext())
                .contextWrite(TransactionContextManager.getOrCreateContextHolder());
      }

      // Any other reactive type, typically a Flux
      return this.adapter.fromPublisher(TransactionContextManager.currentContext()
              .flatMapMany(context -> Flux.usingWhen(createTransactionIfNecessary(rtm, txAttr, joinpointIdentification),
                              tx -> {
                                try {
                                  return this.adapter.toPublisher(invocation.proceedWithInvocation());
                                }
                                catch (Throwable ex) {
                                  return Mono.error(ex);
                                }
                              },
                              this::commitTransactionAfterReturning,
                              this::completeTransactionAfterThrowing,
                              this::rollbackTransactionOnCancel)
                      .onErrorMap(this::unwrapIfResourceCleanupFailure))
              .contextWrite(TransactionContextManager.getOrCreateContext())
              .contextWrite(TransactionContextManager.getOrCreateContextHolder()));
    }

    private Mono<ReactiveTransactionInfo> createTransactionIfNecessary(ReactiveTransactionManager tm,
            @Nullable TransactionAttribute txAttr, final String joinpointIdentification) {

      // If no name specified, apply method identification as transaction name.
      if (txAttr != null && txAttr.getName() == null) {
        txAttr = new JoinPointTransactionAttribute(txAttr, joinpointIdentification);
      }

      final TransactionAttribute attrToUse = txAttr;
      Mono<ReactiveTransaction> tx = attrToUse != null ? tm.getReactiveTransaction(attrToUse) : Mono.empty();
      return tx.map(it -> prepareTransactionInfo(tm, attrToUse, joinpointIdentification, it))
              .switchIfEmpty(Mono.defer(() -> Mono.just(prepareTransactionInfo(tm, attrToUse, joinpointIdentification, null))));
    }

    private ReactiveTransactionInfo prepareTransactionInfo(@Nullable ReactiveTransactionManager tm,
            @Nullable TransactionAttribute txAttr, String joinpointIdentification, @Nullable ReactiveTransaction transaction) {

      ReactiveTransactionInfo txInfo = new ReactiveTransactionInfo(tm, txAttr, joinpointIdentification);
      if (txAttr != null) {
        // We need a transaction for this method...
        if (logger.isDebugEnabled()) {
          logger.trace("Getting transaction for [{}]", txInfo.joinpointIdentification);
        }
        // The transaction manager will flag an error if an incompatible tx already exists.
        txInfo.newReactiveTransaction(transaction);
      }
      else {
        // The TransactionInfo.hasTransaction() method will return false. We created it only
        // to preserve the integrity of the ThreadLocal stack maintained in this class.
        if (logger.isDebugEnabled()) {
          logger.trace("Don't need to create transaction for [{}]: This method isn't transactional.", joinpointIdentification);
        }
      }

      return txInfo;
    }

    private Mono<Void> commitTransactionAfterReturning(@Nullable ReactiveTransactionInfo txInfo) {
      if (txInfo != null && txInfo.reactiveTransaction != null) {
        if (logger.isDebugEnabled()) {
          logger.trace("Completing transaction for [{}]", txInfo.joinpointIdentification);
        }
        return txInfo.commit();
      }
      return Mono.empty();
    }

    private Mono<Void> rollbackTransactionOnCancel(@Nullable ReactiveTransactionInfo txInfo) {
      if (txInfo != null && txInfo.reactiveTransaction != null) {
        if (logger.isDebugEnabled()) {
          logger.trace("Rolling back transaction for [{}] after cancellation", txInfo.joinpointIdentification);
        }
        return txInfo.rollback();
      }
      return Mono.empty();
    }

    private Mono<Void> completeTransactionAfterThrowing(@Nullable ReactiveTransactionInfo txInfo, Throwable ex) {
      if (txInfo != null && txInfo.reactiveTransaction != null) {
        if (logger.isDebugEnabled()) {
          logger.trace("Completing transaction for [{}] after exception: {}",
                  txInfo.joinpointIdentification, ex.toString());
        }
        if (txInfo.transactionAttribute != null && txInfo.transactionAttribute.rollbackOn(ex)) {
          return txInfo.rollback().onErrorMap(ex2 -> {
                    logger.error("Application exception overridden by rollback exception", ex);
                    if (ex2 instanceof TransactionSystemException) {
                      ((TransactionSystemException) ex2).initApplicationException(ex);
                    }
                    else {
                      ex2.addSuppressed(ex);
                    }
                    return ex2;
                  }
          );
        }
        else {
          // We don't roll back on this exception.
          // Will still roll back if TransactionStatus.isRollbackOnly() is true.
          return txInfo.commit().onErrorMap(ex2 -> {
                    logger.error("Application exception overridden by commit exception", ex);
                    if (ex2 instanceof TransactionSystemException) {
                      ((TransactionSystemException) ex2).initApplicationException(ex);
                    }
                    else {
                      ex2.addSuppressed(ex);
                    }
                    return ex2;
                  }
          );
        }
      }
      return Mono.empty();
    }

    /**
     * Unwrap the cause of a throwable, if produced by a failure
     * during the async resource cleanup in {@link Flux#usingWhen}.
     *
     * @param ex the throwable to try to unwrap
     */
    private Throwable unwrapIfResourceCleanupFailure(Throwable ex) {
      if (ex instanceof RuntimeException && ex.getCause() != null) {
        String msg = ex.getMessage();
        if (msg != null && msg.startsWith("Async resource cleanup failed")) {
          return ex.getCause();
        }
      }
      return ex;
    }
  }

  /**
   * Opaque object used to hold transaction information for reactive methods.
   */
  private static final class ReactiveTransactionInfo {

    @Nullable
    public final ReactiveTransactionManager transactionManager;

    @Nullable
    public final TransactionAttribute transactionAttribute;

    /**
     * a String representation of this joinpoint (usually a Method call)
     * for use in logging.
     */
    public final String joinpointIdentification;

    @Nullable
    public ReactiveTransaction reactiveTransaction;

    public ReactiveTransactionInfo(@Nullable ReactiveTransactionManager transactionManager,
            @Nullable TransactionAttribute transactionAttribute, String joinpointIdentification) {

      this.transactionManager = transactionManager;
      this.transactionAttribute = transactionAttribute;
      this.joinpointIdentification = joinpointIdentification;
    }

    public Mono<Void> commit() {
      Assert.state(transactionManager != null, "No ReactiveTransactionManager set");
      return transactionManager.commit(reactiveTransaction);
    }

    public Mono<Void> rollback() {
      Assert.state(transactionManager != null, "No ReactiveTransactionManager set");
      return transactionManager.rollback(reactiveTransaction);
    }

    public void newReactiveTransaction(@Nullable ReactiveTransaction transaction) {
      this.reactiveTransaction = transaction;
    }

    @Override
    public String toString() {
      return this.transactionAttribute != null ? this.transactionAttribute.toString() : "No transaction";
    }
  }

  static class JoinPointTransactionAttribute extends DelegatingTransactionAttribute {
    @Serial
    private static final long serialVersionUID = 1L;

    private final String joinpointIdentification;

    public JoinPointTransactionAttribute(TransactionAttribute targetAttribute, String joinpointIdentification) {
      super(targetAttribute);
      this.joinpointIdentification = joinpointIdentification;
    }

    @Override
    public String getName() {
      return joinpointIdentification;
    }

  }
}
