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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cn.taketoday.web.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import cn.taketoday.core.annotation.AliasFor;
import cn.taketoday.http.HttpMethod;
import cn.taketoday.lang.Constant;

/**
 * @author Today <br>
 * 2018-07-01 14:09:32
 */
@Retention(RetentionPolicy.RUNTIME)
@ActionMapping(method = HttpMethod.TRACE)
@Target({ ElementType.METHOD, ElementType.TYPE })
public @interface TRACE {

  /** urls */
  @AliasFor(annotation = ActionMapping.class)
  String[] value() default Constant.BLANK;

  /**
   * Alias for {@link ActionMapping#path}.
   *
   * @since 4.0
   */
  @AliasFor(annotation = ActionMapping.class)
  String[] path() default Constant.BLANK;

  /** Exclude url on class */
  @AliasFor(annotation = ActionMapping.class)
  boolean exclude() default false;

  /**
   * Alias for {@link ActionMapping#params}.
   *
   * @since 4.0
   */
  @AliasFor(annotation = ActionMapping.class)
  String[] params() default {};

  /**
   * Alias for {@link ActionMapping#consumes}.
   *
   * @since 4.0
   */
  @AliasFor(annotation = ActionMapping.class)
  String[] consumes() default {};

  /**
   * Alias for {@link ActionMapping#produces}.
   *
   * @since 4.0
   */
  @AliasFor(annotation = ActionMapping.class)
  String[] produces() default {};

}
