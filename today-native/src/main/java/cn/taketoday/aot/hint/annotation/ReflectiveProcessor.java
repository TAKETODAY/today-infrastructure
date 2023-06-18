/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.aot.hint.annotation;

import java.lang.reflect.AnnotatedElement;

import cn.taketoday.aot.hint.ReflectionHints;

/**
 * Process an {@link AnnotatedElement} and register the necessary reflection
 * hints for it.
 *
 * <p>{@code ReflectiveProcessor} implementations are registered via
 * {@link Reflective#processors() @Reflective(processors = ...)}.
 *
 * @author Stephane Nicoll
 * @see Reflective @Reflective
 * @since 4.0
 */
public interface ReflectiveProcessor {

  /**
   * Register {@link ReflectionHints} against the specified {@link AnnotatedElement}.
   *
   * @param hints the reflection hints instance to use
   * @param element the element to process
   */
  void registerReflectionHints(ReflectionHints hints, AnnotatedElement element);

}
