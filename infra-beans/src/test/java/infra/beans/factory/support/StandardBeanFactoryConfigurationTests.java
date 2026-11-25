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

package infra.beans.factory.support;

import org.junit.jupiter.api.Test;

import java.beans.PropertyEditorSupport;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.concurrent.Executor;

import infra.beans.PropertyEditorRegistrar;
import infra.beans.PropertyEditorRegistry;
import infra.beans.SimpleTypeConverter;
import infra.beans.factory.BeanCreationException;
import infra.beans.factory.BeanNotOfRequiredTypeException;
import infra.beans.factory.NoSuchBeanDefinitionException;
import infra.beans.factory.NoUniqueBeanDefinitionException;
import infra.beans.factory.annotation.Autowired;
import infra.beans.factory.annotation.AutowiredAnnotationBeanPostProcessor;
import infra.beans.factory.config.BeanDefinition;
import infra.beans.factory.config.BeanExpressionResolver;
import infra.beans.factory.config.ConfigurableBeanFactory;
import infra.beans.factory.config.Scope;
import infra.core.ResolvableType;
import infra.core.conversion.ConversionService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

class StandardBeanFactoryConfigurationTests {

  @Test
  void copiesFactoryConfigurationWithoutCopyingBeanDefinitionsOrSingletons() {
    var source = new StandardBeanFactory();
    var target = new StandardBeanFactory();
    var scope = mock(Scope.class);
    var conversion = mock(ConversionService.class);
    var expressions = mock(BeanExpressionResolver.class);
    var converter = new SimpleTypeConverter();
    var processor = new AutowiredAnnotationBeanPostProcessor();
    var strategy = mock(InstantiationStrategy.class);
    var resolver = mock(AutowireCandidateResolver.class);
    var clonedResolver = mock(AutowireCandidateResolver.class);
    given(resolver.cloneIfNecessary()).willReturn(clonedResolver);
    Executor executor = Runnable::run;
    Comparator<Object> comparator = Comparator.comparing(Object::toString);
    source.setBeanClassLoader(getClass().getClassLoader());
    source.setCacheBeanMetadata(false);
    source.setConversionService(conversion);
    source.setBeanExpressionResolver(expressions);
    source.setTypeConverter(converter);
    source.registerScope("custom", scope);
    source.addBeanPostProcessor(processor);
    source.setInstantiationStrategy(strategy);
    source.setAllowCircularReferences(false);
    source.setBootstrapExecutor(executor);
    source.setDependencyComparator(comparator);
    source.setAllowBeanDefinitionOverriding(false);
    source.setAutowireCandidateResolver(resolver);
    source.registerBeanDefinition("definition", new RootBeanDefinition(StringBuilder.class));
    source.registerSingleton("singleton", "value");

    target.copyConfigurationFrom(source);

    assertThat(target.getBeanClassLoader()).isSameAs(source.getBeanClassLoader());
    assertThat(target.isCacheBeanMetadata()).isFalse();
    assertThat(target.getConversionService()).isSameAs(conversion);
    assertThat(target.getBeanExpressionResolver()).isSameAs(expressions);
    assertThat(target.getTypeConverter()).isSameAs(converter);
    assertThat(target.getRegisteredScope("custom")).isSameAs(scope);
    assertThat(target.getBeanPostProcessorCount()).isEqualTo(1);
    assertThat(target.getInstantiationStrategy()).isSameAs(strategy);
    assertThat(target.isAllowCircularReferences()).isFalse();
    assertThat(target.getBootstrapExecutor()).isSameAs(executor);
    assertThat(target.getDependencyComparator()).isSameAs(comparator);
    assertThat(target.isAllowBeanDefinitionOverriding()).isFalse();
    assertThat(target.getAutowireCandidateResolver()).isSameAs(clonedResolver);
    assertThat(target.getBeanDefinitionNames()).isEmpty();
    assertThat(target.getSingletonNames()).isEmpty();
  }

  @Test
  void copiesScopesFromAnotherConfigurableFactoryImplementation() {
    var source = mock(ConfigurableBeanFactory.class);
    var target = new StandardBeanFactory();
    var scope = mock(Scope.class);
    given(source.getRegisteredScopeNames()).willReturn(new String[] { "foreign" });
    given(source.getRegisteredScope("foreign")).willReturn(scope);

    target.copyConfigurationFrom(source);

    assertThat(target.getRegisteredScopeNames()).containsExactly("foreign");
    assertThat(target.getRegisteredScope("foreign")).isSameAs(scope);
  }

