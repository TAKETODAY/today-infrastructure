/*
 * Copyright 2002-present the original author or authors.
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

package infra.aot.hint.annotation;

import infra.aot.hint.BindingReflectionHintsRegistrar;
import infra.aot.hint.MemberCategory;
import infra.aot.hint.ReflectionHints;

/**
 * A {@link ReflectiveProcessor} implementation that registers reflection hints
 * for data binding purpose, that is class, constructors, fields, properties,
 * record components, including types transitively used on properties and record
 * components.
 *
 * @author Sebastien Deleuze
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see RegisterReflectionForBinding @RegisterReflectionForBinding
 * @since 4.0
 */
class RegisterReflectionForBindingProcessor extends RegisterReflectionReflectiveProcessor {

  private final BindingReflectionHintsRegistrar bindingRegistrar = new BindingReflectionHintsRegistrar();

  @Override
  protected void registerReflectionHints(ReflectionHints hints, Class<?> target, MemberCategory[] memberCategories) {
    this.bindingRegistrar.registerReflectionHints(hints, target);
  }

}
