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

import infra.scripting.ScriptCompilationException;
import infra.scripting.ScriptSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/4/2 20:23
 */
class StandardScriptFactoryTests {

  @Test
  void scriptFactoryWithoutEngineNameCreatesInstance() {
    StandardScriptFactory factory = new StandardScriptFactory("test.js");
    assertThat(factory.getScriptSourceLocator()).isEqualTo("test.js");
    assertThat(factory.getScriptInterfaces()).isNull();
  }

  @Test
  void scriptFactoryWithEngineNameCreatesInstance() {
    StandardScriptFactory factory = new StandardScriptFactory("javascript", "test.js");
    assertThat(factory.getScriptSourceLocator()).isEqualTo("test.js");
  }

  @Test
  void emptyScriptSourceLocatorThrowsException() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> new StandardScriptFactory(""))
            .withMessage("'scriptSourceLocator' must not be empty");
  }

  @Test
  void requiresConfigInterfaceReturnsFalse() {
    StandardScriptFactory factory = new StandardScriptFactory("test.js");
    assertThat(factory.requiresConfigInterface()).isFalse();
  }

  @Test
  void getScriptedObjectTypeReturnsNull() throws IOException {
    StandardScriptFactory factory = new StandardScriptFactory("test.js");
    ScriptSource source = mock(ScriptSource.class);
    assertThat(factory.getScriptedObjectType(source)).isNull();
  }

  @Test
  void requiresScriptedObjectRefreshDelegatesToSource() throws IOException {
    StandardScriptFactory factory = new StandardScriptFactory("test.js");
    ScriptSource source = mock(ScriptSource.class);
    assertThat(factory.requiresScriptedObjectRefresh(source)).isFalse();
  }

  @Test
  void toStringContainsScriptSourceLocator() {
    StandardScriptFactory factory = new StandardScriptFactory("test.js");
    assertThat(factory.toString()).contains("test.js");
  }

  interface TestInterface {
    String getValue();
  }

  @Test
  void scriptFactoryWithInterfacesCreatesInstance() {
    StandardScriptFactory factory = new StandardScriptFactory("test.js", TestInterface.class);
    assertThat(factory.getScriptInterfaces()).containsExactly(TestInterface.class);
  }

  @Test
  void scriptEngineEvaluationErrorWrapsException() throws IOException {
    StandardScriptFactory factory = new StandardScriptFactory("javascript", "test.js");
    ScriptSource source = mock(ScriptSource.class);
    when(source.getScriptAsString()).thenReturn("invalid javascript code");

    assertThatExceptionOfType(ScriptCompilationException.class)
            .isThrownBy(() -> factory.getScriptedObject(source));
  }

  @Test
  void adaptToInterfacesWithNullScriptThrowsException() throws IOException {
    StandardScriptFactory factory = new StandardScriptFactory("javascript", "test.js");
    ScriptSource source = mock(ScriptSource.class);
    when(source.getScriptAsString()).thenReturn("null");

    assertThatExceptionOfType(ScriptCompilationException.class)
            .isThrownBy(() -> factory.getScriptedObject(source, TestInterface.class));
  }

  @Test
  void getScriptedObjectWithMultipleInterfaces() throws IOException {
    interface Interface1 {
      String method1();
    }
    interface Interface2 {
      void method2();
    }

    StandardScriptFactory factory = new StandardScriptFactory("javascript", "test.js");
    ScriptSource source = mock(ScriptSource.class);
    when(source.getScriptAsString()).thenReturn("var obj = { method1: function() { return 'test'; }, method2: function() {} };");

    assertThatExceptionOfType(ScriptCompilationException.class)
            .isThrownBy(() -> factory.getScriptedObject(source, Interface1.class, Interface2.class));
  }

  @Test
  void scriptSourceReturningUndefinedThrowsException() throws IOException {
    StandardScriptFactory factory = new StandardScriptFactory("javascript", "test.js");
    ScriptSource source = mock(ScriptSource.class);
    when(source.getScriptAsString()).thenReturn("undefined");

    assertThatExceptionOfType(ScriptCompilationException.class)
            .isThrownBy(() -> factory.getScriptedObject(source, TestInterface.class));
  }

  @Test
  void getScriptedObjectWithInvalidScriptSourceThrowsException() throws IOException {
    StandardScriptFactory factory = new StandardScriptFactory("javascript", "test.js");
    ScriptSource source = mock(ScriptSource.class);
    when(source.getScriptAsString()).thenThrow(new IOException("Failed to read script"));

    assertThatExceptionOfType(ScriptCompilationException.class)
            .isThrownBy(() -> factory.getScriptedObject(source));
  }

}