  @Test
  void registersDefaultEditorOverridesOnDemandWhenTheRegistrySupportsThem() {
    var factory = new StandardBeanFactory();
    var registrar = mock(PropertyEditorRegistrar.class);
    var plainRegistry = mock(PropertyEditorRegistry.class);
    var converter = new SimpleTypeConverter();
    given(registrar.overridesDefaultEditors()).willReturn(true);
    factory.addPropertyEditorRegistrar(registrar);

    factory.registerCustomEditors(plainRegistry);
    factory.registerCustomEditors(converter);

    then(registrar).should().registerCustomEditors(plainRegistry);
    then(registrar).should(never()).registerCustomEditors(converter);
    assertThat(converter.getDefaultEditor(Integer.class)).isNotNull();
    then(registrar).should().registerCustomEditors(converter);
  }

  @Test
  void appliesCopiedCustomEditorsAndPropagatesRegistrarFailures() {
    var source = new StandardBeanFactory();
    var target = new StandardBeanFactory();
    source.registerCustomEditor(StringBuilder.class, StringBuilderEditor.class);
    target.copyConfigurationFrom(source);

    assertThat(target.getTypeConverter().convertIfNecessary("text", StringBuilder.class)).hasToString("text");

    var failure = new BeanCreationException("editor", "could not initialize");
    target.addPropertyEditorRegistrar(registry -> {
      throw failure;
    });
    assertThatThrownBy(target::getTypeConverter).isSameAs(failure);
  }

  @Test
  void autowiresExistingInstancesWithoutRegisteringThemAsBeans() {
    var factory = new StandardBeanFactory();
    var processor = new AutowiredAnnotationBeanPostProcessor();
    processor.setBeanFactory(factory);
    factory.addBeanPostProcessor(processor);
    var dependency = new StringBuilder("dependency");
    factory.registerSingleton("dependency", dependency);
    var consumer = new Consumer();

    factory.autowireBean(consumer);

    assertThat(consumer.dependency).isSameAs(dependency);
    assertThat(factory.getBeanNamesForType(Consumer.class)).isEmpty();
  }

  @Test
  void looksUpDefinitionsByExactTypeOrPrimaryCandidate() {
    var factory = new StandardBeanFactory();
    var first = new RootBeanDefinition(StringBuilder.class);
    var second = new RootBeanDefinition(StringBuffer.class);
    factory.registerBeanDefinition("first", first);
    assertThat(factory.getBeanDefinition(CharSequence.class)).isSameAs(first);
    assertThat(factory.getBeanDefinition(Number.class)).isNull();
    assertThat(factory.containsBeanDefinition(StringBuilder.class, true)).isTrue();
    assertThat(factory.containsBeanDefinition(CharSequence.class, true)).isFalse();
    assertThat(factory.containsBeanDefinition(CharSequence.class, false)).isTrue();

    factory.getBean("first");
    factory.registerBeanDefinition("second", second);
    assertThatExceptionOfType(NoUniqueBeanDefinitionException.class)
            .isThrownBy(() -> factory.getBeanDefinition(CharSequence.class));
    second.setPrimary(true);
    factory.clearMetadataCache();
    assertThat(factory.getBeanDefinition(CharSequence.class)).isSameAs(second);
    assertThat(factory.containsSingleton("second")).isFalse();
  }

  @Test
  void findsBeansByResolvableTypeAndCanExcludePrototypes() {
    var factory = new StandardBeanFactory();
    var prototype = new RootBeanDefinition(StringBuffer.class);
    prototype.setScope(BeanDefinition.SCOPE_PROTOTYPE);
    factory.registerBeanDefinition("prototype", prototype);
    factory.registerSingleton("singleton", "value");
    var type = ResolvableType.forClass(CharSequence.class);

    assertThat(factory.<CharSequence>getBeansOfType(type, false, false)).containsOnlyKeys("singleton")
            .containsEntry("singleton", "value");
    assertThat(factory.<CharSequence>getBeansOfType(type, true, true)).containsOnlyKeys("prototype", "singleton")
            .hasEntrySatisfying("prototype", bean -> assertThat(bean).isInstanceOf(StringBuffer.class));
  }

