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

import infra.aop.aspectj.AspectInstanceFactory;

/**
 * Subinterface of {@link AspectInstanceFactory}
 * that returns {@link AspectMetadata} associated with AspectJ-annotated classes.
 *
 * @author Rod Johnson
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see AspectMetadata
 * @see org.aspectj.lang.reflect.AjType
 * @since 4.0
 */
public interface MetadataAwareAspectInstanceFactory extends AspectInstanceFactory {

  /**
   * Return the AspectJ AspectMetadata for this factory's aspect.
   *
   * @return the aspect metadata
   */
  AspectMetadata getAspectMetadata();

  /**
   * Return the best possible creation mutex for this factory.
   *
   * @return the mutex object (may be {@code null} for no mutex to use)
   */
  @Nullable
  Object getAspectCreationMutex();

}
