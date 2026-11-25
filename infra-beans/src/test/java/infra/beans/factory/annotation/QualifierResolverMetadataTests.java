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

package infra.beans.factory.annotation;

import org.junit.jupiter.api.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Set;

import infra.beans.factory.config.BeanDefinitionHolder;
import infra.beans.factory.config.DependencyDescriptor;
import infra.beans.factory.support.AutowireCandidateQualifier;
import infra.beans.factory.support.RootBeanDefinition;
import infra.beans.factory.support.StandardBeanFactory;
import infra.core.MethodParameter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

class QualifierResolverMetadataTests {

  @Test
  void recognizesCustomQualifiersAndUsesTheirDefaultAttributes() throws Exception {
    var resolver = new QualifierAnnotationAutowireCandidateResolver(Rank.class);
    var descriptor = descriptor("ranked");
    var definition = new RootBeanDefinition(String.class);
    var holder = new BeanDefinitionHolder(definition, "candidate");

    assertThat(resolver.hasQualifier(descriptor)).isTrue();
    assertThat(resolver.getSuggestedName(descriptor)).isNull();
    assertThat(resolver.isAutowireCandidate(holder, descriptor)).isFalse();

    var qualifier = new AutowireCandidateQualifier(Rank.class);
    definition.addQualifier(qualifier);
    assertThat(resolver.isAutowireCandidate(holder, descriptor)).isTrue();

    qualifier.setAttribute("value", "3");
    assertThat(resolver.isAutowireCandidate(holder, descriptor)).isFalse();
  }

  @Test
  void checksDecoratedFactoryMethodWhenOuterDefinitionHasNoQualifier() throws Exception {
    var factory = new StandardBeanFactory();
    var resolver = new QualifierAnnotationAutowireCandidateResolver(Set.of(Qualifier.class));
    resolver.setBeanFactory(factory);
    var decorated = new RootBeanDefinition(QualifierResolverMetadataTests.class);
    decorated.setResolvedFactoryMethod(QualifierResolverMetadataTests.class.getDeclaredMethod("create"));
    factory.registerBeanDefinition("decorated", decorated);
    var definition = new RootBeanDefinition(String.class);
    definition.setDecoratedDefinition(new BeanDefinitionHolder(decorated, "decorated"));

    assertThat(resolver.isAutowireCandidate(new BeanDefinitionHolder(definition, "outer"), descriptor("named"))).isTrue();
  }

  @Test
  void usesDefinitionMetadataForCandidatesNotRegisteredInFactory() throws Exception {
    var resolver = new QualifierAnnotationAutowireCandidateResolver(Set.of(Qualifier.class));
    resolver.setBeanFactory(new StandardBeanFactory());
    var definition = new RootBeanDefinition(NamedCandidate.class);

    assertThat(resolver.isAutowireCandidate(new BeanDefinitionHolder(definition, "unregistered"), descriptor("named"))).isTrue();
  }

  @Test
  void distinguishesMethodQualifierOnInjectionMethodFromFactoryMethod() throws Exception {
    var resolver = new QualifierAnnotationAutowireCandidateResolver();
    var definition = new RootBeanDefinition(String.class);
    definition.setDefaultCandidate(false);
    var holder = new BeanDefinitionHolder(definition, "named");
    var setter = new DependencyDescriptor(new MethodParameter(
            InjectionPoints.class.getDeclaredMethod("set", String.class), 0), true);
    var factoryMethod = new DependencyDescriptor(new MethodParameter(
            InjectionPoints.class.getDeclaredMethod("produce", String.class), 0), true);

    assertThat(resolver.hasQualifier(setter)).isTrue();
    assertThat(resolver.isAutowireCandidate(holder, setter)).isTrue();
    assertThat(resolver.hasQualifier(factoryMethod)).isFalse();
    assertThat(resolver.isAutowireCandidate(holder, factoryMethod)).isFalse();
  }

  @Test
  void handlesComposedQualifiersAndIgnoresPlainJavaAnnotations() throws Exception {
    var resolver = new QualifierAnnotationAutowireCandidateResolver();
    var definition = new RootBeanDefinition(String.class);
    var matching = new BeanDefinitionHolder(definition, "named");
    var nonMatching = new BeanDefinitionHolder(definition, "other");

    assertThat(resolver.isAutowireCandidate(matching, descriptor("composed"))).isTrue();
    assertThat(resolver.isAutowireCandidate(nonMatching, descriptor("composed"))).isFalse();
    assertThat(resolver.isAutowireCandidate(matching, descriptor("marker"))).isFalse();
  }

  @Test
  void readsConfiguredValueAnnotationAndRejectsAnnotationWithoutValue() throws Exception {
    var resolver = new QualifierAnnotationAutowireCandidateResolver();
    var descriptor = descriptor("literal");
    assertThat(resolver.getSuggestedValue(descriptor)).isNull();
    resolver.setValueAnnotationType(Rank.class);
    assertThat(resolver.getSuggestedValue(descriptor)).isEqualTo(9);

    resolver.setValueAnnotationType(Marker.class);
    assertThatIllegalStateException().isThrownBy(() -> resolver.getSuggestedValue(descriptor("marker")))
            .withMessageContaining("must have a value attribute");
  }

  private static DependencyDescriptor descriptor(String name) throws Exception {
    return new DependencyDescriptor(InjectionPoints.class.getDeclaredField(name), true);
  }

  @Qualifier("named")
  static String create() {
    return "created";
  }

  @Qualifier("named")
  static class NamedCandidate { }

  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.FIELD)
  @interface Rank {
    int value() default 2;
  }

  @Retention(RetentionPolicy.RUNTIME)
  @Qualifier("named")
  @interface Composed { }

  @Retention(RetentionPolicy.RUNTIME)
  @Qualifier
  @interface Marker { }

  static class InjectionPoints {

    @Rank
    String ranked;

    @Qualifier("named")
    Object named;

    @Deprecated
    @Composed
    String composed;

    @Marker
    String marker;

    @Rank(9)
    String literal;

    @Qualifier("named")
    void set(String value) { }

    @Qualifier("named")
    String produce(String value) {
      return value;
    }

  }

}
