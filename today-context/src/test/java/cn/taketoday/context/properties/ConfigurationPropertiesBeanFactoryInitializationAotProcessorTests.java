/*
 * Copyright 2017 - 2023 the original author or authors.
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.context.properties;

import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.AssertProvider;
import org.assertj.core.error.BasicErrorMessageFactory;
import org.junit.jupiter.api.Test;

import java.util.stream.Stream;

import cn.taketoday.aot.hint.TypeHint;
import cn.taketoday.aot.hint.TypeReference;
import cn.taketoday.aot.test.generate.TestGenerationContext;
import cn.taketoday.beans.factory.aot.AotServices;
import cn.taketoday.beans.factory.aot.BeanFactoryInitializationAotProcessor;
import cn.taketoday.beans.factory.config.ConfigurableBeanFactory;
import cn.taketoday.beans.factory.support.StandardBeanFactory;
import cn.taketoday.context.annotation.AnnotationConfigApplicationContext;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.properties.ConfigurationPropertiesBeanFactoryInitializationAotProcessor.ConfigurationPropertiesReflectionHintsContribution;
import cn.taketoday.context.properties.bind.BindMethod;
import cn.taketoday.context.properties.bind.Bindable;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/7/3 22:53
 */
class ConfigurationPropertiesBeanFactoryInitializationAotProcessorTests {

  private final ConfigurationPropertiesBeanFactoryInitializationAotProcessor processor = new ConfigurationPropertiesBeanFactoryInitializationAotProcessor();

  @Test
  void configurationPropertiesBeanFactoryInitializationAotProcessorIsRegistered() {
    assertThat(AotServices.factories().load(BeanFactoryInitializationAotProcessor.class))
            .anyMatch(ConfigurationPropertiesBeanFactoryInitializationAotProcessor.class::isInstance);
  }

  @Test
  void processNoMatchesReturnsNullContribution() {
    assertThat(process(String.class)).isNull();
  }

  @Test
  void manuallyRegisteredSingletonBindsAsJavaBean() {
    StandardBeanFactory beanFactory = new StandardBeanFactory();
    beanFactory.registerSingleton("test", new SampleProperties());
    ConfigurationPropertiesReflectionHintsContribution contribution = process(beanFactory);
    assertThat(singleBindable(contribution)).hasBindMethod(BindMethod.JAVA_BEAN).hasType(SampleProperties.class);
    assertThat(typeHints(contribution).map(TypeHint::getType))
            .containsExactly(TypeReference.of(SampleProperties.class));
  }

  @Test
  void javaBeanConfigurationPropertiesBindAsJavaBean() {
    ConfigurationPropertiesReflectionHintsContribution contribution = process(EnableJavaBeanProperties.class);
    assertThat(singleBindable(contribution)).hasBindMethod(BindMethod.JAVA_BEAN).hasType(JavaBeanProperties.class);
    assertThat(typeHints(contribution).map(TypeHint::getType))
            .containsExactly(TypeReference.of(JavaBeanProperties.class));
  }

  @Test
  void constructorBindingConfigurationPropertiesBindAsValueObject() {
    ConfigurationPropertiesReflectionHintsContribution contribution = process(
            EnableConstructorBindingProperties.class);
    assertThat(singleBindable(contribution)).hasBindMethod(BindMethod.VALUE_OBJECT)
            .hasType(ConstructorBindingProperties.class);
    assertThat(typeHints(contribution).map(TypeHint::getType))
            .containsExactly(TypeReference.of(ConstructorBindingProperties.class));
  }

  @Test
  void possibleConstructorBindingPropertiesDefinedThroughBeanMethodBindAsJavaBean() {
    ConfigurationPropertiesReflectionHintsContribution contribution = process(
            PossibleConstructorBindingPropertiesBeanMethodConfiguration.class);
    assertThat(singleBindable(contribution)).hasBindMethod(BindMethod.JAVA_BEAN)
            .hasType(PossibleConstructorBindingProperties.class);
    assertThat(typeHints(contribution).map(TypeHint::getType))
            .containsExactly(TypeReference.of(PossibleConstructorBindingProperties.class));
  }

