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

package infra.scripting.groovy;

import org.codehaus.groovy.control.CompilationFailedException;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.customizers.ImportCustomizer;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicInteger;

import groovy.lang.GroovyClassLoader;
import groovy.lang.MetaClass;
import groovy.lang.Script;
import infra.beans.factory.config.ConfigurableBeanFactory;
import infra.scripting.ScriptCompilationException;
import infra.scripting.support.StaticScriptSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

class GroovyScriptFactoryTests {

  @Test
  void reusesTheResultOfTypeDiscoveryOnceAndRefreshesWhenTheSourceChanges() throws Exception {
    var executions = new AtomicInteger();
    var factory = new GroovyScriptFactory("inline:counter", object ->
            ((Script) object).setProperty("counter", executions));
    var source = new StaticScriptSource("return counter.incrementAndGet()", "Counter.groovy");

    try (var loader = factory.getGroovyClassLoader()) {
      assertThat(factory.getScriptedObjectType(source)).isEqualTo(Integer.class);
      assertThat(factory.getScriptedObjectType(source)).isEqualTo(Integer.class);
      assertThat(factory.requiresScriptedObjectRefresh(source)).isTrue();
      assertThat(factory.getScriptedObject(source)).isEqualTo(1);
      assertThat(factory.requiresScriptedObjectRefresh(source)).isFalse();
      assertThat(factory.getScriptedObject(source)).isEqualTo(2);

      source.setScript("return counter.incrementAndGet().toString()");
      assertThat(factory.requiresScriptedObjectRefresh(source)).isTrue();
      assertThat(factory.getScriptedObject(source)).isEqualTo("3");
      assertThat(factory.getScriptedObjectType(source)).isEqualTo(String.class);
    }
  }

  @Test
  void returnsInstancesOfDeclaredClassesInsteadOfExecutingThemAsScripts() throws Exception {
    var factory = new GroovyScriptFactory("inline:task");
    var source = new StaticScriptSource("class Task implements Runnable { void run() {} }", "Task.groovy");

    try (var loader = factory.getGroovyClassLoader()) {
      assertThat(factory.getScriptedObjectType(source)).isAssignableTo(Runnable.class);
      Object first = factory.getScriptedObject(source);
      assertThat(first).isInstanceOf(Runnable.class);
      assertThat(factory.getScriptedObject(source)).isInstanceOf(first.getClass()).isNotSameAs(first);
      assertThat(factory.getScriptInterfaces()).isNull();
      assertThat(factory.requiresConfigInterface()).isFalse();
    }
  }

  @Test
  void canRecoverFromCompilationErrorsDuringTypeDiscoveryAndObjectCreation() throws Exception {
    var factory = new GroovyScriptFactory("inline:recover");
    var source = new StaticScriptSource("return (", "Recover.groovy");

    try (var loader = factory.getGroovyClassLoader()) {
      assertThatThrownBy(() -> factory.getScriptedObjectType(source))
              .isInstanceOf(ScriptCompilationException.class).hasCauseInstanceOf(CompilationFailedException.class);
      assertThatThrownBy(() -> factory.getScriptedObject(source))
              .isInstanceOf(ScriptCompilationException.class).hasCauseInstanceOf(CompilationFailedException.class);
      source.setScript("return null");
      assertThat(factory.getScriptedObjectType(source)).isNull();
      assertThat(factory.getScriptedObject(source)).isNull();
      source.setScript("return 42");
      assertThat(factory.getScriptedObject(source)).isEqualTo(42);
    }
  }

  @Test
  void usesCompilationCustomizersWithAnIncompatibleParentGroovyLoader() throws Exception {
    var importCustomizer = new ImportCustomizer().addImports(LocalDate.class.getName());
    var factory = new GroovyScriptFactory("inline:date", importCustomizer);
    var beanFactory = mock(ConfigurableBeanFactory.class);
    factory.setBeanFactory(beanFactory);

    try (var parent = new GroovyClassLoader(getClass().getClassLoader())) {
      factory.setBeanClassLoader(parent);
      try (var loader = factory.getGroovyClassLoader()) {
        assertThat(loader).isNotSameAs(parent);
        assertThat(factory.getScriptedObject(new StaticScriptSource("return LocalDate.of(2026, 1, 1)", "DateScript.groovy")))
                .isEqualTo(LocalDate.of(2026, 1, 1));
      }
    }
    then(beanFactory).should().ignoreDependencyType(MetaClass.class);
  }

  @Test
  void reusesAnExplicitlyCompatibleGroovyClassLoader() throws Exception {
    var configuration = new CompilerConfiguration();
    var factory = new GroovyScriptFactory("inline:script", configuration);
    try (var loader = new GroovyClassLoader(getClass().getClassLoader(), configuration)) {
      factory.setBeanClassLoader(loader);
      assertThat(factory.getGroovyClassLoader()).isSameAs(loader);
    }
  }

}
