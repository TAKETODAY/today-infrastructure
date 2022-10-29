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

package cn.taketoday.web.config.jackson;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import cn.taketoday.core.annotation.AliasFor;

/**
 * Provides a mixin class implementation that registers with Jackson when using
 * {@link JsonMixinModule}.
 *
 * @author Guirong Hu
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see JsonMixinModule
 * @since 4.0 2022/10/29 21:29
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface JsonMixin {

  /**
   * Alias for the {@link #type()} attribute. Allows for more concise annotation
   * declarations e.g.: {@code @JsonMixin(MyType.class)} instead of
   * {@code @JsonMixin(type=MyType.class)}.
   *
   * @return the mixed-in classes
   */
  @AliasFor("type")
  Class<?>[] value() default {};

  /**
   * The types that are handled by the provided mix-in class. {@link #value()} is an
   * alias for (and mutually exclusive with) this attribute.
   *
   * @return the mixed-in classes
   */
  @AliasFor("value")
  Class<?>[] type() default {};

}
