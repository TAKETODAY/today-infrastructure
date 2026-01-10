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

package infra.beans.factory.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import infra.beans.factory.support.DependencyResolvingStrategy;

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
