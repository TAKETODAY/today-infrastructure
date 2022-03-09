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

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;

/**
 * Decorator to cause a {@link MetadataAwareAspectInstanceFactory} to instantiate only once.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 4.0
 */
@SuppressWarnings("serial")
public class LazySingletonAspectInstanceFactoryDecorator implements MetadataAwareAspectInstanceFactory, Serializable {

  private final MetadataAwareAspectInstanceFactory maaif;

  @Nullable
  private volatile Object materialized;

  /**
   * Create a new lazily initializing decorator for the given AspectInstanceFactory.
   *
   * @param maaif the MetadataAwareAspectInstanceFactory to decorate
   */
  public LazySingletonAspectInstanceFactoryDecorator(MetadataAwareAspectInstanceFactory maaif) {
    Assert.notNull(maaif, "AspectInstanceFactory must not be null");
    this.maaif = maaif;
  }

  @Override
  public Object getAspectInstance() {
    Object aspectInstance = this.materialized;
    if (aspectInstance == null) {
      Object mutex = this.maaif.getAspectCreationMutex();
      if (mutex == null) {
        aspectInstance = this.maaif.getAspectInstance();
        this.materialized = aspectInstance;
      }
      else {
        synchronized(mutex) {
          aspectInstance = this.materialized;
          if (aspectInstance == null) {
            aspectInstance = this.maaif.getAspectInstance();
            this.materialized = aspectInstance;
          }
        }
      }
    }
    return aspectInstance;
  }

  public boolean isMaterialized() {
    return (this.materialized != null);
  }

  @Override
  @Nullable
  public ClassLoader getAspectClassLoader() {
    return this.maaif.getAspectClassLoader();
  }

  @Override
  public AspectMetadata getAspectMetadata() {
    return this.maaif.getAspectMetadata();
  }

  @Override
  @Nullable
  public Object getAspectCreationMutex() {
    return this.maaif.getAspectCreationMutex();
  }

  @Override
  public int getOrder() {
    return this.maaif.getOrder();
  }

  @Override
  public String toString() {
    return "LazySingletonAspectInstanceFactoryDecorator: decorating " + this.maaif;
  }

}
