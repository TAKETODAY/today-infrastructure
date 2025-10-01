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

package infra.core.annotation;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Date;
import java.util.List;
import java.util.Set;

import javax.annotation.RegEx;
import javax.annotation.Syntax;
import javax.annotation.concurrent.ThreadSafe;
import javax.annotation.meta.TypeQualifierNickname;
import javax.annotation.meta.When;

import infra.core.annotation.AnnotationUtilsTests.ExtendsBaseClassWithGenericAnnotatedMethod;
import infra.core.annotation.AnnotationUtilsTests.ImplementsInterfaceWithGenericAnnotatedMethod;
import infra.core.annotation.AnnotationUtilsTests.WebController;
import infra.core.annotation.AnnotationUtilsTests.WebMapping;
import infra.core.testfixture.stereotype.Component;
import infra.core.testfixture.stereotype.Indexed;
import infra.lang.Contract;
import infra.util.MultiValueMap;
import jakarta.annotation.Resource;

import static infra.core.annotation.AnnotatedElementUtils.findAllMergedAnnotations;
import static infra.core.annotation.AnnotatedElementUtils.findMergedAnnotation;
import static infra.core.annotation.AnnotatedElementUtils.getAllAnnotationAttributes;
import static infra.core.annotation.AnnotatedElementUtils.getAllMergedAnnotations;
import static infra.core.annotation.AnnotatedElementUtils.getMergedAnnotation;
import static infra.core.annotation.AnnotatedElementUtils.getMergedAnnotationAttributes;
import static infra.core.annotation.AnnotatedElementUtils.getMetaAnnotationTypes;
import static infra.core.annotation.AnnotatedElementUtils.hasAnnotation;
import static infra.core.annotation.AnnotatedElementUtils.hasMetaAnnotationTypes;
import static infra.core.annotation.AnnotatedElementUtils.isAnnotated;
import static infra.core.annotation.AnnotationUtilsTests.asArray;
import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link AnnotatedElementUtils}.
 *
 * @author Sam Brannen
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @see AnnotationUtilsTests
 * @see MultipleComposedAnnotationsOnSingleAnnotatedElementTests
 * @see ComposedRepeatableAnnotationsTests
 * @since 4.0
 */
class AnnotatedElementUtilsTests {

  private static final String TX_NAME = Transactional.class.getName();

  @Nested
  class ConventionBasedAnnotationAttributeOverrideTests {

    @Test
    void getMergedAnnotationAttributesWithConventionBasedComposedAnnotation() {
      Class<?> element = ConventionBasedComposedContextConfigClass.class;
      String name = ContextConfig.class.getName();
      AnnotationAttributes attributes = getMergedAnnotationAttributes(element, name);

      assertThat(attributes).as("Should find @ContextConfig on " + element.getSimpleName()).isNotNull();
      // Convention-based annotation attribute overrides are no longer supported as of
      assertThat(attributes.getStringArray("locations")).as("locations").isEmpty();
      assertThat(attributes.getStringArray("value")).as("value").isEmpty();

      // Verify contracts between utility methods:
      assertThat(isAnnotated(element, name)).isTrue();
    }

    @Test
    void getMergedAnnotationAttributesWithHalfConventionBasedAndHalfAliasedComposedAnnotationV1() {
      Class<?> clazz = HalfConventionBasedAndHalfAliasedComposedContextConfigClassV1.class;
      String name = ContextConfig.class.getName();
      String simpleName = clazz.getSimpleName();
      AnnotationAttributes attributes = getMergedAnnotationAttributes(clazz, name);

      assertThat(attributes).as("Should find @ContextConfig on " + simpleName).isNotNull();
      assertThat(attributes.getStringArray("locations")).as("locations for class [" + simpleName + "]")
              .containsExactly("explicitDeclaration");
      assertThat(attributes.getStringArray("value")).as("value for class [" + simpleName + "]")
              .containsExactly("explicitDeclaration");

      // Verify contracts between utility methods:
      assertThat(isAnnotated(clazz, name)).isTrue();
    }

    @Test
    void getMergedAnnotationAttributesWithHalfConventionBasedAndHalfAliasedComposedAnnotationV2() {
      Class<?> clazz = HalfConventionBasedAndHalfAliasedComposedContextConfigClassV2.class;
      String name = ContextConfig.class.getName();
      String simpleName = clazz.getSimpleName();
      AnnotationAttributes attributes = getMergedAnnotationAttributes(clazz, name);

      assertThat(attributes).as("Should find @ContextConfig on " + simpleName).isNotNull();
      // Convention-based annotation attribute overrides are no longer supported as of
      assertThat(attributes.getStringArray("locations")).as("locations for class [" + simpleName + "]").isEmpty();
      assertThat(attributes.getStringArray("value")).as("value for class [" + simpleName + "]").isEmpty();

      // Verify contracts between utility methods:
      assertThat(isAnnotated(clazz, name)).isTrue();
    }

    @Test
    void findMergedAnnotationAttributesWithSingleElementOverridingAnArrayViaConvention() {
      // Convention-based annotation attribute overrides are no longer supported as of
      assertComponentScanAttributes(ConventionBasedSinglePackageComponentScanClass.class);
    }

    @Test
    void findMergedAnnotationWithLocalAliasesThatConflictWithAttributesInMetaAnnotationByConvention() {
      Class<?> element = InfraAppConfigClass.class;
      ContextConfig contextConfig = findMergedAnnotation(element, ContextConfig.class);

      assertThat(contextConfig).as("Should find @ContextConfig on " + element).isNotNull();
      assertThat(contextConfig.locations()).as("locations for " + element).isEmpty();
      // 'value' in @InfraAppConfig should not override 'value' in @ContextConfig
      assertThat(contextConfig.value()).as("value for " + element).isEmpty();
      // Convention-based annotation attribute overrides are no longer supported as of
      assertThat(contextConfig.classes()).as("classes for " + element).isEmpty();
    }

    @Test
    void findMergedAnnotationWithSingleElementOverridingAnArrayViaConvention() throws Exception {
      // Convention-based annotation attribute overrides are no longer supported as of
      assertWebMapping(WebController.class.getMethod("postMappedWithPathAttribute"), "");
    }

  }

  @Test
  void getMetaAnnotationTypesOnNonAnnotatedClass() {
    assertThat(getMetaAnnotationTypes(NonAnnotatedClass.class, TransactionalComponent.class)).isEmpty();
    assertThat(getMetaAnnotationTypes(NonAnnotatedClass.class, TransactionalComponent.class.getName())).isEmpty();
  }

  @Test
  void getMetaAnnotationTypesOnClassWithMetaDepth1() {
    Set<String> names = getMetaAnnotationTypes(TransactionalComponentClass.class, TransactionalComponent.class);
    assertThat(names).isEqualTo(names(Transactional.class, Component.class, Indexed.class));

    names = getMetaAnnotationTypes(TransactionalComponentClass.class, TransactionalComponent.class.getName());
    assertThat(names).isEqualTo(names(Transactional.class, Component.class, Indexed.class));
  }

  @Test
  void getMetaAnnotationTypesOnClassWithMetaDepth2() {
    Set<String> names = getMetaAnnotationTypes(ComposedTransactionalComponentClass.class, ComposedTransactionalComponent.class);
    assertThat(names).isEqualTo(names(TransactionalComponent.class, Transactional.class, Component.class, Indexed.class));

    names = getMetaAnnotationTypes(ComposedTransactionalComponentClass.class, ComposedTransactionalComponent.class.getName());
    assertThat(names).isEqualTo(names(TransactionalComponent.class, Transactional.class, Component.class, Indexed.class));
  }

  private Set<String> names(Class<?>... classes) {
    return stream(classes).map(Class::getName).collect(toSet());
  }

  @Test
  void hasMetaAnnotationTypesOnNonAnnotatedClass() {
    assertThat(hasMetaAnnotationTypes(NonAnnotatedClass.class, TX_NAME)).isFalse();
  }

  @Test
  void hasMetaAnnotationTypesOnClassWithMetaDepth0() {
    assertThat(hasMetaAnnotationTypes(TransactionalComponentClass.class, TransactionalComponent.class.getName())).isFalse();
  }

  @Test
  void hasMetaAnnotationTypesOnClassWithMetaDepth1() {
    assertThat(hasMetaAnnotationTypes(TransactionalComponentClass.class, TX_NAME)).isTrue();
    assertThat(hasMetaAnnotationTypes(TransactionalComponentClass.class, Component.class.getName())).isTrue();
  }

