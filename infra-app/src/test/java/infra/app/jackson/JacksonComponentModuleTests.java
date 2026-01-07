/*
 * Copyright 2017 - 2026 the original author or authors.
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

package infra.app.jackson;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import infra.aot.hint.MemberCategory;
import infra.aot.hint.RuntimeHints;
import infra.aot.hint.predicate.RuntimeHintsPredicates;
import infra.aot.test.generate.TestGenerationContext;
import infra.app.jackson.JacksonComponentModule.JacksonComponentBeanFactoryInitializationAotProcessor;
import infra.app.jackson.types.Name;
import infra.app.jackson.types.NameAndAge;
import infra.app.jackson.types.NameAndCareer;
import infra.beans.factory.aot.BeanFactoryInitializationAotContribution;
import infra.beans.factory.aot.BeanFactoryInitializationCode;
import infra.beans.factory.config.ConfigurableBeanFactory;
import infra.context.annotation.AnnotationConfigApplicationContext;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JacksonModule;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link JacksonComponentModule}.
 *
 * @author Phillip Webb
 * @author Vladimir Tsanev
 * @author Paul Aly
 */
class JacksonComponentModuleTests {

  private @Nullable AnnotationConfigApplicationContext context;

  @AfterEach
  void closeContext() {
    if (this.context != null) {
      this.context.close();
    }
  }

  @Test
  void moduleShouldRegisterSerializers() throws Exception {
    load(OnlySerializer.class);
    JacksonComponentModule module = getContext().getBean(JacksonComponentModule.class);
    assertSerialize(module);
  }

  @Test
  void moduleShouldRegisterDeserializers() throws Exception {
    load(OnlyDeserializer.class);
    JacksonComponentModule module = getContext().getBean(JacksonComponentModule.class);
    assertDeserialize(module);
  }

  @Test
  void moduleShouldRegisterInnerClasses() throws Exception {
    load(NameAndAgeJacksonComponent.class);
    JacksonComponentModule module = getContext().getBean(JacksonComponentModule.class);
    assertSerialize(module);
    assertDeserialize(module);
  }

