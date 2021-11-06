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
package cn.taketoday.lang;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * When used as a type-level annotation in conjunction with
 * {@link Component @Component}, {@code @Scope} indicates the
 * name of a scope to use for instances of the annotated type.
 *
 * <p>When used as a method-level annotation in conjunction with
 * {@link Component @Component}, {@code @Scope} indicates the name of a scope to use
 * for the instance returned from the method.
 *
 * <p><b>NOTE:</b> {@code @Scope} annotations are only introspected on the
 * concrete bean class (for annotated components) or the factory method
 * (for {@code @Component} methods).
 *
 * <p>In this context, <em>scope</em> means the lifecycle of an instance,
 * such as {@code singleton}, {@code prototype}.
 *
 * <p>To register additional custom scopes, see
 * {@link cn.taketoday.beans.factory.CustomScopeConfigurer CustomScopeConfigurer}.
 *
 * @author TODAY 2021/10/26 15:33
 * @see Component
 * @since 4.0
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Scope {

  /**
   * @since 2.1.7
   */
  String SINGLETON = "singleton";

  /**
   * @since 2.1.7
   */
  String PROTOTYPE = "prototype";

  /**
   * Specifies the name of the scope to use for the annotated component/bean.
   * <p>Defaults is {@link cn.taketoday.beans.factory.Scope#SINGLETON SCOPE_SINGLETON}.
   *
   * @see cn.taketoday.beans.factory.Scope#PROTOTYPE
   * @see cn.taketoday.beans.factory.Scope#SINGLETON
   * @see cn.taketoday.web.WebApplicationContext#SCOPE_REQUEST
   * @see cn.taketoday.web.WebApplicationContext#SCOPE_SESSION
   */
  String value() default SINGLETON;

}