  @Test
  void hasMetaAnnotationTypesOnClassWithMetaDepth2() {
    assertThat(hasMetaAnnotationTypes(ComposedTransactionalComponentClass.class, TX_NAME)).isTrue();
    assertThat(hasMetaAnnotationTypes(ComposedTransactionalComponentClass.class, Component.class.getName())).isTrue();
    assertThat(hasMetaAnnotationTypes(ComposedTransactionalComponentClass.class, ComposedTransactionalComponent.class.getName())).isFalse();
  }

  @Test
  void isAnnotatedOnNonAnnotatedClass() {
    assertThat(isAnnotated(NonAnnotatedClass.class, Transactional.class)).isFalse();
  }

  @Test
  void isAnnotatedOnClassWithMetaDepth() {
    assertThat(isAnnotated(TransactionalComponentClass.class, TransactionalComponent.class)).isTrue();
    assertThat(isAnnotated(SubTransactionalComponentClass.class, TransactionalComponent.class)).as("isAnnotated() does not search the class hierarchy.").isFalse();
    assertThat(isAnnotated(TransactionalComponentClass.class, Transactional.class)).isTrue();
    assertThat(isAnnotated(TransactionalComponentClass.class, Component.class)).isTrue();
    assertThat(isAnnotated(ComposedTransactionalComponentClass.class, Transactional.class)).isTrue();
    assertThat(isAnnotated(ComposedTransactionalComponentClass.class, Component.class)).isTrue();
    assertThat(isAnnotated(ComposedTransactionalComponentClass.class, ComposedTransactionalComponent.class)).isTrue();
  }

  @Test
  void isAnnotatedForPlainTypes() {
    assertThat(isAnnotated(Order.class, Documented.class)).isTrue();
    assertThat(isAnnotated(Inherited.class, Documented.class)).isTrue();
    assertThat(isAnnotated(Contract.class, Documented.class)).isTrue();
  }

  @Test
  void isAnnotatedWithNameOnNonAnnotatedClass() {
    assertThat(isAnnotated(NonAnnotatedClass.class, TX_NAME)).isFalse();
  }

  @Test
  void isAnnotatedWithNameOnClassWithMetaDepth() {
    assertThat(isAnnotated(TransactionalComponentClass.class, TransactionalComponent.class.getName())).isTrue();
    assertThat(isAnnotated(SubTransactionalComponentClass.class, TransactionalComponent.class.getName())).as("isAnnotated() does not search the class hierarchy.").isFalse();
    assertThat(isAnnotated(TransactionalComponentClass.class, TX_NAME)).isTrue();
    assertThat(isAnnotated(TransactionalComponentClass.class, Component.class.getName())).isTrue();
    assertThat(isAnnotated(ComposedTransactionalComponentClass.class, TX_NAME)).isTrue();
    assertThat(isAnnotated(ComposedTransactionalComponentClass.class, Component.class.getName())).isTrue();
    assertThat(isAnnotated(ComposedTransactionalComponentClass.class, ComposedTransactionalComponent.class.getName())).isTrue();
  }

  @Test
  void hasAnnotationOnNonAnnotatedClass() {
    assertThat(hasAnnotation(NonAnnotatedClass.class, Transactional.class)).isFalse();
  }

  @Test
  void hasAnnotationOnClassWithMetaDepth() {
    assertThat(hasAnnotation(TransactionalComponentClass.class, TransactionalComponent.class)).isTrue();
    assertThat(hasAnnotation(SubTransactionalComponentClass.class, TransactionalComponent.class)).isTrue();
    assertThat(hasAnnotation(TransactionalComponentClass.class, Transactional.class)).isTrue();
    assertThat(hasAnnotation(TransactionalComponentClass.class, Component.class)).isTrue();
    assertThat(hasAnnotation(ComposedTransactionalComponentClass.class, Transactional.class)).isTrue();
    assertThat(hasAnnotation(ComposedTransactionalComponentClass.class, Component.class)).isTrue();
    assertThat(hasAnnotation(ComposedTransactionalComponentClass.class, ComposedTransactionalComponent.class)).isTrue();
  }

  @Test
  @SuppressWarnings("deprecation")
  void hasAnnotationForPlainTypes() {
    assertThat(hasAnnotation(Order.class, Documented.class)).isTrue();
    assertThat(hasAnnotation(Inherited.class, Documented.class)).isTrue();
    assertThat(hasAnnotation(Contract.class, Documented.class)).isTrue();
  }

  @Test
  void getAllAnnotationAttributesOnNonAnnotatedClass() {
    assertThat(getAllAnnotationAttributes(NonAnnotatedClass.class, TX_NAME)).isNull();
  }

  @Test
  void getAllAnnotationAttributesOnClassWithLocalAnnotation() {
    MultiValueMap<String, Object> attributes = getAllAnnotationAttributes(TxConfig.class, TX_NAME);
    assertThat(attributes).as("Annotation attributes map for @Transactional on TxConfig").isNotNull();
    assertThat(attributes.get("value")).as("value for TxConfig").isEqualTo(List.of("TxConfig"));
  }

  @Test
  void getAllAnnotationAttributesOnClassWithLocalComposedAnnotationAndInheritedAnnotation() {
    MultiValueMap<String, Object> attributes = getAllAnnotationAttributes(SubClassWithInheritedAnnotation.class, TX_NAME);
    assertThat(attributes).as("Annotation attributes map for @Transactional on SubClassWithInheritedAnnotation").isNotNull();
    assertThat(attributes.get("qualifier")).isEqualTo(asList("composed2", "transactionManager"));
  }

  @Test
  void getAllAnnotationAttributesFavorsInheritedAnnotationsOverMoreLocallyDeclaredComposedAnnotations() {
    MultiValueMap<String, Object> attributes = getAllAnnotationAttributes(SubSubClassWithInheritedAnnotation.class, TX_NAME);
    assertThat(attributes).as("Annotation attributes map for @Transactional on SubSubClassWithInheritedAnnotation").isNotNull();
    assertThat(attributes.get("qualifier")).isEqualTo(List.of("transactionManager"));
  }

  @Test
  void getAllAnnotationAttributesFavorsInheritedComposedAnnotationsOverMoreLocallyDeclaredComposedAnnotations() {
    MultiValueMap<String, Object> attributes = getAllAnnotationAttributes(SubSubClassWithInheritedComposedAnnotation.class, TX_NAME);
    assertThat(attributes).as("Annotation attributes map for @Transactional on SubSubClassWithInheritedComposedAnnotation").isNotNull();
    assertThat(attributes.get("qualifier")).isEqualTo(List.of("composed1"));
  }

  /**
   * If the "value" entry contains both "DerivedTxConfig" AND "TxConfig", then
   * the algorithm is accidentally picking up shadowed annotations of the same
   * type within the class hierarchy. Such undesirable behavior would cause the
   * logic in {@code infra.context.annotation.ProfileCondition}
   * to fail.
   */
  @Test
  void getAllAnnotationAttributesOnClassWithLocalAnnotationThatShadowsAnnotationFromSuperclass() {
    // See infra.core.env.EnvironmentSystemIntegrationTests#mostSpecificDerivedClassDrivesEnvironment_withDevEnvAndDerivedDevConfigClass
    MultiValueMap<String, Object> attributes = getAllAnnotationAttributes(DerivedTxConfig.class, TX_NAME);
    assertThat(attributes).as("Annotation attributes map for @Transactional on DerivedTxConfig").isNotNull();
    assertThat(attributes.get("value")).as("value for DerivedTxConfig").isEqualTo(List.of("DerivedTxConfig"));
  }

  /**
   * Note: this functionality is required by {@code infra.context.annotation.ProfileCondition}.
   */
  @Test
  void getAllAnnotationAttributesOnClassWithMultipleComposedAnnotations() {
    // See infra.core.env.EnvironmentSystemIntegrationTests
    MultiValueMap<String, Object> attributes = getAllAnnotationAttributes(TxFromMultipleComposedAnnotations.class, TX_NAME);
    assertThat(attributes).as("Annotation attributes map for @Transactional on TxFromMultipleComposedAnnotations").isNotNull();
    assertThat(attributes.get("value")).as("value for TxFromMultipleComposedAnnotations.").isEqualTo(asList("TxInheritedComposed", "TxComposed"));
  }

