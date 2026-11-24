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

package infra.scripting.support;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.script.SimpleBindings;

import infra.core.io.ByteArrayResource;
import infra.scripting.ScriptCompilationException;
import infra.scripting.ScriptSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

class StandardScriptEvaluatorTests {

  @Test
  void usesGlobalBindingsUnlessTheScriptArgumentsOverrideThem() {
    var evaluator = new StandardScriptEvaluator();
    evaluator.setLanguage("groovy");
    evaluator.setGlobalBindings(Map.of("name", "global"));
    evaluator.setBeanClassLoader(getClass().getClassLoader());
    var source = new StaticScriptSource("name.toUpperCase(java.util.Locale.ROOT)");

    assertThat(evaluator.evaluate(source)).isEqualTo("GLOBAL");
    assertThat(evaluator.evaluate(source, Map.of("name", "local"))).isEqualTo("LOCAL");
    assertThat(evaluator.evaluate(source, Map.of())).isEqualTo("GLOBAL");
  }

  @Test
  void passesThroughExistingBindingsToTheManagerAndEngine() throws Exception {
    var manager = mock(ScriptEngineManager.class);
    var engine = mock(ScriptEngine.class);
    var evaluator = new StandardScriptEvaluator(manager);
    var globals = new SimpleBindings();
    var arguments = new SimpleBindings(Map.of("value", "local"));
    evaluator.setEngineName("test");
    evaluator.setGlobalBindings(globals);
    given(manager.getEngineByName("test")).willReturn(engine);
    given(engine.eval("value", arguments)).willReturn("local");

    assertThat(evaluator.evaluate(new StaticScriptSource("value"), arguments)).isEqualTo("local");
    then(manager).should().setBindings(globals);
  }

  @Test
  void selectsAnEngineByFileExtensionForBothEvaluationAndScriptedObjects() throws Exception {
    var evaluator = new StandardScriptEvaluator(getClass().getClassLoader());
    var source = resource("expression.groovy");

    assertThat(evaluator.evaluate(source)).isEqualTo(42);
    assertThat(new StandardScriptFactory("expression.groovy").getScriptedObject(source)).isEqualTo(42);
    assertThat(new StandardScriptFactory("groovy", "inline:expression").getScriptedObject(new StaticScriptSource("6 * 7")))
            .isEqualTo(42);
  }

  @Test
  void reportsMissingOrUnknownScriptLanguage() {
    var evaluator = new StandardScriptEvaluator();

    assertThatIllegalStateException().isThrownBy(() -> evaluator.evaluate(new StaticScriptSource("6 * 7")))
            .withMessageContaining("no resource associated");
    assertThatIllegalStateException().isThrownBy(() -> evaluator.evaluate(resource("expression")))
            .withMessageContaining("no file extension defined");
    assertThatIllegalStateException().isThrownBy(() -> evaluator.evaluate(resource("expression.no-test-engine")))
            .withMessageContaining("No matching engine found");
  }

  @Test
  void distinguishesSourceReadErrorsFromEngineEvaluationErrors() throws Exception {
    var manager = mock(ScriptEngineManager.class);
    var engine = mock(ScriptEngine.class);
    var evaluator = new StandardScriptEvaluator(manager);
    evaluator.setEngineName("test");
    given(manager.getEngineByName("test")).willReturn(engine);
    var source = mock(ScriptSource.class);
    var readFailure = new IOException("script unavailable");
    given(source.getScriptAsString()).willThrow(readFailure);
    var scriptFailure = new ScriptException("script failed");
    given(engine.eval("invalid")).willThrow(scriptFailure);

    assertThatThrownBy(() -> evaluator.evaluate(source)).isInstanceOf(ScriptCompilationException.class)
            .hasCause(readFailure).hasMessageContaining("Cannot access script");
    assertThatThrownBy(() -> evaluator.evaluate(new StaticScriptSource("invalid")))
            .isInstanceOf(ScriptCompilationException.class)
            .cause().isInstanceOfSatisfying(StandardScriptEvalException.class,
                cause -> assertThat(cause.getScriptException()).isSameAs(scriptFailure));
  }

  private static ResourceScriptSource resource(String name) {
    return new ResourceScriptSource(new ByteArrayResource("6 * 7".getBytes(StandardCharsets.UTF_8)) {
      @Override
      public String getName() {
        return name;
      }
    });
  }

}
