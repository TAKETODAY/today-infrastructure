/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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

package cn.taketoday.core.annotation;

import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import cn.taketoday.core.MultiValueMap;
import cn.taketoday.core.Ordered;
import cn.taketoday.core.annotation.MergedAnnotation.Adapt;
import cn.taketoday.core.annotation.MergedAnnotations.SearchStrategy;
import cn.taketoday.core.annotation.subpackage.NonPublicAnnotatedClass;
import cn.taketoday.lang.Indexed;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.ReflectionUtils;
import jakarta.annotation.Resource;

import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.entry;

/**
 * Tests for {@link MergedAnnotations} and {@link MergedAnnotation}. These tests
 * cover common usage scenarios and were mainly ported from the original
 * {@code AnnotationUtils} and {@code AnnotatedElementUtils} tests.
 *
 * @author Phillip Webb
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @author Chris Beams
 * @author Oleg Zhurakousky
 * @author Rossen Stoyanchev
 * @see MergedAnnotationsRepeatableAnnotationTests
 * @see MergedAnnotationClassLoaderTests
 */
class MergedAnnotationsTests {

  @Test
  void fromPreconditions() {
    SearchStrategy strategy = SearchStrategy.DIRECT;
    RepeatableContainers containers = RepeatableContainers.standard();

    assertThatIllegalArgumentException()
            .isThrownBy(() -> MergedAnnotations.from(getClass(), strategy, null, AnnotationFilter.PLAIN))
            .withMessage("RepeatableContainers must not be null");
    assertThatIllegalArgumentException()
            .isThrownBy(() -> MergedAnnotations.from(getClass(), strategy, containers, null))
            .withMessage("AnnotationFilter must not be null");

    assertThatIllegalArgumentException()
            .isThrownBy(() -> MergedAnnotations.from(getClass(), new Annotation[0], null, AnnotationFilter.PLAIN))
            .withMessage("RepeatableContainers must not be null");
    assertThatIllegalArgumentException()
            .isThrownBy(() -> MergedAnnotations.from(getClass(), new Annotation[0], containers, null))
            .withMessage("AnnotationFilter must not be null");
  }

  @Test
  void streamWhenFromNonAnnotatedClass() {
    assertThat(MergedAnnotations.from(NonAnnotatedClass.class).
            stream(TransactionalComponent.class)).isEmpty();
  }

  @Test
  void streamWhenFromClassWithMetaDepth1() {
    Stream<Class<?>> classes = MergedAnnotations.from(TransactionalComponent.class)
            .stream().map(MergedAnnotation::getType);
    assertThat(classes).containsExactly(Transactional.class, Component.class, Indexed.class);
  }

  @Test
  void streamWhenFromClassWithMetaDepth2() {
    Stream<Class<?>> classes = MergedAnnotations.from(ComposedTransactionalComponent.class)
            .stream().map(MergedAnnotation::getType);
    assertThat(classes).containsExactly(TransactionalComponent.class,
            Transactional.class, Component.class, Indexed.class);
  }

  @Test
  void isPresentWhenFromNonAnnotatedClass() {
    assertThat(MergedAnnotations.from(NonAnnotatedClass.class).
            isPresent(Transactional.class)).isFalse();
  }

  @Test
  void isPresentWhenFromAnnotationClassWithMetaDepth0() {
    assertThat(MergedAnnotations.from(TransactionalComponent.class).
            isPresent(TransactionalComponent.class)).isFalse();
  }

  @Test
  void isPresentWhenFromAnnotationClassWithMetaDepth1() {
    MergedAnnotations annotations = MergedAnnotations.from(TransactionalComponent.class);
    assertThat(annotations.isPresent(Transactional.class)).isTrue();
    assertThat(annotations.isPresent(Component.class)).isTrue();
  }

  @Test
  void isPresentWhenFromAnnotationClassWithMetaDepth2() {
    MergedAnnotations annotations = MergedAnnotations.from(
            ComposedTransactionalComponent.class);
    assertThat(annotations.isPresent(Transactional.class)).isTrue();
    assertThat(annotations.isPresent(Component.class)).isTrue();
    assertThat(annotations.isPresent(ComposedTransactionalComponent.class)).isFalse();
  }

  @Test
  void isPresentWhenFromClassWithMetaDepth0() {
    assertThat(MergedAnnotations.from(TransactionalComponentClass.class).isPresent(
            TransactionalComponent.class)).isTrue();
  }

  @Test
  void isPresentWhenFromSubclassWithMetaDepth0() {
    assertThat(MergedAnnotations.from(SubTransactionalComponentClass.class).isPresent(
            TransactionalComponent.class)).isFalse();
  }

  @Test
  void isPresentWhenFromClassWithMetaDepth1() {
    MergedAnnotations annotations = MergedAnnotations.from(
            TransactionalComponentClass.class);
    assertThat(annotations.isPresent(Transactional.class)).isTrue();
    assertThat(annotations.isPresent(Component.class)).isTrue();
  }

  @Test
  void isPresentWhenFromClassWithMetaDepth2() {
    MergedAnnotations annotations = MergedAnnotations.from(
            ComposedTransactionalComponentClass.class);
    assertThat(annotations.isPresent(Transactional.class)).isTrue();
    assertThat(annotations.isPresent(Component.class)).isTrue();
    assertThat(annotations.isPresent(ComposedTransactionalComponent.class)).isTrue();
  }

  @Test
  void getParent() {
    MergedAnnotations annotations = MergedAnnotations.from(ComposedTransactionalComponentClass.class);
    assertThat(annotations.get(TransactionalComponent.class).getMetaSource().getType())
            .isEqualTo(ComposedTransactionalComponent.class);
  }

  @Test
  void getRootWhenNotDirect() {
    MergedAnnotations annotations = MergedAnnotations.from(ComposedTransactionalComponentClass.class);
    MergedAnnotation<?> annotation = annotations.get(TransactionalComponent.class);
    assertThat(annotation.getDistance()).isGreaterThan(0);
    assertThat(annotation.getRoot().getType()).isEqualTo(ComposedTransactionalComponent.class);
  }

  @Test
  void getRootWhenDirect() {
    MergedAnnotations annotations = MergedAnnotations.from(ComposedTransactionalComponentClass.class);
    MergedAnnotation<?> annotation = annotations.get(ComposedTransactionalComponent.class);
    assertThat(annotation.getDistance()).isEqualTo(0);
    assertThat(annotation.getRoot()).isSameAs(annotation);
  }

  @Test
  void getMetaTypes() {
    MergedAnnotation<?> annotation = MergedAnnotations.from(
            ComposedTransactionalComponentClass.class).get(
            TransactionalComponent.class);
    assertThat(annotation.getMetaTypes()).containsExactly(
            ComposedTransactionalComponent.class, TransactionalComponent.class);
  }

  @Test
  void collectMultiValueMapFromNonAnnotatedClass() {
    MultiValueMap<String, Object> map = MergedAnnotations.from(
            NonAnnotatedClass.class).stream(Transactional.class).collect(
            MergedAnnotationCollectors.toMultiValueMap());
    assertThat(map).isEmpty();
  }

  @Test
  void collectMultiValueMapFromClassWithLocalAnnotation() {
    MultiValueMap<String, Object> map = MergedAnnotations.from(TxConfig.class).stream(
            Transactional.class).collect(
            MergedAnnotationCollectors.toMultiValueMap());
    assertThat(map).contains(entry("value", Arrays.asList("TxConfig")));
  }

  @Test
  void collectMultiValueMapFromClassWithLocalComposedAnnotationAndInheritedAnnotation() {
    MultiValueMap<String, Object> map = MergedAnnotations.from(
            SubClassWithInheritedAnnotation.class,
            SearchStrategy.INHERITED_ANNOTATIONS).stream(Transactional.class).collect(
            MergedAnnotationCollectors.toMultiValueMap());
    assertThat(map).contains(
            entry("qualifier", Arrays.asList("composed2", "transactionManager")));
  }

  @Test
  void collectMultiValueMapFavorsInheritedAnnotationsOverMoreLocallyDeclaredComposedAnnotations() {
    MultiValueMap<String, Object> map = MergedAnnotations.from(
            SubSubClassWithInheritedAnnotation.class,
            SearchStrategy.INHERITED_ANNOTATIONS).stream(Transactional.class).collect(
            MergedAnnotationCollectors.toMultiValueMap());
    assertThat(map).contains(entry("qualifier", Arrays.asList("transactionManager")));
  }

  @Test
  void collectMultiValueMapFavorsInheritedComposedAnnotationsOverMoreLocallyDeclaredComposedAnnotations() {
    MultiValueMap<String, Object> map = MergedAnnotations.from(
            SubSubClassWithInheritedComposedAnnotation.class,
            SearchStrategy.INHERITED_ANNOTATIONS).stream(Transactional.class).collect(
            MergedAnnotationCollectors.toMultiValueMap());
    assertThat(map).contains(entry("qualifier", Arrays.asList("composed1")));
  }

  /**
   * If the "value" entry contains both "DerivedTxConfig" AND "TxConfig", then
   * the algorithm is accidentally picking up shadowed annotations of the same
   * type within the class hierarchy. Such undesirable behavior would cause
   * the logic in
   * {@code cn.taketoday.context.annotation.ProfileCondition} to fail.
   */
  @Test
  void collectMultiValueMapFromClassWithLocalAnnotationThatShadowsAnnotationFromSuperclass() {
    MultiValueMap<String, Object> map = MergedAnnotations.from(DerivedTxConfig.class,
            SearchStrategy.INHERITED_ANNOTATIONS).stream(Transactional.class).collect(
            MergedAnnotationCollectors.toMultiValueMap());
    assertThat(map).contains(entry("value", Arrays.asList("DerivedTxConfig")));
  }

  /**
   * Note: this functionality is required by
   * {@code cn.taketoday.context.annotation.ProfileCondition}.
   */
  @Test
  void collectMultiValueMapFromClassWithMultipleComposedAnnotations() {
    MultiValueMap<String, Object> map = MergedAnnotations.from(
            TxFromMultipleComposedAnnotations.class,
            SearchStrategy.INHERITED_ANNOTATIONS).stream(Transactional.class).collect(
            MergedAnnotationCollectors.toMultiValueMap());
    assertThat(map).contains(
            entry("value", Arrays.asList("TxInheritedComposed", "TxComposed")));
  }

  @Test
  void getWithInheritedAnnotationsFromClassWithLocalAnnotation() {
    MergedAnnotation<?> annotation = MergedAnnotations.from(TxConfig.class,
            SearchStrategy.INHERITED_ANNOTATIONS).get(Transactional.class);
    assertThat(annotation.getStringValue()).isEqualTo("TxConfig");
  }

  @Test
  void getWithInheritedAnnotationsFromClassWithLocalAnnotationThatShadowsAnnotationFromSuperclass() {
    MergedAnnotation<?> annotation = MergedAnnotations.from(DerivedTxConfig.class,
            SearchStrategy.INHERITED_ANNOTATIONS).get(Transactional.class);
    assertThat(annotation.getString("value")).isEqualTo("DerivedTxConfig");
  }

  @Test
  void getWithInheritedAnnotationsFromMetaCycleAnnotatedClassWithMissingTargetMetaAnnotation() {
    MergedAnnotation<?> annotation = MergedAnnotations.from(
            MetaCycleAnnotatedClass.class, SearchStrategy.INHERITED_ANNOTATIONS).get(Transactional.class);
    assertThat(annotation.isPresent()).isFalse();
  }

  @Test
  void getWithInheritedAnnotationsFavorsLocalComposedAnnotationOverInheritedAnnotation() {
    MergedAnnotation<?> annotation = MergedAnnotations.from(
            SubClassWithInheritedAnnotation.class,
            SearchStrategy.INHERITED_ANNOTATIONS).get(Transactional.class);
    assertThat(annotation.getBoolean("readOnly")).isTrue();
  }

  @Test
  void getWithInheritedAnnotationsFavorsInheritedAnnotationsOverMoreLocallyDeclaredComposedAnnotations() {
    MergedAnnotation<?> annotation = MergedAnnotations.from(
            SubSubClassWithInheritedAnnotation.class,
            SearchStrategy.INHERITED_ANNOTATIONS).get(Transactional.class);
    assertThat(annotation.getBoolean("readOnly")).isFalse();
  }

  @Test
  void getWithInheritedAnnotationsFavorsInheritedComposedAnnotationsOverMoreLocallyDeclaredComposedAnnotations() {
    MergedAnnotation<?> annotation = MergedAnnotations.from(
            SubSubClassWithInheritedComposedAnnotation.class,
            SearchStrategy.INHERITED_ANNOTATIONS).get(Transactional.class);
    assertThat(annotation.getBoolean("readOnly")).isFalse();
  }

  @Test
  void getWithInheritedAnnotationsFromInterfaceImplementedBySuperclass() {
    MergedAnnotation<?> annotation = MergedAnnotations.from(
            ConcreteClassWithInheritedAnnotation.class,
            SearchStrategy.INHERITED_ANNOTATIONS).get(Transactional.class);
    assertThat(annotation.isPresent()).isFalse();
  }

  @Test
  void getWithInheritedAnnotationsFromInheritedAnnotationInterface() {
    MergedAnnotation<?> annotation = MergedAnnotations.from(
            InheritedAnnotationInterface.class,
            SearchStrategy.INHERITED_ANNOTATIONS).get(Transactional.class);
    assertThat(annotation.isPresent()).isTrue();
  }

  @Test
  void getWithInheritedAnnotationsFromNonInheritedAnnotationInterface() {
    MergedAnnotation<?> annotation = MergedAnnotations.from(
            NonInheritedAnnotationInterface.class,
            SearchStrategy.INHERITED_ANNOTATIONS).get(Order.class);
    assertThat(annotation.isPresent()).isTrue();
  }

  @Test
  void getWithInheritedAnnotationsAttributesWithConventionBasedComposedAnnotation() {
    MergedAnnotation<?> annotation = MergedAnnotations.from(
            ConventionBasedComposedContextConfigurationClass.class,
            SearchStrategy.INHERITED_ANNOTATIONS).get(ContextConfiguration.class);
    assertThat(annotation.isPresent()).isTrue();
    assertThat(annotation.getStringArray("locations")).containsExactly(
            "explicitDeclaration");
    assertThat(annotation.getStringArray("value")).containsExactly(
            "explicitDeclaration");
  }

  @Test
  void getWithInheritedAnnotationsFromHalfConventionBasedAndHalfAliasedComposedAnnotation1() {
    // SPR-13554: convention mapping mixed with AliasFor annotations
    // xmlConfigFiles can be used because it has an AliasFor annotation
    MergedAnnotation<?> annotation = MergedAnnotations.from(
            HalfConventionBasedAndHalfAliasedComposedContextConfigurationClass1.class,
            SearchStrategy.INHERITED_ANNOTATIONS).get(ContextConfiguration.class);
    assertThat(annotation.getStringArray("locations")).containsExactly(
            "explicitDeclaration");
    assertThat(annotation.getStringArray("value")).containsExactly(
            "explicitDeclaration");
  }

  @Test
  void withInheritedAnnotationsFromHalfConventionBasedAndHalfAliasedComposedAnnotation2() {
    // SPR-13554: convention mapping mixed with AliasFor annotations
    // locations doesn't apply because it has no AliasFor annotation
    MergedAnnotation<?> annotation = MergedAnnotations.from(
            HalfConventionBasedAndHalfAliasedComposedContextConfigurationClass2.class,
            SearchStrategy.INHERITED_ANNOTATIONS).get(ContextConfiguration.class);
    assertThat(annotation.getStringArray("locations")).isEmpty();
    assertThat(annotation.getStringArray("value")).isEmpty();
  }

  @Test
  void withInheritedAnnotationsFromAliasedComposedAnnotation() {
    MergedAnnotation<?> annotation = MergedAnnotations.from(
            AliasedComposedContextConfigurationClass.class,
            SearchStrategy.INHERITED_ANNOTATIONS).get(ContextConfiguration.class);
    assertThat(annotation.getStringArray("value")).containsExactly("test.xml");
    assertThat(annotation.getStringArray("locations")).containsExactly("test.xml");
  }

  @Test
  void withInheritedAnnotationsFromAliasedValueComposedAnnotation() {
    MergedAnnotation<?> annotation = MergedAnnotations.from(
            AliasedValueComposedContextConfigurationClass.class,
            SearchStrategy.INHERITED_ANNOTATIONS).get(ContextConfiguration.class);
    assertThat(annotation.getStringArray("value")).containsExactly("test.xml");
    assertThat(annotation.getStringArray("locations")).containsExactly("test.xml");
  }

  @Test
  void getWithInheritedAnnotationsFromImplicitAliasesInMetaAnnotationOnComposedAnnotation() {
    MergedAnnotation<?> annotation = MergedAnnotations.from(
            ComposedImplicitAliasesContextConfigurationClass.class,
            SearchStrategy.INHERITED_ANNOTATIONS).get(
            ImplicitAliasesContextConfiguration.class);
    assertThat(annotation.getStringArray("groovyScripts")).containsExactly("A.xml",
            "B.xml");
    assertThat(annotation.getStringArray("xmlFiles")).containsExactly("A.xml",
            "B.xml");
    assertThat(annotation.getStringArray("locations")).containsExactly("A.xml",
            "B.xml");
    assertThat(annotation.getStringArray("value")).containsExactly("A.xml", "B.xml");
  }

  @Test
  void getWithInheritedAnnotationsFromAliasedValueComposedAnnotation() {
    testGetWithInherited(AliasedValueComposedContextConfigurationClass.class, "test.xml");
  }

  @Test
  void getWithInheritedAnnotationsFromImplicitAliasesForSameAttributeInComposedAnnotation() {
    testGetWithInherited(ImplicitAliasesContextConfigurationClass1.class, "foo.xml");
    testGetWithInherited(ImplicitAliasesContextConfigurationClass2.class, "bar.xml");
    testGetWithInherited(ImplicitAliasesContextConfigurationClass3.class, "baz.xml");
  }

  @Test
  void getWithInheritedAnnotationsFromTransitiveImplicitAliases() {
    testGetWithInherited(TransitiveImplicitAliasesContextConfigurationClass.class, "test.groovy");
  }

