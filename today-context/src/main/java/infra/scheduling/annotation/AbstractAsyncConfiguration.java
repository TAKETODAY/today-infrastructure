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

package infra.scheduling.annotation;

import org.jspecify.annotations.Nullable;

import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.function.Supplier;

import infra.aop.interceptor.AsyncUncaughtExceptionHandler;
import infra.beans.BeansException;
import infra.beans.factory.BeanFactory;
import infra.beans.factory.BeanFactoryAware;
import infra.beans.factory.annotation.DisableDependencyInjection;
import infra.context.annotation.Configuration;
import infra.context.annotation.ImportAware;
import infra.core.annotation.MergedAnnotation;
import infra.core.type.AnnotationMetadata;
import infra.util.ObjectUtils;
import infra.util.function.SingletonSupplier;

/**
 * Abstract base {@code Configuration} class providing common structure for enabling
 * asynchronous method execution capability.
 *
 * @author Chris Beams
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 * @see EnableAsync
 * @since 4.0
 */
@SuppressWarnings("NullAway")
@DisableDependencyInjection
@Configuration(proxyBeanMethods = false)
public abstract class AbstractAsyncConfiguration implements ImportAware, BeanFactoryAware {

  @Nullable
  protected MergedAnnotation<EnableAsync> enableAsync;

  @Nullable
  protected Supplier<@Nullable Executor> executor;

  @Nullable
  protected Supplier<@Nullable AsyncUncaughtExceptionHandler> exceptionHandler;

  @Override
  public void setImportMetadata(AnnotationMetadata importMetadata) {
    this.enableAsync = importMetadata.getAnnotation(EnableAsync.class);
    if (!enableAsync.isPresent()) {
      throw new IllegalArgumentException(
              "@EnableAsync is not present on importing class " + importMetadata.getClassName());
    }
  }

  /**
   * Collect any {@link AsyncConfigurer} beans through autowiring.
   */
  @Override
  public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
    var asyncConfigurer = SingletonSupplier.ofNullable(() -> {
      var configurers = beanFactory.getBeanNamesForType(AsyncConfigurer.class);
      if (ObjectUtils.isEmpty(configurers)) {
        return null;
      }
      if (configurers.length > 1) {
        throw new IllegalStateException("Only one AsyncConfigurer may exist");
      }
      return beanFactory.getBean(configurers[0], AsyncConfigurer.class);
    });

    this.executor = adapt(asyncConfigurer, AsyncConfigurer::getAsyncExecutor);
    this.exceptionHandler = adapt(asyncConfigurer, AsyncConfigurer::getAsyncUncaughtExceptionHandler);
  }

  private <T> Supplier<@Nullable T> adapt(SingletonSupplier<AsyncConfigurer> supplier, Function<AsyncConfigurer, @Nullable T> provider) {
    return () -> {
      AsyncConfigurer asyncConfigurer = supplier.get();
      return asyncConfigurer != null ? provider.apply(asyncConfigurer) : null;
    };
  }

}