  @Test
  void delegatesTypeLookupsToAParentThatIsNotAStandardBeanFactory() {
    var parent = new StaticListableBeanFactory();
    var child = new StandardBeanFactory(parent);
    parent.addBean("parentValue", "inherited");
    var provider = child.getBeanProvider(String.class);

    assertThat(provider.getIfAvailable()).isEqualTo("inherited");
    assertThat(provider.getIfUnique()).isEqualTo("inherited");
    assertThat(provider.get(new Object[0])).isEqualTo("inherited");
    assertThatExceptionOfType(NoSuchBeanDefinitionException.class)
            .isThrownBy(() -> child.resolveNamedBean(String.class));

    var standardParent = new StandardBeanFactory();
    standardParent.registerSingleton("parentValue", "inherited");
    var inherited = new StandardBeanFactory(standardParent).resolveNamedBean(String.class);
    assertThat(inherited.getBeanName()).isEqualTo("parentValue");
    assertThat(inherited.getBeanInstance()).isEqualTo("inherited");
  }

  @Test
  void convertsNamedBeansToTheRequestedTypeAndRejectsInvalidValues() {
    var factory = new StandardBeanFactory();
    factory.registerSingleton("valid", "42");
    factory.registerSingleton("invalid", "not a number");

    assertThat(factory.getBean("valid", Integer.class)).isEqualTo(42);
    assertThatExceptionOfType(BeanNotOfRequiredTypeException.class)
            .isThrownBy(() -> factory.getBean("invalid", Integer.class));
  }

  @Test
  void resolvesClassNameExpressionsWithoutCachingTheirResultsOnTheDefinition() {
    var factory = new StandardBeanFactory();
    factory.setBeanExpressionResolver((value, context) -> switch (value) {
      case "className" -> StringBuilder.class.getName();
      case "classObject" -> StringBuffer.class;
      default -> 42;
    });
    var byName = new RootBeanDefinition();
    byName.setBeanClassName("className");
    var byClass = new RootBeanDefinition();
    byClass.setBeanClassName("classObject");
    var invalid = new RootBeanDefinition();
    invalid.setBeanClassName("invalid");

    assertThat(factory.resolveBeanClass("byName", byName)).isEqualTo(StringBuilder.class);
    assertThat(factory.resolveBeanClass("byClass", byClass)).isEqualTo(StringBuffer.class);
    assertThat(byName.hasBeanClass()).isFalse();
    assertThat(byClass.hasBeanClass()).isFalse();
    assertThatIllegalStateException().isThrownBy(() -> factory.resolveBeanClass("invalid", invalid))
            .withMessageContaining("Invalid class name expression result");
  }

  @Test
  void discoversAnnotationsOnBothFactoryMethodsAndTheirProductTypes() {
    var factory = new StandardBeanFactory();
    factory.registerBeanDefinition("product", BeanDefinitionBuilder.rootBeanDefinition(AnnotatedProduct.class, "create")
            .getBeanDefinition());

    assertThat(factory.findAllAnnotationsOnBean("product", Label.class, true))
            .extracting(Label::value).containsExactly("type", "method");
  }

  @Test
  void serializesByIdAndFallsBackToAnUnregisteredFactory() throws Exception {
    var factory = new StandardBeanFactory();
    assertThatThrownBy(() -> serialize(factory)).isInstanceOf(NotSerializableException.class);

    factory.setSerializationId("factory");
    assertThat(factory.getSerializationId()).isEqualTo("factory");
    assertThat(deserialize(serialize(factory))).isSameAs(factory);

    factory.setSerializationId(null);
    assertThat(factory.getSerializationId()).isNull();
    var fallback = deserialize(serializeWithId("missing"));
    assertThat(fallback).isInstanceOf(StandardBeanFactory.class)
            .extracting(StandardBeanFactory::getSerializationId).isEqualTo("missing");
  }

  @Test
  void maintainsDefinitionAndManualSingletonIndexesAcrossFreezeAndRemoval() {
    var factory = new StandardBeanFactory();
    factory.setAllowEagerClassLoading(false);
    assertThat(factory.isAllowEagerClassLoading()).isFalse();
    factory.registerBeanDefinition("first", new RootBeanDefinition(StringBuilder.class));
    factory.registerBeanDefinition("second", new RootBeanDefinition(StringBuffer.class));
    factory.registerSingleton("manual", new Object());

    assertThat(factory.getBeanDefinitionCount()).isEqualTo(2);
    assertThat(factory.containsBeanDefinition("first")).isTrue();
    assertThat(factory.containsBeanDefinition("missing")).isFalse();
    var names = new ArrayList<String>();
    factory.getBeanNamesIterator().forEachRemaining(names::add);
    assertThat(names).containsExactly("first", "second", "manual");
    factory.freezeConfiguration();
    String[] frozenNames = factory.getBeanDefinitionNames();
    frozenNames[0] = "changed";
    assertThat(factory.getBeanDefinitionNames()).containsExactly("first", "second");
    assertThat(factory.getBeanNamesForType(StringBuilder.class)).containsExactly("first");
    assertThat(factory.getBeanNamesForType(StringBuilder.class)).containsExactly("first");

    factory.removeBeanDefinition("first");
    assertThat(factory.getBeanDefinitionCount()).isEqualTo(1);
    assertThat(factory.containsBeanDefinition("first")).isFalse();
    assertThat(factory.getBeanDefinitionNames()).containsExactly("second");
    assertThatExceptionOfType(NoSuchBeanDefinitionException.class)
            .isThrownBy(() -> factory.removeBeanDefinition("missing"));
  }

