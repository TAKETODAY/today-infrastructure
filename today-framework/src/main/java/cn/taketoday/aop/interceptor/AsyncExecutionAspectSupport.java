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

package cn.taketoday.aop.interceptor;

import java.lang.reflect.Method;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.function.Supplier;

import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.beans.factory.BeanFactoryAware;
import cn.taketoday.beans.factory.NoSuchBeanDefinitionException;
import cn.taketoday.beans.factory.NoUniqueBeanDefinitionException;
import cn.taketoday.beans.factory.annotation.BeanFactoryAnnotationUtils;
import cn.taketoday.beans.factory.config.ConfigurableBeanFactory;
import cn.taketoday.beans.factory.config.EmbeddedValueResolver;
import cn.taketoday.core.task.AsyncListenableTaskExecutor;
import cn.taketoday.core.task.AsyncTaskExecutor;
import cn.taketoday.core.task.TaskExecutor;
import cn.taketoday.core.task.support.TaskExecutorAdapter;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.ReflectionUtils;
import cn.taketoday.util.SingletonSupplier;
import cn.taketoday.util.StringUtils;
import cn.taketoday.util.concurrent.ListenableFuture;

/**
 * Base class for asynchronous method execution aspects, such as
 * {@code cn.taketoday.scheduling.annotation.AnnotationAsyncExecutionInterceptor}
 * or {@code cn.taketoday.scheduling.aspectj.AnnotationAsyncExecutionAspect}.
 *
 * <p>Provides support for <i>executor qualification</i> on a method-by-method basis.
 * {@code AsyncExecutionAspectSupport} objects must be constructed with a default {@code
 * Executor}, but each individual method may further qualify a specific {@code Executor}
 * bean to be used when executing it, e.g. through an annotation attribute.
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 * @since 4.0
 */
public abstract class AsyncExecutionAspectSupport implements BeanFactoryAware {
  private static final Logger log = LoggerFactory.getLogger(AsyncExecutionAspectSupport.class);

  /**
   * The default name of the {@link TaskExecutor} bean to pick up: "taskExecutor".
   * <p>Note that the initial lookup happens by type; this is just the fallback
   * in case of multiple executor beans found in the context.
   */
  public static final String DEFAULT_TASK_EXECUTOR_BEAN_NAME = "taskExecutor";

  @Nullable
  private BeanFactory beanFactory;
  private SingletonSupplier<Executor> defaultExecutor;
  private SingletonSupplier<AsyncUncaughtExceptionHandler> exceptionHandler;
  private final ConcurrentHashMap<Method, AsyncTaskExecutor> executors = new ConcurrentHashMap<>(16);

  /**
   * Create a new instance with a default {@link AsyncUncaughtExceptionHandler}.
   *
   * @param defaultExecutor the {@code Executor} (typically a Framework {@code AsyncTaskExecutor}
   * or {@link java.util.concurrent.ExecutorService}) to delegate to, unless a more specific
   * executor has been requested via a qualifier on the async method, in which case the
   * executor will be looked up at invocation time against the enclosing bean factory
   */
  public AsyncExecutionAspectSupport(@Nullable Executor defaultExecutor) {
    this.defaultExecutor = new SingletonSupplier<>(defaultExecutor, () -> getDefaultExecutor(this.beanFactory));
    this.exceptionHandler = SingletonSupplier.from(SimpleAsyncUncaughtExceptionHandler::new);
  }

  /**
   * Create a new {@link AsyncExecutionAspectSupport} with the given exception handler.
   *
   * @param defaultExecutor the {@code Executor} (typically a Framework {@code AsyncTaskExecutor}
   * or {@link java.util.concurrent.ExecutorService}) to delegate to, unless a more specific
   * executor has been requested via a qualifier on the async method, in which case the
   * executor will be looked up at invocation time against the enclosing bean factory
   * @param exceptionHandler the {@link AsyncUncaughtExceptionHandler} to use
   */
  public AsyncExecutionAspectSupport(@Nullable Executor defaultExecutor, AsyncUncaughtExceptionHandler exceptionHandler) {
    this.defaultExecutor = new SingletonSupplier<>(defaultExecutor, () -> getDefaultExecutor(this.beanFactory));
    this.exceptionHandler = SingletonSupplier.valueOf(exceptionHandler);
  }

