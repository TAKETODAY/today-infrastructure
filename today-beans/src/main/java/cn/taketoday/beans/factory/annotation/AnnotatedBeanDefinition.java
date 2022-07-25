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

package cn.taketoday.beans.factory.annotation;

import cn.taketoday.beans.factory.config.BeanDefinition;
import cn.taketoday.core.type.AnnotationMetadata;
import cn.taketoday.core.type.MethodMetadata;
import cn.taketoday.lang.Nullable;

/**
 * Extended {@link cn.taketoday.beans.factory.config.BeanDefinition}
 * interface that exposes {@link cn.taketoday.core.type.AnnotationMetadata}
 * about its bean class - without requiring the class to be loaded yet.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see AnnotatedGenericBeanDefinition
 * @see cn.taketoday.core.type.AnnotationMetadata
 * @since 4.0 2022/3/8 21:00
 */
public interface AnnotatedBeanDefinition extends BeanDefinition {

  /**
   * Obtain the annotation metadata (as well as basic class metadata)
   * for this bean definition's bean class.
   *
   * @return the annotation metadata object (never {@code null})
   */
  AnnotationMetadata getMetadata();

  /**
   * Obtain metadata for this bean definition's factory method, if any.
   *
   * @return the factory method metadata, or {@code null} if none
   */
  @Nullable
  MethodMetadata getFactoryMethodMetadata();

}