  @Test
  void updatesDefinitionNamesSafelyAfterBeanCreationHasStarted() {
    var factory = new StandardBeanFactory();
    factory.registerBeanDefinition("first", new RootBeanDefinition(StringBuilder.class));
    factory.getBean("first");
    factory.registerBeanDefinition("second", new RootBeanDefinition(StringBuffer.class));

    assertThat(factory.getBeanDefinitionNames()).containsExactly("first", "second");
    assertThat(factory.getBeanDefinitionCount()).isEqualTo(2);
    assertThat(factory.getBean("second")).isInstanceOf(StringBuffer.class);
  }

  @Test
  void exposesEmptyAmbiguousAndStreamProviderBehaviors() {
    var emptyFactory = new StandardBeanFactory();
    var empty = emptyFactory.getBeanProvider(String.class);
    assertThat(empty.getIfAvailable()).isNull();
    assertThat(empty.getIfUnique()).isNull();
    assertThat(empty.ifAvailable(value -> { })).isFalse();
    assertThat(empty.ifUnique(value -> { })).isFalse();
    assertThatExceptionOfType(NoSuchBeanDefinitionException.class).isThrownBy(empty::get);

    var factory = new StandardBeanFactory();
    factory.registerSingleton("first", "first");
    factory.registerSingleton("second", "second");
    var provider = factory.getBeanProvider(String.class);
    assertThat(provider.getIfUnique()).isNull();
    assertThat(provider.ifUnique(value -> { })).isFalse();
    assertThatExceptionOfType(NoUniqueBeanDefinitionException.class).isThrownBy(provider::getIfAvailable);
    assertThat(provider.stream()).containsExactly("first", "second");
    assertThat(provider.orderedStream()).containsExactly("first", "second");
    assertThat(provider.stream(type -> type == String.class, true)).containsExactly("first", "second");
  }

  @Test
  void ignoresInactiveScopesForOptionalProviderLookups() {
    var factory = new StandardBeanFactory();
    var scope = mock(Scope.class);
    given(scope.get(org.mockito.ArgumentMatchers.eq("scoped"), org.mockito.ArgumentMatchers.any())).willThrow(
            new IllegalStateException("scope is inactive"));
    factory.registerScope("request", scope);
    var definition = new RootBeanDefinition(StringBuilder.class);
    definition.setScope("request");
    factory.registerBeanDefinition("scoped", definition);
    var provider = factory.getBeanProvider(StringBuilder.class);

    assertThat(provider.getIfAvailable()).isNull();
    assertThat(provider.getIfUnique()).isNull();
    assertThat(provider.ifAvailable(value -> { })).isFalse();
    assertThat(provider.ifUnique(value -> { })).isFalse();
  }

  private byte[] serialize(Object value) throws Exception {
    var bytes = new ByteArrayOutputStream();
    try (var output = new ObjectOutputStream(bytes)) {
      output.writeObject(value);
    }
    return bytes.toByteArray();
  }

  private byte[] serializeWithId(String id) throws Exception {
    var factory = new StandardBeanFactory();
    factory.setSerializationId(id);
    byte[] result = serialize(factory);
    factory.setSerializationId(null);
    return result;
  }

  private StandardBeanFactory deserialize(byte[] bytes) throws Exception {
    try (var input = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
      return (StandardBeanFactory) input.readObject();
    }
  }

  @Retention(RetentionPolicy.RUNTIME)
  @interface Label {

    String value();
  }

  @Label("type")
  static class AnnotatedProduct {

    @Label("method")
    static AnnotatedProduct create() {
      return new AnnotatedProduct();
    }
  }

  static class StringBuilderEditor extends PropertyEditorSupport {

    @Override
    public void setAsText(String text) {
      setValue(new StringBuilder(text));
    }
  }

  static class Consumer {

    @Autowired
    StringBuilder dependency;
  }

}