  @Test
  void getAllAnnotationAttributesOnJavaxType() {
    MultiValueMap<String, Object> attributes = getAllAnnotationAttributes(RegEx.class, Syntax.class.getName());
    assertThat(attributes).as("Annotation attributes map for @Syntax on @RegEx").isNotNull();
    assertThat(attributes.get("when")).as("value for @RegEx").isEqualTo(List.of(When.ALWAYS));
  }

  @Test
  void getMergedAnnotationAttributesOnClassWithLocalAnnotation() {
    Class<?> element = TxConfig.class;
    String name = TX_NAME;
    AnnotationAttributes attributes = getMergedAnnotationAttributes(element, name);
    assertThat(attributes).as("Annotation attributes for @Transactional on TxConfig").isNotNull();
    assertThat(attributes.getString("value")).as("value for TxConfig").isEqualTo("TxConfig");
    // Verify contracts between utility methods:
    assertThat(isAnnotated(element, name)).isTrue();
  }

  @Test
  void getMergedAnnotationAttributesOnClassWithLocalAnnotationThatShadowsAnnotationFromSuperclass() {
    Class<?> element = DerivedTxConfig.class;
    String name = TX_NAME;
    AnnotationAttributes attributes = getMergedAnnotationAttributes(element, name);
    assertThat(attributes).as("Annotation attributes for @Transactional on DerivedTxConfig").isNotNull();
    assertThat(attributes.getString("value")).as("value for DerivedTxConfig").isEqualTo("DerivedTxConfig");
    // Verify contracts between utility methods:
    assertThat(isAnnotated(element, name)).isTrue();
  }

  @Test
  void getMergedAnnotationAttributesOnMetaCycleAnnotatedClassWithMissingTargetMetaAnnotation() {
    AnnotationAttributes attributes = getMergedAnnotationAttributes(MetaCycleAnnotatedClass.class, TX_NAME);
    assertThat(attributes).as("Should not find annotation attributes for @Transactional on MetaCycleAnnotatedClass").isNull();
  }

  @Test
  void getMergedAnnotationAttributesFavorsLocalComposedAnnotationOverInheritedAnnotation() {
    Class<?> element = SubClassWithInheritedAnnotation.class;
    String name = TX_NAME;
    AnnotationAttributes attributes = getMergedAnnotationAttributes(element, name);
    assertThat(attributes).as("AnnotationAttributes for @Transactional on SubClassWithInheritedAnnotation").isNotNull();
    // Verify contracts between utility methods:
    assertThat(isAnnotated(element, name)).isTrue();
    assertThat(attributes.getBoolean("readOnly")).as("readOnly flag for SubClassWithInheritedAnnotation.").isTrue();
  }

  @Test
  void getMergedAnnotationAttributesFavorsInheritedAnnotationsOverMoreLocallyDeclaredComposedAnnotations() {
    Class<?> element = SubSubClassWithInheritedAnnotation.class;
    String name = TX_NAME;
    AnnotationAttributes attributes = getMergedAnnotationAttributes(element, name);
    assertThat(attributes).as("AnnotationAttributes for @Transactional on SubSubClassWithInheritedAnnotation").isNotNull();
    // Verify contracts between utility methods:
    assertThat(isAnnotated(element, name)).isTrue();
    assertThat(attributes.getBoolean("readOnly")).as("readOnly flag for SubSubClassWithInheritedAnnotation.").isFalse();
  }

  @Test
  void getMergedAnnotationAttributesFavorsInheritedComposedAnnotationsOverMoreLocallyDeclaredComposedAnnotations() {
    Class<?> element = SubSubClassWithInheritedComposedAnnotation.class;
    String name = TX_NAME;
    AnnotationAttributes attributes = getMergedAnnotationAttributes(element, name);
    assertThat(attributes).as("AnnotationAttributes for @Transactional on SubSubClassWithInheritedComposedAnnotation.").isNotNull();
    // Verify contracts between utility methods:
    assertThat(isAnnotated(element, name)).isTrue();
    assertThat(attributes.getBoolean("readOnly")).as("readOnly flag for SubSubClassWithInheritedComposedAnnotation.").isFalse();
  }

  @Test
  void getMergedAnnotationAttributesFromInterfaceImplementedBySuperclass() {
    Class<?> element = ConcreteClassWithInheritedAnnotation.class;
    String name = TX_NAME;
    AnnotationAttributes attributes = getMergedAnnotationAttributes(element, name);
    assertThat(attributes).as("Should not find @Transactional on ConcreteClassWithInheritedAnnotation").isNull();
    // Verify contracts between utility methods:
    assertThat(isAnnotated(element, name)).isFalse();
  }

  @Test
  void getMergedAnnotationAttributesOnInheritedAnnotationInterface() {
    Class<?> element = InheritedAnnotationInterface.class;
    String name = TX_NAME;
    AnnotationAttributes attributes = getMergedAnnotationAttributes(element, name);
    assertThat(attributes).as("Should find @Transactional on InheritedAnnotationInterface").isNotNull();
    // Verify contracts between utility methods:
    assertThat(isAnnotated(element, name)).isTrue();
  }

  @Test
  void getMergedAnnotationAttributesOnNonInheritedAnnotationInterface() {
    Class<?> element = NonInheritedAnnotationInterface.class;
    String name = Order.class.getName();
    AnnotationAttributes attributes = getMergedAnnotationAttributes(element, name);
    assertThat(attributes).as("Should find @Order on NonInheritedAnnotationInterface").isNotNull();
    // Verify contracts between utility methods:
    assertThat(isAnnotated(element, name)).isTrue();
  }

  @Test
  void getMergedAnnotationAttributesWithAliasedComposedAnnotation() {
    Class<?> element = AliasedComposedContextConfigClass.class;
    String name = ContextConfig.class.getName();
    AnnotationAttributes attributes = getMergedAnnotationAttributes(element, name);

    assertThat(attributes).as("Should find @ContextConfig on " + element.getSimpleName()).isNotNull();
    assertThat(attributes.getStringArray("value")).as("value").isEqualTo(asArray("test.xml"));
    assertThat(attributes.getStringArray("locations")).as("locations").isEqualTo(asArray("test.xml"));

    // Verify contracts between utility methods:
    assertThat(isAnnotated(element, name)).isTrue();
  }

  @Test
  void getMergedAnnotationAttributesWithAliasedValueComposedAnnotation() {
    Class<?> element = AliasedValueComposedContextConfigClass.class;
    String name = ContextConfig.class.getName();
    AnnotationAttributes attributes = getMergedAnnotationAttributes(element, name);

    assertThat(attributes).as("Should find @ContextConfig on " + element.getSimpleName()).isNotNull();
    assertThat(attributes.getStringArray("locations")).as("locations").isEqualTo(asArray("test.xml"));
    assertThat(attributes.getStringArray("value")).as("value").isEqualTo(asArray("test.xml"));

    // Verify contracts between utility methods:
    assertThat(isAnnotated(element, name)).isTrue();
  }

  @Test
  void getMergedAnnotationAttributesWithImplicitAliasesInMetaAnnotationOnComposedAnnotation() {
    Class<?> element = ComposedImplicitAliasesContextConfigClass.class;
    String name = ImplicitAliasesContextConfig.class.getName();
    AnnotationAttributes attributes = getMergedAnnotationAttributes(element, name);
    String[] expected = asArray("A.xml", "B.xml");

    assertThat(attributes).as("Should find @ImplicitAliasesContextConfig on " + element.getSimpleName()).isNotNull();
    assertThat(attributes.getStringArray("groovyScripts")).as("groovyScripts").isEqualTo(expected);
    assertThat(attributes.getStringArray("xmlFiles")).as("xmlFiles").isEqualTo(expected);
    assertThat(attributes.getStringArray("locations")).as("locations").isEqualTo(expected);
    assertThat(attributes.getStringArray("value")).as("value").isEqualTo(expected);

    // Verify contracts between utility methods:
    assertThat(isAnnotated(element, name)).isTrue();
  }

  @Test
  void getMergedAnnotationWithAliasedValueComposedAnnotation() {
    assertGetMergedAnnotation(AliasedValueComposedContextConfigClass.class, "test.xml");
  }

