/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.context.condition;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import cn.taketoday.context.annotation.Conditional;
import cn.taketoday.core.JavaVersion;

/**
 * {@link Conditional @Conditional} that only matches when virtual threads are available
 * and enabled.
 *
 * @author Moritz Halbritter
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/7/1 23:17
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@ConditionalOnJava(JavaVersion.TWENTY_ONE)
@ConditionalOnThreading(Threading.VIRTUAL)
@Target({ ElementType.TYPE, ElementType.METHOD })
public @interface ConditionalOnVirtualThreads {

}