  @Test
  void getWithInheritedAnnotationsFromTransitiveImplicitAliasesWithSingleElementOverridingAnArrayViaAliasFor() {
    testGetWithInherited(
            SingleLocationTransitiveImplicitAliasesContextConfigurationClass.class, "test.groovy");
  }

  @Test
  void getWithInheritedAnnotationsFromTransitiveImplicitAliasesWithSkippedLevel() {
    testGetWithInherited(
            TransitiveImplicitAliasesWithSkippedLevelContextConfigurationClass.class, "test.xml");
  }

  @Test
  void getWithInheritedAnnotationsFromTransitiveImplicitAliasesWithSkippedLevelWithSingleElementOverridingAnArrayViaAliasFor() {
    testGetWithInherited(
            SingleLocationTransitiveImplicitAliasesWithSkippedLevelContextConfigurationClass.class, "test.xml");
  }

  private void testGetWithInherited(Class<?> element, String... expected) {
    MergedAnnotation<?> annotation = MergedAnnotations.from(element,
            SearchStrategy.INHERITED_ANNOTATIONS).get(ContextConfiguration.class);
    assertThat(annotation.getStringArray("locations")).isEqualTo(expected);
    assertThat(annotation.getStringArray("value")).isEqualTo(expected);
    assertThat(annotation.getClassArray("classes")).isEmpty();
  }

  @Test
  void getWithInheritedAnnotationsFromInvalidConventionBasedComposedAnnotation() {
    assertThatExceptionOfType(AnnotationConfigurationException.class).isThrownBy(() ->
            MergedAnnotations.from(InvalidConventionBasedComposedContextConfigurationClass.class,
                            SearchStrategy.INHERITED_ANNOTATIONS)
                    .get(ContextConfiguration.class));
  }

  @Test
  void getWithInheritedAnnotationsFromShadowedAliasComposedAnnotation() {
    MergedAnnotation<?> annotation = MergedAnnotations.from(
            ShadowedAliasComposedContextConfigurationClass.class,
            SearchStrategy.INHERITED_ANNOTATIONS).get(ContextConfiguration.class);
    assertThat(annotation.getStringArray("locations")).containsExactly("test.xml");
    assertThat(annotation.getStringArray("value")).containsExactly("test.xml");
  }

  @Test
  void getWithTypeHierarchyFromInheritedAnnotationInterface() {
    MergedAnnotation<?> annotation = MergedAnnotations.from(
            InheritedAnnotationInterface.class, SearchStrategy.TYPE_HIERARCHY).get(Transactional.class);
    assertThat(annotation.isPresent()).isTrue();
    assertThat(annotation.getAggregateIndex()).isEqualTo(0);
  }

  @Test
  void getWithTypeHierarchyFromSubInheritedAnnotationInterface() {
    MergedAnnotation<?> annotation = MergedAnnotations.from(
            SubInheritedAnnotationInterface.class, SearchStrategy.TYPE_HIERARCHY).get(Transactional.class);
    assertThat(annotation.isPresent()).isTrue();
    assertThat(annotation.getAggregateIndex()).isEqualTo(1);
  }

  @Test
  void getWithTypeHierarchyFromSubSubInheritedAnnotationInterface() {
    MergedAnnotation<?> annotation = MergedAnnotations.from(
            SubSubInheritedAnnotationInterface.class, SearchStrategy.TYPE_HIERARCHY).get(Transactional.class);
    assertThat(annotation.isPresent()).isTrue();
    assertThat(annotation.getAggregateIndex()).isEqualTo(2);
  }

  @Test
  void getWithTypeHierarchyFromNonInheritedAnnotationInterface() {
    MergedAnnotation<?> annotation = MergedAnnotations.from(
            NonInheritedAnnotationInterface.class, SearchStrategy.TYPE_HIERARCHY).get(Order.class);
    assertThat(annotation.isPresent()).isTrue();
    assertThat(annotation.getAggregateIndex()).isEqualTo(0);
  }

  @Test
  void getWithTypeHierarchyFromSubNonInheritedAnnotationInterface() {
    MergedAnnotation<?> annotation = MergedAnnotations.from(
            SubNonInheritedAnnotationInterface.class, SearchStrategy.TYPE_HIERARCHY).get(Order.class);
    assertThat(annotation.isPresent()).isTrue();
    assertThat(annotation.getAggregateIndex()).isEqualTo(1);
  }

  @Test
  void getWithTypeHierarchyFromSubSubNonInheritedAnnotationInterface() {
    MergedAnnotation<?> annotation = MergedAnnotations.from(
            SubSubNonInheritedAnnotationInterface.class,
            SearchStrategy.TYPE_HIERARCHY).get(Order.class);
    assertThat(annotation.isPresent()).isTrue();
    assertThat(annotation.getAggregateIndex()).isEqualTo(2);
  }

  @Test
  void getWithTypeHierarchyInheritedFromInterfaceMethod()
          throws NoSuchMethodException {
    Method method = ConcreteClassWithInheritedAnnotation.class.getMethod(
            "handleFromInterface");
    MergedAnnotation<?> annotation = MergedAnnotations.from(method,
            SearchStrategy.TYPE_HIERARCHY).get(Order.class);
    assertThat(annotation.isPresent()).isTrue();
    assertThat(annotation.getAggregateIndex()).isEqualTo(1);
  }

  @Test
  void getWithTypeHierarchyInheritedFromAbstractMethod() throws NoSuchMethodException {
    Method method = ConcreteClassWithInheritedAnnotation.class.getMethod("handle");
    MergedAnnotation<?> annotation = MergedAnnotations.from(method,
            SearchStrategy.TYPE_HIERARCHY).get(Transactional.class);
    assertThat(annotation.isPresent()).isTrue();
    assertThat(annotation.getAggregateIndex()).isEqualTo(1);
  }

  @Test
  void getWithTypeHierarchyInheritedFromBridgedMethod() throws NoSuchMethodException {
    Method method = ConcreteClassWithInheritedAnnotation.class.getMethod(
            "handleParameterized", String.class);
    MergedAnnotation<?> annotation = MergedAnnotations.from(method,
            SearchStrategy.TYPE_HIERARCHY).get(Transactional.class);
    assertThat(annotation.isPresent()).isTrue();
    assertThat(annotation.getAggregateIndex()).isEqualTo(1);
  }

  @Test
  void getWithTypeHierarchyFromBridgeMethod() {
    List<Method> methods = new ArrayList<>();
    ReflectionUtils.doWithLocalMethods(StringGenericParameter.class, method -> {
      if ("getFor".equals(method.getName())) {
        methods.add(method);
      }
    });
    Method bridgeMethod = methods.get(0).getReturnType().equals(Object.class) ?
                          methods.get(0) : methods.get(1);
    Method bridgedMethod = methods.get(0).getReturnType().equals(Object.class) ?
                           methods.get(1) : methods.get(0);
    assertThat(bridgeMethod.isBridge()).isTrue();
    assertThat(bridgedMethod.isBridge()).isFalse();
    MergedAnnotation<?> annotation = MergedAnnotations.from(bridgeMethod,
            SearchStrategy.TYPE_HIERARCHY).get(Order.class);
    assertThat(annotation.isPresent()).isTrue();
    assertThat(annotation.getAggregateIndex()).isEqualTo(0);
  }

  @Test
  void getWithTypeHierarchyFromClassWithMetaAndLocalTxConfig() {
    MergedAnnotation<?> annotation = MergedAnnotations.from(
            MetaAndLocalTxConfigClass.class, SearchStrategy.TYPE_HIERARCHY).get(
            Transactional.class);
    assertThat(annotation.getString("qualifier")).isEqualTo("localTxMgr");
  }

  @Test
  void getWithTypeHierarchyFromClassWithAttributeAliasesInTargetAnnotation() {
    MergedAnnotation<AliasedTransactional> mergedAnnotation = MergedAnnotations.from(
            AliasedTransactionalComponentClass.class, SearchStrategy.TYPE_HIERARCHY).get(
            AliasedTransactional.class);
    AliasedTransactional synthesizedAnnotation = mergedAnnotation.synthesize();
    String qualifier = "aliasForQualifier";
    assertThat(mergedAnnotation.getString("value")).isEqualTo(qualifier);
    assertThat(mergedAnnotation.getString("qualifier")).isEqualTo(qualifier);
    assertThat(synthesizedAnnotation.value()).isEqualTo(qualifier);
    assertThat(synthesizedAnnotation.qualifier()).isEqualTo(qualifier);
  }

  @Test
    // gh-23767
  void getWithTypeHierarchyFromClassWithComposedMetaTransactionalAnnotation() {
    MergedAnnotation<AliasedTransactional> mergedAnnotation = MergedAnnotations.from(
            ComposedTransactionalClass.class, SearchStrategy.TYPE_HIERARCHY).get(
            AliasedTransactional.class);
    assertThat(mergedAnnotation.getString("value")).isEqualTo("anotherTransactionManager");
    assertThat(mergedAnnotation.getString("qualifier")).isEqualTo("anotherTransactionManager");
  }

  @Test
    // gh-23767
  void getWithTypeHierarchyFromClassWithMetaMetaAliasedTransactional() {
    MergedAnnotation<AliasedTransactional> mergedAnnotation = MergedAnnotations.from(
            MetaMetaAliasedTransactionalClass.class, SearchStrategy.TYPE_HIERARCHY).get(
            AliasedTransactional.class);
    assertThat(mergedAnnotation.getString("value")).isEqualTo("meta");
    assertThat(mergedAnnotation.getString("qualifier")).isEqualTo("meta");
  }

  @Test
  void getWithTypeHierarchyFromClassWithAttributeAliasInComposedAnnotationAndNestedAnnotationsInTargetAnnotation() {
    MergedAnnotation<?> annotation = testGetWithTypeHierarchy(
            TestComponentScanClass.class, "com.example.app.test");
    MergedAnnotation<Filter>[] excludeFilters = annotation.getAnnotationArray(
            "excludeFilters", Filter.class);
    assertThat(Arrays.stream(excludeFilters).map(
            filter -> filter.getString("pattern"))).containsExactly("*Test", "*Tests");
  }

  @Test
  void getWithTypeHierarchyFromClassWithBothAttributesOfAnAliasPairDeclared() {
    testGetWithTypeHierarchy(ComponentScanWithBasePackagesAndValueAliasClass.class, "com.example.app.test");
  }

  @Test
  void getWithTypeHierarchyWithSingleElementOverridingAnArrayViaConvention() {
    testGetWithTypeHierarchy(ConventionBasedSinglePackageComponentScanClass.class, "com.example.app.test");
  }

  @Test
  void getWithTypeHierarchyWithSingleElementOverridingAnArrayViaAliasFor() {
    testGetWithTypeHierarchy(AliasForBasedSinglePackageComponentScanClass.class, "com.example.app.test");
  }

  private MergedAnnotation<?> testGetWithTypeHierarchy(Class<?> element, String... expected) {
    MergedAnnotation<?> annotation = MergedAnnotations.from(element,
            SearchStrategy.TYPE_HIERARCHY).get(ComponentScan.class);
    assertThat(annotation.getStringArray("value")).containsExactly(expected);
    assertThat(annotation.getStringArray("basePackages")).containsExactly(expected);
    return annotation;
  }

  @Test
  void getWithTypeHierarchyWhenMultipleMetaAnnotationsHaveClashingAttributeNames() {
    MergedAnnotations annotations = MergedAnnotations.from(
            AliasedComposedContextConfigurationAndTestPropertySourceClass.class,
            SearchStrategy.TYPE_HIERARCHY);
    MergedAnnotation<?> contextConfig = annotations.get(ContextConfiguration.class);
    assertThat(contextConfig.getStringArray("locations")).containsExactly("test.xml");
    assertThat(contextConfig.getStringArray("value")).containsExactly("test.xml");
    MergedAnnotation<?> testPropSource = annotations.get(TestPropertySource.class);
    assertThat(testPropSource.getStringArray("locations")).containsExactly(
            "test.properties");
    assertThat(testPropSource.getStringArray("value")).containsExactly(
            "test.properties");
  }

  @Test
  void getWithTypeHierarchyWithLocalAliasesThatConflictWithAttributesInMetaAnnotationByConvention() {
    MergedAnnotation<?> annotation = MergedAnnotations.from(
            SpringApplicationConfigurationClass.class, SearchStrategy.TYPE_HIERARCHY).get(
            ContextConfiguration.class);
    assertThat(annotation.getStringArray("locations")).isEmpty();
    assertThat(annotation.getStringArray("value")).isEmpty();
    Class<?>[] classArrayValues = annotation.getClassArray("classes");
    assertThat(classArrayValues).containsExactly(Number.class);
  }

  @Test
  void getWithTypeHierarchyOnMethodWithSingleElementOverridingAnArrayViaConvention() throws Exception {
    testGetWithTypeHierarchyWebMapping(
            WebController.class.getMethod("postMappedWithPathAttribute"));
  }

  @Test
  void getWithTypeHierarchyOnMethodWithSingleElementOverridingAnArrayViaAliasFor() throws Exception {
    testGetWithTypeHierarchyWebMapping(
            WebController.class.getMethod("getMappedWithValueAttribute"));
    testGetWithTypeHierarchyWebMapping(
            WebController.class.getMethod("getMappedWithPathAttribute"));
  }

  private void testGetWithTypeHierarchyWebMapping(AnnotatedElement element) {
    MergedAnnotation<?> annotation = MergedAnnotations.from(element,
            SearchStrategy.TYPE_HIERARCHY).get(RequestMapping.class);
    assertThat(annotation.getStringArray("value")).containsExactly("/test");
    assertThat(annotation.getStringArray("path")).containsExactly("/test");
  }

  @Test
  void getDirectWithJavaxAnnotationType() throws Exception {
    assertThat(MergedAnnotations.from(ResourceHolder.class).get(
            Resource.class).getString("name")).isEqualTo("x");
  }

  @Test
  void streamInheritedFromClassWithInterface() throws Exception {
    Method method = TransactionalServiceImpl.class.getMethod("doIt");
    assertThat(MergedAnnotations.from(method, SearchStrategy.INHERITED_ANNOTATIONS).stream(
            Transactional.class)).isEmpty();
  }

  @Test
  void streamTypeHierarchyFromClassWithInterface() throws Exception {
    Method method = TransactionalServiceImpl.class.getMethod("doIt");
    assertThat(MergedAnnotations.from(method, SearchStrategy.TYPE_HIERARCHY).stream(
            Transactional.class)).hasSize(1);
  }

  @Test
  void streamTypeHierarchyAndEnclosingClassesFromNonAnnotatedInnerClassWithAnnotatedEnclosingClass() {
    Stream<Class<?>> classes = MergedAnnotations.from(AnnotatedClass.NonAnnotatedInnerClass.class,
            SearchStrategy.TYPE_HIERARCHY_AND_ENCLOSING_CLASSES).stream().map(MergedAnnotation::getType);
    assertThat(classes).containsExactly(Component.class, Indexed.class);
  }

  @Test
  void streamTypeHierarchyAndEnclosingClassesFromNonAnnotatedStaticNestedClassWithAnnotatedEnclosingClass() {
    Stream<Class<?>> classes = MergedAnnotations.from(AnnotatedClass.NonAnnotatedStaticNestedClass.class,
            SearchStrategy.TYPE_HIERARCHY_AND_ENCLOSING_CLASSES).stream().map(MergedAnnotation::getType);
    assertThat(classes).containsExactly(Component.class, Indexed.class);
  }

  @Test
  void getFromMethodWithMethodAnnotationOnLeaf() throws Exception {
    Method method = Leaf.class.getMethod("annotatedOnLeaf");
    assertThat(method.getAnnotation(Order.class)).isNotNull();
    assertThat(MergedAnnotations.from(method).get(Order.class).getDistance()).isEqualTo(0);
    assertThat(MergedAnnotations.from(method, SearchStrategy.TYPE_HIERARCHY).get(
            Order.class).getDistance()).isEqualTo(0);
  }

  @Test
  void getFromMethodWithAnnotationOnMethodInInterface() throws Exception {
    Method method = Leaf.class.getMethod("fromInterfaceImplementedByRoot");
    assertThat(method.getAnnotation(Order.class)).isNull();
    assertThat(MergedAnnotations.from(method).get(Order.class).getDistance()).isEqualTo(-1);
    assertThat(MergedAnnotations.from(method, SearchStrategy.TYPE_HIERARCHY).get(
            Order.class).getDistance()).isEqualTo(0);
  }

  @Test
  void getFromMethodWithMetaAnnotationOnLeaf() throws Exception {
    Method method = Leaf.class.getMethod("metaAnnotatedOnLeaf");
    assertThat(method.getAnnotation(Order.class)).isNull();
    assertThat(MergedAnnotations.from(method).get(Order.class).getDistance()).isEqualTo(1);
    assertThat(MergedAnnotations.from(method, SearchStrategy.TYPE_HIERARCHY).get(
            Order.class).getDistance()).isEqualTo(1);
  }

  @Test
  void getFromMethodWithMetaMetaAnnotationOnLeaf() throws Exception {
    Method method = Leaf.class.getMethod("metaMetaAnnotatedOnLeaf");
    assertThat(method.getAnnotation(Component.class)).isNull();
    assertThat(MergedAnnotations.from(method).get(Component.class).getDistance()).isEqualTo(2);
    assertThat(MergedAnnotations.from(method, SearchStrategy.TYPE_HIERARCHY).get(
            Component.class).getDistance()).isEqualTo(2);
  }

  @Test
  void getWithAnnotationOnRoot() throws Exception {
    Method method = Leaf.class.getMethod("annotatedOnRoot");
    assertThat(method.getAnnotation(Order.class)).isNotNull();
    assertThat(MergedAnnotations.from(method).get(Order.class).getDistance()).isEqualTo(0);
    assertThat(MergedAnnotations.from(method, SearchStrategy.TYPE_HIERARCHY).get(
            Order.class).getDistance()).isEqualTo(0);
  }

  @Test
  void getFromMethodWithMetaAnnotationOnRoot() throws Exception {
    Method method = Leaf.class.getMethod("metaAnnotatedOnRoot");
    assertThat(method.getAnnotation(Order.class)).isNull();
    assertThat(MergedAnnotations.from(method).get(Order.class).getDistance()).isEqualTo(1);
    assertThat(MergedAnnotations.from(method, SearchStrategy.TYPE_HIERARCHY).get(
            Order.class).getDistance()).isEqualTo(1);
  }