  /**
   * Configure this aspect with the given executor and exception handler suppliers,
   * applying the corresponding default if a supplier is not resolvable.
   */
  public void configure(
          @Nullable Supplier<Executor> defaultExecutor,
          @Nullable Supplier<AsyncUncaughtExceptionHandler> exceptionHandler) {

    this.defaultExecutor = new SingletonSupplier<>(defaultExecutor, () -> getDefaultExecutor(this.beanFactory));
    this.exceptionHandler = new SingletonSupplier<>(exceptionHandler, SimpleAsyncUncaughtExceptionHandler::new);
  }

  /**
   * Supply the executor to be used when executing async methods.
   *
   * @param defaultExecutor the {@code Executor} (typically a Framework {@code AsyncTaskExecutor}
   * or {@link java.util.concurrent.ExecutorService}) to delegate to, unless a more specific
   * executor has been requested via a qualifier on the async method, in which case the
   * executor will be looked up at invocation time against the enclosing bean factory
   * @see #getExecutorQualifier(Method)
   * @see #setBeanFactory(BeanFactory)
   * @see #getDefaultExecutor(BeanFactory)
   */
  public void setExecutor(Executor defaultExecutor) {
    this.defaultExecutor = SingletonSupplier.valueOf(defaultExecutor);
  }

  /**
   * Supply the {@link AsyncUncaughtExceptionHandler} to use to handle exceptions
   * thrown by invoking asynchronous methods with a {@code void} return type.
   */
  public void setExceptionHandler(AsyncUncaughtExceptionHandler exceptionHandler) {
    this.exceptionHandler = SingletonSupplier.valueOf(exceptionHandler);
  }

  /**
   * Set the {@link BeanFactory} to be used when looking up executors by qualifier
   * or when relying on the default executor lookup algorithm.
   *
   * @see #findQualifiedExecutor(BeanFactory, String)
   * @see #getDefaultExecutor(BeanFactory)
   */
  @Override
  public void setBeanFactory(BeanFactory beanFactory) {
    this.beanFactory = beanFactory;
  }

  /**
   * Determine the specific executor to use when executing the given method.
   * <p>Should preferably return an {@link AsyncListenableTaskExecutor} implementation.
   *
   * @return the executor to use (or {@code null}, but just if no default executor is available)
   */
  @Nullable
  protected AsyncTaskExecutor determineAsyncExecutor(Method method) {
    AsyncTaskExecutor executor = this.executors.get(method);
    if (executor == null) {
      Executor targetExecutor;
      String qualifier = getExecutorQualifier(method);
      if (StringUtils.isNotEmpty(qualifier)) {
        targetExecutor = findQualifiedExecutor(this.beanFactory, qualifier);
      }
      else {
        targetExecutor = this.defaultExecutor.get();
      }
      if (targetExecutor == null) {
        return null;
      }
      executor = targetExecutor instanceof AsyncListenableTaskExecutor
                 ? (AsyncListenableTaskExecutor) targetExecutor : new TaskExecutorAdapter(targetExecutor);
      this.executors.put(method, executor);
    }
    return executor;
  }

  /**
   * Return the qualifier or bean name of the executor to be used when executing the
   * given async method, typically specified in the form of an annotation attribute.
   * Returning an empty string or {@code null} indicates that no specific executor has
   * been specified and that the {@linkplain #setExecutor(Executor) default executor}
   * should be used.
   *
   * @param method the method to inspect for executor qualifier metadata
   * @return the qualifier if specified, otherwise empty String or {@code null}
   * @see #determineAsyncExecutor(Method)
   * @see #findQualifiedExecutor(BeanFactory, String)
   */
  @Nullable
  protected abstract String getExecutorQualifier(Method method);

  /**
   * Retrieve a target executor for the given qualifier.
   *
   * @param qualifier the qualifier to resolve
   * @return the target executor, or {@code null} if none available
   * @see #getExecutorQualifier(Method)
   */
  @Nullable
  protected Executor findQualifiedExecutor(@Nullable BeanFactory beanFactory, String qualifier) {
    if (beanFactory == null) {
      throw new IllegalStateException("BeanFactory must be set on " + getClass().getSimpleName() +
              " to access qualified executor '" + qualifier + "'");
    }
    if (beanFactory instanceof ConfigurableBeanFactory factory) {
      EmbeddedValueResolver embeddedValueResolver = new EmbeddedValueResolver(factory);
      qualifier = embeddedValueResolver.resolveStringValue(qualifier);
    }
    return BeanFactoryAnnotationUtils.qualifiedBeanOfType(beanFactory, Executor.class, qualifier);
  }