  @Test
  void getMergedAnnotationWithImplicitAliasesForSameAttributeInComposedAnnotation() {
    assertGetMergedAnnotation(ImplicitAliasesContextConfigClass1.class, "foo.xml");
    assertGetMergedAnnotation(ImplicitAliasesContextConfigClass2.class, "bar.xml");
    assertGetMergedAnnotation(ImplicitAliasesContextConfigClass3.class, "baz.xml");
  }

  @Test
  void getMergedAnnotationWithTransitiveImplicitAliases() {
    assertGetMergedAnnotation(TransitiveImplicitAliasesContextConfigClass.class, "test.groovy");
  }

  @Test
  void getMergedAnnotationWithTransitiveImplicitAliasesWithSingleElementOverridingAnArrayViaAliasFor() {
    assertGetMergedAnnotation(SingleLocationTransitiveImplicitAliasesContextConfigClass.class, "test.groovy");
  }

  @Test
  void getMergedAnnotationWithTransitiveImplicitAliasesWithSkippedLevel() {
    assertGetMergedAnnotation(TransitiveImplicitAliasesWithSkippedLevelContextConfigClass.class, "test.xml");
  }

  @Test
  void getMergedAnnotationWithTransitiveImplicitAliasesWithSkippedLevelWithSingleElementOverridingAnArrayViaAliasFor() {
    assertGetMergedAnnotation(SingleLocationTransitiveImplicitAliasesWithSkippedLevelContextConfigClass.class, "test.xml");
  }

  private void assertGetMergedAnnotation(Class<?> element, String... expected) {
    String name = ContextConfig.class.getName();
    ContextConfig contextConfig = getMergedAnnotation(element, ContextConfig.class);

    assertThat(contextConfig).as("Should find @ContextConfig on " + element.getSimpleName()).isNotNull();
    assertThat(contextConfig.locations()).as("locations").isEqualTo(expected);
    assertThat(contextConfig.value()).as("value").isEqualTo(expected);
    Object[] expecteds = new Class<?>[0];
    assertThat(contextConfig.classes()).as("classes").isEqualTo(expecteds);

    // Verify contracts between utility methods:
    assertThat(isAnnotated(element, name)).isTrue();
  }

  @Test
  void getMergedAnnotationWithImplicitAliasesInMetaAnnotationOnComposedAnnotation() {
    Class<?> element = ComposedImplicitAliasesContextConfigClass.class;
    String name = ImplicitAliasesContextConfig.class.getName();
    ImplicitAliasesContextConfig config = getMergedAnnotation(element, ImplicitAliasesContextConfig.class);
    String[] expected = asArray("A.xml", "B.xml");

    assertThat(config).as("Should find @ImplicitAliasesContextConfig on " + element.getSimpleName()).isNotNull();
    assertThat(config.groovyScripts()).as("groovyScripts").isEqualTo(expected);
    assertThat(config.xmlFiles()).as("xmlFiles").isEqualTo(expected);
    assertThat(config.locations()).as("locations").isEqualTo(expected);
    assertThat(config.value()).as("value").isEqualTo(expected);

    // Verify contracts between utility methods:
    assertThat(isAnnotated(element, name)).isTrue();
  }

  @Test
  void getMergedAnnotationWithImplicitAliasesWithDefaultsInMetaAnnotationOnComposedAnnotation() {
    Class<?> element = ImplicitAliasesWithDefaultsClass.class;
    String name = AliasesWithDefaults.class.getName();
    AliasesWithDefaults annotation = getMergedAnnotation(element, AliasesWithDefaults.class);

    assertThat(annotation).as("Should find @AliasesWithDefaults on " + element.getSimpleName()).isNotNull();
    assertThat(annotation.a1()).as("a1").isEqualTo("ImplicitAliasesWithDefaults");
    assertThat(annotation.a2()).as("a2").isEqualTo("ImplicitAliasesWithDefaults");

    // Verify contracts between utility methods:
    assertThat(isAnnotated(element, name)).isTrue();
  }

  @Test
  void getMergedAnnotationAttributesWithShadowedAliasComposedAnnotation() {
    Class<?> element = ShadowedAliasComposedContextConfigClass.class;
    AnnotationAttributes attributes = getMergedAnnotationAttributes(element, ContextConfig.class);

    String[] expected = asArray("test.xml");

    assertThat(attributes).as("Should find @ContextConfig on " + element.getSimpleName()).isNotNull();
    assertThat(attributes.getStringArray("locations")).as("locations").isEqualTo(expected);
    assertThat(attributes.getStringArray("value")).as("value").isEqualTo(expected);
  }

  @Test
  void findMergedAnnotationAttributesOnInheritedAnnotationInterface() {
    AnnotationAttributes attributes = findMergedAnnotationAttributes(InheritedAnnotationInterface.class, Transactional.class);
    assertThat(attributes).as("Should find @Transactional on InheritedAnnotationInterface").isNotNull();
  }

  @Test
  void findMergedAnnotationAttributesOnSubInheritedAnnotationInterface() {
    AnnotationAttributes attributes = findMergedAnnotationAttributes(SubInheritedAnnotationInterface.class, Transactional.class);
    assertThat(attributes).as("Should find @Transactional on SubInheritedAnnotationInterface").isNotNull();
  }

  @Test
  void findMergedAnnotationAttributesOnSubSubInheritedAnnotationInterface() {
    AnnotationAttributes attributes = findMergedAnnotationAttributes(SubSubInheritedAnnotationInterface.class, Transactional.class);
    assertThat(attributes).as("Should find @Transactional on SubSubInheritedAnnotationInterface").isNotNull();
  }

  @Test
  void findMergedAnnotationAttributesOnNonInheritedAnnotationInterface() {
    AnnotationAttributes attributes = findMergedAnnotationAttributes(NonInheritedAnnotationInterface.class, Order.class);
    assertThat(attributes).as("Should find @Order on NonInheritedAnnotationInterface").isNotNull();
  }

  @Test
  void findMergedAnnotationAttributesOnSubNonInheritedAnnotationInterface() {
    AnnotationAttributes attributes = findMergedAnnotationAttributes(SubNonInheritedAnnotationInterface.class, Order.class);
    assertThat(attributes).as("Should find @Order on SubNonInheritedAnnotationInterface").isNotNull();
  }

  @Test
  void findMergedAnnotationAttributesOnSubSubNonInheritedAnnotationInterface() {
    AnnotationAttributes attributes = findMergedAnnotationAttributes(SubSubNonInheritedAnnotationInterface.class, Order.class);
    assertThat(attributes).as("Should find @Order on SubSubNonInheritedAnnotationInterface").isNotNull();
  }

  @Test
  void findMergedAnnotationAttributesInheritedFromInterfaceMethod() throws NoSuchMethodException {
    Method method = ConcreteClassWithInheritedAnnotation.class.getMethod("handleFromInterface");
    AnnotationAttributes attributes = findMergedAnnotationAttributes(method, Order.class);
    assertThat(attributes).as("Should find @Order on ConcreteClassWithInheritedAnnotation.handleFromInterface() method").isNotNull();
  }

  @Test
  void findMergedAnnotationAttributesInheritedFromAbstractMethod() throws NoSuchMethodException {
    Method method = ConcreteClassWithInheritedAnnotation.class.getMethod("handle");
    AnnotationAttributes attributes = findMergedAnnotationAttributes(method, Transactional.class);
    assertThat(attributes).as("Should find @Transactional on ConcreteClassWithInheritedAnnotation.handle() method").isNotNull();
  }

  @Test
  void findMergedAnnotationAttributesInheritedFromBridgedMethod() throws NoSuchMethodException {
    Method method = ConcreteClassWithInheritedAnnotation.class.getMethod("handleParameterized", String.class);
    AnnotationAttributes attributes = findMergedAnnotationAttributes(method, Transactional.class);
    assertThat(attributes).as("Should find @Transactional on bridged ConcreteClassWithInheritedAnnotation.handleParameterized()").isNotNull();
  }

  /**
   * Bridge/bridged method setup code copied from
   */
  @Test
  void findMergedAnnotationAttributesFromBridgeMethod() {
    Method[] methods = StringGenericParameter.class.getMethods();
    Method bridgeMethod = null;
    Method bridgedMethod = null;

    for (Method method : methods) {
      if ("getFor".equals(method.getName()) && !method.getParameterTypes()[0].equals(Integer.class)) {
        if (method.getReturnType().equals(Object.class)) {
          bridgeMethod = method;
        }
        else {
          bridgedMethod = method;
        }
      }
    }
    assertThat(bridgeMethod != null && bridgeMethod.isBridge()).isTrue();
    boolean condition = bridgedMethod != null && !bridgedMethod.isBridge();
    assertThat(condition).isTrue();

    AnnotationAttributes attributes = findMergedAnnotationAttributes(bridgeMethod, Order.class);
    assertThat(attributes).as("Should find @Order on StringGenericParameter.getFor() bridge method").isNotNull();
  }