  @Test
  void getFromMethodWithOnRootButOverridden() throws Exception {
    Method method = Leaf.class.getMethod("overrideWithoutNewAnnotation");
    assertThat(method.getAnnotation(Order.class)).isNull();
    assertThat(MergedAnnotations.from(method).get(Order.class).getDistance()).isEqualTo(-1);
    assertThat(MergedAnnotations.from(method, SearchStrategy.TYPE_HIERARCHY).get(
            Order.class).getDistance()).isEqualTo(0);
  }

  @Test
  void getFromMethodWithNotAnnotated() throws Exception {
    Method method = Leaf.class.getMethod("notAnnotated");
    assertThat(method.getAnnotation(Order.class)).isNull();
    assertThat(MergedAnnotations.from(method).get(Order.class).getDistance()).isEqualTo(-1);
    assertThat(MergedAnnotations.from(method, SearchStrategy.TYPE_HIERARCHY).get(
            Order.class).getDistance()).isEqualTo(-1);
  }

  @Test
  void getFromMethodWithBridgeMethod() throws Exception {
    Method method = TransactionalStringGeneric.class.getMethod("something", Object.class);
    assertThat(method.isBridge()).isTrue();
    assertThat(method.getAnnotation(Order.class)).isNull();
    assertThat(MergedAnnotations.from(method).get(Order.class).getDistance()).isEqualTo(-1);
    assertThat(MergedAnnotations.from(method, SearchStrategy.TYPE_HIERARCHY).get(
            Order.class).getDistance()).isEqualTo(0);
    boolean runningInEclipse = Arrays.stream(
            new Exception().getStackTrace()).anyMatch(
            element -> element.getClassName().startsWith("org.eclipse.jdt"));
    // As of JDK 8, invoking getAnnotation() on a bridge method actually finds an
    // annotation on its 'bridged' method [1]; however, the Eclipse compiler
    // will not support this until Eclipse 4.9 [2]. Thus, we effectively ignore the
    // following assertion if the test is currently executing within the Eclipse IDE.
    // [1] https://bugs.openjdk.java.net/browse/JDK-6695379
    // [2] https://bugs.eclipse.org/bugs/show_bug.cgi?id=495396
    if (!runningInEclipse) {
      assertThat(method.getAnnotation(Transactional.class)).isNotNull();
    }
    assertThat(MergedAnnotations.from(method).get(
            Transactional.class).getDistance()).isEqualTo(0);
    assertThat(MergedAnnotations.from(method, SearchStrategy.TYPE_HIERARCHY).get(
            Transactional.class).getDistance()).isEqualTo(0);
  }

  @Test
  void getFromMethodWithBridgedMethod() throws Exception {
    Method method = TransactionalStringGeneric.class.getMethod("something", String.class);
    assertThat(method.isBridge()).isFalse();
    assertThat(method.getAnnotation(Order.class)).isNull();
    assertThat(MergedAnnotations.from(method).get(Order.class).getDistance()).isEqualTo(-1);
    assertThat(MergedAnnotations.from(method, SearchStrategy.TYPE_HIERARCHY).get(
            Order.class).getDistance()).isEqualTo(0);
    assertThat(method.getAnnotation(Transactional.class)).isNotNull();
    assertThat(MergedAnnotations.from(method).get(
            Transactional.class).getDistance()).isEqualTo(0);
    assertThat(MergedAnnotations.from(method, SearchStrategy.TYPE_HIERARCHY).get(
            Transactional.class).getDistance()).isEqualTo(0);
  }

  @Test
  void getFromMethodWithInterface() throws Exception {
    Method method = ImplementsInterfaceWithAnnotatedMethod.class.getMethod("foo");
    assertThat(MergedAnnotations.from(method, SearchStrategy.TYPE_HIERARCHY).get(
            Order.class).getDistance()).isEqualTo(0);
  }

  @Test
    // SPR-16060
  void getFromMethodWithGenericInterface() throws Exception {
    Method method = ImplementsInterfaceWithGenericAnnotatedMethod.class.getMethod("foo", String.class);
    assertThat(MergedAnnotations.from(method, SearchStrategy.TYPE_HIERARCHY).get(
            Order.class).getDistance()).isEqualTo(0);
  }

  @Test
    // SPR-17146
  void getFromMethodWithGenericSuperclass() throws Exception {
    Method method = ExtendsBaseClassWithGenericAnnotatedMethod.class.getMethod("foo", String.class);
    assertThat(MergedAnnotations.from(method, SearchStrategy.TYPE_HIERARCHY).get(
            Order.class).getDistance()).isEqualTo(0);
  }

  @Test
  void getFromMethodWithInterfaceOnSuper() throws Exception {
    Method method = SubOfImplementsInterfaceWithAnnotatedMethod.class.getMethod("foo");
    assertThat(MergedAnnotations.from(method, SearchStrategy.TYPE_HIERARCHY).get(
            Order.class).getDistance()).isEqualTo(0);
  }

  @Test
  void getFromMethodWhenInterfaceWhenSuperDoesNotImplementMethod() throws Exception {
    Method method = SubOfAbstractImplementsInterfaceWithAnnotatedMethod.class.getMethod("foo");
    assertThat(MergedAnnotations.from(method, SearchStrategy.TYPE_HIERARCHY).get(
            Order.class).getDistance()).isEqualTo(0);
  }

  @Test
  void getDirectFromClassFavorsMoreLocallyDeclaredComposedAnnotationsOverAnnotationsOnInterfaces() {
    MergedAnnotation<?> annotation = MergedAnnotations.from(
            ClassWithLocalMetaAnnotationAndMetaAnnotatedInterface.class,
            SearchStrategy.TYPE_HIERARCHY).get(Component.class);
    assertThat(annotation.getStringValue()).isEqualTo("meta2");
  }

  @Test
  void getDirectFromClassFavorsMoreLocallyDeclaredComposedAnnotationsOverInheritedAnnotations() {
    MergedAnnotation<?> annotation = MergedAnnotations.from(
            SubSubClassWithInheritedAnnotation.class, SearchStrategy.TYPE_HIERARCHY).get(Transactional.class);
    assertThat(annotation.getBoolean("readOnly")).isTrue();
  }

  @Test
  void getDirectFromClassFavorsMoreLocallyDeclaredComposedAnnotationsOverInheritedComposedAnnotations() {
    MergedAnnotation<?> annotation = MergedAnnotations.from(
            SubSubClassWithInheritedMetaAnnotation.class,
            SearchStrategy.TYPE_HIERARCHY).get(Component.class);
    assertThat(annotation.getString("value")).isEqualTo("meta2");
  }

  @Test
  void getDirectFromClassgetDirectFromClassMetaMetaAnnotatedClass() {
    MergedAnnotation<?> annotation = MergedAnnotations.from(
            MetaMetaAnnotatedClass.class, SearchStrategy.TYPE_HIERARCHY).get(Component.class);
    assertThat(annotation.getString("value")).isEqualTo("meta2");
  }

  @Test
  void getDirectFromClassWithMetaMetaMetaAnnotatedClass() {
    MergedAnnotation<?> annotation = MergedAnnotations.from(
            MetaMetaMetaAnnotatedClass.class, SearchStrategy.TYPE_HIERARCHY).get(Component.class);
    assertThat(annotation.getString("value")).isEqualTo("meta2");
  }

  @Test
  void getDirectFromClassWithAnnotatedClassWithMissingTargetMetaAnnotation() {
    // TransactionalClass is NOT annotated or meta-annotated with @Component
    MergedAnnotation<?> annotation = MergedAnnotations.from(TransactionalClass.class,
            SearchStrategy.TYPE_HIERARCHY).get(Component.class);
    assertThat(annotation.isPresent()).isFalse();
  }

  @Test
  void getDirectFromClassWithMetaCycleAnnotatedClassWithMissingTargetMetaAnnotation() {
    MergedAnnotation<?> annotation = MergedAnnotations.from(
            MetaCycleAnnotatedClass.class, SearchStrategy.TYPE_HIERARCHY).get(Component.class);
    assertThat(annotation.isPresent()).isFalse();
  }

  @Test
  void getDirectFromClassWithInheritedAnnotationInterface() {
    MergedAnnotation<?> annotation = MergedAnnotations.from(
            InheritedAnnotationInterface.class, SearchStrategy.TYPE_HIERARCHY).get(Transactional.class);
    assertThat(annotation.getAggregateIndex()).isEqualTo(0);
  }

  @Test
  void getDirectFromClassWithSubInheritedAnnotationInterface() {
    MergedAnnotation<?> annotation = MergedAnnotations.from(
            SubInheritedAnnotationInterface.class, SearchStrategy.TYPE_HIERARCHY).get(Transactional.class);
    assertThat(annotation.getAggregateIndex()).isEqualTo(1);
  }

  @Test
  void getDirectFromClassWithSubSubInheritedAnnotationInterface() {
    MergedAnnotation<?> annotation = MergedAnnotations.from(
            SubSubInheritedAnnotationInterface.class, SearchStrategy.TYPE_HIERARCHY).get(Transactional.class);
    assertThat(annotation.getAggregateIndex()).isEqualTo(2);
  }

  @Test
  void getDirectFromClassWithNonInheritedAnnotationInterface() {
    MergedAnnotation<?> annotation = MergedAnnotations.from(
            NonInheritedAnnotationInterface.class, SearchStrategy.TYPE_HIERARCHY).get(Order.class);
    assertThat(annotation.getAggregateIndex()).isEqualTo(0);
  }

  @Test
  void getDirectFromClassWithSubNonInheritedAnnotationInterface() {
    MergedAnnotation<?> annotation = MergedAnnotations.from(
            SubNonInheritedAnnotationInterface.class, SearchStrategy.TYPE_HIERARCHY).get(Order.class);
    assertThat(annotation.getAggregateIndex()).isEqualTo(1);
  }

  @Test
  void getDirectFromClassWithSubSubNonInheritedAnnotationInterface() {
    MergedAnnotation<?> annotation = MergedAnnotations.from(
            SubSubNonInheritedAnnotationInterface.class,
            SearchStrategy.TYPE_HIERARCHY).get(Order.class);
    assertThat(annotation.getAggregateIndex()).isEqualTo(2);
  }

  @Test
  void getSuperClassForAllScenarios() {
    // no class-level annotation
    assertThat(MergedAnnotations.from(NonAnnotatedInterface.class,
            SearchStrategy.SUPERCLASS).get(
            Transactional.class).getSource()).isNull();
    assertThat(MergedAnnotations.from(NonAnnotatedClass.class,
            SearchStrategy.SUPERCLASS).get(
            Transactional.class).getSource()).isNull();
    // inherited class-level annotation; note: @Transactional is inherited
    assertThat(MergedAnnotations.from(InheritedAnnotationInterface.class,
            SearchStrategy.SUPERCLASS).get(
            Transactional.class).getSource()).isEqualTo(
            InheritedAnnotationInterface.class);
    assertThat(MergedAnnotations.from(SubInheritedAnnotationInterface.class,
            SearchStrategy.SUPERCLASS).get(
            Transactional.class).getSource()).isNull();
    assertThat(MergedAnnotations.from(InheritedAnnotationClass.class,
            SearchStrategy.SUPERCLASS).get(
            Transactional.class).getSource()).isEqualTo(
            InheritedAnnotationClass.class);
    assertThat(MergedAnnotations.from(SubInheritedAnnotationClass.class,
            SearchStrategy.SUPERCLASS).get(
            Transactional.class).getSource()).isEqualTo(
            InheritedAnnotationClass.class);
    // non-inherited class-level annotation; note: @Order is not inherited,
    // but we should still find it on classes.
    assertThat(MergedAnnotations.from(NonInheritedAnnotationInterface.class,
            SearchStrategy.SUPERCLASS).get(Order.class).getSource()).isEqualTo(
            NonInheritedAnnotationInterface.class);
    assertThat(MergedAnnotations.from(SubNonInheritedAnnotationInterface.class,
            SearchStrategy.SUPERCLASS).get(Order.class).getSource()).isNull();
    assertThat(MergedAnnotations.from(NonInheritedAnnotationClass.class,
            SearchStrategy.SUPERCLASS).get(Order.class).getSource()).isEqualTo(
            NonInheritedAnnotationClass.class);
    assertThat(MergedAnnotations.from(SubNonInheritedAnnotationClass.class,
            SearchStrategy.SUPERCLASS).get(Order.class).getSource()).isEqualTo(
            NonInheritedAnnotationClass.class);
  }

  @Test
  void getSuperClassSourceForTypesWithSingleCandidateType() {
    // no class-level annotation
    List<Class<? extends Annotation>> transactionalCandidateList = Collections.singletonList(Transactional.class);
    assertThat(getSuperClassSourceWithTypeIn(NonAnnotatedInterface.class,
            transactionalCandidateList)).isNull();
    assertThat(getSuperClassSourceWithTypeIn(NonAnnotatedClass.class,
            transactionalCandidateList)).isNull();
    // inherited class-level annotation; note: @Transactional is inherited
    assertThat(getSuperClassSourceWithTypeIn(InheritedAnnotationInterface.class,
            transactionalCandidateList)).isEqualTo(
            InheritedAnnotationInterface.class);
    assertThat(getSuperClassSourceWithTypeIn(SubInheritedAnnotationInterface.class,
            transactionalCandidateList)).isNull();
    assertThat(getSuperClassSourceWithTypeIn(InheritedAnnotationClass.class,
            transactionalCandidateList)).isEqualTo(InheritedAnnotationClass.class);
    assertThat(getSuperClassSourceWithTypeIn(SubInheritedAnnotationClass.class,
            transactionalCandidateList)).isEqualTo(InheritedAnnotationClass.class);
    // non-inherited class-level annotation; note: @Order is not inherited,
    // but should still find it on classes.
    List<Class<? extends Annotation>> orderCandidateList = Collections.singletonList(
            Order.class);
    assertThat(getSuperClassSourceWithTypeIn(NonInheritedAnnotationInterface.class,
            orderCandidateList)).isEqualTo(NonInheritedAnnotationInterface.class);
    assertThat(getSuperClassSourceWithTypeIn(SubNonInheritedAnnotationInterface.class,
            orderCandidateList)).isNull();
    assertThat(getSuperClassSourceWithTypeIn(NonInheritedAnnotationClass.class,
            orderCandidateList)).isEqualTo(NonInheritedAnnotationClass.class);
    assertThat(getSuperClassSourceWithTypeIn(SubNonInheritedAnnotationClass.class,
            orderCandidateList)).isEqualTo(NonInheritedAnnotationClass.class);
  }

  @Test
  void getSuperClassSourceForTypesWithMultipleCandidateTypes() {
    List<Class<? extends Annotation>> candidates = Arrays.asList(Transactional.class, Order.class);
    // no class-level annotation
    assertThat(getSuperClassSourceWithTypeIn(NonAnnotatedInterface.class,
            candidates)).isNull();
    assertThat(getSuperClassSourceWithTypeIn(NonAnnotatedClass.class,
            candidates)).isNull();
    // inherited class-level annotation; note: @Transactional is inherited
    assertThat(getSuperClassSourceWithTypeIn(InheritedAnnotationInterface.class,
            candidates)).isEqualTo(InheritedAnnotationInterface.class);
    assertThat(getSuperClassSourceWithTypeIn(SubInheritedAnnotationInterface.class,
            candidates)).isNull();
    assertThat(getSuperClassSourceWithTypeIn(InheritedAnnotationClass.class,
            candidates)).isEqualTo(InheritedAnnotationClass.class);
    assertThat(getSuperClassSourceWithTypeIn(SubInheritedAnnotationClass.class,
            candidates)).isEqualTo(InheritedAnnotationClass.class);
    // non-inherited class-level annotation; note: @Order is not inherited,
    // but findAnnotationDeclaringClassForTypes() should still find it on
    // classes.
    assertThat(getSuperClassSourceWithTypeIn(NonInheritedAnnotationInterface.class,
            candidates)).isEqualTo(NonInheritedAnnotationInterface.class);
    assertThat(getSuperClassSourceWithTypeIn(SubNonInheritedAnnotationInterface.class,
            candidates)).isNull();
    assertThat(getSuperClassSourceWithTypeIn(NonInheritedAnnotationClass.class,
            candidates)).isEqualTo(NonInheritedAnnotationClass.class);
    assertThat(getSuperClassSourceWithTypeIn(SubNonInheritedAnnotationClass.class,
            candidates)).isEqualTo(NonInheritedAnnotationClass.class);
    // class hierarchy mixed with @Transactional and @Order declarations
    assertThat(getSuperClassSourceWithTypeIn(TransactionalClass.class,
            candidates)).isEqualTo(TransactionalClass.class);
    assertThat(getSuperClassSourceWithTypeIn(TransactionalAndOrderedClass.class,
            candidates)).isEqualTo(TransactionalAndOrderedClass.class);
    assertThat(getSuperClassSourceWithTypeIn(SubTransactionalAndOrderedClass.class,
            candidates)).isEqualTo(TransactionalAndOrderedClass.class);
  }

  private Object getSuperClassSourceWithTypeIn(Class<?> clazz, List<Class<? extends Annotation>> annotationTypes) {
    return MergedAnnotations.from(clazz, SearchStrategy.SUPERCLASS).stream().filter(
            MergedAnnotationPredicates.typeIn(annotationTypes).and(
                    MergedAnnotation::isDirectlyPresent)).map(
            MergedAnnotation::getSource).findFirst().orElse(null);
  }

