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