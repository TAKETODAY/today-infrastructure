/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.scheduling.annotation;

import java.util.Set;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.function.Supplier;

import cn.taketoday.aop.interceptor.AsyncUncaughtExceptionHandler;
import cn.taketoday.beans.BeansException;
import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.beans.factory.BeanFactoryAware;
import cn.taketoday.beans.factory.annotation.DisableDependencyInjection;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.ImportAware;
import cn.taketoday.core.annotation.MergedAnnotation;
import cn.taketoday.core.type.AnnotationMetadata;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.function.SingletonSupplier;

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
      Set<String> configurers = beanFactory.getBeanNamesForType(AsyncConfigurer.class);
      if (ObjectUtils.isEmpty(configurers)) {
        return null;
      }
      if (configurers.size() > 1) {
        throw new IllegalStateException("Only one AsyncConfigurer may exist");
      }
      return beanFactory.getBean(CollectionUtils.firstElement(configurers), AsyncConfigurer.class);
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
