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
package cn.taketoday.web.socket;

import cn.taketoday.beans.factory.config.AutowireCapableBeanFactory;
import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.beans.factory.BeanFactoryAware;
import cn.taketoday.beans.BeanUtils;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;

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
    Assert.notNull(handlerType, "handlerType must not be null");
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
