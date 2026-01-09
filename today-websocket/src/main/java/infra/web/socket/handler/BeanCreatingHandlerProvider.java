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

package infra.web.socket.handler;

import org.jspecify.annotations.Nullable;

import infra.beans.BeanUtils;
import infra.beans.factory.BeanFactory;
import infra.beans.factory.BeanFactoryAware;
import infra.beans.factory.config.AutowireCapableBeanFactory;
import infra.lang.Assert;

/**
 * Instantiates a target handler through a Framework {@link BeanFactory} and also provides
 * an equivalent destroy method. Mainly for internal use to assist with initializing and
 * destroying handlers with per-connection lifecycle.
 *
 * @param <T> the handler type
 * @author Rossen Stoyanchev
 * @author TODAY 2021/11/12 17:28
 * @since 4.0
 */
public class BeanCreatingHandlerProvider<T> implements BeanFactoryAware {

  private final Class<? extends T> handlerType;

  @Nullable
  private AutowireCapableBeanFactory beanFactory;

  public BeanCreatingHandlerProvider(Class<? extends T> handlerType) {
    Assert.notNull(handlerType, "handlerType is required");
    this.handlerType = handlerType;
  }

  @Override
  public void setBeanFactory(BeanFactory beanFactory) {
    if (beanFactory instanceof AutowireCapableBeanFactory) {
      this.beanFactory = (AutowireCapableBeanFactory) beanFactory;
    }
  }

  public void destroy(T handler) {
    if (this.beanFactory != null) {
      this.beanFactory.destroyBean(handler);
    }
  }

  public Class<? extends T> getHandlerType() {
    return this.handlerType;
  }

  public T getHandler() {
    if (this.beanFactory != null) {
      return this.beanFactory.createBean(this.handlerType);
    }
    else {
      return BeanUtils.newInstance(this.handlerType);
    }
  }

  @Override
  public String toString() {
    return "BeanCreatingHandlerProvider[handlerType=" + this.handlerType + "]";
  }

}