  /**
   * Retrieve or build a default executor for this advice instance.
   * An executor returned from here will be cached for further use.
   * <p>The default implementation searches for a unique {@link TaskExecutor} bean
   * in the context, or for an {@link Executor} bean named "taskExecutor" otherwise.
   * If neither of the two is resolvable, this implementation will return {@code null}.
   *
   * @param beanFactory the BeanFactory to use for a default executor lookup
   * @return the default executor, or {@code null} if none available
   * @see #findQualifiedExecutor(BeanFactory, String)
   * @see #DEFAULT_TASK_EXECUTOR_BEAN_NAME
   */
  @Nullable
  protected Executor getDefaultExecutor(@Nullable BeanFactory beanFactory) {
    if (beanFactory != null) {
      try {
        // Search for TaskExecutor bean... not plain Executor since that would
        // match with ScheduledExecutorService as well, which is unusable for
        // our purposes here. TaskExecutor is more clearly designed for it.
        return beanFactory.getBean(TaskExecutor.class);
      }
      catch (NoUniqueBeanDefinitionException ex) {
        log.debug("Could not find default TaskExecutor bean. " +
                "Continuing search for an Executor bean named 'taskExecutor'", ex);
        try {
          return beanFactory.getBean(DEFAULT_TASK_EXECUTOR_BEAN_NAME, Executor.class);
        }
        catch (NoSuchBeanDefinitionException ex2) {
          if (log.isInfoEnabled()) {
            log.info("More than one TaskExecutor bean found within the context, and none is named " +
                    "'taskExecutor'. Mark one of them as primary or name it 'taskExecutor' (possibly " +
                    "as an alias) in order to use it for async processing: {}", ex.getBeanNamesFound());
          }
        }
      }
      catch (NoSuchBeanDefinitionException ex) {
        log.debug("Could not find default TaskExecutor bean. " +
                "Continuing search for an Executor bean named 'taskExecutor'", ex);
        try {
          return beanFactory.getBean(DEFAULT_TASK_EXECUTOR_BEAN_NAME, Executor.class);
        }
        catch (NoSuchBeanDefinitionException ex2) {
          log.info("No task executor bean found for async processing: " +
                  "no bean of type TaskExecutor and no bean named 'taskExecutor' either");
        }
        // Giving up -> either using local default executor or none at all...
      }
    }
    return null;
  }

  /**
   * Delegate for actually executing the given task with the chosen executor.
   *
   * @param task the task to execute
   * @param executor the chosen executor
   * @param returnType the declared return type (potentially a {@link Future} variant)
   * @return the execution result (potentially a corresponding {@link Future} handle)
   */
  @Nullable
  protected Object doSubmit(Callable<Object> task, AsyncTaskExecutor executor, Class<?> returnType) {
    if (CompletableFuture.class.isAssignableFrom(returnType)) {
      return CompletableFuture.supplyAsync(() -> {
        try {
          return task.call();
        }
        catch (Throwable ex) {
          throw new CompletionException(ex);
        }
      }, executor);
    }
    else if (ListenableFuture.class.isAssignableFrom(returnType)) {
      return ((AsyncListenableTaskExecutor) executor).submitListenable(task);
    }
    else if (Future.class.isAssignableFrom(returnType)) {
      return executor.submit(task);
    }
    else {
      executor.submit(task);
      return null;
    }
  }

  /**
   * Handles a fatal error thrown while asynchronously invoking the specified
   * {@link Method}.
   * <p>If the return type of the method is a {@link Future} object, the original
   * exception can be propagated by just throwing it at the higher level. However,
   * for all other cases, the exception will not be transmitted back to the client.
   * In that later case, the current {@link AsyncUncaughtExceptionHandler} will be
   * used to manage such exception.
   *
   * @param ex the exception to handle
   * @param method the method that was invoked
   * @param params the parameters used to invoke the method
   */
  protected void handleError(Throwable ex, Method method, Object... params) throws Exception {
    if (Future.class.isAssignableFrom(method.getReturnType())) {
      ReflectionUtils.rethrowException(ex);
    }
    else {
      // Could not transmit the exception to the caller with default executor
      try {
        this.exceptionHandler.obtain().handleUncaughtException(ex, method, params);
      }
      catch (Throwable ex2) {
        log.warn("Exception handler for async method '{}' threw unexpected exception itself",
                method.toGenericString(), ex2);
      }
    }
  }

}
