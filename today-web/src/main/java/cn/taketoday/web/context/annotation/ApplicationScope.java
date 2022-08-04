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

package cn.taketoday.web.context.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import cn.taketoday.context.annotation.Scope;
import cn.taketoday.web.WebApplicationContext;

/**
 * {@code @ApplicationScope} is a specialization of {@link Scope @Scope} for a
 * component whose lifecycle is bound to the current web application.
 *
 * <p>{@code @ApplicationScope} may be used as a meta-annotation to create custom
 * composed annotations.
 *
 * @author Sam Brannen
 * @see RequestScope
 * @see SessionScope
 * @see cn.taketoday.context.annotation.Scope
 * @see cn.taketoday.web.WebApplicationContext#SCOPE_APPLICATION
 * @see cn.taketoday.web.context.support.ServletContextScope
 * @see cn.taketoday.stereotype.Component
 * @see cn.taketoday.context.annotation.Bean
 * @since 4.0
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Scope(WebApplicationContext.SCOPE_APPLICATION)
public @interface ApplicationScope {

}
