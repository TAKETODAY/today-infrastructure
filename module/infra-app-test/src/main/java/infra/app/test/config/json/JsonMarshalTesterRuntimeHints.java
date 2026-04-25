/*
 * Copyright 2012-present the original author or authors.
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

package infra.app.test.config.json;

import org.jspecify.annotations.Nullable;

import java.lang.reflect.Method;

import infra.aot.hint.ExecutableMode;
import infra.aot.hint.MemberCategory;
import infra.aot.hint.ReflectionHints;
import infra.aot.hint.RuntimeHints;
import infra.aot.hint.RuntimeHintsRegistrar;
import infra.app.test.json.AbstractJsonMarshalTester;
import infra.core.ResolvableType;
import infra.lang.Assert;
import infra.util.ReflectionUtils;

/**
 * Base class for {@link AbstractJsonMarshalTester} runtime hints.
 *
 * @author Phillip Webb
 * @since 5.0
 */
@SuppressWarnings("rawtypes")
public abstract class JsonMarshalTesterRuntimeHints implements RuntimeHintsRegistrar {

  private final Class<? extends AbstractJsonMarshalTester> tester;

  protected JsonMarshalTesterRuntimeHints(Class<? extends AbstractJsonMarshalTester> tester) {
    this.tester = tester;
  }

  @Override
  public void registerHints(RuntimeHints hints, @Nullable ClassLoader classLoader) {
    ReflectionHints reflection = hints.reflection();
    reflection.registerType(this.tester, MemberCategory.INVOKE_DECLARED_CONSTRUCTORS);
    Method method = ReflectionUtils.findMethod(this.tester, "initialize", Class.class, ResolvableType.class);
    Assert.state(method != null, "'method' is required");
    reflection.registerMethod(method, ExecutableMode.INVOKE);
  }

}
