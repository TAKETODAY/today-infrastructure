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

package infra.aop.aspectj.annotation;

import org.jspecify.annotations.Nullable;

import java.io.Serializable;

import infra.lang.Assert;

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
    Assert.notNull(maaif, "AspectInstanceFactory is required");
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
