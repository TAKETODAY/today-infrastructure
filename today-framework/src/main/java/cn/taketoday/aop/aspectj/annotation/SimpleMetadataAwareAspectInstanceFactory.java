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

import cn.taketoday.aop.aspectj.SimpleAspectInstanceFactory;
import cn.taketoday.core.Ordered;
import cn.taketoday.core.annotation.OrderUtils;

/**
 * Implementation of {@link MetadataAwareAspectInstanceFactory} that
 * creates a new instance of the specified aspect class for every
 * {@link #getAspectInstance()} call.
 *
 * @author Juergen Hoeller
 * @since 4.0
 */
public class SimpleMetadataAwareAspectInstanceFactory extends SimpleAspectInstanceFactory
        implements MetadataAwareAspectInstanceFactory {

  private final AspectMetadata metadata;

  /**
   * Create a new SimpleMetadataAwareAspectInstanceFactory for the given aspect class.
   *
   * @param aspectClass the aspect class
   * @param aspectName the aspect name
   */
  public SimpleMetadataAwareAspectInstanceFactory(Class<?> aspectClass, String aspectName) {
    super(aspectClass);
    this.metadata = new AspectMetadata(aspectClass, aspectName);
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
    return OrderUtils.getOrder(aspectClass, Ordered.LOWEST_PRECEDENCE);
  }

}