  @Test
  void findMergedAnnotationAttributesOnClassWithMetaAndLocalTxConfig() {
    AnnotationAttributes attributes = findMergedAnnotationAttributes(MetaAndLocalTxConfigClass.class, Transactional.class);
    assertThat(attributes).as("Should find @Transactional on MetaAndLocalTxConfigClass").isNotNull();
    assertThat(attributes.getString("qualifier")).as("TX qualifier for MetaAndLocalTxConfigClass.").isEqualTo("localTxMgr");
  }

  @Test
  void findAndSynthesizeAnnotationAttributesOnClassWithAttributeAliasesInTargetAnnotation() {
    String qualifier = "aliasForQualifier";

    // 1) Find and merge AnnotationAttributes from the annotation hierarchy
    AnnotationAttributes attributes = findMergedAnnotationAttributes(
            AliasedTransactionalComponentClass.class, AliasedTransactional.class);
    assertThat(attributes).as("@AliasedTransactional on AliasedTransactionalComponentClass.").isNotNull();

    // 2) Synthesize the AnnotationAttributes back into the target annotation
    AliasedTransactional annotation = AnnotationUtils.synthesizeAnnotation(attributes,
            AliasedTransactional.class, AliasedTransactionalComponentClass.class);
    assertThat(annotation).isNotNull();

    // 3) Verify that the AnnotationAttributes and synthesized annotation are equivalent
    assertThat(attributes.getString("value")).as("TX value via attributes.").isEqualTo(qualifier);
    assertThat(annotation.value()).as("TX value via synthesized annotation.").isEqualTo(qualifier);
    assertThat(attributes.getString("qualifier")).as("TX qualifier via attributes.").isEqualTo(qualifier);
    assertThat(annotation.qualifier()).as("TX qualifier via synthesized annotation.").isEqualTo(qualifier);
  }

  @Test
  void findMergedAnnotationAttributesOnClassWithAttributeAliasInComposedAnnotationAndNestedAnnotationsInTargetAnnotation() {
    AnnotationAttributes attributes = assertComponentScanAttributes(TestComponentScanClass.class, "com.example.app.test");

    Filter[] excludeFilters = attributes.getAnnotationArray("excludeFilters", Filter.class);
    assertThat(excludeFilters).isNotNull();

    List<String> patterns = stream(excludeFilters).map(Filter::pattern).collect(toList());
    assertThat(patterns).isEqualTo(asList("*Test", "*Tests"));
  }

  /**
   * This test ensures that {@link AnnotationUtils#postProcessAnnotationAttributes}
   * uses {@code ObjectUtils.nullSafeEquals()} to check for equality between annotation
   * attributes since attributes may be arrays.
   */
  @Test
  void findMergedAnnotationAttributesOnClassWithBothAttributesOfAnAliasPairDeclared() {
    assertComponentScanAttributes(ComponentScanWithBasePackagesAndValueAliasClass.class, "com.example.app.test");
  }

  @Test
  void findMergedAnnotationAttributesOnMethodWithComposedMetaTransactionalAnnotation() throws Exception {
    Method method = getClass().getDeclaredMethod("composedTransactionalMethod");

    AnnotationAttributes attributes = findMergedAnnotationAttributes(method, AliasedTransactional.class);
    assertThat(attributes).as("Should find @AliasedTransactional on " + method).isNotNull();
    assertThat(attributes.getString("value")).as("TX qualifier for " + method).isEqualTo("anotherTransactionManager");
    assertThat(attributes.getString("qualifier")).as("TX qualifier for " + method).isEqualTo("anotherTransactionManager");
  }

  @Test
  void findMergedAnnotationOnMethodWithComposedMetaTransactionalAnnotation() throws Exception {
    Method method = getClass().getDeclaredMethod("composedTransactionalMethod");

    AliasedTransactional annotation = findMergedAnnotation(method, AliasedTransactional.class);
    assertThat(annotation).as("Should find @AliasedTransactional on " + method).isNotNull();
    assertThat(annotation.value()).as("TX qualifier for " + method).isEqualTo("anotherTransactionManager");
    assertThat(annotation.qualifier()).as("TX qualifier for " + method).isEqualTo("anotherTransactionManager");
  }

  @Test
  void findMergedAnnotationAttributesOnClassWithComposedMetaTransactionalAnnotation() {
    Class<?> clazz = ComposedTransactionalClass.class;

    AnnotationAttributes attributes = findMergedAnnotationAttributes(clazz, AliasedTransactional.class);
    assertThat(attributes).as("Should find @AliasedTransactional on " + clazz).isNotNull();
    assertThat(attributes.getString("value")).as("TX qualifier for " + clazz).isEqualTo("anotherTransactionManager");
    assertThat(attributes.getString("qualifier")).as("TX qualifier for " + clazz).isEqualTo("anotherTransactionManager");
  }

  @Test
  void findMergedAnnotationOnClassWithComposedMetaTransactionalAnnotation() {
    Class<?> clazz = ComposedTransactionalClass.class;

    AliasedTransactional annotation = findMergedAnnotation(clazz, AliasedTransactional.class);
    assertThat(annotation).as("Should find @AliasedTransactional on " + clazz).isNotNull();
    assertThat(annotation.value()).as("TX qualifier for " + clazz).isEqualTo("anotherTransactionManager");
    assertThat(annotation.qualifier()).as("TX qualifier for " + clazz).isEqualTo("anotherTransactionManager");
  }

  @Test
  void findMergedAnnotationAttributesWithSingleElementOverridingAnArrayViaAliasFor() {
    assertComponentScanAttributes(AliasForBasedSinglePackageComponentScanClass.class, "com.example.app.test");
  }

  private AnnotationAttributes assertComponentScanAttributes(Class<?> element, String... expected) {
    AnnotationAttributes attributes = findMergedAnnotationAttributes(element, ComponentScan.class);

    assertThat(attributes).as("Should find @ComponentScan on " + element).isNotNull();
    assertThat(attributes.getStringArray("value")).as("value: ").isEqualTo(expected);
    assertThat(attributes.getStringArray("basePackages")).as("basePackages: ").isEqualTo(expected);

    return attributes;
  }

  private AnnotationAttributes findMergedAnnotationAttributes(AnnotatedElement element, Class<? extends Annotation> annotationType) {
    return AnnotatedElementUtils.findMergedAnnotationAttributes(element, annotationType.getName(), false, false);
  }

  @Test
  void findMergedAnnotationWithAttributeAliasesInTargetAnnotation() {
    Class<?> element = AliasedTransactionalComponentClass.class;
    AliasedTransactional annotation = findMergedAnnotation(element, AliasedTransactional.class);
    assertThat(annotation).as("@AliasedTransactional on " + element).isNotNull();
    assertThat(annotation.value()).as("TX value via synthesized annotation.").isEqualTo("aliasForQualifier");
    assertThat(annotation.qualifier()).as("TX qualifier via synthesized annotation.").isEqualTo("aliasForQualifier");
  }

  @Test
  void findMergedAnnotationForMultipleMetaAnnotationsWithClashingAttributeNames() {
    String[] xmlLocations = asArray("test.xml");
    String[] propFiles = asArray("test.properties");

    Class<?> element = AliasedComposedContextConfigAndTestPropSourceClass.class;

    ContextConfig contextConfig = findMergedAnnotation(element, ContextConfig.class);
    assertThat(contextConfig).as("@ContextConfig on " + element).isNotNull();
    assertThat(contextConfig.locations()).as("locations").isEqualTo(xmlLocations);
    assertThat(contextConfig.value()).as("value").isEqualTo(xmlLocations);

    // Synthesized annotation
    TestPropSource testPropSource = AnnotationUtils.findAnnotation(element, TestPropSource.class);
    assertThat(testPropSource.locations()).as("locations").isEqualTo(propFiles);
    assertThat(testPropSource.value()).as("value").isEqualTo(propFiles);

    // Merged annotation
    testPropSource = findMergedAnnotation(element, TestPropSource.class);
    assertThat(testPropSource).as("@TestPropSource on " + element).isNotNull();
    assertThat(testPropSource.locations()).as("locations").isEqualTo(propFiles);
    assertThat(testPropSource.value()).as("value").isEqualTo(propFiles);
  }

