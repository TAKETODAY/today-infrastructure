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
package cn.taketoday.web.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import cn.taketoday.core.annotation.AliasFor;
import cn.taketoday.http.HttpMethod;

/**
 * Annotation for mapping HTTP {@code POST} requests onto specific handler
 * methods.
 *
 * <p>Specifically, {@code @PostMapping} is a <em>composed annotation</em> that
 * acts as a shortcut for {@code @ActionMapping(method = HttpMethod.POST)}.
 *
 * @author TODAY 2020/12/8 21:48
 */
@Retention(RetentionPolicy.RUNTIME)
@ActionMapping(method = HttpMethod.POST)
@Target({ ElementType.METHOD, ElementType.TYPE })
public @interface PostMapping {

  /**
   * Alias for {@link ActionMapping#name}.
   */
  @AliasFor(annotation = ActionMapping.class)
  String name() default "";

  /**
   * Alias for {@link ActionMapping#value}.
   */
  @AliasFor(annotation = ActionMapping.class)
  String[] value() default {};

  /**
   * Alias for {@link ActionMapping#path}.
   */
  @AliasFor(annotation = ActionMapping.class)
  String[] path() default {};

  /**
   * Combine this condition with another such as conditions from a
   * type-level and method-level {@code @RequestMapping} annotation.
   */
  @AliasFor(annotation = ActionMapping.class)
  boolean combine() default true;

  /**
   * Alias for {@link ActionMapping#params}.
   */
  @AliasFor(annotation = ActionMapping.class)
  String[] params() default {};

  /**
   * Alias for {@link ActionMapping#headers}.
   */
  @AliasFor(annotation = ActionMapping.class)
  String[] headers() default {};

  /**
   * Alias for {@link ActionMapping#consumes}.
   */
  @AliasFor(annotation = ActionMapping.class)
  String[] consumes() default {};

  /**
   * Alias for {@link ActionMapping#produces}.
   */
  @AliasFor(annotation = ActionMapping.class)
  String[] produces() default {};

}
