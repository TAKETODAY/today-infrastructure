/*
 * Copyright 2017 - 2025 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package infra.session.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import infra.context.annotation.Import;

/**
 * Annotation to enable web session support.
 *
 * <p>
 * This annotation enables web session functionality, such as HTTP session management
 * in servlet-based applications. When present on a configuration class, it imports
 * the {@link WebSessionConfiguration} which provides the necessary infrastructure
 * for handling web sessions.
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * @Configuration
 * @EnableWebSession
 * public class MyWebConfiguration {
 *     // Configuration code here
 * }
 * }</pre>
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @see WebSessionConfiguration
 * @since 2019-10-03 00:30
 */
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Import(WebSessionConfiguration.class)
public @interface EnableWebSession {

}
