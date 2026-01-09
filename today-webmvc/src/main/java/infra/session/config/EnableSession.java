/*
 * Copyright 2017 - 2026 the TODAY authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
 * @EnableSession
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
public @interface EnableSession {

}