  @Test
  void isDirectlyPresentForAllScenarios() throws Exception {
    // no class-level annotation
    assertThat(MergedAnnotations.from(NonAnnotatedInterface.class).get(
            Transactional.class).isDirectlyPresent()).isFalse();
    assertThat(MergedAnnotations.from(NonAnnotatedInterface.class).isDirectlyPresent(
            Transactional.class)).isFalse();
    assertThat(MergedAnnotations.from(NonAnnotatedClass.class).get(
            Transactional.class).isDirectlyPresent()).isFalse();
    assertThat(MergedAnnotations.from(NonAnnotatedClass.class).isDirectlyPresent(
            Transactional.class)).isFalse();
    // inherited class-level annotation; note: @Transactional is inherited
    assertThat(MergedAnnotations.from(InheritedAnnotationInterface.class).get(
            Transactional.class).isDirectlyPresent()).isTrue();
    assertThat(MergedAnnotations.from(
            InheritedAnnotationInterface.class).isDirectlyPresent(
            Transactional.class)).isTrue();
    assertThat(MergedAnnotations.from(SubInheritedAnnotationInterface.class).get(
            Transactional.class).isDirectlyPresent()).isFalse();
    assertThat(MergedAnnotations.from(
            SubInheritedAnnotationInterface.class).isDirectlyPresent(
            Transactional.class)).isFalse();
    assertThat(MergedAnnotations.from(InheritedAnnotationClass.class).get(
            Transactional.class).isDirectlyPresent()).isTrue();
    assertThat(
            MergedAnnotations.from(InheritedAnnotationClass.class).isDirectlyPresent(
                    Transactional.class)).isTrue();
    assertThat(MergedAnnotations.from(SubInheritedAnnotationClass.class).get(
            Transactional.class).isDirectlyPresent()).isFalse();
    assertThat(MergedAnnotations.from(
            SubInheritedAnnotationClass.class).isDirectlyPresent(
            Transactional.class)).isFalse();
    // non-inherited class-level annotation; note: @Order is not inherited
    assertThat(MergedAnnotations.from(NonInheritedAnnotationInterface.class).get(
            Order.class).isDirectlyPresent()).isTrue();
    assertThat(MergedAnnotations.from(
            NonInheritedAnnotationInterface.class).isDirectlyPresent(
            Order.class)).isTrue();
    assertThat(MergedAnnotations.from(SubNonInheritedAnnotationInterface.class).get(
            Order.class).isDirectlyPresent()).isFalse();
    assertThat(MergedAnnotations.from(
            SubNonInheritedAnnotationInterface.class).isDirectlyPresent(
            Order.class)).isFalse();
    assertThat(MergedAnnotations.from(NonInheritedAnnotationClass.class).get(
            Order.class).isDirectlyPresent()).isTrue();
    assertThat(MergedAnnotations.from(
            NonInheritedAnnotationClass.class).isDirectlyPresent(
            Order.class)).isTrue();
    assertThat(MergedAnnotations.from(SubNonInheritedAnnotationClass.class).get(
            Order.class).isDirectlyPresent()).isFalse();
    assertThat(MergedAnnotations.from(
            SubNonInheritedAnnotationClass.class).isDirectlyPresent(
            Order.class)).isFalse();
  }

  @Test
  void getAggregateIndexForAllScenarios() {
    // no class-level annotation
    assertThat(MergedAnnotations.from(NonAnnotatedInterface.class,
            SearchStrategy.INHERITED_ANNOTATIONS).get(
            Transactional.class).getAggregateIndex()).isEqualTo(-1);
    assertThat(MergedAnnotations.from(NonAnnotatedClass.class,
            SearchStrategy.INHERITED_ANNOTATIONS).get(
            Transactional.class).getAggregateIndex()).isEqualTo(-1);
    // inherited class-level annotation; note: @Transactional is inherited
    assertThat(MergedAnnotations.from(InheritedAnnotationInterface.class,
            SearchStrategy.INHERITED_ANNOTATIONS).get(
            Transactional.class).getAggregateIndex()).isEqualTo(0);
    // Since we're not traversing interface hierarchies the following,
    // though perhaps counter intuitive, must be false:
    assertThat(MergedAnnotations.from(SubInheritedAnnotationInterface.class,
            SearchStrategy.INHERITED_ANNOTATIONS).get(
            Transactional.class).getAggregateIndex()).isEqualTo(-1);
    assertThat(MergedAnnotations.from(InheritedAnnotationClass.class,
            SearchStrategy.INHERITED_ANNOTATIONS).get(
            Transactional.class).getAggregateIndex()).isEqualTo(0);
    assertThat(MergedAnnotations.from(SubInheritedAnnotationClass.class,
            SearchStrategy.INHERITED_ANNOTATIONS).get(
            Transactional.class).getAggregateIndex()).isEqualTo(1);
    // non-inherited class-level annotation; note: @Order is not inherited
    assertThat(MergedAnnotations.from(NonInheritedAnnotationInterface.class,
            SearchStrategy.INHERITED_ANNOTATIONS).get(
            Order.class).getAggregateIndex()).isEqualTo(0);
    assertThat(MergedAnnotations.from(SubNonInheritedAnnotationInterface.class,
            SearchStrategy.INHERITED_ANNOTATIONS).get(
            Order.class).getAggregateIndex()).isEqualTo(-1);
    assertThat(MergedAnnotations.from(NonInheritedAnnotationClass.class,
            SearchStrategy.INHERITED_ANNOTATIONS).get(
            Order.class).getAggregateIndex()).isEqualTo(0);
    assertThat(
            MergedAnnotations.from(
                            SubNonInheritedAnnotationClass.class, SearchStrategy.INHERITED_ANNOTATIONS)
                    .get(Order.class).getAggregateIndex()
    ).isEqualTo(-1);
  }

  @Test
  void getDirectWithoutAttributeAliases() {
    MergedAnnotation<?> annotation = MergedAnnotations.from(WebController.class).get(Component.class);
    // adapt array to single value
    assertThat(annotation.getString("value")).isEqualTo("webController");
  }

  @Test
  void getDirectWithNestedAnnotations() {
    MergedAnnotation<?> annotation = MergedAnnotations.from(ComponentScanClass.class).get(ComponentScan.class);
    MergedAnnotation<Filter>[] filters = annotation.getAnnotationArray("excludeFilters", Filter.class);
    assertThat(Arrays.stream(filters).map(
            filter -> filter.getString("pattern"))).containsExactly("*Foo", "*Bar");
  }

  @Test
  void getDirectWithAttributeAliases1() throws Exception {
    Method method = WebController.class.getMethod("handleMappedWithValueAttribute");
    MergedAnnotation<?> annotation = MergedAnnotations.from(method).get(RequestMapping.class);
    assertThat(annotation.getString("name")).isEqualTo("foo");
    assertThat(annotation.getStringArray("value")).containsExactly("/test");
    assertThat(annotation.getStringArray("path")).containsExactly("/test");
  }

  @Test
  void getDirectWithAttributeAliases2() throws Exception {
    Method method = WebController.class.getMethod("handleMappedWithPathAttribute");
    MergedAnnotation<?> annotation = MergedAnnotations.from(method).get(RequestMapping.class);
    assertThat(annotation.getString("name")).isEqualTo("bar");
    assertThat(annotation.getStringArray("value")).containsExactly("/test");
    assertThat(annotation.getStringArray("path")).containsExactly("/test");
  }

  @Test
  void getDirectWithAttributeAliasesWithDifferentValues() throws Exception {
    Method method = WebController.class.getMethod("handleMappedWithDifferentPathAndValueAttributes");
    assertThatExceptionOfType(AnnotationConfigurationException.class).isThrownBy(() ->
                    MergedAnnotations.from(method).get(RequestMapping.class))
            .withMessageContaining("attribute 'path' and its alias 'value'")
            .withMessageContaining("values of [{/test}] and [{/enigma}]");
  }

  @Test
  void getValueFromAnnotation() throws Exception {
    Method method = TransactionalStringGeneric.class.getMethod("something", Object.class);
    MergedAnnotation<?> annotation =
            MergedAnnotations.from(method, SearchStrategy.TYPE_HIERARCHY).get(Order.class);
    assertThat(annotation.getInt("value")).isEqualTo(1);
  }

  @Test
  void getValueFromNonPublicAnnotation() throws Exception {
    Annotation[] declaredAnnotations = NonPublicAnnotatedClass.class.getDeclaredAnnotations();
    assertThat(declaredAnnotations).hasSize(1);
    Annotation annotation = declaredAnnotations[0];
    MergedAnnotation<Annotation> mergedAnnotation = MergedAnnotation.from(annotation);
    assertThat(mergedAnnotation.getType().getSimpleName()).isEqualTo("NonPublicAnnotation");
    assertThat(mergedAnnotation.synthesize().annotationType().getSimpleName()).isEqualTo("NonPublicAnnotation");
    assertThat(mergedAnnotation.getInt("value")).isEqualTo(42);
  }

  @Test
  void getDefaultValueFromAnnotation() throws Exception {
    Method method = TransactionalStringGeneric.class.getMethod("something", Object.class);
    MergedAnnotation<Order> annotation =
            MergedAnnotations.from(method, SearchStrategy.TYPE_HIERARCHY).get(Order.class);
    assertThat(annotation.getDefaultValue("value")).contains(Ordered.LOWEST_PRECEDENCE);
  }

  @Test
  void getDefaultValueFromNonPublicAnnotation() {
    Annotation[] declaredAnnotations = NonPublicAnnotatedClass.class.getDeclaredAnnotations();
    assertThat(declaredAnnotations).hasSize(1);
    Annotation declaredAnnotation = declaredAnnotations[0];
    MergedAnnotation<?> annotation = MergedAnnotation.from(declaredAnnotation);
    assertThat(annotation.getType().getName()).isEqualTo(
            "cn.taketoday.core.annotation.subpackage.NonPublicAnnotation");
    assertThat(annotation.getDefaultValue("value")).contains(-1);
  }

  @Test
  void getDefaultValueFromAnnotationType() {
    MergedAnnotation<?> annotation = MergedAnnotation.valueOf(Order.class);
    assertThat(annotation.getDefaultValue("value")).contains(Ordered.LOWEST_PRECEDENCE);
  }

  @Test
  void getRepeatableDeclaredOnMethod() throws Exception {
    Method method = InterfaceWithRepeated.class.getMethod("foo");
    Stream<MergedAnnotation<MyRepeatable>> annotations = MergedAnnotations.from(
            method, SearchStrategy.TYPE_HIERARCHY).stream(MyRepeatable.class);
    Stream<String> values = annotations.map(
            annotation -> annotation.getString("value"));
    assertThat(values).containsExactly("A", "B", "C", "meta1");
  }

  @Test
  @SuppressWarnings("deprecation")
  void getRepeatableDeclaredOnClassWithAttributeAliases() {
    assertThat(MergedAnnotations.from(HierarchyClass.class).stream(
            TestConfiguration.class)).isEmpty();
    RepeatableContainers containers = RepeatableContainers.valueOf(TestConfiguration.class,
            Hierarchy.class);
    MergedAnnotations annotations = MergedAnnotations.from(HierarchyClass.class,
            SearchStrategy.DIRECT, containers, AnnotationFilter.NONE);
    assertThat(annotations.stream(TestConfiguration.class).map(
            annotation -> annotation.getString("location"))).containsExactly("A", "B");
    assertThat(annotations.stream(TestConfiguration.class).map(
            MergedAnnotation::getStringValue)).containsExactly("A", "B");
  }

  @Test
  void getRepeatableDeclaredOnClass() {
    Class<?> element = MyRepeatableClass.class;
    String[] expectedValuesJava = { "A", "B", "C" };
    String[] expectedValuesSpring = { "A", "B", "C", "meta1" };
    testRepeatables(SearchStrategy.SUPERCLASS, element, expectedValuesJava, expectedValuesSpring);
  }

  @Test
  void getRepeatableDeclaredOnSuperclass() {
    Class<?> element = SubMyRepeatableClass.class;
    String[] expectedValuesJava = { "A", "B", "C" };
    String[] expectedValuesSpring = { "A", "B", "C", "meta1" };
    testRepeatables(SearchStrategy.SUPERCLASS, element, expectedValuesJava, expectedValuesSpring);
  }

  @Test
  void getRepeatableDeclaredOnClassAndSuperclass() {
    Class<?> element = SubMyRepeatableWithAdditionalLocalDeclarationsClass.class;
    String[] expectedValuesJava = { "X", "Y", "Z" };
    String[] expectedValuesSpring = { "X", "Y", "Z", "meta2" };
    testRepeatables(SearchStrategy.SUPERCLASS, element, expectedValuesJava, expectedValuesSpring);
  }

  @Test
  void getRepeatableDeclaredOnMultipleSuperclasses() {
    Class<?> element = SubSubMyRepeatableWithAdditionalLocalDeclarationsClass.class;
    String[] expectedValuesJava = { "X", "Y", "Z" };
    String[] expectedValuesSpring = { "X", "Y", "Z", "meta2" };
    testRepeatables(SearchStrategy.SUPERCLASS, element, expectedValuesJava, expectedValuesSpring);
  }

  @Test
  void getDirectRepeatablesDeclaredOnClass() {
    Class<?> element = MyRepeatableClass.class;
    String[] expectedValuesJava = { "A", "B", "C" };
    String[] expectedValuesSpring = { "A", "B", "C", "meta1" };
    testRepeatables(SearchStrategy.DIRECT, element, expectedValuesJava, expectedValuesSpring);
  }

  @Test
  void getDirectRepeatablesDeclaredOnSuperclass() {
    Class<?> element = SubMyRepeatableClass.class;
    String[] expectedValuesJava = {};
    String[] expectedValuesSpring = {};
    testRepeatables(SearchStrategy.DIRECT, element, expectedValuesJava, expectedValuesSpring);
  }

  private void testRepeatables(SearchStrategy searchStrategy, Class<?> element,
          String[] expectedValuesJava, String[] expectedValuesSpring) {

    testJavaRepeatables(searchStrategy, element, expectedValuesJava);
    testExplicitRepeatables(searchStrategy, element, expectedValuesSpring);
    testStandardRepeatables(searchStrategy, element, expectedValuesSpring);
  }

  private void testJavaRepeatables(SearchStrategy searchStrategy, Class<?> element, String[] expected) {
    MyRepeatable[] annotations = searchStrategy == SearchStrategy.DIRECT ?
                                 element.getDeclaredAnnotationsByType(MyRepeatable.class) :
                                 element.getAnnotationsByType(MyRepeatable.class);
    assertThat(Arrays.stream(annotations).map(MyRepeatable::value)).containsExactly(
            expected);
  }

  private void testExplicitRepeatables(SearchStrategy searchStrategy, Class<?> element, String[] expected) {
    MergedAnnotations annotations = MergedAnnotations.from(element, searchStrategy,
            RepeatableContainers.valueOf(MyRepeatable.class, MyRepeatableContainer.class),
            AnnotationFilter.PLAIN);
    assertThat(annotations.stream(MyRepeatable.class).filter(
            MergedAnnotationPredicates.firstRunOf(
                    MergedAnnotation::getAggregateIndex)).map(
            annotation -> annotation.getString(
                    "value"))).containsExactly(expected);
  }

  private void testStandardRepeatables(SearchStrategy searchStrategy, Class<?> element, String[] expected) {
    MergedAnnotations annotations = MergedAnnotations.from(element, searchStrategy);
    assertThat(annotations.stream(MyRepeatable.class).filter(
            MergedAnnotationPredicates.firstRunOf(
                    MergedAnnotation::getAggregateIndex)).map(
            annotation -> annotation.getString(
                    "value"))).containsExactly(expected);
  }

  @Test
  void synthesizeWithoutAttributeAliases() throws Exception {
    Component component = WebController.class.getAnnotation(Component.class);
    assertThat(component).isNotNull();
    Component synthesizedComponent = MergedAnnotation.from(component).synthesize();
    assertThat(synthesizedComponent).isNotNull();
    assertThat(synthesizedComponent).isEqualTo(component);
    assertThat(synthesizedComponent.value()).contains("webController");
  }

  @Indexed
  @Retention(RetentionPolicy.RUNTIME)
  @Target({ ElementType.TYPE, ElementType.METHOD })
  public @interface Component {
    String value() default "";
  }

  @Test
  void synthesizeAlreadySynthesized() throws Exception {
    Method method = WebController.class.getMethod("handleMappedWithValueAttribute");
    RequestMapping webMapping = method.getAnnotation(RequestMapping.class);
    assertThat(webMapping).isNotNull();

    RequestMapping synthesizedWebMapping = MergedAnnotation.from(webMapping).synthesize();
    RequestMapping synthesizedAgainWebMapping = MergedAnnotation.from(synthesizedWebMapping).synthesize();

    assertThat(synthesizedWebMapping).isInstanceOf(SynthesizedAnnotation.class);
    assertThat(synthesizedAgainWebMapping).isInstanceOf(SynthesizedAnnotation.class);
    assertThat(synthesizedWebMapping).isEqualTo(synthesizedAgainWebMapping);
    assertThat(synthesizedWebMapping.name()).isEqualTo("foo");
    assertThat(synthesizedWebMapping.path()).containsExactly("/test");
    assertThat(synthesizedWebMapping.value()).containsExactly("/test");
  }

  @Test
  void synthesizeShouldNotSynthesizeNonsynthesizableAnnotations() throws Exception {
    Method method = getClass().getDeclaredMethod("getId");

    Id id = method.getAnnotation(Id.class);
    assertThat(id).isNotNull();
    Id synthesizedId = MergedAnnotation.from(id).synthesize();
    assertThat(id).isEqualTo(synthesizedId);
    // It doesn't make sense to synthesize @Id since it declares zero attributes.
    assertThat(synthesizedId).isNotInstanceOf(SynthesizedAnnotation.class);
    assertThat(id).isSameAs(synthesizedId);

    GeneratedValue generatedValue = method.getAnnotation(GeneratedValue.class);
    assertThat(generatedValue).isNotNull();
    GeneratedValue synthesizedGeneratedValue = MergedAnnotation.from(generatedValue).synthesize();
    assertThat(generatedValue).isEqualTo(synthesizedGeneratedValue);
    // It doesn't make sense to synthesize @GeneratedValue since it declares zero attributes with aliases.
    assertThat(synthesizedGeneratedValue).isNotInstanceOf(SynthesizedAnnotation.class);
    assertThat(generatedValue).isSameAs(synthesizedGeneratedValue);
  }

