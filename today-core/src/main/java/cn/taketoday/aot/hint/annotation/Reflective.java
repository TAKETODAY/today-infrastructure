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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import cn.taketoday.core.annotation.AliasFor;

/**
 * Indicate that the annotated element requires reflection.
 *
 * <p>When present, either directly or as a meta-annotation, this annotation
 * triggers the configured {@linkplain ReflectiveProcessor processors} against
 * the annotated element. By default, a reflection hint is registered for the
 * annotated element so that it can be discovered and invoked if necessary.
 *
 * @author Stephane Nicoll
 * @author Sam Brannen
 * @see SimpleReflectiveProcessor
 * @see ReflectiveRuntimeHintsRegistrar
 * @see RegisterReflectionForBinding @RegisterReflectionForBinding
 * @since 4.0
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.ANNOTATION_TYPE, ElementType.TYPE,
        ElementType.CONSTRUCTOR, ElementType.FIELD, ElementType.METHOD })
public @interface Reflective {

  /**
   * Alias for {@link #processors()}.
   */
  @AliasFor("processors")
  Class<? extends ReflectiveProcessor>[] value() default SimpleReflectiveProcessor.class;

  /**
   * {@link ReflectiveProcessor} implementations to invoke against the
   * annotated element.
   */
  @AliasFor("value")
  Class<? extends ReflectiveProcessor>[] processors() default SimpleReflectiveProcessor.class;

}
