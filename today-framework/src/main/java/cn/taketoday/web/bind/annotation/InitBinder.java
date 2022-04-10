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

package cn.taketoday.web.bind.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import cn.taketoday.web.RequestContext;
import cn.taketoday.web.annotation.RequestMapping;

/**
 * Annotation that identifies methods which initialize the
 * {@link cn.taketoday.web.bind.WebDataBinder} which
 * will be used for populating command and form object arguments
 * of annotated handler methods.
 *
 * <p>Such init-binder methods support all arguments that {@link RequestMapping}
 * supports, except for command/form objects and corresponding validation result
 * objects. Init-binder methods must not have a return value; they are usually
 * declared as {@code void}.
 *
 * <p>Typical arguments are {@link cn.taketoday.web.bind.WebDataBinder}
 * in combination with {@link RequestContext}
 * or {@link java.util.Locale}, allowing to register context-specific editors.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see cn.taketoday.web.bind.WebDataBinder
 * @since 4.0 2022/4/8 22:49
 */
@Documented
@Target({ ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface InitBinder {

  /**
   * The names of command/form attributes and/or request parameters
   * that this init-binder method is supposed to apply to.
   * <p>Default is to apply to all command/form attributes and all request parameters
   * processed by the annotated handler class. Specifying model attribute names or
   * request parameter names here restricts the init-binder method to those specific
   * attributes/parameters, with different init-binder methods typically applying to
   * different groups of attributes or parameters.
   */
  String[] value() default {};

}