  /**
   * If an attempt is made to synthesize an annotation from an annotation instance
   * that has already been synthesized, the original synthesized annotation should
   * ideally be returned as-is without creating a new proxy instance with the same
   * values.
   */
  @Test
  void synthesizeShouldNotResynthesizeAlreadySynthesizedAnnotations() throws Exception {
    Method method = WebController.class.getMethod("handleMappedWithValueAttribute");
    RequestMapping webMapping = method.getAnnotation(RequestMapping.class);
    assertThat(webMapping).isNotNull();

    MergedAnnotation<RequestMapping> mergedAnnotation1 = MergedAnnotation.from(webMapping);
    RequestMapping synthesizedWebMapping1 = mergedAnnotation1.synthesize();
    RequestMapping synthesizedWebMapping2 = MergedAnnotation.from(webMapping).synthesize();

    assertThat(synthesizedWebMapping1).isInstanceOf(SynthesizedAnnotation.class);
    assertThat(synthesizedWebMapping2).isInstanceOf(SynthesizedAnnotation.class);
    assertThat(synthesizedWebMapping1).isEqualTo(synthesizedWebMapping2);

    // Synthesizing an annotation from a different MergedAnnotation results in a different synthesized annotation instance.
    assertThat(synthesizedWebMapping1).isNotSameAs(synthesizedWebMapping2);
    // Synthesizing an annotation from the same MergedAnnotation results in the same synthesized annotation instance.
    assertThat(synthesizedWebMapping1).isSameAs(mergedAnnotation1.synthesize());

    RequestMapping synthesizedAgainWebMapping = MergedAnnotation.from(synthesizedWebMapping1).synthesize();
    assertThat(synthesizedWebMapping1).isEqualTo(synthesizedAgainWebMapping);
    // Synthesizing an already synthesized annotation results in the original synthesized annotation instance.
    assertThat(synthesizedWebMapping1).isSameAs(synthesizedAgainWebMapping);
  }

  @Test
  void synthesizeWhenAliasForIsMissingAttributeDeclaration() throws Exception {
    AliasForWithMissingAttributeDeclaration annotation =
            AliasForWithMissingAttributeDeclarationClass.class.getAnnotation(
                    AliasForWithMissingAttributeDeclaration.class);
    assertThatExceptionOfType(AnnotationConfigurationException.class).isThrownBy(() ->
                    MergedAnnotation.from(annotation))
            .withMessageStartingWith("@AliasFor declaration on attribute 'foo' in annotation")
            .withMessageContaining(AliasForWithMissingAttributeDeclaration.class.getName())
            .withMessageContaining("points to itself");
  }

  @Test
  void synthesizeWhenAliasForHasDuplicateAttributeDeclaration() throws Exception {
    AliasForWithDuplicateAttributeDeclaration annotation = AliasForWithDuplicateAttributeDeclarationClass.class.getAnnotation(
            AliasForWithDuplicateAttributeDeclaration.class);
    assertThatExceptionOfType(AnnotationConfigurationException.class).isThrownBy(() ->
                    MergedAnnotation.from(annotation))
            .withMessageStartingWith("In @AliasFor declared on attribute 'foo' in annotation")
            .withMessageContaining(AliasForWithDuplicateAttributeDeclaration.class.getName())
            .withMessageContaining("attribute 'attribute' and its alias 'value' are present with values of 'baz' and 'bar'");
  }

  @Test
  void synthesizeWhenAttributeAliasForNonexistentAttribute() throws Exception {
    AliasForNonexistentAttribute annotation = AliasForNonexistentAttributeClass.class.getAnnotation(
            AliasForNonexistentAttribute.class);
    assertThatExceptionOfType(AnnotationConfigurationException.class).isThrownBy(() ->
                    MergedAnnotation.from(annotation))
            .withMessageStartingWith("@AliasFor declaration on attribute 'foo' in annotation")
            .withMessageContaining(AliasForNonexistentAttribute.class.getName())
            .withMessageContaining("declares an alias for 'bar' which is not present");
  }

  @Test
  void synthesizeWhenAttributeAliasWithMirroredAliasForWrongAttribute() throws Exception {
    AliasForWithMirroredAliasForWrongAttribute annotation =
            AliasForWithMirroredAliasForWrongAttributeClass.class.getAnnotation(
                    AliasForWithMirroredAliasForWrongAttribute.class);
    assertThatExceptionOfType(AnnotationConfigurationException.class).isThrownBy(() ->
                    MergedAnnotation.from(annotation))
            .withMessage("@AliasFor declaration on attribute 'bar' in annotation ["
                    + AliasForWithMirroredAliasForWrongAttribute.class.getName()
                    + "] declares an alias for 'quux' which is not present.");
  }

  @Test
  void synthesizeWhenAttributeAliasForAttributeOfDifferentType() throws Exception {
    AliasForAttributeOfDifferentType annotation = AliasForAttributeOfDifferentTypeClass.class.getAnnotation(
            AliasForAttributeOfDifferentType.class);
    assertThatExceptionOfType(AnnotationConfigurationException.class).isThrownBy(() ->
                    MergedAnnotation.from(annotation))
            .withMessageStartingWith("Mis-configured aliases")
            .withMessageContaining(AliasForAttributeOfDifferentType.class.getName())
            .withMessageContaining("attribute 'foo'")
            .withMessageContaining("attribute 'bar'")
            .withMessageContaining("same return type");
  }

  @Test
  void synthesizeWhenAttributeAliasForWithMissingDefaultValues() throws Exception {
    AliasForWithMissingDefaultValues annotation = AliasForWithMissingDefaultValuesClass.class.getAnnotation(
            AliasForWithMissingDefaultValues.class);
    assertThatExceptionOfType(AnnotationConfigurationException.class).isThrownBy(() ->
                    MergedAnnotation.from(annotation))
            .withMessageStartingWith("Misconfigured aliases")
            .withMessageContaining(AliasForWithMissingDefaultValues.class.getName())
            .withMessageContaining("attribute 'foo' in annotation")
            .withMessageContaining("attribute 'bar' in annotation")
            .withMessageContaining("default values");
  }

  @Test
  void synthesizeWhenAttributeAliasForAttributeWithDifferentDefaultValue() throws Exception {
    AliasForAttributeWithDifferentDefaultValue annotation =
            AliasForAttributeWithDifferentDefaultValueClass.class.getAnnotation(
                    AliasForAttributeWithDifferentDefaultValue.class);
    assertThatExceptionOfType(AnnotationConfigurationException.class).isThrownBy(() ->
                    MergedAnnotation.from(annotation))
            .withMessageStartingWith("Misconfigured aliases")
            .withMessageContaining(AliasForAttributeWithDifferentDefaultValue.class.getName())
            .withMessageContaining("attribute 'foo' in annotation")
            .withMessageContaining("attribute 'bar' in annotation")
            .withMessageContaining("same default value");
  }

  @Test
  void synthesizeWhenAttributeAliasForMetaAnnotationThatIsNotMetaPresent() throws Exception {
    AliasedComposedTestConfigurationNotMetaPresent annotation =
            AliasedComposedTestConfigurationNotMetaPresentClass.class.getAnnotation(
                    AliasedComposedTestConfigurationNotMetaPresent.class);
    assertThatExceptionOfType(AnnotationConfigurationException.class).isThrownBy(() ->
                    MergedAnnotation.from(annotation))
            .withMessageStartingWith("@AliasFor declaration on attribute 'xmlConfigFile' in annotation")
            .withMessageContaining(AliasedComposedTestConfigurationNotMetaPresent.class.getName())
            .withMessageContaining("declares an alias for attribute 'location' in annotation")
            .withMessageContaining(TestConfiguration.class.getName())
            .withMessageContaining("not meta-present");
  }

  @Test
  void synthesizeWithImplicitAliases() throws Exception {
    testSynthesisWithImplicitAliases(ValueImplicitAliasesTestConfigurationClass.class, "value");
    testSynthesisWithImplicitAliases(Location1ImplicitAliasesTestConfigurationClass.class, "location1");
    testSynthesisWithImplicitAliases(XmlImplicitAliasesTestConfigurationClass.class, "xmlFile");
    testSynthesisWithImplicitAliases(GroovyImplicitAliasesSimpleTestConfigurationClass.class, "groovyScript");
  }

  private void testSynthesisWithImplicitAliases(Class<?> clazz, String expected)
          throws Exception {
    ImplicitAliasesTestConfiguration config = clazz.getAnnotation(
            ImplicitAliasesTestConfiguration.class);
    assertThat(config).isNotNull();
    ImplicitAliasesTestConfiguration synthesized = MergedAnnotation.from(
            config).synthesize();
    assertThat(synthesized).isInstanceOf(SynthesizedAnnotation.class);
    assertThat(synthesized.value()).isEqualTo(expected);
    assertThat(synthesized.location1()).isEqualTo(expected);
    assertThat(synthesized.xmlFile()).isEqualTo(expected);
    assertThat(synthesized.groovyScript()).isEqualTo(expected);
  }

  @Test
  void synthesizeWithImplicitAliasesWithImpliedAliasNamesOmitted()
          throws Exception {
    testSynthesisWithImplicitAliasesWithImpliedAliasNamesOmitted(
            ValueImplicitAliasesWithImpliedAliasNamesOmittedTestConfigurationClass.class,
            "value");
    testSynthesisWithImplicitAliasesWithImpliedAliasNamesOmitted(
            LocationsImplicitAliasesWithImpliedAliasNamesOmittedTestConfigurationClass.class,
            "location");
    testSynthesisWithImplicitAliasesWithImpliedAliasNamesOmitted(
            XmlFilesImplicitAliasesWithImpliedAliasNamesOmittedTestConfigurationClass.class,
            "xmlFile");
  }

  private void testSynthesisWithImplicitAliasesWithImpliedAliasNamesOmitted(
          Class<?> clazz, String expected) {
    ImplicitAliasesWithImpliedAliasNamesOmittedTestConfiguration config = clazz.getAnnotation(
            ImplicitAliasesWithImpliedAliasNamesOmittedTestConfiguration.class);
    assertThat(config).isNotNull();
    ImplicitAliasesWithImpliedAliasNamesOmittedTestConfiguration synthesized =
            MergedAnnotation.from(config).synthesize();
    assertThat(synthesized).isInstanceOf(SynthesizedAnnotation.class);
    assertThat(synthesized.value()).isEqualTo(expected);
    assertThat(synthesized.location()).isEqualTo(expected);
    assertThat(synthesized.xmlFile()).isEqualTo(expected);
  }

  @Test
  void synthesizeWithImplicitAliasesForAliasPair() throws Exception {
    ImplicitAliasesForAliasPairTestConfiguration config =
            ImplicitAliasesForAliasPairTestConfigurationClass.class.getAnnotation(
                    ImplicitAliasesForAliasPairTestConfiguration.class);
    ImplicitAliasesForAliasPairTestConfiguration synthesized = MergedAnnotation.from(config).synthesize();
    assertThat(synthesized).isInstanceOf(SynthesizedAnnotation.class);
    assertThat(synthesized.xmlFile()).isEqualTo("test.xml");
    assertThat(synthesized.groovyScript()).isEqualTo("test.xml");
  }

  @Test
  void synthesizeWithTransitiveImplicitAliases() throws Exception {
    TransitiveImplicitAliasesTestConfiguration config =
            TransitiveImplicitAliasesTestConfigurationClass.class.getAnnotation(
                    TransitiveImplicitAliasesTestConfiguration.class);
    TransitiveImplicitAliasesTestConfiguration synthesized = MergedAnnotation.from(config).synthesize();
    assertThat(synthesized).isInstanceOf(SynthesizedAnnotation.class);
    assertThat(synthesized.xml()).isEqualTo("test.xml");
    assertThat(synthesized.groovy()).isEqualTo("test.xml");
  }

  @Test
  void synthesizeWithTransitiveImplicitAliasesForAliasPair() throws Exception {
    TransitiveImplicitAliasesForAliasPairTestConfiguration config =
            TransitiveImplicitAliasesForAliasPairTestConfigurationClass.class.getAnnotation(
                    TransitiveImplicitAliasesForAliasPairTestConfiguration.class);
    TransitiveImplicitAliasesForAliasPairTestConfiguration synthesized = MergedAnnotation.from(
            config).synthesize();
    assertThat(synthesized).isInstanceOf(SynthesizedAnnotation.class);
    assertThat(synthesized.xml()).isEqualTo("test.xml");
    assertThat(synthesized.groovy()).isEqualTo("test.xml");
  }

  @Test
  void synthesizeWithImplicitAliasesWithMissingDefaultValues() throws Exception {
    Class<?> clazz = ImplicitAliasesWithMissingDefaultValuesTestConfigurationClass.class;
    Class<ImplicitAliasesWithMissingDefaultValuesTestConfiguration> annotationType =
            ImplicitAliasesWithMissingDefaultValuesTestConfiguration.class;
    ImplicitAliasesWithMissingDefaultValuesTestConfiguration config = clazz.getAnnotation(
            annotationType);
    assertThatExceptionOfType(AnnotationConfigurationException.class).isThrownBy(() ->
                    MergedAnnotation.from(clazz, config))
            .withMessageStartingWith("Misconfigured aliases:")
            .withMessageContaining("attribute 'location1' in annotation [" + annotationType.getName() + "]")
            .withMessageContaining("attribute 'location2' in annotation [" + annotationType.getName() + "]")
            .withMessageContaining("default values");
  }

  @Test
  void synthesizeWithImplicitAliasesWithDifferentDefaultValues()
          throws Exception {
    Class<?> clazz = ImplicitAliasesWithDifferentDefaultValuesTestConfigurationClass.class;
    Class<ImplicitAliasesWithDifferentDefaultValuesTestConfiguration> annotationType =
            ImplicitAliasesWithDifferentDefaultValuesTestConfiguration.class;
    ImplicitAliasesWithDifferentDefaultValuesTestConfiguration config = clazz.getAnnotation(
            annotationType);
    assertThatExceptionOfType(AnnotationConfigurationException.class).isThrownBy(() ->
                    MergedAnnotation.from(clazz, config))
            .withMessageStartingWith("Misconfigured aliases:")
            .withMessageContaining("attribute 'location1' in annotation [" + annotationType.getName() + "]")
            .withMessageContaining("attribute 'location2' in annotation [" + annotationType.getName() + "]")
            .withMessageContaining("same default value");
  }

  @Test
  void synthesizeWithImplicitAliasesWithDuplicateValues() throws Exception {
    Class<?> clazz = ImplicitAliasesWithDuplicateValuesTestConfigurationClass.class;
    Class<ImplicitAliasesWithDuplicateValuesTestConfiguration> annotationType =
            ImplicitAliasesWithDuplicateValuesTestConfiguration.class;
    ImplicitAliasesWithDuplicateValuesTestConfiguration config = clazz.getAnnotation(
            annotationType);
    assertThatExceptionOfType(AnnotationConfigurationException.class).isThrownBy(() ->
                    MergedAnnotation.from(clazz, config))
            .withMessageStartingWith("Different @AliasFor mirror values for annotation")
            .withMessageContaining(annotationType.getName())
            .withMessageContaining("declared on class")
            .withMessageContaining(clazz.getName())
            .withMessageContaining("are declared with values of");
  }

  @Test
  void synthesizeFromMapWithoutAttributeAliases() throws Exception {
    Component component = WebController.class.getAnnotation(Component.class);
    assertThat(component).isNotNull();
    Map<String, Object> map = Collections.singletonMap("value", "webController");
    MergedAnnotation<Component> annotation = MergedAnnotation.valueOf(Component.class, map);
    Component synthesizedComponent = annotation.synthesize();
    assertThat(synthesizedComponent).isInstanceOf(SynthesizedAnnotation.class);
    assertThat(synthesizedComponent.value()).contains("webController");
  }

  @Test
  @SuppressWarnings("unchecked")
  void synthesizeFromMapWithNestedMap() throws Exception {
    ComponentScanSingleFilter componentScan = ComponentScanSingleFilterClass.class.getAnnotation(
            ComponentScanSingleFilter.class);
    assertThat(componentScan).isNotNull();
    assertThat(componentScan.value().pattern()).isEqualTo("*Foo");
    Map<String, Object> map = MergedAnnotation.from(componentScan).asMap(
            annotation -> new LinkedHashMap<String, Object>(),
            Adapt.ANNOTATION_TO_MAP);
    Map<String, Object> filterMap = (Map<String, Object>) map.get("value");
    assertThat(filterMap.get("pattern")).isEqualTo("*Foo");
    filterMap.put("pattern", "newFoo");
    filterMap.put("enigma", 42);
    MergedAnnotation<ComponentScanSingleFilter> annotation = MergedAnnotation.valueOf(
            ComponentScanSingleFilter.class, map);
    ComponentScanSingleFilter synthesizedComponentScan = annotation.synthesize();
    assertThat(synthesizedComponentScan).isInstanceOf(SynthesizedAnnotation.class);
    assertThat(synthesizedComponentScan.value().pattern()).isEqualTo("newFoo");
  }

  @Test
  @SuppressWarnings("unchecked")
  void synthesizeFromMapWithNestedArrayOfMaps() throws Exception {
    ComponentScan componentScan = ComponentScanClass.class.getAnnotation(
            ComponentScan.class);
    assertThat(componentScan).isNotNull();
    Map<String, Object> map = MergedAnnotation.from(componentScan).asMap(
            annotation -> new LinkedHashMap<String, Object>(),
            Adapt.ANNOTATION_TO_MAP);
    Map<String, Object>[] filters = (Map[]) map.get("excludeFilters");
    List<String> patterns = Arrays.stream(filters).map(
            m -> (String) m.get("pattern")).collect(Collectors.toList());
    assertThat(patterns).containsExactly("*Foo", "*Bar");
    filters[0].put("pattern", "newFoo");
    filters[0].put("enigma", 42);
    filters[1].put("pattern", "newBar");
    filters[1].put("enigma", 42);
    MergedAnnotation<ComponentScan> annotation = MergedAnnotation.valueOf(
            ComponentScan.class, map);
    ComponentScan synthesizedComponentScan = annotation.synthesize();
    assertThat(synthesizedComponentScan).isInstanceOf(SynthesizedAnnotation.class);
    assertThat(Arrays.stream(synthesizedComponentScan.excludeFilters()).map(
            Filter::pattern)).containsExactly("newFoo", "newBar");
  }

  @Test
  void synthesizeFromDefaultsWithoutAttributeAliases() throws Exception {
    MergedAnnotation<AnnotationWithDefaults> annotation = MergedAnnotation.valueOf(
            AnnotationWithDefaults.class);
    AnnotationWithDefaults synthesized = annotation.synthesize();
    assertThat(synthesized.text()).isEqualTo("enigma");
    assertThat(synthesized.predicate()).isTrue();
    assertThat(synthesized.characters()).containsExactly('a', 'b', 'c');
  }

