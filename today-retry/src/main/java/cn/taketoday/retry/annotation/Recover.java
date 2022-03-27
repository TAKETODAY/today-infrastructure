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

package cn.taketoday.retry.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import cn.taketoday.context.annotation.Import;

/**
 * Annotation for a method invocation that is a recovery handler. A suitable recovery
 * handler has a first parameter of type Throwable (or a subtype of Throwable) and a
 * return value of the same type as the <code>@Retryable</code> method to recover from.
 * The Throwable first argument is optional (but a method without it will only be called
 * if no others match). Subsequent arguments are populated from the argument list of the
 * failed method in order.
 *
 * @author Dave Syer
 * @since 4.0
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Import(RetryConfiguration.class)
@Target({ ElementType.METHOD, ElementType.TYPE })
public @interface Recover {

}
