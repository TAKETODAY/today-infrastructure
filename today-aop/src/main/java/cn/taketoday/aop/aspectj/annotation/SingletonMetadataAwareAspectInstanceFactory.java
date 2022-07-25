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

package cn.taketoday.aop.aspectj.annotation;

import java.io.Serializable;

import cn.taketoday.aop.aspectj.SingletonAspectInstanceFactory;
import cn.taketoday.core.annotation.OrderUtils;

/**
 * Implementation of {@link MetadataAwareAspectInstanceFactory} that is backed
 * by a specified singleton object, returning the same instance for every
 * {@link #getAspectInstance()} call.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see SimpleMetadataAwareAspectInstanceFactory
 * @since 4.0
 */
@SuppressWarnings("serial")
public class SingletonMetadataAwareAspectInstanceFactory extends SingletonAspectInstanceFactory
        implements MetadataAwareAspectInstanceFactory, Serializable {

  private final AspectMetadata metadata;

  /**
   * Create a new SingletonMetadataAwareAspectInstanceFactory for the given aspect.
   *
   * @param aspectInstance the singleton aspect instance
   * @param aspectName the name of the aspect
   */
  public SingletonMetadataAwareAspectInstanceFactory(Object aspectInstance, String aspectName) {
    super(aspectInstance);
    this.metadata = new AspectMetadata(aspectInstance.getClass(), aspectName);
  }

  @Override
  public final AspectMetadata getAspectMetadata() {
    return this.metadata;
  }

  @Override
  public Object getAspectCreationMutex() {
    return this;
  }

  @Override
  protected int getOrderForAspectClass(Class<?> aspectClass) {
    return OrderUtils.getOrder(aspectClass, LOWEST_PRECEDENCE);
  }

}