  @Test
  void synthesizeFromDefaultsWithAttributeAliases() throws Exception {
    MergedAnnotation<TestConfiguration> annotation = MergedAnnotation.valueOf(
            TestConfiguration.class);
    TestConfiguration synthesized = annotation.synthesize();
    assertThat(synthesized.value()).isEqualTo("");
    assertThat(synthesized.location()).isEqualTo("");
  }

  @Test
  void synthesizeWhenAttributeAliasesWithDifferentValues() throws Exception {
    assertThatExceptionOfType(AnnotationConfigurationException.class).isThrownBy(() ->
            MergedAnnotation.from(
                            TestConfigurationMismatch.class.getAnnotation(TestConfiguration.class))
                    .synthesize());
  }

  @Test
  void synthesizeFromMapWithMinimalAttributesWithAttributeAliases()
          throws Exception {
    Map<String, Object> map = Collections.singletonMap("location", "test.xml");
    MergedAnnotation<TestConfiguration> annotation = MergedAnnotation.valueOf(
            TestConfiguration.class, map);
    TestConfiguration synthesized = annotation.synthesize();
    assertThat(synthesized.value()).isEqualTo("test.xml");
    assertThat(synthesized.location()).isEqualTo("test.xml");
  }

  @Test
  void synthesizeFromMapWithAttributeAliasesThatOverrideArraysWithSingleElements()
          throws Exception {
    synthesizeFromMapWithAttributeAliasesThatOverrideArraysWithSingleElements(
            Collections.singletonMap("value", "/foo"));
    synthesizeFromMapWithAttributeAliasesThatOverrideArraysWithSingleElements(
            Collections.singletonMap("path", "/foo"));
  }

  private void synthesizeFromMapWithAttributeAliasesThatOverrideArraysWithSingleElements(
          Map<String, Object> map) {
    MergedAnnotation<GetMapping> annotation = MergedAnnotation.valueOf(GetMapping.class,
            map);
    GetMapping synthesized = annotation.synthesize();
    assertThat(synthesized.value()).isEqualTo("/foo");
    assertThat(synthesized.path()).isEqualTo("/foo");
  }

  @Test
  void synthesizeFromMapWithImplicitAttributeAliases() throws Exception {
    testSynthesisFromMapWithImplicitAliases("value");
    testSynthesisFromMapWithImplicitAliases("location1");
    testSynthesisFromMapWithImplicitAliases("location2");
    testSynthesisFromMapWithImplicitAliases("location3");
    testSynthesisFromMapWithImplicitAliases("xmlFile");
    testSynthesisFromMapWithImplicitAliases("groovyScript");
  }

  private void testSynthesisFromMapWithImplicitAliases(String attributeNameAndValue)
          throws Exception {
    Map<String, Object> map = Collections.singletonMap(attributeNameAndValue,
            attributeNameAndValue);
    MergedAnnotation<ImplicitAliasesTestConfiguration> annotation = MergedAnnotation.valueOf(
            ImplicitAliasesTestConfiguration.class, map);
    ImplicitAliasesTestConfiguration synthesized = annotation.synthesize();
    assertThat(synthesized.value()).isEqualTo(attributeNameAndValue);
    assertThat(synthesized.location1()).isEqualTo(attributeNameAndValue);
    assertThat(synthesized.location2()).isEqualTo(attributeNameAndValue);
    assertThat(synthesized.location2()).isEqualTo(attributeNameAndValue);
    assertThat(synthesized.xmlFile()).isEqualTo(attributeNameAndValue);
    assertThat(synthesized.groovyScript()).isEqualTo(attributeNameAndValue);
  }

  @Test
  void synthesizeFromMapWithMissingAttributeValue() throws Exception {
    testMissingTextAttribute(Collections.emptyMap());
  }

  @Test
  void synthesizeFromMapWithNullAttributeValue() throws Exception {
    Map<String, Object> map = Collections.singletonMap("text", null);
    assertThat(map).containsKey("text");
    testMissingTextAttribute(map);
  }

  private void testMissingTextAttribute(Map<String, Object> attributes) {
    assertThatExceptionOfType(NoSuchElementException.class)
            .isThrownBy(() -> MergedAnnotation.valueOf(AnnotationWithoutDefaults.class, attributes).synthesize().text())
            .withMessage("No value found for attribute named 'text' in merged annotation " +
                    AnnotationWithoutDefaults.class.getName());
  }

  @Test
  void synthesizeFromMapWithAttributeOfIncorrectType() throws Exception {
    Map<String, Object> map = Collections.singletonMap("value", 42L);
    MergedAnnotation<Component> annotation = MergedAnnotation.valueOf(Component.class, map);
    assertThatIllegalStateException()
            .isThrownBy(() -> annotation.synthesize().value())
            .withMessage("Attribute 'value' in annotation " +
                    "cn.taketoday.core.annotation.MergedAnnotationsTests$Component should be " +
                    "compatible with java.lang.String but a java.lang.Long value was returned");
  }

  @Test
  void synthesizeFromAnnotationAttributesWithoutAttributeAliases() throws Exception {
    Component component = WebController.class.getAnnotation(Component.class);
    assertThat(component).isNotNull();
    Map<String, Object> attributes = MergedAnnotation.from(component).asMap();
    Component synthesized = MergedAnnotation.valueOf(Component.class, attributes).synthesize();
    assertThat(synthesized).isInstanceOf(SynthesizedAnnotation.class);
    assertThat(synthesized).isEqualTo(component);
  }

  @Test
  void toStringForSynthesizedAnnotations() throws Exception {
    Method methodWithPath = WebController.class.getMethod("handleMappedWithPathAttribute");
    RequestMapping webMappingWithAliases = methodWithPath.getAnnotation(RequestMapping.class);
    assertThat(webMappingWithAliases).isNotNull();
    Method methodWithPathAndValue = WebController.class.getMethod("handleMappedWithSamePathAndValueAttributes");
    RequestMapping webMappingWithPathAndValue = methodWithPathAndValue.getAnnotation(RequestMapping.class);
    assertThat(methodWithPathAndValue).isNotNull();
    RequestMapping synthesizedWebMapping1 = MergedAnnotation.from(webMappingWithAliases).synthesize();
    RequestMapping synthesizedWebMapping2 = MergedAnnotation.from(webMappingWithPathAndValue).synthesize();
    assertThat(webMappingWithAliases.toString()).isNotEqualTo(synthesizedWebMapping1.toString());
    assertToStringForWebMappingWithPathAndValue(synthesizedWebMapping1);
    assertToStringForWebMappingWithPathAndValue(synthesizedWebMapping2);
  }

  private void assertToStringForWebMappingWithPathAndValue(RequestMapping webMapping) {
    String prefix = "@" + RequestMapping.class.getName() + "(";
    assertThat(webMapping.toString()).startsWith(prefix).contains("value=[/test]",
            "path=[/test]", "name=bar", "method=", "[GET, POST]").endsWith(")");
  }

  @Test
  void equalsForSynthesizedAnnotations() throws Exception {
    Method methodWithPath = WebController.class.getMethod(
            "handleMappedWithPathAttribute");
    RequestMapping webMappingWithAliases = methodWithPath.getAnnotation(
            RequestMapping.class);
    assertThat(webMappingWithAliases).isNotNull();
    Method methodWithPathAndValue = WebController.class.getMethod(
            "handleMappedWithSamePathAndValueAttributes");
    RequestMapping webMappingWithPathAndValue = methodWithPathAndValue.getAnnotation(
            RequestMapping.class);
    assertThat(webMappingWithPathAndValue).isNotNull();
    RequestMapping synthesizedWebMapping1 = MergedAnnotation.from(
            webMappingWithAliases).synthesize();
    RequestMapping synthesizedWebMapping2 = MergedAnnotation.from(
            webMappingWithPathAndValue).synthesize();
    // Equality amongst standard annotations
    assertThat(webMappingWithAliases).isEqualTo(webMappingWithAliases);
    assertThat(webMappingWithPathAndValue).isEqualTo(webMappingWithPathAndValue);
    // Inequality amongst standard annotations
    assertThat(webMappingWithAliases).isNotEqualTo(webMappingWithPathAndValue);
    assertThat(webMappingWithPathAndValue).isNotEqualTo(webMappingWithAliases);
    // Equality amongst synthesized annotations
    assertThat(synthesizedWebMapping1).isEqualTo(synthesizedWebMapping1);
    assertThat(synthesizedWebMapping2).isEqualTo(synthesizedWebMapping2);
    assertThat(synthesizedWebMapping1).isEqualTo(synthesizedWebMapping2);
    assertThat(synthesizedWebMapping2).isEqualTo(synthesizedWebMapping1);
    // Equality between standard and synthesized annotations
    assertThat(synthesizedWebMapping1).isEqualTo(webMappingWithPathAndValue);
    assertThat(webMappingWithPathAndValue).isEqualTo(synthesizedWebMapping1);
    // Inequality between standard and synthesized annotations
    assertThat(synthesizedWebMapping1).isNotEqualTo(webMappingWithAliases);
    assertThat(webMappingWithAliases).isNotEqualTo(synthesizedWebMapping1);
  }

  @Test
  void hashCodeForSynthesizedAnnotations() throws Exception {
    Method methodWithPath = WebController.class.getMethod(
            "handleMappedWithPathAttribute");
    RequestMapping webMappingWithAliases = methodWithPath.getAnnotation(
            RequestMapping.class);
    assertThat(webMappingWithAliases).isNotNull();
    Method methodWithPathAndValue = WebController.class.getMethod(
            "handleMappedWithSamePathAndValueAttributes");
    RequestMapping webMappingWithPathAndValue = methodWithPathAndValue.getAnnotation(
            RequestMapping.class);
    assertThat(webMappingWithPathAndValue).isNotNull();
    RequestMapping synthesizedWebMapping1 = MergedAnnotation.from(
            webMappingWithAliases).synthesize();
    assertThat(synthesizedWebMapping1).isNotNull();
    RequestMapping synthesizedWebMapping2 = MergedAnnotation.from(
            webMappingWithPathAndValue).synthesize();
    assertThat(synthesizedWebMapping2).isNotNull();
    // Equality amongst standard annotations
    assertThat(webMappingWithAliases.hashCode()).isEqualTo(
            webMappingWithAliases.hashCode());
    assertThat(webMappingWithPathAndValue.hashCode()).isEqualTo(
            webMappingWithPathAndValue.hashCode());
    // Inequality amongst standard annotations
    assertThat(webMappingWithAliases.hashCode()).isNotEqualTo(
            webMappingWithPathAndValue.hashCode());
    assertThat(webMappingWithPathAndValue.hashCode()).isNotEqualTo(
            webMappingWithAliases.hashCode());
    // Equality amongst synthesized annotations
    assertThat(synthesizedWebMapping1.hashCode()).isEqualTo(
            synthesizedWebMapping1.hashCode());
    assertThat(synthesizedWebMapping2.hashCode()).isEqualTo(
            synthesizedWebMapping2.hashCode());
    assertThat(synthesizedWebMapping1.hashCode()).isEqualTo(
            synthesizedWebMapping2.hashCode());
    assertThat(synthesizedWebMapping2.hashCode()).isEqualTo(
            synthesizedWebMapping1.hashCode());
    // Equality between standard and synthesized annotations
    assertThat(synthesizedWebMapping1.hashCode()).isEqualTo(
            webMappingWithPathAndValue.hashCode());
    assertThat(webMappingWithPathAndValue.hashCode()).isEqualTo(
            synthesizedWebMapping1.hashCode());
    // Inequality between standard and synthesized annotations
    assertThat(synthesizedWebMapping1.hashCode()).isNotEqualTo(
            webMappingWithAliases.hashCode());
    assertThat(webMappingWithAliases.hashCode()).isNotEqualTo(
            synthesizedWebMapping1.hashCode());
  }

  /**
   * Fully reflection-based test that verifies support for synthesizing
   * annotations across packages with non-public visibility of user types
   * (e.g., a non-public annotation that uses {@code @AliasFor}).
   */
  @Test
  @SuppressWarnings("unchecked")
  void synthesizeNonPublicWithAttributeAliasesFromDifferentPackage() throws Exception {
    Class<?> type = ClassUtils.forName(
            "cn.taketoday.core.annotation.subpackage.NonPublicAliasedAnnotatedClass",
            null);
    Class<? extends Annotation> annotationType = ClassUtils.forName(
            "cn.taketoday.core.annotation.subpackage.NonPublicAliasedAnnotation",
            null);
    Annotation annotation = type.getAnnotation(annotationType);
    assertThat(annotation).isNotNull();
    MergedAnnotation<Annotation> mergedAnnotation = MergedAnnotation.from(annotation);
    Annotation synthesizedAnnotation = mergedAnnotation.synthesize();
    assertThat(synthesizedAnnotation).isInstanceOf(SynthesizedAnnotation.class);
    assertThat(mergedAnnotation.getString("name")).isEqualTo("test");
    assertThat(mergedAnnotation.getString("path")).isEqualTo("/test");
    assertThat(mergedAnnotation.getString("value")).isEqualTo("/test");
  }

  @Test
  void synthesizeWithArrayOfAnnotations() throws Exception {
    Hierarchy hierarchy = HierarchyClass.class.getAnnotation(Hierarchy.class);
    assertThat(hierarchy).isNotNull();
    Hierarchy synthesizedHierarchy = MergedAnnotation.from(hierarchy).synthesize();
    assertThat(synthesizedHierarchy).isInstanceOf(SynthesizedAnnotation.class);
    TestConfiguration[] configs = synthesizedHierarchy.value();
    assertThat(configs).isNotNull();
    assertThat(configs).allMatch(SynthesizedAnnotation.class::isInstance);
    assertThat(configs).extracting(TestConfiguration::value).containsExactly("A", "B");
    assertThat(configs).extracting(TestConfiguration::location).containsExactly("A", "B");

    TestConfiguration contextConfig = TestConfigurationClass.class.getAnnotation(TestConfiguration.class);
    assertThat(contextConfig).isNotNull();
    // Alter array returned from synthesized annotation
    configs[0] = contextConfig;
    assertThat(configs).extracting(TestConfiguration::value).containsExactly("simple.xml", "B");
    // Re-retrieve the array from the synthesized annotation
    configs = synthesizedHierarchy.value();
    assertThat(configs).extracting(TestConfiguration::value).containsExactly("A", "B");
  }

  @Test
  void synthesizeWithArrayOfChars() throws Exception {
    CharsContainer charsContainer = GroupOfCharsClass.class.getAnnotation(
            CharsContainer.class);
    assertThat(charsContainer).isNotNull();
    CharsContainer synthesizedCharsContainer = MergedAnnotation.from(
            charsContainer).synthesize();
    assertThat(synthesizedCharsContainer).isInstanceOf(SynthesizedAnnotation.class);
    char[] chars = synthesizedCharsContainer.chars();
    assertThat(chars).containsExactly('x', 'y', 'z');
    // Alter array returned from synthesized annotation
    chars[0] = '?';
    // Re-retrieve the array from the synthesized annotation
    chars = synthesizedCharsContainer.chars();
    assertThat(chars).containsExactly('x', 'y', 'z');
  }

  @Test
  void getValueWhenHasDefaultOverride() {
    MergedAnnotation<?> annotation = MergedAnnotations.from(
            DefaultOverrideClass.class).get(DefaultOverrideRoot.class);
    assertThat(annotation.getString("text")).isEqualTo("metameta");
  }

  @Test
    // gh-22654
  void getValueWhenHasDefaultOverrideWithImplicitAlias() {
    MergedAnnotation<?> annotation1 = MergedAnnotations.from(
            DefaultOverrideImplicitAliasMetaClass1.class).get(DefaultOverrideRoot.class);
    assertThat(annotation1.getString("text")).isEqualTo("alias-meta-1");
    MergedAnnotation<?> annotation2 = MergedAnnotations.from(
            DefaultOverrideImplicitAliasMetaClass2.class).get(DefaultOverrideRoot.class);
    assertThat(annotation2.getString("text")).isEqualTo("alias-meta-2");
  }

  @Test
    // gh-22654
  void getValueWhenHasDefaultOverrideWithExplicitAlias() {
    MergedAnnotation<?> annotation = MergedAnnotations.from(
            DefaultOverrideExplicitAliasRootMetaMetaClass.class).get(
            DefaultOverrideExplicitAliasRoot.class);
    assertThat(annotation.getString("text")).isEqualTo("meta");
    assertThat(annotation.getString("value")).isEqualTo("meta");
  }

  @Test
    // gh-22703
  void getValueWhenThreeDeepMetaWithValue() {
    MergedAnnotation<?> annotation = MergedAnnotations.from(
            ValueAttributeMetaMetaClass.class).get(ValueAttribute.class);
    assertThat(annotation.getStringArray(MergedAnnotation.VALUE)).containsExactly(
            "FromValueAttributeMeta");
  }

  @Test
  void asAnnotationAttributesReturnsPopulatedAnnotationAttributes() {
    MergedAnnotation<?> annotation = MergedAnnotations.from(
            SpringApplicationConfigurationClass.class).get(
            SpringApplicationConfiguration.class);
    AnnotationAttributes attributes = annotation.asAnnotationAttributes(
            Adapt.CLASS_TO_STRING);
    assertThat(attributes).containsEntry("classes", new String[] { Number.class.getName() });
    assertThat(attributes.annotationType()).isEqualTo(SpringApplicationConfiguration.class);
  }

  // @formatter:off

  @Retention(RetentionPolicy.RUNTIME)
  @Target({ ElementType.TYPE, ElementType.METHOD })
  @Inherited
  @interface Transactional {

    String value() default "";

    String qualifier() default "transactionManager";

    boolean readOnly() default false;
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

  static class NonAnnotatedClass {
  }

  @Component
  static class AnnotatedClass {

    class NonAnnotatedInnerClass {
    }

    static class NonAnnotatedStaticNestedClass {
    }
  }

  static interface NonAnnotatedInterface {
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

  @Retention(RetentionPolicy.RUNTIME)
  @Target({ ElementType.TYPE, ElementType.METHOD })
  @Inherited
  @interface AliasedTransactional {

    @AliasFor(attribute = "qualifier")
    String value() default "";

    @AliasFor(attribute = "value")
    String qualifier() default "";
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