  @Test
  void possibleConstructorBindingPropertiesDefinedThroughEnabledAnnotationBindAsValueObject() {
    ConfigurationPropertiesReflectionHintsContribution contribution = process(
            EnablePossibleConstructorBindingProperties.class);
    assertThat(singleBindable(contribution)).hasBindMethod(BindMethod.VALUE_OBJECT)
            .hasType(PossibleConstructorBindingProperties.class);
    assertThat(typeHints(contribution).map(TypeHint::getType))
            .containsExactly(TypeReference.of(PossibleConstructorBindingProperties.class));
  }

  private Stream<TypeHint> typeHints(ConfigurationPropertiesReflectionHintsContribution contribution) {
    TestGenerationContext generationContext = new TestGenerationContext();
    contribution.applyTo(generationContext, null);
    return generationContext.getRuntimeHints().reflection().typeHints();
  }

  private ConfigurationPropertiesReflectionHintsContribution process(Class<?> config) {
    try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(config)) {
      return process(context.getBeanFactory());
    }
  }

  private ConfigurationPropertiesReflectionHintsContribution process(ConfigurableBeanFactory beanFactory) {
    return this.processor.processAheadOfTime(beanFactory);
  }

  private BindableAssertProvider singleBindable(ConfigurationPropertiesReflectionHintsContribution contribution) {
    assertThat(contribution.getBindables()).hasSize(1);
    return new BindableAssertProvider(contribution.getBindables().iterator().next());
  }

  @ConfigurationProperties("test")
  static class SampleProperties {

  }

  @EnableConfigurationProperties(JavaBeanProperties.class)
  static class EnableJavaBeanProperties {

  }

  @ConfigurationProperties("java-bean")
  static class JavaBeanProperties {

    private String value;

    String getValue() {
      return this.value;
    }

    void setValue(String value) {
      this.value = value;
    }

  }

  @EnableConfigurationProperties(ConstructorBindingProperties.class)
  static class EnableConstructorBindingProperties {

  }

  @ConfigurationProperties("constructor-binding")
  static class ConstructorBindingProperties {

    private final String value;

    ConstructorBindingProperties(String value) {
      this.value = value;
    }

    String getValue() {
      return this.value;
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class PossibleConstructorBindingPropertiesBeanMethodConfiguration {

    @Bean
    @ConfigurationProperties(prefix = "bean-method")
    static PossibleConstructorBindingProperties possibleConstructorBindingProperties() {
      return new PossibleConstructorBindingProperties("alpha");
    }

  }

  @EnableConfigurationProperties(PossibleConstructorBindingProperties.class)
  static class EnablePossibleConstructorBindingProperties {

  }

  @ConfigurationProperties("possible-constructor-binding")
  static class PossibleConstructorBindingProperties {

    private String value;

    PossibleConstructorBindingProperties(String arg) {

    }

    String getValue() {
      return this.value;
    }

    void setValue(String value) {
      this.value = value;
    }

  }

  static class BindableAssertProvider implements AssertProvider<BindableAssert> {

    private final Bindable<?> bindable;

    BindableAssertProvider(Bindable<?> bindable) {
      this.bindable = bindable;
    }

    @Override
    public BindableAssert assertThat() {
      return new BindableAssert(this.bindable);
    }

  }

  static class BindableAssert extends AbstractAssert<BindableAssert, Bindable<?>> {

    BindableAssert(Bindable<?> bindable) {
      super(bindable, BindableAssert.class);
    }

    BindableAssert hasBindMethod(BindMethod bindMethod) {
      if (this.actual.getBindMethod() != bindMethod) {
        throwAssertionError(
                new BasicErrorMessageFactory("Expected %s to have bind method %s but bind method was %s",
                        this.actual, bindMethod, this.actual.getBindMethod()));
      }
      return this;
    }

    BindableAssert hasType(Class<?> type) {
      if (!type.equals(this.actual.getType().resolve())) {
        throwAssertionError(new BasicErrorMessageFactory("Expected %s to have type %s but type was %s",
                this.actual, type, this.actual.getType().resolve()));
      }
      return this;
    }

  }

}