  @Test
  void findMergedAnnotationWithSingleElementOverridingAnArrayViaAliasFor() throws Exception {
    assertWebMapping(WebController.class.getMethod("getMappedWithValueAttribute"), "/test");
    assertWebMapping(WebController.class.getMethod("getMappedWithPathAttribute"), "/test");
  }

  private void assertWebMapping(AnnotatedElement element, String expectedPath) {
    WebMapping webMapping = findMergedAnnotation(element, WebMapping.class);
    assertThat(webMapping).isNotNull();
    assertThat(webMapping.value()).as("value attribute: ").isEqualTo(asArray(expectedPath));
    assertThat(webMapping.path()).as("path attribute: ").isEqualTo(asArray(expectedPath));
  }

  @Test
  void javaLangAnnotationTypeViaFindMergedAnnotation() throws Exception {
    Constructor<?> deprecatedCtor = Date.class.getConstructor(String.class);
    assertThat(findMergedAnnotation(deprecatedCtor, Deprecated.class)).isEqualTo(deprecatedCtor.getAnnotation(Deprecated.class));
    assertThat(findMergedAnnotation(Date.class, Deprecated.class)).isEqualTo(Date.class.getAnnotation(Deprecated.class));
  }

  @Test
  void javaxAnnotationTypeViaFindMergedAnnotation() {
    assertThat(findMergedAnnotation(ResourceHolder.class, Resource.class)).isEqualTo(ResourceHolder.class.getAnnotation(Resource.class));
    assertThat(findMergedAnnotation(InfraAppConfigClass.class, Resource.class)).isEqualTo(InfraAppConfigClass.class.getAnnotation(Resource.class));
  }

  @Test
  void javaxMetaAnnotationTypeViaFindMergedAnnotation() {
    assertThat(findMergedAnnotation(ThreadSafe.class, Documented.class))
            .isEqualTo(ThreadSafe.class.getAnnotation(Documented.class));
    assertThat(findMergedAnnotation(ResourceHolder.class, TypeQualifierNickname.class))
            .isEqualTo(RegEx.class.getAnnotation(TypeQualifierNickname.class));
  }

  @Test
  void nullableAnnotationTypeViaFindMergedAnnotation() throws Exception {
    Method method = TransactionalServiceImpl.class.getMethod("doIt");
    assertThat(findMergedAnnotation(method, Resource.class)).isEqualTo(method.getAnnotation(Resource.class));
  }

  @Test
  void getAllMergedAnnotationsOnClassWithInterface() throws Exception {
    Method method = TransactionalServiceImpl.class.getMethod("doIt");
    Set<Transactional> allMergedAnnotations = getAllMergedAnnotations(method, Transactional.class);
    assertThat(allMergedAnnotations).isEmpty();
  }

  @Test
  void findAllMergedAnnotationsOnClassWithInterface() throws Exception {
    Method method = TransactionalServiceImpl.class.getMethod("doIt");
    Set<Transactional> allMergedAnnotations = findAllMergedAnnotations(method, Transactional.class);
    assertThat(allMergedAnnotations).hasSize(1);
  }

  @Test
  void findMethodAnnotationFromGenericInterface() throws Exception {
    Method method = ImplementsInterfaceWithGenericAnnotatedMethod.class.getMethod("foo", String.class);
    Order order = findMergedAnnotation(method, Order.class);
    assertThat(order).isNotNull();
  }

  @Test
  void findMethodAnnotationFromGenericSuperclass() throws Exception {
    Method method = ExtendsBaseClassWithGenericAnnotatedMethod.class.getMethod("foo", String.class);
    Order order = findMergedAnnotation(method, Order.class);
    assertThat(order).isNotNull();
  }

  @Test
  void forAnnotationsCreatesCopyOfArrayOnEachCall() {
    AnnotatedElement element = AnnotatedElementUtils.forAnnotations(ForAnnotationsClass.class.getDeclaredAnnotations());
    // Trigger the NPE as originally reported in the bug
    AnnotationsScanner.getDeclaredAnnotations(element, false);
    AnnotationsScanner.getDeclaredAnnotations(element, false);
    // Also specifically test we get different instances
    assertThat(element.getDeclaredAnnotations()).isNotSameAs(element.getDeclaredAnnotations());
  }

  @Test
  void getMergedAnnotationOnThreeDeepMetaWithValue() {
    ValueAttribute annotation = AnnotatedElementUtils.getMergedAnnotation(
            ValueAttributeMetaMetaClass.class, ValueAttribute.class);
    assertThat(annotation.value()).containsExactly("FromValueAttributeMeta");
  }

  @Test
  void getMergedRepeatableAnnotationsWithContainerWithMultipleAttributes() {
    Set<StandardRepeatableWithContainerWithMultipleAttributes> repeatableAnnotations =
            AnnotatedElementUtils.getMergedRepeatableAnnotations(
                    StandardRepeatablesWithContainerWithMultipleAttributesTestCase.class,
                    StandardRepeatableWithContainerWithMultipleAttributes.class);
    assertThat(repeatableAnnotations).map(StandardRepeatableWithContainerWithMultipleAttributes::value)
            .containsExactly("a", "b");
  }

  @Test
  void findMergedRepeatableAnnotationsWithContainerWithMultipleAttributes() {
    Set<StandardRepeatableWithContainerWithMultipleAttributes> repeatableAnnotations =
            AnnotatedElementUtils.findMergedRepeatableAnnotations(
                    StandardRepeatablesWithContainerWithMultipleAttributesTestCase.class,
                    StandardRepeatableWithContainerWithMultipleAttributes.class);
    assertThat(repeatableAnnotations).map(StandardRepeatableWithContainerWithMultipleAttributes::value)
            .containsExactly("a", "b");
  }

  // -------------------------------------------------------------------------