  @AliasedTransactional(value = "aliasForQualifier")
  @Component
  @Retention(RetentionPolicy.RUNTIME)
  @interface AliasedTransactionalComponent {
  }

  @AliasedTransactional
  @Retention(RetentionPolicy.RUNTIME)
  @interface MyAliasedTransactional {

    @AliasFor(annotation = AliasedTransactional.class, attribute = "value")
    String value() default "defaultTransactionManager";
  }

  @MyAliasedTransactional("anotherTransactionManager")
  @Retention(RetentionPolicy.RUNTIME)
  @Target({ ElementType.TYPE, ElementType.METHOD })
  @interface ComposedMyAliasedTransactional {
  }

  @ComposedMyAliasedTransactional
  static class ComposedTransactionalClass {
  }

  @AliasedTransactional("meta")
  @Retention(RetentionPolicy.RUNTIME)
  @interface MetaAliasedTransactional {
  }

  @MetaAliasedTransactional
  @Retention(RetentionPolicy.RUNTIME)
  @interface MetaMetaAliasedTransactional {
  }

  @MetaMetaAliasedTransactional
  static class MetaMetaAliasedTransactionalClass {
  }

  @TxComposedWithOverride
  // Override default "txMgr" from @TxComposedWithOverride with "localTxMgr"
  @Transactional(qualifier = "localTxMgr")
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.TYPE)
  @interface MetaAndLocalTxConfig {
  }

  @Retention(RetentionPolicy.RUNTIME)
  @interface TestPropertySource {

    @AliasFor("locations")
    String[] value() default { };

    @AliasFor("value")
    String[] locations() default { };
  }

  @Retention(RetentionPolicy.RUNTIME)
  @interface ContextConfiguration {

    @AliasFor(attribute = "locations")
    String[] value() default { };

    @AliasFor(attribute = "value")
    String[] locations() default { };

    Class<?>[] classes() default { };
  }

  @ContextConfiguration
  @Retention(RetentionPolicy.RUNTIME)
  @interface ConventionBasedComposedContextConfiguration {

    String[] locations() default { };
  }

  @ContextConfiguration(value = "duplicateDeclaration")
  @Retention(RetentionPolicy.RUNTIME)
  @interface InvalidConventionBasedComposedContextConfiguration {

    String[] locations();
  }

  /**
   * This hybrid approach for annotation attribute overrides with transitive implicit
   * aliases is unsupported. See SPR-13554 for details.
   */
  @ContextConfiguration
  @Retention(RetentionPolicy.RUNTIME)
  @interface HalfConventionBasedAndHalfAliasedComposedContextConfiguration {

    String[] locations() default { };

    @AliasFor(annotation = ContextConfiguration.class, attribute = "locations")
    String[] xmlConfigFiles() default { };
  }

  @ContextConfiguration
  @Retention(RetentionPolicy.RUNTIME)
  @interface AliasedComposedContextConfiguration {

    @AliasFor(annotation = ContextConfiguration.class, attribute = "locations")
    String[] xmlConfigFiles();
  }

  @ContextConfiguration
  @Retention(RetentionPolicy.RUNTIME)
  @interface AliasedValueComposedContextConfiguration {

    @AliasFor(annotation = ContextConfiguration.class, attribute = "value")
    String[] locations();
  }

  @ContextConfiguration
  @Retention(RetentionPolicy.RUNTIME)
  @interface ImplicitAliasesContextConfiguration {

    @AliasFor(annotation = ContextConfiguration.class, attribute = "locations")
    String[] groovyScripts() default { };

    @AliasFor(annotation = ContextConfiguration.class, attribute = "locations")
    String[] xmlFiles() default { };

    // intentionally omitted: attribute = "locations"
    @AliasFor(annotation = ContextConfiguration.class)
    String[] locations() default { };

    // intentionally omitted: attribute = "locations" (SPR-14069)
    @AliasFor(annotation = ContextConfiguration.class)
    String[] value() default { };
  }

  @ImplicitAliasesContextConfiguration(xmlFiles = { "A.xml", "B.xml" })
  @Retention(RetentionPolicy.RUNTIME)
  @interface ComposedImplicitAliasesContextConfiguration {
  }

  @ImplicitAliasesContextConfiguration
  @Retention(RetentionPolicy.RUNTIME)
  @interface TransitiveImplicitAliasesContextConfiguration {

    @AliasFor(annotation = ImplicitAliasesContextConfiguration.class, attribute = "xmlFiles")
    String[] xml() default { };

    @AliasFor(annotation = ImplicitAliasesContextConfiguration.class, attribute = "groovyScripts")
    String[] groovy() default { };
  }

  @ImplicitAliasesContextConfiguration
  @Retention(RetentionPolicy.RUNTIME)
  @interface SingleLocationTransitiveImplicitAliasesContextConfiguration {

    @AliasFor(annotation = ImplicitAliasesContextConfiguration.class, attribute = "xmlFiles")
    String xml() default "";

    @AliasFor(annotation = ImplicitAliasesContextConfiguration.class, attribute = "groovyScripts")
    String groovy() default "";
  }

  @ImplicitAliasesContextConfiguration
  @Retention(RetentionPolicy.RUNTIME)
  @interface TransitiveImplicitAliasesWithSkippedLevelContextConfiguration {

    @AliasFor(annotation = ContextConfiguration.class, attribute = "locations")
    String[] xml() default { };

    @AliasFor(annotation = ImplicitAliasesContextConfiguration.class, attribute = "groovyScripts")
    String[] groovy() default { };
  }

  @ImplicitAliasesContextConfiguration
  @Retention(RetentionPolicy.RUNTIME)
  @interface SingleLocationTransitiveImplicitAliasesWithSkippedLevelContextConfiguration {

    @AliasFor(annotation = ContextConfiguration.class, attribute = "locations")
    String xml() default "";

    @AliasFor(annotation = ImplicitAliasesContextConfiguration.class, attribute = "groovyScripts")
    String groovy() default "";
  }

  /**
   * Although the configuration declares an explicit value for 'value' and requires a
   * value for the aliased 'locations', this does not result in an error since
   * 'locations' effectively shadows the 'value' attribute (which cannot be set via the
   * composed annotation anyway). If 'value' were not shadowed, such a declaration would
   * not make sense.
   */
  @ContextConfiguration(value = "duplicateDeclaration")
  @Retention(RetentionPolicy.RUNTIME)
  @interface ShadowedAliasComposedContextConfiguration {

    @AliasFor(annotation = ContextConfiguration.class, attribute = "locations")
    String[] xmlConfigFiles();
  }

  @ContextConfiguration(locations = "shadowed.xml")
  @TestPropertySource(locations = "test.properties")
  @Retention(RetentionPolicy.RUNTIME)
  @interface AliasedComposedContextConfigurationAndTestPropertySource {

    @AliasFor(annotation = ContextConfiguration.class, attribute = "locations")
    String[] xmlConfigFiles() default "default.xml";
  }

  @ContextConfiguration
  @Retention(RetentionPolicy.RUNTIME)
  @interface SpringApplicationConfiguration {

    @AliasFor(annotation = ContextConfiguration.class, attribute = "locations")
    String[] locations() default { };

    @AliasFor("value")
    Class<?>[] classes() default { };

    @AliasFor("classes")
    Class<?>[] value() default { };
  }

  @Retention(RetentionPolicy.RUNTIME)
  @interface ComponentScan {

    @AliasFor("basePackages")
    String[] value() default { };

    @AliasFor("value")
    String[] basePackages() default { };

    Filter[] excludeFilters() default { };
  }

  @Retention(RetentionPolicy.RUNTIME)
  @Target({ })
  @interface Filter {

    String pattern();
  }

  @ComponentScan(excludeFilters = { @Filter(pattern = "*Test"),
          @Filter(pattern = "*Tests") })
  @Retention(RetentionPolicy.RUNTIME)
  @interface TestComponentScan {

    @AliasFor(attribute = "basePackages", annotation = ComponentScan.class)
    String[] packages();
  }

  @ComponentScan
  @Retention(RetentionPolicy.RUNTIME)
  @interface ConventionBasedSinglePackageComponentScan {

    String basePackages();
  }

  @ComponentScan
  @Retention(RetentionPolicy.RUNTIME)
  @interface AliasForBasedSinglePackageComponentScan {

    @AliasFor(attribute = "basePackages", annotation = ComponentScan.class)
    String pkg();
  }

  @Transactional
  static class ClassWithInheritedAnnotation {
  }

  @Composed
  static class SubClassWithInheritedAnnotation extends ClassWithInheritedAnnotation {
  }

  static class SubSubClassWithInheritedAnnotation
          extends SubClassWithInheritedAnnotation {
  }

  @InheritedComposed
  static class ClassWithInheritedComposedAnnotation {
  }

  @Composed
  static class SubClassWithInheritedComposedAnnotation
          extends ClassWithInheritedComposedAnnotation {
  }

