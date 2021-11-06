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

package cn.taketoday.core.annotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Map;

import cn.taketoday.lang.Nullable;

/**
 * Strategy API for extracting a value for an annotation attribute from a given
 * source object which is typically an {@link Annotation}, {@link Map}, or
 * {@link cn.taketoday.core.annotation.TypeMappedAnnotation}.
 *
 * @author Sam Brannen
 * @since 4.0
 */
@FunctionalInterface
interface ValueExtractor {

  /**
   * Extract the annotation attribute represented by the supplied {@link Method}
   * from the supplied source {@link Object}.
   */
  @Nullable
  Object extract(Method attribute, @Nullable Object object);

}
