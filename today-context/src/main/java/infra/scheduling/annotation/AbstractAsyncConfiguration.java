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

package infra.scheduling.annotation;

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
import infra.lang.Nullable;
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
@DisableDependencyInjection
@Configuration(proxyBeanMethods = false)
public abstract class AbstractAsyncConfiguration implements ImportAware, BeanFactoryAware {

  @Nullable
  protected MergedAnnotation<EnableAsync> enableAsync;

  @Nullable
  protected Supplier<Executor> executor;

  @Nullable
  protected Supplier<AsyncUncaughtExceptionHandler> exceptionHandler;

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
    var asyncConfigurer = SingletonSupplier.from(() -> {
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

  private <T> Supplier<T> adapt(Supplier<AsyncConfigurer> supplier, Function<AsyncConfigurer, T> provider) {
    return () -> {
      AsyncConfigurer asyncConfigurer = supplier.get();
      return asyncConfigurer != null ? provider.apply(asyncConfigurer) : null;
    };
  }

}