  static class SubSubClassWithInheritedComposedAnnotation
          extends SubClassWithInheritedComposedAnnotation {
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
  static interface InterfaceWithInheritedAnnotation {

    @Order
    void handleFromInterface();
  }

  static abstract class AbstractClassWithInheritedAnnotation<T>
          implements InterfaceWithInheritedAnnotation {

    @Transactional
    public abstract void handle();

    @Transactional
    public void handleParameterized(T t) {
    }
  }

  static class ConcreteClassWithInheritedAnnotation
          extends AbstractClassWithInheritedAnnotation<String> {

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

  public interface SubInheritedAnnotationInterface
          extends InheritedAnnotationInterface {
  }

  public interface SubSubInheritedAnnotationInterface
          extends SubInheritedAnnotationInterface {
  }

  @Order
  public interface NonInheritedAnnotationInterface {
  }

  public interface SubNonInheritedAnnotationInterface
          extends NonInheritedAnnotationInterface {
  }

  public interface SubSubNonInheritedAnnotationInterface
          extends SubNonInheritedAnnotationInterface {
  }

  @ConventionBasedComposedContextConfiguration(locations = "explicitDeclaration")
  static class ConventionBasedComposedContextConfigurationClass {
  }

  @InvalidConventionBasedComposedContextConfiguration(locations = "requiredLocationsDeclaration")
  static class InvalidConventionBasedComposedContextConfigurationClass {
  }

  @HalfConventionBasedAndHalfAliasedComposedContextConfiguration(xmlConfigFiles = "explicitDeclaration")
  static class HalfConventionBasedAndHalfAliasedComposedContextConfigurationClass1 {
  }

  @HalfConventionBasedAndHalfAliasedComposedContextConfiguration(locations = "explicitDeclaration")
  static class HalfConventionBasedAndHalfAliasedComposedContextConfigurationClass2 {
  }

  @AliasedComposedContextConfiguration(xmlConfigFiles = "test.xml")
  static class AliasedComposedContextConfigurationClass {
  }

  @AliasedValueComposedContextConfiguration(locations = "test.xml")
  static class AliasedValueComposedContextConfigurationClass {
  }

  @ImplicitAliasesContextConfiguration("foo.xml")
  static class ImplicitAliasesContextConfigurationClass1 {
  }

  @ImplicitAliasesContextConfiguration(locations = "bar.xml")
  static class ImplicitAliasesContextConfigurationClass2 {
  }

  @ImplicitAliasesContextConfiguration(xmlFiles = "baz.xml")
  static class ImplicitAliasesContextConfigurationClass3 {
  }

  @TransitiveImplicitAliasesContextConfiguration(groovy = "test.groovy")
  static class TransitiveImplicitAliasesContextConfigurationClass {
  }

  @SingleLocationTransitiveImplicitAliasesContextConfiguration(groovy = "test.groovy")
  static class SingleLocationTransitiveImplicitAliasesContextConfigurationClass {
  }

  @TransitiveImplicitAliasesWithSkippedLevelContextConfiguration(xml = "test.xml")
  static class TransitiveImplicitAliasesWithSkippedLevelContextConfigurationClass {
  }

  @SingleLocationTransitiveImplicitAliasesWithSkippedLevelContextConfiguration(xml = "test.xml")
  static class SingleLocationTransitiveImplicitAliasesWithSkippedLevelContextConfigurationClass {
  }

  @ComposedImplicitAliasesContextConfiguration
  static class ComposedImplicitAliasesContextConfigurationClass {
  }

  @ShadowedAliasComposedContextConfiguration(xmlConfigFiles = "test.xml")
  static class ShadowedAliasComposedContextConfigurationClass {
  }

  @AliasedComposedContextConfigurationAndTestPropertySource(xmlConfigFiles = "test.xml")
  static class AliasedComposedContextConfigurationAndTestPropertySourceClass {
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

  @SpringApplicationConfiguration(Number.class)
  static class SpringApplicationConfigurationClass {
  }

  @Resource(name = "x")
  static class ResourceHolder {
  }

  interface TransactionalService {

    @Transactional
    void doIt();
  }

  class TransactionalServiceImpl implements TransactionalService {

    @Override
    public void doIt() {
    }
  }

  @Component("meta1")
  @Order
  @Retention(RetentionPolicy.RUNTIME)
  @Inherited
  @interface Meta1 {
  }

  @Component("meta2")
  @Transactional(readOnly = true)
  @Retention(RetentionPolicy.RUNTIME)
  @interface Meta2 {
  }

  @Meta2
  @Retention(RetentionPolicy.RUNTIME)
  @interface MetaMeta {
  }

  @MetaMeta
  @Retention(RetentionPolicy.RUNTIME)
  @interface MetaMetaMeta {
  }

  @MetaCycle3
  @Retention(RetentionPolicy.RUNTIME)
  @interface MetaCycle1 {
  }

  @MetaCycle1
  @Retention(RetentionPolicy.RUNTIME)
  @interface MetaCycle2 {
  }

  @MetaCycle2
  @Retention(RetentionPolicy.RUNTIME)
  @interface MetaCycle3 {
  }

  @Meta1
  interface InterfaceWithMetaAnnotation {
  }

  @Meta2
  static class ClassWithLocalMetaAnnotationAndMetaAnnotatedInterface
          implements InterfaceWithMetaAnnotation {
  }

  @Meta1
  static class ClassWithInheritedMetaAnnotation {
  }

  @Meta2
  static class SubClassWithInheritedMetaAnnotation
          extends ClassWithInheritedMetaAnnotation {
  }

  static class SubSubClassWithInheritedMetaAnnotation
          extends SubClassWithInheritedMetaAnnotation {
  }

  @MetaMeta
  static class MetaMetaAnnotatedClass {
  }

  @MetaMetaMeta
  static class MetaMetaMetaAnnotatedClass {
  }

  @MetaCycle3
  static class MetaCycleAnnotatedClass {
  }

  interface AnnotatedInterface {

    @Order(0)
    void fromInterfaceImplementedByRoot();
  }

  interface NullableAnnotatedInterface {

    @Nullable
    void fromInterfaceImplementedByRoot();
  }

  static class Root implements AnnotatedInterface {

    @Order(27)
    public void annotatedOnRoot() {
    }

    @Meta1
    public void metaAnnotatedOnRoot() {
    }

    public void overrideToAnnotate() {
    }

    @Order(27)
    public void overrideWithoutNewAnnotation() {
    }

    public void notAnnotated() {
    }

    @Override
    public void fromInterfaceImplementedByRoot() {
    }
  }

  public static class Leaf extends Root {

    @Order(25)
    public void annotatedOnLeaf() {
    }

    @Meta1
    public void metaAnnotatedOnLeaf() {
    }

    @MetaMeta
    public void metaMetaAnnotatedOnLeaf() {
    }

    @Override
    @Order(1)
    public void overrideToAnnotate() {
    }

    @Override
    public void overrideWithoutNewAnnotation() {
    }
  }

  public static abstract class SimpleGeneric<T> {

    @Order(1)
    public abstract void something(T arg);

  }

  public static class TransactionalStringGeneric extends SimpleGeneric<String> {

    @Override
    @Transactional
    public void something(final String arg) {
    }
  }

  @Transactional
  public static class InheritedAnnotationClass {
  }

  public static class SubInheritedAnnotationClass extends InheritedAnnotationClass {
  }

  @Order
  public static class NonInheritedAnnotationClass {
  }

  public static class SubNonInheritedAnnotationClass
          extends NonInheritedAnnotationClass {
  }

  @Transactional
  public static class TransactionalClass {
  }

  @Order
  public static class TransactionalAndOrderedClass extends TransactionalClass {
  }

  public static class SubTransactionalAndOrderedClass
          extends TransactionalAndOrderedClass {
  }

  public interface InterfaceWithAnnotatedMethod {

    @Order
    void foo();
  }

  public static class ImplementsInterfaceWithAnnotatedMethod
          implements InterfaceWithAnnotatedMethod {

    @Override
    public void foo() {
    }
  }

  public static class SubOfImplementsInterfaceWithAnnotatedMethod
          extends ImplementsInterfaceWithAnnotatedMethod {

    @Override
    public void foo() {
    }
  }

  public abstract static class AbstractDoesNotImplementInterfaceWithAnnotatedMethod
          implements InterfaceWithAnnotatedMethod {
  }

  public static class SubOfAbstractImplementsInterfaceWithAnnotatedMethod
          extends AbstractDoesNotImplementInterfaceWithAnnotatedMethod {

    @Override
    public void foo() {
    }
  }

  public interface InterfaceWithGenericAnnotatedMethod<T> {

    @Order
    void foo(T t);
  }

  public static class ImplementsInterfaceWithGenericAnnotatedMethod
          implements InterfaceWithGenericAnnotatedMethod<String> {

    @Override
    public void foo(String t) {
    }
  }

  public static abstract class BaseClassWithGenericAnnotatedMethod<T> {

    @Order
    abstract void foo(T t);
  }

  public static class ExtendsBaseClassWithGenericAnnotatedMethod
          extends BaseClassWithGenericAnnotatedMethod<String> {

    @Override
    public void foo(String t) {
    }
  }

  @Retention(RetentionPolicy.RUNTIME)
  @Inherited
  @interface MyRepeatableContainer {

    MyRepeatable[] value();
  }

  @Retention(RetentionPolicy.RUNTIME)
  @Inherited
  @Repeatable(MyRepeatableContainer.class)
  @interface MyRepeatable {

    String value();
  }

  @Retention(RetentionPolicy.RUNTIME)
  @Inherited
  @MyRepeatable("meta1")
  @interface MyRepeatableMeta1 {
  }

  @Retention(RetentionPolicy.RUNTIME)
  @Inherited
  @MyRepeatable("meta2")
  @interface MyRepeatableMeta2 {
  }

  interface InterfaceWithRepeated {

    @MyRepeatable("A")
    @MyRepeatableContainer({ @MyRepeatable("B"), @MyRepeatable("C") })
    @MyRepeatableMeta1
    void foo();
  }

  @MyRepeatable("A")
  @MyRepeatableContainer({ @MyRepeatable("B"), @MyRepeatable("C") })
  @MyRepeatableMeta1
  static class MyRepeatableClass {
  }

  static class SubMyRepeatableClass extends MyRepeatableClass {
  }

  @MyRepeatable("X")
  @MyRepeatableContainer({ @MyRepeatable("Y"), @MyRepeatable("Z") })
  @MyRepeatableMeta2
  static class SubMyRepeatableWithAdditionalLocalDeclarationsClass
          extends MyRepeatableClass {
  }

  static class SubSubMyRepeatableWithAdditionalLocalDeclarationsClass
          extends SubMyRepeatableWithAdditionalLocalDeclarationsClass {
  }

  enum RequestMethod {
    GET, POST
  }

  @Retention(RetentionPolicy.RUNTIME)
  @interface RequestMapping {

    String name();

    @AliasFor("path")
    String[] value() default "";

    @AliasFor(attribute = "value")
    String[] path() default "";

    RequestMethod[] method() default { };
  }

  @Retention(RetentionPolicy.RUNTIME)
  @RequestMapping(method = RequestMethod.GET, name = "")
  @interface GetMapping {

    @AliasFor(annotation = RequestMapping.class)
    String value() default "";

    @AliasFor(annotation = RequestMapping.class)
    String path() default "";
  }

  @Retention(RetentionPolicy.RUNTIME)
  @RequestMapping(method = RequestMethod.POST, name = "")
  @interface PostMapping {

    String path() default "";
  }

  @Component("webController")
  static class WebController {

    @RequestMapping(value = "/test", name = "foo")
    public void handleMappedWithValueAttribute() {
    }

    @RequestMapping(path = "/test", name = "bar", method = { RequestMethod.GET,
            RequestMethod.POST })
    public void handleMappedWithPathAttribute() {
    }

    @GetMapping("/test")
    public void getMappedWithValueAttribute() {
    }

    @GetMapping(path = "/test")
    public void getMappedWithPathAttribute() {
    }

    @PostMapping(path = "/test")
    public void postMappedWithPathAttribute() {
    }

    @RequestMapping(value = "/test", path = "/test", name = "bar", method = {
            RequestMethod.GET, RequestMethod.POST })
    public void handleMappedWithSamePathAndValueAttributes() {
    }

    @RequestMapping(value = "/enigma", path = "/test", name = "baz")
    public void handleMappedWithDifferentPathAndValueAttributes() {
    }
  }

  /**
   * Mimics jakarta.persistence.Id
   */
  @Retention(RUNTIME)
  @interface Id {
  }

  /**
   * Mimics jakarta.persistence.GeneratedValue
   */
  @Retention(RUNTIME)
  @interface GeneratedValue {
    String strategy();
  }

  @Id
  @GeneratedValue(strategy = "AUTO")
  private Long getId() {
    return 42L;
  }

  @Retention(RetentionPolicy.RUNTIME)
  @interface TestConfiguration {

    @AliasFor("location")
    String value() default "";

    @AliasFor("value")
    String location() default "";

    Class<?> configClass() default Object.class;
  }

  @Retention(RetentionPolicy.RUNTIME)
  @interface Hierarchy {

    TestConfiguration[] value();
  }

  @Hierarchy({ @TestConfiguration("A"), @TestConfiguration(location = "B") })
  static class HierarchyClass {
  }

  @TestConfiguration("simple.xml")
  static class TestConfigurationClass {
  }

  @Retention(RetentionPolicy.RUNTIME)
  @interface CharsContainer {

    @AliasFor(attribute = "chars")
    char[] value() default { };

    @AliasFor(attribute = "value")
    char[] chars() default { };
  }

  @CharsContainer(chars = { 'x', 'y', 'z' })
  static class GroupOfCharsClass {
  }

  @Retention(RetentionPolicy.RUNTIME)
  @interface AliasForWithMissingAttributeDeclaration {

    @AliasFor
    String foo() default "";
  }

  @AliasForWithMissingAttributeDeclaration
  static class AliasForWithMissingAttributeDeclarationClass {
  }

  @Retention(RetentionPolicy.RUNTIME)
  @interface AliasForWithDuplicateAttributeDeclaration {

    @AliasFor(value = "bar", attribute = "baz")
    String foo() default "";
  }

  @AliasForWithDuplicateAttributeDeclaration
  static class AliasForWithDuplicateAttributeDeclarationClass {
  }

  @Retention(RetentionPolicy.RUNTIME)
  @interface AliasForNonexistentAttribute {

    @AliasFor("bar")
    String foo() default "";
  }

  @AliasForNonexistentAttribute
  static class AliasForNonexistentAttributeClass {
  }

  @Retention(RetentionPolicy.RUNTIME)
  @interface AliasForWithoutMirroredAliasFor {

    @AliasFor("bar")
    String foo() default "";

    String bar() default "";
  }

  @AliasForWithoutMirroredAliasFor
  static class AliasForWithoutMirroredAliasForClass {
  }

  @Retention(RetentionPolicy.RUNTIME)
  @interface AliasForWithMirroredAliasForWrongAttribute {

    @AliasFor(attribute = "bar")
    String[] foo() default "";

    @AliasFor(attribute = "quux")
    String[] bar() default "";
  }

  @AliasForWithMirroredAliasForWrongAttribute
  static class AliasForWithMirroredAliasForWrongAttributeClass {
  }

  @Retention(RetentionPolicy.RUNTIME)
  @interface AliasForAttributeOfDifferentType {

    @AliasFor("bar")
    String[] foo() default "";

    @AliasFor("foo")
    boolean bar() default true;
  }

  @AliasForAttributeOfDifferentType
  static class AliasForAttributeOfDifferentTypeClass {
  }

  @Retention(RetentionPolicy.RUNTIME)
  @interface AliasForWithMissingDefaultValues {

    @AliasFor(attribute = "bar")
    String foo();

    @AliasFor(attribute = "foo")
    String bar();
  }

  @AliasForWithMissingDefaultValues(foo = "foo", bar = "bar")
  static class AliasForWithMissingDefaultValuesClass {
  }

  @Retention(RetentionPolicy.RUNTIME)
  @interface AliasForAttributeWithDifferentDefaultValue {

    @AliasFor("bar")
    String foo() default "X";

    @AliasFor("foo")
    String bar() default "Z";
  }

  @AliasForAttributeWithDifferentDefaultValue
  static class AliasForAttributeWithDifferentDefaultValueClass {
  }

  @Retention(RetentionPolicy.RUNTIME)
  @interface AliasedComposedTestConfigurationNotMetaPresent {

    @AliasFor(annotation = TestConfiguration.class, attribute = "location")
    String xmlConfigFile();
  }

  @AliasedComposedTestConfigurationNotMetaPresent(xmlConfigFile = "test.xml")
  static class AliasedComposedTestConfigurationNotMetaPresentClass {
  }

  @TestConfiguration
  @Retention(RetentionPolicy.RUNTIME)
  @interface AliasedComposedTestConfiguration {

    @AliasFor(annotation = TestConfiguration.class, attribute = "location")
    String xmlConfigFile();
  }

  @TestConfiguration
  @Retention(RetentionPolicy.RUNTIME)
  public @interface ImplicitAliasesTestConfiguration {

    @AliasFor(annotation = TestConfiguration.class, attribute = "location")
    String xmlFile() default "";

    @AliasFor(annotation = TestConfiguration.class, attribute = "location")
    String groovyScript() default "";

    @AliasFor(annotation = TestConfiguration.class, attribute = "location")
    String value() default "";

    @AliasFor(annotation = TestConfiguration.class, attribute = "location")
    String location1() default "";

    @AliasFor(annotation = TestConfiguration.class, attribute = "location")
    String location2() default "";

    @AliasFor(annotation = TestConfiguration.class, attribute = "location")
    String location3() default "";

    @AliasFor(annotation = TestConfiguration.class, attribute = "configClass")
    Class<?> configClass() default Object.class;

    String nonAliasedAttribute() default "";
  }

  @ImplicitAliasesTestConfiguration(groovyScript = "groovyScript")
  static class GroovyImplicitAliasesSimpleTestConfigurationClass {
  }

  @ImplicitAliasesTestConfiguration(xmlFile = "xmlFile")
  static class XmlImplicitAliasesTestConfigurationClass {
  }

  @ImplicitAliasesTestConfiguration("value")
  static class ValueImplicitAliasesTestConfigurationClass {
  }

  @ImplicitAliasesTestConfiguration(location1 = "location1")
  static class Location1ImplicitAliasesTestConfigurationClass {
  }

  @ImplicitAliasesTestConfiguration(location2 = "location2")
  static class Location2ImplicitAliasesTestConfigurationClass {
  }

  @ImplicitAliasesTestConfiguration(location3 = "location3")
  static class Location3ImplicitAliasesTestConfigurationClass {
  }

  @TestConfiguration
  @Retention(RetentionPolicy.RUNTIME)
  @interface ImplicitAliasesWithImpliedAliasNamesOmittedTestConfiguration {

    @AliasFor(annotation = TestConfiguration.class)
    String value() default "";

    @AliasFor(annotation = TestConfiguration.class)
    String location() default "";

    @AliasFor(annotation = TestConfiguration.class, attribute = "location")
    String xmlFile() default "";
  }

  @ImplicitAliasesWithImpliedAliasNamesOmittedTestConfiguration
  @Retention(RetentionPolicy.RUNTIME)
  @interface TransitiveImplicitAliasesWithImpliedAliasNamesOmittedTestConfiguration {

    @AliasFor(annotation = ImplicitAliasesWithImpliedAliasNamesOmittedTestConfiguration.class, attribute = "xmlFile")
    String xml() default "";

    @AliasFor(annotation = ImplicitAliasesWithImpliedAliasNamesOmittedTestConfiguration.class, attribute = "location")
    String groovy() default "";
  }

  @ImplicitAliasesWithImpliedAliasNamesOmittedTestConfiguration("value")
  static class ValueImplicitAliasesWithImpliedAliasNamesOmittedTestConfigurationClass {
  }

  @ImplicitAliasesWithImpliedAliasNamesOmittedTestConfiguration(location = "location")
  static class LocationsImplicitAliasesWithImpliedAliasNamesOmittedTestConfigurationClass {
  }

  @ImplicitAliasesWithImpliedAliasNamesOmittedTestConfiguration(xmlFile = "xmlFile")
  static class XmlFilesImplicitAliasesWithImpliedAliasNamesOmittedTestConfigurationClass {
  }

  @TestConfiguration
  @Retention(RetentionPolicy.RUNTIME)
  @interface ImplicitAliasesWithMissingDefaultValuesTestConfiguration {

    @AliasFor(annotation = TestConfiguration.class, attribute = "location")
    String location1();

    @AliasFor(annotation = TestConfiguration.class, attribute = "location")
    String location2();
  }

  @ImplicitAliasesWithMissingDefaultValuesTestConfiguration(location1 = "1", location2 = "2")
  static class ImplicitAliasesWithMissingDefaultValuesTestConfigurationClass {
  }

  @TestConfiguration
  @Retention(RetentionPolicy.RUNTIME)
  @interface ImplicitAliasesWithDifferentDefaultValuesTestConfiguration {

    @AliasFor(annotation = TestConfiguration.class, attribute = "location")
    String location1() default "foo";

    @AliasFor(annotation = TestConfiguration.class, attribute = "location")
    String location2() default "bar";
  }

  @ImplicitAliasesWithDifferentDefaultValuesTestConfiguration(location1 = "1", location2 = "2")
  static class ImplicitAliasesWithDifferentDefaultValuesTestConfigurationClass {
  }

  @TestConfiguration
  @Retention(RetentionPolicy.RUNTIME)
  @interface ImplicitAliasesWithDuplicateValuesTestConfiguration {

    @AliasFor(annotation = TestConfiguration.class, attribute = "location")
    String location1() default "";

    @AliasFor(annotation = TestConfiguration.class, attribute = "location")
    String location2() default "";
  }

  @ImplicitAliasesWithDuplicateValuesTestConfiguration(location1 = "1", location2 = "2")
  static class ImplicitAliasesWithDuplicateValuesTestConfigurationClass {
  }

  @TestConfiguration
  @Retention(RetentionPolicy.RUNTIME)
  @interface ImplicitAliasesForAliasPairTestConfiguration {

    @AliasFor(annotation = TestConfiguration.class, attribute = "location")
    String xmlFile() default "";

    @AliasFor(annotation = TestConfiguration.class, value = "value")
    String groovyScript() default "";
  }

  @ImplicitAliasesForAliasPairTestConfiguration(xmlFile = "test.xml")
  static class ImplicitAliasesForAliasPairTestConfigurationClass {
  }

  @ImplicitAliasesTestConfiguration
  @Retention(RetentionPolicy.RUNTIME)
  @interface TransitiveImplicitAliasesTestConfiguration {

    @AliasFor(annotation = ImplicitAliasesTestConfiguration.class, attribute = "xmlFile")
    String xml() default "";

    @AliasFor(annotation = ImplicitAliasesTestConfiguration.class, attribute = "groovyScript")
    String groovy() default "";
  }

  @TransitiveImplicitAliasesTestConfiguration(xml = "test.xml")
  static class TransitiveImplicitAliasesTestConfigurationClass {
  }

  @ImplicitAliasesForAliasPairTestConfiguration
  @Retention(RetentionPolicy.RUNTIME)
  @interface TransitiveImplicitAliasesForAliasPairTestConfiguration {

    @AliasFor(annotation = ImplicitAliasesForAliasPairTestConfiguration.class, attribute = "xmlFile")
    String xml() default "";

    @AliasFor(annotation = ImplicitAliasesForAliasPairTestConfiguration.class, attribute = "groovyScript")
    String groovy() default "";
  }

  @TransitiveImplicitAliasesForAliasPairTestConfiguration(xml = "test.xml")
  static class TransitiveImplicitAliasesForAliasPairTestConfigurationClass {
  }

  @ComponentScan(excludeFilters = { @Filter(pattern = "*Foo"),
          @Filter(pattern = "*Bar") })
  static class ComponentScanClass {
  }

  @Retention(RetentionPolicy.RUNTIME)
  @interface ComponentScanSingleFilter {

    Filter value();
  }

  @ComponentScanSingleFilter(@Filter(pattern = "*Foo"))
  static class ComponentScanSingleFilterClass {
  }

  @Retention(RetentionPolicy.RUNTIME)
  @interface AnnotationWithDefaults {

    String text() default "enigma";

    boolean predicate() default true;

    char[] characters() default { 'a', 'b', 'c' };
  }

  @Retention(RetentionPolicy.RUNTIME)
  @interface AnnotationWithoutDefaults {

    String text();
  }

  @TestConfiguration(value = "foo", location = "bar")
  interface TestConfigurationMismatch {
  }

  @Retention(RetentionPolicy.RUNTIME)
  @interface DefaultOverrideRoot {

    String text() default "root";

  }

  @Retention(RetentionPolicy.RUNTIME)
  @DefaultOverrideRoot
  @interface DefaultOverrideMeta {

  }

  @Retention(RetentionPolicy.RUNTIME)
  @DefaultOverrideMeta
  @interface DefaultOverrideMetaMeta {

    String text() default "metameta";

  }

  @Retention(RetentionPolicy.RUNTIME)
  @DefaultOverrideMetaMeta
  @interface DefaultOverrideMetaMetaMeta {

  }

  @DefaultOverrideMetaMetaMeta
  static class DefaultOverrideClass {

  }

  @Retention(RetentionPolicy.RUNTIME)
  @DefaultOverrideRoot
  @interface DefaultOverrideImplicitAlias {

    @AliasFor(annotation = DefaultOverrideRoot.class, attribute = "text")
    String text1() default "alias";

    @AliasFor(annotation = DefaultOverrideRoot.class, attribute = "text")
    String text2() default "alias";

  }

  @Retention(RetentionPolicy.RUNTIME)
  @DefaultOverrideImplicitAlias(text1 = "alias-meta-1")
  @interface DefaultOverrideAliasImplicitMeta1 {

  }

  @Retention(RetentionPolicy.RUNTIME)
  @DefaultOverrideImplicitAlias(text2 = "alias-meta-2")
  @interface DefaultOverrideImplicitAliasMeta2 {

  }

  @DefaultOverrideAliasImplicitMeta1
  static class DefaultOverrideImplicitAliasMetaClass1 {

  }

  @DefaultOverrideImplicitAliasMeta2
  static class DefaultOverrideImplicitAliasMetaClass2 {

  }

  @Retention(RetentionPolicy.RUNTIME)
  @interface DefaultOverrideExplicitAliasRoot {

    @AliasFor("value")
    String text() default "";

    @AliasFor("text")
    String value() default "";

  }

  @Retention(RetentionPolicy.RUNTIME)
  @DefaultOverrideExplicitAliasRoot("meta")
  @interface DefaultOverrideExplicitAliasRootMeta {

  }

  @Retention(RetentionPolicy.RUNTIME)
  @DefaultOverrideExplicitAliasRootMeta
  @interface DefaultOverrideExplicitAliasRootMetaMeta {

  }

  @DefaultOverrideExplicitAliasRootMetaMeta
  static class DefaultOverrideExplicitAliasRootMetaMetaClass {

  }

  @Retention(RetentionPolicy.RUNTIME)
  @interface ValueAttribute {

    String[] value();

  }

  @Retention(RetentionPolicy.RUNTIME)
  @ValueAttribute("FromValueAttributeMeta")
  @interface ValueAttributeMeta {

    String[] value() default { };

  }

  @Retention(RetentionPolicy.RUNTIME)
  @ValueAttributeMeta("FromValueAttributeMetaMeta")
  @interface ValueAttributeMetaMeta {

  }

  @ValueAttributeMetaMeta
  static class ValueAttributeMetaMetaClass {

  }
  // @formatter:on

}
