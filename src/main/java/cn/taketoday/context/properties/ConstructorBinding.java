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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package cn.taketoday.context.properties;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation that can be used to indicate which constructor to use when binding
 * configuration properties using constructor arguments rather than by calling setters. A
 * single parameterized constructor implicitly indicates that constructor binding should
 * be used unless the constructor is annotated with `@Autowired`.
 * <p>
 * Note: To use constructor binding the class must be enabled using
 * {@link EnableConfigurationProperties @EnableConfigurationProperties} or configuration
 * property scanning. Constructor binding cannot be used with beans that are created by
 * the regular Spring mechanisms (e.g.
 * {@link cn.taketoday.lang.Component @Component} beans, beans created via
 * {@link cn.taketoday.context.annotation.Bean @Bean} methods or beans loaded using
 * {@link cn.taketoday.context.annotation.Import @Import}).
 *
 * @author Phillip Webb
 * @see ConfigurationProperties
 * @since 4.0
 */
@Documented
@Target(ElementType.CONSTRUCTOR)
@Retention(RetentionPolicy.RUNTIME)
public @interface ConstructorBinding {

}
