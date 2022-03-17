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

import cn.taketoday.aop.aspectj.AspectInstanceFactory;
import cn.taketoday.lang.Nullable;

/**
 * Subinterface of {@link AspectInstanceFactory}
 * that returns {@link AspectMetadata} associated with AspectJ-annotated classes.
 *
 * <p>Ideally, AspectInstanceFactory would include this method itself, but because
 * AspectMetadata uses Java-5-only {@link org.aspectj.lang.reflect.AjType},
 * we need to split out this subinterface.
 *
 * @author Rod Johnson
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
