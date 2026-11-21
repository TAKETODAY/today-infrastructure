/*
 * Copyright 2023 the original author or authors.
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

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.shell.command.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import infra.context.annotation.Import;
import infra.shell.command.annotation.support.EnableCommandRegistrar;
import infra.stereotype.Component;

/**
 * Enable support for {@link Command @Command} annotated classes. {@code @Command} classes
 * can be registered directly on this annotation.
 *
 * @author Janne Valkealahti
 * @author Mahmoud Ben Hassine
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(EnableCommandRegistrar.class)
@Component
public @interface EnableCommand {

  /**
   * Defines candidate classes for shell commands.
   *
   * @return candidate classes for shell commands
   */
  Class<?>[] value() default {};

}