  @MetaCycle3
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.ANNOTATION_TYPE)
  @interface MetaCycle1 {
  }

  @MetaCycle1
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.ANNOTATION_TYPE)
  @interface MetaCycle2 {
  }

  @MetaCycle2
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.TYPE)
  @interface MetaCycle3 {
  }

  @MetaCycle3
  static class MetaCycleAnnotatedClass {
  }

  // -------------------------------------------------------------------------

  @Retention(RetentionPolicy.RUNTIME)
  @Target({ ElementType.TYPE, ElementType.METHOD })
  @Inherited
  @interface Transactional {

    String value() default "";

    String qualifier() default "transactionManager";

    boolean readOnly() default false;
  }

  @Retention(RetentionPolicy.RUNTIME)
  @Target({ ElementType.TYPE, ElementType.METHOD })
  @Inherited
  @interface AliasedTransactional {

    @AliasFor("qualifier")
    String value() default "";

    @AliasFor("value")
    String qualifier() default "";
  }

  @AliasedTransactional
  @Retention(RetentionPolicy.RUNTIME)
  @Target({ ElementType.TYPE, ElementType.METHOD })
  @interface MyAliasedTransactional {

    @AliasFor(annotation = AliasedTransactional.class, attribute = "value")
    String value() default "defaultTransactionManager";
  }

  @MyAliasedTransactional("anotherTransactionManager")
  @Retention(RetentionPolicy.RUNTIME)
  @Target({ ElementType.TYPE, ElementType.METHOD })
  @interface ComposedMyAliasedTransactional {
  }

  @Transactional(qualifier = "composed1")
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.TYPE)
  @Inherited
  @interface InheritedComposed {
  }

  @Transactional(qualifier = "composed2", readOnly = true)
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.TYPE)
  @interface Composed {
  }

  @Transactional
  @Retention(RetentionPolicy.RUNTIME)
  @interface TxComposedWithOverride {

    String qualifier() default "txMgr";
  }

  @Transactional("TxInheritedComposed")
  @Retention(RetentionPolicy.RUNTIME)
  @interface TxInheritedComposed {
  }

  @Transactional("TxComposed")
  @Retention(RetentionPolicy.RUNTIME)
  @interface TxComposed {
  }

  @Transactional
  @Component
  @Retention(RetentionPolicy.RUNTIME)
  @interface TransactionalComponent {
  }

  @TransactionalComponent
  @Retention(RetentionPolicy.RUNTIME)
  @interface ComposedTransactionalComponent {
  }

  @AliasedTransactional(value = "aliasForQualifier")
  @Component
  @Retention(RetentionPolicy.RUNTIME)
  @interface AliasedTransactionalComponent {
  }

  @TxComposedWithOverride
  // Override default "txMgr" from @TxComposedWithOverride with "localTxMgr"
  @Transactional(qualifier = "localTxMgr")
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.TYPE)
  @interface MetaAndLocalTxConfig {
  }

  /**
   * Mock of {@code infra.test.context.TestPropertySource}.
   */
  @Retention(RetentionPolicy.RUNTIME)
  @interface TestPropSource {

    @AliasFor("locations")
    String[] value() default {};

    @AliasFor("value")
    String[] locations() default {};
  }

  /**
   * Mock of {@code infra.test.context.ContextConfiguration}.
   */
  @Retention(RetentionPolicy.RUNTIME)
  @interface ContextConfig {

    @AliasFor("locations")
    String[] value() default {};

    @AliasFor("value")
    String[] locations() default {};

    Class<?>[] classes() default {};
  }

  @ContextConfig
  @Retention(RetentionPolicy.RUNTIME)
  @interface ConventionBasedComposedContextConfig {

    // Do NOT use @AliasFor here
    String[] locations() default {};
  }

  @ContextConfig(value = "duplicateDeclaration")
  @Retention(RetentionPolicy.RUNTIME)
  @interface InvalidConventionBasedComposedContextConfig {

    // Do NOT use @AliasFor here
    String[] locations();
  }

  /**
   * This hybrid approach for annotation attribute overrides with transitive implicit
   * aliases is unsupported. See SPR-13554 for details.
   */
  @ContextConfig
  @Retention(RetentionPolicy.RUNTIME)
  @interface HalfConventionBasedAndHalfAliasedComposedContextConfig {

    String[] locations() default {};

    @AliasFor(annotation = ContextConfig.class, attribute = "locations")
    String[] xmlConfigFiles() default {};
  }

  @ContextConfig
  @Retention(RetentionPolicy.RUNTIME)
  @interface AliasedComposedContextConfig {

    @AliasFor(annotation = ContextConfig.class, attribute = "locations")
    String[] xmlConfigFiles();
  }

  @ContextConfig
  @Retention(RetentionPolicy.RUNTIME)
  @interface AliasedValueComposedContextConfig {

    @AliasFor(annotation = ContextConfig.class, attribute = "value")
    String[] locations();
  }

  @ContextConfig
  @Retention(RetentionPolicy.RUNTIME)
  @interface ImplicitAliasesContextConfig {

    @AliasFor(annotation = ContextConfig.class, attribute = "locations")
    String[] groovyScripts() default {};

    @AliasFor(annotation = ContextConfig.class, attribute = "locations")
    String[] xmlFiles() default {};

    // intentionally omitted: attribute = "locations"
    @AliasFor(annotation = ContextConfig.class)
    String[] locations() default {};

    // intentionally omitted: attribute = "locations" (SPR-14069)
    @AliasFor(annotation = ContextConfig.class)
    String[] value() default {};
  }

  @ImplicitAliasesContextConfig(xmlFiles = { "A.xml", "B.xml" })
  @Retention(RetentionPolicy.RUNTIME)
  @interface ComposedImplicitAliasesContextConfig {
  }

  @Retention(RetentionPolicy.RUNTIME)
  @interface AliasesWithDefaults {

    @AliasFor("a2")
    String a1() default "AliasesWithDefaults";

    @AliasFor("a1")
    String a2() default "AliasesWithDefaults";
  }

  @Retention(RetentionPolicy.RUNTIME)
  @AliasesWithDefaults
  @interface ImplicitAliasesWithDefaults {

    @AliasFor(annotation = AliasesWithDefaults.class, attribute = "a1")
    String b1() default "ImplicitAliasesWithDefaults";

    @AliasFor(annotation = AliasesWithDefaults.class, attribute = "a2")
    String b2() default "ImplicitAliasesWithDefaults";
  }

  @ImplicitAliasesContextConfig
  @Retention(RetentionPolicy.RUNTIME)
  @interface TransitiveImplicitAliasesContextConfig {

    @AliasFor(annotation = ImplicitAliasesContextConfig.class, attribute = "xmlFiles")
    String[] xml() default {};

    @AliasFor(annotation = ImplicitAliasesContextConfig.class, attribute = "groovyScripts")
    String[] groovy() default {};
  }

  @ImplicitAliasesContextConfig
  @Retention(RetentionPolicy.RUNTIME)
  @interface SingleLocationTransitiveImplicitAliasesContextConfig {

    @AliasFor(annotation = ImplicitAliasesContextConfig.class, attribute = "xmlFiles")
    String xml() default "";

    @AliasFor(annotation = ImplicitAliasesContextConfig.class, attribute = "groovyScripts")
    String groovy() default "";
  }

  @ImplicitAliasesContextConfig
  @Retention(RetentionPolicy.RUNTIME)
  @interface TransitiveImplicitAliasesWithSkippedLevelContextConfig {

    @AliasFor(annotation = ContextConfig.class, attribute = "locations")
    String[] xml() default {};

    @AliasFor(annotation = ImplicitAliasesContextConfig.class, attribute = "groovyScripts")
    String[] groovy() default {};
  }

  @ImplicitAliasesContextConfig
  @Retention(RetentionPolicy.RUNTIME)
  @interface SingleLocationTransitiveImplicitAliasesWithSkippedLevelContextConfig {

    @AliasFor(annotation = ContextConfig.class, attribute = "locations")
    String xml() default "";

    @AliasFor(annotation = ImplicitAliasesContextConfig.class, attribute = "groovyScripts")
    String groovy() default "";
  }

  /**
   * Although the configuration declares an explicit value for 'value' and
   * requires a value for the aliased 'locations', this does not result in
   * an error since 'locations' effectively <em>shadows</em> the 'value'
   * attribute (which cannot be set via the composed annotation anyway).
   *
   * If 'value' were not shadowed, such a declaration would not make sense.
   */
  @ContextConfig(value = "duplicateDeclaration")
  @Retention(RetentionPolicy.RUNTIME)
  @interface ShadowedAliasComposedContextConfig {

    @AliasFor(annotation = ContextConfig.class, attribute = "locations")
    String[] xmlConfigFiles();
  }

  @ContextConfig(locations = "shadowed.xml")
  @TestPropSource(locations = "test.properties")
  @Retention(RetentionPolicy.RUNTIME)
  @interface AliasedComposedContextConfigAndTestPropSource {

    @AliasFor(annotation = ContextConfig.class, attribute = "locations")
    String[] xmlConfigFiles() default "default.xml";
  }

  @ContextConfig
  @Retention(RetentionPolicy.RUNTIME)
  @interface InfraAppConfig {

    @AliasFor(annotation = ContextConfig.class, attribute = "locations")
    String[] locations() default {};

    // Do NOT use @AliasFor(annotation = ...)
    @AliasFor("value")
    Class<?>[] classes() default {};

    // Do NOT use @AliasFor(annotation = ...)
    @AliasFor("classes")
    Class<?>[] value() default {};
  }

  /**
   * Mock of {@code infra.context.annotation.ComponentScan}
   */
  @Retention(RetentionPolicy.RUNTIME)
  @interface ComponentScan {

    @AliasFor("basePackages")
    String[] value() default {};

    // Intentionally no alias declaration for "value"
    String[] basePackages() default {};

    Filter[] excludeFilters() default {};
  }

  @Retention(RetentionPolicy.RUNTIME)
  @Target({})
  @interface Filter {

    String pattern();
  }

  @ComponentScan(excludeFilters = { @Filter(pattern = "*Test"), @Filter(pattern = "*Tests") })
  @Retention(RetentionPolicy.RUNTIME)
  @interface TestComponentScan {

    @AliasFor(attribute = "basePackages", annotation = ComponentScan.class)
    String[] packages();
  }

  @ComponentScan
  @Retention(RetentionPolicy.RUNTIME)
  @interface ConventionBasedSinglePackageComponentScan {

    // Do NOT use @AliasFor here
    String basePackages();
  }

  @ComponentScan
  @Retention(RetentionPolicy.RUNTIME)
  @interface AliasForBasedSinglePackageComponentScan {

    @AliasFor(attribute = "basePackages", annotation = ComponentScan.class)
    String pkg();
  }

  // -------------------------------------------------------------------------

  static class NonAnnotatedClass {
  }

  @TransactionalComponent
  static class TransactionalComponentClass {
  }

  static class SubTransactionalComponentClass extends TransactionalComponentClass {
  }

  @ComposedTransactionalComponent
  static class ComposedTransactionalComponentClass {
  }

  @AliasedTransactionalComponent
  static class AliasedTransactionalComponentClass {
  }

  @ComposedMyAliasedTransactional
  void composedTransactionalMethod() {
  }

  @ComposedMyAliasedTransactional
  static class ComposedTransactionalClass {
  }

  @Transactional
  static class ClassWithInheritedAnnotation {
  }

  @Composed
  static class SubClassWithInheritedAnnotation extends ClassWithInheritedAnnotation {
  }

  static class SubSubClassWithInheritedAnnotation extends SubClassWithInheritedAnnotation {
  }

  @InheritedComposed
  static class ClassWithInheritedComposedAnnotation {
  }

  @Composed
  static class SubClassWithInheritedComposedAnnotation extends ClassWithInheritedComposedAnnotation {
  }

  static class SubSubClassWithInheritedComposedAnnotation extends SubClassWithInheritedComposedAnnotation {
  }

  @MetaAndLocalTxConfig
  static class MetaAndLocalTxConfigClass {
  }

  @Transactional("TxConfig")
  static class TxConfig {
  }

  @Transactional("DerivedTxConfig")
  static class DerivedTxConfig extends TxConfig {
  }

  @TxInheritedComposed
  @TxComposed
  static class TxFromMultipleComposedAnnotations {
  }

  @Transactional
  interface InterfaceWithInheritedAnnotation {

    @Order
    void handleFromInterface();
  }

  abstract static class AbstractClassWithInheritedAnnotation<T> implements InterfaceWithInheritedAnnotation {

    @Transactional
    public abstract void handle();

    @Transactional
    public void handleParameterized(T t) {
    }
  }

  static class ConcreteClassWithInheritedAnnotation extends AbstractClassWithInheritedAnnotation<String> {

    @Override
    public void handle() {
    }

    @Override
    public void handleParameterized(String s) {
    }

    @Override
    public void handleFromInterface() {
    }
  }

  public interface GenericParameter<T> {

    T getFor(Class<T> cls);
  }

  @SuppressWarnings("unused")
  private static class StringGenericParameter implements GenericParameter<String> {

    @Order
    @Override
    public String getFor(Class<String> cls) {
      return "foo";
    }

    public String getFor(Integer integer) {
      return "foo";
    }
  }

  @Transactional
  public interface InheritedAnnotationInterface {
  }

  public interface SubInheritedAnnotationInterface extends InheritedAnnotationInterface {
  }

  public interface SubSubInheritedAnnotationInterface extends SubInheritedAnnotationInterface {
  }

  @Order
  public interface NonInheritedAnnotationInterface {
  }

  public interface SubNonInheritedAnnotationInterface extends NonInheritedAnnotationInterface {
  }

  public interface SubSubNonInheritedAnnotationInterface extends SubNonInheritedAnnotationInterface {
  }

  @ConventionBasedComposedContextConfig(locations = "explicitDeclaration")
  static class ConventionBasedComposedContextConfigClass {
  }

  @InvalidConventionBasedComposedContextConfig(locations = "requiredLocationsDeclaration")
  static class InvalidConventionBasedComposedContextConfigClass {
  }

  @HalfConventionBasedAndHalfAliasedComposedContextConfig(xmlConfigFiles = "explicitDeclaration")
  static class HalfConventionBasedAndHalfAliasedComposedContextConfigClassV1 {
  }

  @HalfConventionBasedAndHalfAliasedComposedContextConfig(locations = "explicitDeclaration")
  static class HalfConventionBasedAndHalfAliasedComposedContextConfigClassV2 {
  }

  @AliasedComposedContextConfig(xmlConfigFiles = "test.xml")
  static class AliasedComposedContextConfigClass {
  }

  @AliasedValueComposedContextConfig(locations = "test.xml")
  static class AliasedValueComposedContextConfigClass {
  }

  @ImplicitAliasesContextConfig("foo.xml")
  static class ImplicitAliasesContextConfigClass1 {
  }

  @ImplicitAliasesContextConfig(locations = "bar.xml")
  static class ImplicitAliasesContextConfigClass2 {
  }

  @ImplicitAliasesContextConfig(xmlFiles = "baz.xml")
  static class ImplicitAliasesContextConfigClass3 {
  }

  @ImplicitAliasesWithDefaults
  static class ImplicitAliasesWithDefaultsClass {
  }

  @TransitiveImplicitAliasesContextConfig(groovy = "test.groovy")
  static class TransitiveImplicitAliasesContextConfigClass {
  }

  @SingleLocationTransitiveImplicitAliasesContextConfig(groovy = "test.groovy")
  static class SingleLocationTransitiveImplicitAliasesContextConfigClass {
  }

  @TransitiveImplicitAliasesWithSkippedLevelContextConfig(xml = "test.xml")
  static class TransitiveImplicitAliasesWithSkippedLevelContextConfigClass {
  }

  @SingleLocationTransitiveImplicitAliasesWithSkippedLevelContextConfig(xml = "test.xml")
  static class SingleLocationTransitiveImplicitAliasesWithSkippedLevelContextConfigClass {
  }

  @ComposedImplicitAliasesContextConfig
  static class ComposedImplicitAliasesContextConfigClass {
  }

  @ShadowedAliasComposedContextConfig(xmlConfigFiles = "test.xml")
  static class ShadowedAliasComposedContextConfigClass {
  }

  @AliasedComposedContextConfigAndTestPropSource(xmlConfigFiles = "test.xml")
  static class AliasedComposedContextConfigAndTestPropSourceClass {
  }

  @ComponentScan(value = "com.example.app.test", basePackages = "com.example.app.test")
  static class ComponentScanWithBasePackagesAndValueAliasClass {
  }

  @TestComponentScan(packages = "com.example.app.test")
  static class TestComponentScanClass {
  }

  @ConventionBasedSinglePackageComponentScan(basePackages = "com.example.app.test")
  static class ConventionBasedSinglePackageComponentScanClass {
  }

  @AliasForBasedSinglePackageComponentScan(pkg = "com.example.app.test")
  static class AliasForBasedSinglePackageComponentScanClass {
  }

  @InfraAppConfig(Number.class)
  static class InfraAppConfigClass {
  }

  @Resource(name = "x")
  @RegEx
  static class ResourceHolder {
  }

  interface TransactionalService {

    @Transactional
    @org.jspecify.annotations.Nullable
    Object doIt();
  }

  class TransactionalServiceImpl implements TransactionalService {

    @Override
    public @Nullable Object doIt() {
      return null;
    }
  }

  @Deprecated
  @ComponentScan
  class ForAnnotationsClass {
  }

  @Retention(RetentionPolicy.RUNTIME)
  @interface ValueAttribute {

    String[] value();

  }

  @Retention(RetentionPolicy.RUNTIME)
  @ValueAttribute("FromValueAttributeMeta")
  @interface ValueAttributeMeta {

    @AliasFor("alias")
    String[] value() default {};

    @AliasFor("value")
    String[] alias() default {};

  }

  @Retention(RetentionPolicy.RUNTIME)
  @ValueAttributeMeta("FromValueAttributeMetaMeta")
  @interface ValueAttributeMetaMeta {
  }

  @ValueAttributeMetaMeta
  static class ValueAttributeMetaMetaClass {
  }

  @Retention(RetentionPolicy.RUNTIME)
  @interface StandardContainerWithMultipleAttributes {

    StandardRepeatableWithContainerWithMultipleAttributes[] value();

    String name() default "";
  }

  @Retention(RetentionPolicy.RUNTIME)
  @Repeatable(StandardContainerWithMultipleAttributes.class)
  @interface StandardRepeatableWithContainerWithMultipleAttributes {

    String value() default "";
  }

  @StandardRepeatableWithContainerWithMultipleAttributes("a")
  @StandardRepeatableWithContainerWithMultipleAttributes("b")
  static class StandardRepeatablesWithContainerWithMultipleAttributesTestCase {
  }

}