  @Test
  void moduleShouldAllowInnerAbstractClasses() throws Exception {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
            JacksonComponentModule.class, ComponentWithInnerAbstractClass.class);
    JacksonComponentModule module = context.getBean(JacksonComponentModule.class);
    assertSerialize(module);
    context.close();
  }

  @Test
  void moduleShouldRegisterKeySerializers() throws Exception {
    load(OnlyKeySerializer.class);
    JacksonComponentModule module = getContext().getBean(JacksonComponentModule.class);
    assertKeySerialize(module);
  }

  @Test
  void moduleShouldRegisterKeyDeserializers() throws Exception {
    load(OnlyKeyDeserializer.class);
    JacksonComponentModule module = getContext().getBean(JacksonComponentModule.class);
    assertKeyDeserialize(module);
  }

  @Test
  void moduleShouldRegisterInnerClassesForKeyHandlers() throws Exception {
    load(NameAndAgeJacksonKeyComponent.class);
    JacksonComponentModule module = getContext().getBean(JacksonComponentModule.class);
    assertKeySerialize(module);
    assertKeyDeserialize(module);
  }

  @Test
  void moduleShouldRegisterOnlyForSpecifiedClasses() throws Exception {
    load(NameAndCareerJacksonComponent.class);
    JacksonComponentModule module = getContext().getBean(JacksonComponentModule.class);
    assertSerialize(module, new NameAndCareer("spring", "developer"), "{\"name\":\"spring\"}");
    assertSerialize(module, NameAndAge.create("spring", 100), "{\"age\":100,\"name\":\"spring\"}");
    assertDeserializeForSpecifiedClasses(module);
  }

  @Test
  void aotContributionRegistersReflectionHintsForSuitableInnerClasses() {
    load(ComponentWithInnerAbstractClass.class);
    ConfigurableBeanFactory beanFactory = getContext().getBeanFactory();
    BeanFactoryInitializationAotContribution contribution = new JacksonComponentBeanFactoryInitializationAotProcessor()
            .processAheadOfTime(beanFactory);
    TestGenerationContext generationContext = new TestGenerationContext();
    assertThat(contribution).isNotNull();
    contribution.applyTo(generationContext, mock(BeanFactoryInitializationCode.class));
    RuntimeHints runtimeHints = generationContext.getRuntimeHints();
    assertThat(RuntimeHintsPredicates.reflection().onType(ComponentWithInnerAbstractClass.class))
            .accepts(runtimeHints);
    assertThat(RuntimeHintsPredicates.reflection()
            .onType(ComponentWithInnerAbstractClass.ConcreteSerializer.class)
            .withMemberCategory(MemberCategory.INVOKE_DECLARED_CONSTRUCTORS)).accepts(runtimeHints);
    assertThat(RuntimeHintsPredicates.reflection()
            .onType(ComponentWithInnerAbstractClass.AbstractSerializer.class)
            .withMemberCategory(MemberCategory.INVOKE_DECLARED_CONSTRUCTORS)
            .negate()).accepts(runtimeHints);
    assertThat(RuntimeHintsPredicates.reflection()
            .onType(ComponentWithInnerAbstractClass.NotSuitable.class)
            .withMemberCategory(MemberCategory.INVOKE_DECLARED_CONSTRUCTORS)
            .negate()).accepts(runtimeHints);
  }

  private void load(Class<?>... configs) {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
    context.register(configs);
    context.register(JacksonComponentModule.class);
    context.refresh();
    this.context = context;
  }

  private void assertSerialize(JacksonModule module, Name value, String expectedJson) throws Exception {
    JsonMapper mapper = JsonMapper.builder().addModule(module).build();
    String json = mapper.writeValueAsString(value);
    assertThat(json).isEqualToIgnoringWhitespace(expectedJson);
  }

  private void assertSerialize(JacksonModule module) throws Exception {
    assertSerialize(module, NameAndAge.create("spring", 100), "{\"theName\":\"spring\",\"theAge\":100}");
  }

  private void assertDeserialize(JacksonModule module) throws Exception {
    JsonMapper mapper = JsonMapper.builder().addModule(module).build();
    NameAndAge nameAndAge = mapper.readValue("{\"name\":\"spring\",\"age\":100}", NameAndAge.class);
    assertThat(nameAndAge.getName()).isEqualTo("spring");
    assertThat(nameAndAge.getAge()).isEqualTo(100);
  }

  private void assertDeserializeForSpecifiedClasses(JacksonComponentModule module) {
    JsonMapper mapper = JsonMapper.builder().addModule(module).build();
    assertThatExceptionOfType(JacksonException.class)
            .isThrownBy(() -> mapper.readValue("{\"name\":\"spring\",\"age\":100}", NameAndAge.class));
    NameAndCareer nameAndCareer = mapper.readValue("{\"name\":\"spring\",\"career\":\"developer\"}",
            NameAndCareer.class);
    assertThat(nameAndCareer.getName()).isEqualTo("spring");
    assertThat(nameAndCareer.getCareer()).isEqualTo("developer");
  }

  private void assertKeySerialize(JacksonModule module) {
    JsonMapper mapper = JsonMapper.builder().addModule(module).build();
    Map<NameAndAge, Boolean> map = new HashMap<>();
    map.put(NameAndAge.create("spring", 100), true);
    String json = mapper.writeValueAsString(map);
    assertThat(json).isEqualToIgnoringWhitespace("{\"spring is 100\":  true}");
  }

  private void assertKeyDeserialize(JacksonModule module) {
    JsonMapper mapper = JsonMapper.builder().addModule(module).build();
    TypeReference<Map<NameAndAge, Boolean>> typeRef = new TypeReference<>() {
    };
    Map<NameAndAge, Boolean> map = mapper.readValue("{\"spring is 100\":  true}", typeRef);
    assertThat(map).containsEntry(NameAndAge.create("spring", 100), true);
  }

  private AnnotationConfigApplicationContext getContext() {
    AnnotationConfigApplicationContext context = this.context;
    assertThat(context).isNotNull();
    return context;
  }

  @JacksonComponent
  static class OnlySerializer extends NameAndAgeJacksonComponent.Serializer {

  }

  @JacksonComponent
  static class OnlyDeserializer extends NameAndAgeJacksonComponent.Deserializer {

  }

  @JacksonComponent
  static class ComponentWithInnerAbstractClass {

    abstract static class AbstractSerializer extends NameAndAgeJacksonComponent.Serializer {

    }

    static class ConcreteSerializer extends AbstractSerializer {

    }

    static class NotSuitable {

    }

  }

  @JacksonComponent(scope = JacksonComponent.Scope.KEYS)
  static class OnlyKeySerializer extends NameAndAgeJacksonKeyComponent.Serializer {

  }

  @JacksonComponent(scope = JacksonComponent.Scope.KEYS, type = NameAndAge.class)
  static class OnlyKeyDeserializer extends NameAndAgeJacksonKeyComponent.Deserializer {

  }

}
