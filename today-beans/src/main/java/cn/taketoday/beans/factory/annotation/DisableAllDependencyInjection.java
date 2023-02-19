/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.beans.factory.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import cn.taketoday.beans.factory.support.DependencyResolvingStrategy;

/**
 * Disable dependency-injection for all the factory method bean
 * <pre>{@code
 * @Configuration
 * @DisableDependencyInjection
 * @DisableAllDependencyInjection
 * class Config {
 *
 *   @Singleton
 *   Bean bean() {
 *     return new Bean();
 *   }
 *
 *   @Singleton
 *   @DisableDependencyInjection
 *   Bean bean() {
 *     return new Bean();
 *   }
 *
 *   @Autowired
 *   void bean(Bean bean) {
 *    // Autowired ignored because @DisableDependencyInjection on class
 *    // all DependencyResolvingStrategy disabled
 *   }
 * }
 * }</pre>
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang 2021/11/23 22:06</a>
 * @see DependencyResolvingStrategy
 * @since 4.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
public @interface DisableAllDependencyInjection {

}
