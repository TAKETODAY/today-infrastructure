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

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptException;

import infra.scripting.ScriptCompilationException;
import infra.scripting.ScriptSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.withSettings;

class StandardScriptFactoryExecutionTests {

  @Test
  void instantiatesCompatibleClassesAndReevaluatesModifiedSourceUsingTheSameEngine() throws Exception {
    var engine = mock(ScriptEngine.class);
    var factory = factoryWithEngine(engine);
    var source = new StaticScriptSource("first");
    given(engine.eval("first")).willReturn(StringBuilder.class);
    given(engine.eval("second")).willReturn("new value");

    assertThat(factory.requiresScriptedObjectRefresh(source)).isTrue();
    assertThat(factory.getScriptedObject(source, CharSequence.class)).isInstanceOf(StringBuilder.class);
    assertThat(factory.requiresScriptedObjectRefresh(source)).isFalse();
    assertThat(factory.getScriptedObjectType(source)).isNull();
    source.setScript("second");
    assertThat(factory.requiresScriptedObjectRefresh(source)).isTrue();
    assertThat(factory.getScriptedObject(source, CharSequence.class)).isEqualTo("new value");
  }

  @Test
  void adaptsNonMatchingResultsUsingTheInvocableEngine() throws Exception {
    var engine = mock(ScriptEngine.class, withSettings().extraInterfaces(Invocable.class));
    var invocable = (Invocable) engine;
    var factory = factoryWithEngine(engine);
    var source = new StaticScriptSource("script");
    var scriptObject = new Object();
    Runnable adapter = () -> { };
    given(engine.eval("script")).willReturn(scriptObject).willReturn(null);
    given(invocable.getInterface(scriptObject, Runnable.class)).willReturn(adapter);
    given(invocable.getInterface(Runnable.class)).willReturn(adapter);

    assertThat(factory.getScriptedObject(source, Runnable.class)).isSameAs(adapter);
    then(invocable).should().getInterface(scriptObject, Runnable.class);
    assertThat(factory.getScriptedObject(source, Runnable.class)).isSameAs(adapter);
    then(invocable).should().getInterface(Runnable.class);
  }

  @Test
  void reportsUnsupportedOrUnavailableInterfaceAdaptation() throws Exception {
    var source = new StaticScriptSource("script");
    var plainEngine = mock(ScriptEngine.class);
    var invocableEngine = mock(ScriptEngine.class, withSettings().extraInterfaces(Invocable.class));
    var plainFactory = factoryWithEngine(plainEngine);
    var invocableFactory = factoryWithEngine(invocableEngine);

    assertThatThrownBy(() -> plainFactory.getScriptedObject(source, Runnable.class))
            .isInstanceOf(ScriptCompilationException.class).hasMessageContaining("must implement Invocable");
    assertThatThrownBy(() -> invocableFactory.getScriptedObject(source, Runnable.class, AutoCloseable.class))
            .isInstanceOf(ScriptCompilationException.class).hasMessageContaining("Could not adapt script to interface");
  }

  @Test
  void reportsInstantiationFailuresWithTheirOriginalCauses() throws Exception {
    var engine = mock(ScriptEngine.class);
    var factory = factoryWithEngine(engine);
    var source = new StaticScriptSource("script");
    given(engine.eval("script")).willReturn(Integer.class, AbstractScript.class, ThrowingConstructor.class);

    assertThatThrownBy(() -> factory.getScriptedObject(source))
            .isInstanceOf(ScriptCompilationException.class).hasCauseInstanceOf(NoSuchMethodException.class);
    assertThatThrownBy(() -> factory.getScriptedObject(source))
            .isInstanceOf(ScriptCompilationException.class).hasCauseInstanceOf(InstantiationException.class);
    assertThatThrownBy(() -> factory.getScriptedObject(source))
            .isInstanceOf(ScriptCompilationException.class).hasCauseInstanceOf(IOException.class);
  }

  @Test
  void distinguishesEvaluationFailureFromMissingScriptEngine() throws Exception {
    var engine = mock(ScriptEngine.class);
    var factory = factoryWithEngine(engine);
    var source = new StaticScriptSource("script");
    var failure = new ScriptException("invalid script");
    given(engine.eval("script")).willThrow(failure);

    assertThatThrownBy(() -> factory.getScriptedObject(source))
            .isInstanceOf(ScriptCompilationException.class).hasCause(failure);
    assertThatThrownBy(() -> new StandardScriptFactory("inline:script").getScriptedObject(source))
            .isInstanceOf(ScriptCompilationException.class).hasCauseInstanceOf(IllegalStateException.class);
  }

  private static StandardScriptFactory factoryWithEngine(ScriptEngine engine) {
    return new StandardScriptFactory("inline:script", Runnable.class) {
      private boolean engineRequested;

      @Override
      protected ScriptEngine retrieveScriptEngine(ScriptSource scriptSource) {
        if (this.engineRequested) {
          throw new AssertionError("The script engine was not cached");
        }
        this.engineRequested = true;
        return engine;
      }
    };
  }

  private abstract static class AbstractScript {
  }

  private static class ThrowingConstructor {

    ThrowingConstructor() throws IOException {
      throw new IOException("constructor failed");
    }
  }

}
