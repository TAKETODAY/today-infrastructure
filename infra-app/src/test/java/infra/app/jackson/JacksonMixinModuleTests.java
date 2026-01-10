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

package infra.app.jackson;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import infra.app.jackson.scan.a.RenameMixInClass;
import infra.app.jackson.scan.b.RenameMixInAbstractClass;
import infra.app.jackson.scan.c.RenameMixInInterface;
import infra.app.jackson.scan.d.EmptyMixInClass;
import infra.app.jackson.scan.f.EmptyMixIn;
import infra.app.jackson.types.Name;
import infra.app.jackson.types.NameAndAge;
import infra.beans.factory.BeanCreationException;
import infra.context.annotation.AnnotationConfigApplicationContext;
import infra.util.ClassUtils;
import tools.jackson.databind.JacksonModule;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link JacksonMixinModule}.
 *
 * @author Guirong Hu
 */
class JacksonMixinModuleTests {

  private @Nullable AnnotationConfigApplicationContext context;

  @AfterEach
  void closeContext() {
    if (this.context != null) {
      this.context.close();
    }
  }

  @Test
  void emptyMixInWithEmptyTypesShouldFail() {
    assertThatExceptionOfType(BeanCreationException.class).isThrownBy(() -> load(EmptyMixIn.class))
            .withMessageContaining("Error creating bean with name 'jacksonMixinModule'")
            .withStackTraceContaining("@JacksonMixin annotation on class "
                    + "'infra.app.jackson.scan.f.EmptyMixIn' does not specify any types");
  }

  @Test
  void moduleWithRenameMixInClassShouldBeMixedIn() throws Exception {
    load(RenameMixInClass.class);
    JacksonMixinModule module = getContext().getBean(JacksonMixinModule.class);
    assertMixIn(module, new Name("infra"), "{\"username\":\"infra\"}");
    assertMixIn(module, NameAndAge.create("infra", 100), "{\"age\":100,\"username\":\"infra\"}");
  }

  @Test
  void moduleWithEmptyMixInClassShouldNotBeMixedIn() throws Exception {
    load(EmptyMixInClass.class);
    JacksonMixinModule module = getContext().getBean(JacksonMixinModule.class);
    assertMixIn(module, new Name("infra"), "{\"name\":\"infra\"}");
    assertMixIn(module, NameAndAge.create("infra", 100), "{\"age\":100,\"name\":\"infra\"}");
  }

  @Test
  void moduleWithRenameMixInAbstractClassShouldBeMixedIn() throws Exception {
    load(RenameMixInAbstractClass.class);
    JacksonMixinModule module = getContext().getBean(JacksonMixinModule.class);
    assertMixIn(module, NameAndAge.create("infra", 100), "{\"age\":100,\"username\":\"infra\"}");
  }

  @Test
  void moduleWithRenameMixInInterfaceShouldBeMixedIn() throws Exception {
    load(RenameMixInInterface.class);
    JacksonMixinModule module = getContext().getBean(JacksonMixinModule.class);
    assertMixIn(module, NameAndAge.create("infra", 100), "{\"age\":100,\"username\":\"infra\"}");
  }

  private AnnotationConfigApplicationContext getContext() {
    AnnotationConfigApplicationContext context = this.context;
    assertThat(context).isNotNull();
    return context;
  }

  private void load(Class<?>... basePackageClasses) {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
    context.registerBean(JacksonMixinModule.class, () -> createJacksonMixinModule(context, basePackageClasses));
    context.refresh();
    this.context = context;
  }

  private JacksonMixinModule createJacksonMixinModule(AnnotationConfigApplicationContext context,
          Class<?>... basePackageClasses) {
    List<String> basePackages = Arrays.stream(basePackageClasses).map(ClassUtils::getPackageName).toList();
    JacksonMixinModuleEntries entries = JacksonMixinModuleEntries.scan(context, basePackages);
    JacksonMixinModule jacksonMixinModule = new JacksonMixinModule();
    jacksonMixinModule.registerEntries(entries, context.getClassLoader());
    return jacksonMixinModule;
  }

  private void assertMixIn(JacksonModule module, Name value, String expectedJson) throws Exception {
    JsonMapper mapper = JsonMapper.builder().addModule(module).build();
    String json = mapper.writeValueAsString(value);
    assertThat(json).isEqualToIgnoringWhitespace(expectedJson);
  }

}
