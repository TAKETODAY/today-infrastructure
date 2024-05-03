/*
 * Copyright 2017 - 2024 the original author or authors.
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

package cn.taketoday.core.annotation;

import org.assertj.core.api.ThrowableTypeAssert;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.AnnotatedElement;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Stream;

import cn.taketoday.core.annotation.MergedAnnotations.SearchStrategy;

import static cn.taketoday.core.annotation.MergedAnnotations.SearchStrategy.TYPE_HIERARCHY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link MergedAnnotations} and {@link RepeatableContainers} that
 * verify support for repeatable annotations.
 *
 * @author Phillip Webb
 * @author Sam Brannen
 */
class MergedAnnotationsRepeatableAnnotationTests {

  // See SPR-13973

  @Test
  void inheritedAnnotationsWhenNonRepeatableThrowsException() {
    assertThatIllegalArgumentException().isThrownBy(() ->
                    getAnnotations(null, NonRepeatable.class, SearchStrategy.INHERITED_ANNOTATIONS, getClass()))
            .satisfies(this::nonRepeatableRequirements);
  }

  @Test
  void inheritedAnnotationsWhenContainerMissingValueAttributeThrowsException() {
    assertThatAnnotationConfigurationException().isThrownBy(() ->
                    getAnnotations(ContainerMissingValueAttribute.class, InvalidRepeatable.class,
                            SearchStrategy.INHERITED_ANNOTATIONS, getClass()))
            .satisfies(this::missingValueAttributeRequirements);
  }

  @Test
  void inheritedAnnotationsWhenWhenNonArrayValueAttributeThrowsException() {
    assertThatAnnotationConfigurationException().isThrownBy(() ->
                    getAnnotations(ContainerWithNonArrayValueAttribute.class, InvalidRepeatable.class,
                            SearchStrategy.INHERITED_ANNOTATIONS, getClass()))
            .satisfies(this::nonArrayValueAttributeRequirements);
  }

  @Test
  void inheritedAnnotationsWhenWrongComponentTypeThrowsException() {
    assertThatAnnotationConfigurationException().isThrownBy(() ->
                    getAnnotations(ContainerWithArrayValueAttributeButWrongComponentType.class,
                            InvalidRepeatable.class, SearchStrategy.INHERITED_ANNOTATIONS, getClass()))
            .satisfies(this::wrongComponentTypeRequirements);
  }

  @Test
  void inheritedAnnotationsWhenOnClassReturnsAnnotations() {
    Set<PeteRepeat> annotations = getAnnotations(null, PeteRepeat.class,
            SearchStrategy.INHERITED_ANNOTATIONS, RepeatableClass.class);
    assertThat(annotations.stream().map(PeteRepeat::value)).containsExactly("A", "B",
            "C");
  }

  @Test
  void inheritedAnnotationsWhenWhenOnSuperclassReturnsAnnotations() {
    Set<PeteRepeat> annotations = getAnnotations(null, PeteRepeat.class,
            SearchStrategy.INHERITED_ANNOTATIONS, SubRepeatableClass.class);
    assertThat(annotations.stream().map(PeteRepeat::value)).containsExactly("A", "B",
            "C");
  }

  @Test
  void inheritedAnnotationsWhenComposedOnClassReturnsAnnotations() {
    Set<PeteRepeat> annotations = getAnnotations(null, PeteRepeat.class,
            SearchStrategy.INHERITED_ANNOTATIONS, ComposedRepeatableClass.class);
    assertThat(annotations.stream().map(PeteRepeat::value)).containsExactly("A", "B",
            "C");
  }

  @Test
  void inheritedAnnotationsWhenComposedMixedWithContainerOnClassReturnsAnnotations() {
    Set<PeteRepeat> annotations = getAnnotations(null, PeteRepeat.class,
            SearchStrategy.INHERITED_ANNOTATIONS,
            ComposedRepeatableMixedWithContainerClass.class);
    assertThat(annotations.stream().map(PeteRepeat::value)).containsExactly("A", "B",
            "C");
  }

  @Test
  void inheritedAnnotationsWhenComposedContainerForRepeatableOnClassReturnsAnnotations() {
    Set<PeteRepeat> annotations = getAnnotations(null, PeteRepeat.class,
            SearchStrategy.INHERITED_ANNOTATIONS, ComposedContainerClass.class);
    assertThat(annotations.stream().map(PeteRepeat::value)).containsExactly("A", "B",
            "C");
  }

  @Test
  void inheritedAnnotationsWhenNoninheritedComposedRepeatableOnClassReturnsAnnotations() {
    Set<Noninherited> annotations = getAnnotations(null, Noninherited.class,
            SearchStrategy.INHERITED_ANNOTATIONS, NoninheritedRepeatableClass.class);
    assertThat(annotations.stream().map(Noninherited::value)).containsExactly("A",
            "B", "C");
  }

  @Test
  void inheritedAnnotationsWhenNoninheritedComposedRepeatableOnSuperclassReturnsAnnotations() {
    Set<Noninherited> annotations = getAnnotations(null, Noninherited.class,
            SearchStrategy.INHERITED_ANNOTATIONS,
            SubNoninheritedRepeatableClass.class);
    assertThat(annotations).isEmpty();
  }

  @Test
  void typeHierarchyWhenNonRepeatableThrowsException() {
    assertThatIllegalArgumentException().isThrownBy(() ->
                    getAnnotations(null, NonRepeatable.class, TYPE_HIERARCHY, getClass()))
            .satisfies(this::nonRepeatableRequirements);
  }

  @Test
  void typeHierarchyWhenContainerMissingValueAttributeThrowsException() {
    assertThatAnnotationConfigurationException().isThrownBy(() ->
                    getAnnotations(ContainerMissingValueAttribute.class, InvalidRepeatable.class,
                            TYPE_HIERARCHY, getClass()))
            .satisfies(this::missingValueAttributeRequirements);
  }

  @Test
  void typeHierarchyWhenWhenNonArrayValueAttributeThrowsException() {
    assertThatAnnotationConfigurationException().isThrownBy(() ->
                    getAnnotations(ContainerWithNonArrayValueAttribute.class, InvalidRepeatable.class,
                            TYPE_HIERARCHY, getClass()))
            .satisfies(this::nonArrayValueAttributeRequirements);
  }

  @Test
  void typeHierarchyWhenWrongComponentTypeThrowsException() {
    assertThatAnnotationConfigurationException().isThrownBy(() ->
                    getAnnotations(ContainerWithArrayValueAttributeButWrongComponentType.class,
                            InvalidRepeatable.class, TYPE_HIERARCHY, getClass()))
            .satisfies(this::wrongComponentTypeRequirements);
  }

  @Test
  void typeHierarchyWhenOnClassReturnsAnnotations() {
    Set<PeteRepeat> annotations = getAnnotations(null, PeteRepeat.class,
            TYPE_HIERARCHY, RepeatableClass.class);
    assertThat(annotations.stream().map(PeteRepeat::value)).containsExactly("A", "B",
            "C");
  }

  @Test
  void typeHierarchyWhenWhenOnSuperclassReturnsAnnotations() {
    Set<PeteRepeat> annotations = getAnnotations(null, PeteRepeat.class,
            TYPE_HIERARCHY, SubRepeatableClass.class);
    assertThat(annotations.stream().map(PeteRepeat::value)).containsExactly("A", "B",
            "C");
  }

  @Test
  void typeHierarchyWhenComposedOnClassReturnsAnnotations() {
    Set<PeteRepeat> annotations = getAnnotations(null, PeteRepeat.class,
            TYPE_HIERARCHY, ComposedRepeatableClass.class);
    assertThat(annotations.stream().map(PeteRepeat::value)).containsExactly("A", "B",
            "C");
  }

  @Test
  void typeHierarchyWhenComposedMixedWithContainerOnClassReturnsAnnotations() {
    Set<PeteRepeat> annotations = getAnnotations(null, PeteRepeat.class,
            TYPE_HIERARCHY,
            ComposedRepeatableMixedWithContainerClass.class);
    assertThat(annotations.stream().map(PeteRepeat::value)).containsExactly("A", "B",
            "C");
  }

  @Test
  void typeHierarchyWhenComposedContainerForRepeatableOnClassReturnsAnnotations() {
    Set<PeteRepeat> annotations = getAnnotations(null, PeteRepeat.class,
            TYPE_HIERARCHY, ComposedContainerClass.class);
    assertThat(annotations.stream().map(PeteRepeat::value)).containsExactly("A", "B",
            "C");
  }

  @Test
  void typeHierarchyAnnotationsWhenNoninheritedComposedRepeatableOnClassReturnsAnnotations() {
    Set<Noninherited> annotations = getAnnotations(null, Noninherited.class,
            TYPE_HIERARCHY, NoninheritedRepeatableClass.class);
    assertThat(annotations.stream().map(Noninherited::value)).containsExactly("A",
            "B", "C");
  }

  @Test
  void typeHierarchyAnnotationsWhenNoninheritedComposedRepeatableOnSuperclassReturnsAnnotations() {
    Set<Noninherited> annotations = getAnnotations(null, Noninherited.class,
            TYPE_HIERARCHY, SubNoninheritedRepeatableClass.class);
    assertThat(annotations.stream().map(Noninherited::value)).containsExactly("A",
            "B", "C");
  }

  @Test
  void typeHierarchyAnnotationsWithLocalComposedAnnotationWhoseRepeatableMetaAnnotationsAreFiltered() {
    Class<WithRepeatedMetaAnnotationsClass> element = WithRepeatedMetaAnnotationsClass.class;
    SearchStrategy searchStrategy = TYPE_HIERARCHY;
    AnnotationFilter annotationFilter = PeteRepeat.class.getName()::equals;

    Set<PeteRepeat> annotations = getAnnotations(null, PeteRepeat.class, searchStrategy, element, annotationFilter);
    assertThat(annotations).isEmpty();

    MergedAnnotations mergedAnnotations = MergedAnnotations.from(element, searchStrategy,
            RepeatableContainers.standard(), annotationFilter);
    Stream<Class<? extends Annotation>> annotationTypes = mergedAnnotations.stream()
            .map(MergedAnnotation::synthesize)
            .map(Annotation::annotationType);
    assertThat(annotationTypes).containsExactly(WithRepeatedMetaAnnotations.class, Noninherited.class, Noninherited.class);
  }

  @Test
  void searchFindsRepeatableContainerAnnotationAndRepeatedAnnotations() {
    Class<?> clazz = StandardRepeatablesWithContainerWithMultipleAttributesTestCase.class;

    // NO RepeatableContainers
    MergedAnnotations mergedAnnotations = MergedAnnotations.from(clazz, TYPE_HIERARCHY, RepeatableContainers.none());
    ContainerWithMultipleAttributes container = mergedAnnotations
            .get(ContainerWithMultipleAttributes.class)
            .synthesize(MergedAnnotation::isPresent).orElse(null);
    assertThat(container).as("container").isNotNull();
    assertThat(container.name()).isEqualTo("enigma");
    RepeatableWithContainerWithMultipleAttributes[] repeatedAnnotations = container.value();
    assertThat(Arrays.stream(repeatedAnnotations).map(RepeatableWithContainerWithMultipleAttributes::value))
            .containsExactly("A", "B");
    Set<RepeatableWithContainerWithMultipleAttributes> set =
            mergedAnnotations.stream(RepeatableWithContainerWithMultipleAttributes.class)
                    .collect(MergedAnnotationCollectors.toAnnotationSet());
    // Only finds the locally declared repeated annotation.
    assertThat(set.stream().map(RepeatableWithContainerWithMultipleAttributes::value))
            .containsExactly("C");

    // Standard RepeatableContainers
    mergedAnnotations = MergedAnnotations.from(clazz, TYPE_HIERARCHY, RepeatableContainers.standard());
    container = mergedAnnotations
            .get(ContainerWithMultipleAttributes.class)
            .synthesize(MergedAnnotation::isPresent).orElse(null);
    assertThat(container).as("container").isNotNull();
    assertThat(container.name()).isEqualTo("enigma");
    repeatedAnnotations = container.value();
    assertThat(Arrays.stream(repeatedAnnotations).map(RepeatableWithContainerWithMultipleAttributes::value))
            .containsExactly("A", "B");
    set = mergedAnnotations.stream(RepeatableWithContainerWithMultipleAttributes.class)
            .collect(MergedAnnotationCollectors.toAnnotationSet());
    // Finds the locally declared repeated annotation plus the 2 in the container.
    assertThat(set.stream().map(RepeatableWithContainerWithMultipleAttributes::value))
            .containsExactly("A", "B", "C");
  }

  private <A extends Annotation> Set<A> getAnnotations(Class<? extends Annotation> container,
          Class<A> repeatable, SearchStrategy searchStrategy, AnnotatedElement element) {

    return getAnnotations(container, repeatable, searchStrategy, element, AnnotationFilter.PLAIN);
  }

  private <A extends Annotation> Set<A> getAnnotations(Class<? extends Annotation> container,
          Class<A> repeatable, SearchStrategy searchStrategy, AnnotatedElement element, AnnotationFilter annotationFilter) {

    RepeatableContainers containers = RepeatableContainers.valueOf(repeatable, container);
    MergedAnnotations annotations = MergedAnnotations.from(element, searchStrategy, containers, annotationFilter);
    return annotations.stream(repeatable).collect(MergedAnnotationCollectors.toAnnotationSet());
  }

  private void nonRepeatableRequirements(Exception ex) {
    assertThat(ex.getMessage()).startsWith(
            "Annotation type must be a repeatable annotation").contains(
            "failed to resolve container type for",
            NonRepeatable.class.getName());
  }

  private void missingValueAttributeRequirements(Exception ex) {
    assertThat(ex.getMessage()).startsWith(
            "Invalid declaration of container type").contains(
            ContainerMissingValueAttribute.class.getName(),
            "for repeatable annotation", InvalidRepeatable.class.getName());
    assertThat(ex).hasCauseInstanceOf(NoSuchMethodException.class);
  }

  private void nonArrayValueAttributeRequirements(Exception ex) {
    assertThat(ex.getMessage()).startsWith("Container type").contains(
            ContainerWithNonArrayValueAttribute.class.getName(),
            "must declare a 'value' attribute for an array of type",
            InvalidRepeatable.class.getName());
  }

  private void wrongComponentTypeRequirements(Exception ex) {
    assertThat(ex.getMessage()).startsWith("Container type").contains(
            ContainerWithArrayValueAttributeButWrongComponentType.class.getName(),
            "must declare a 'value' attribute for an array of type",
            InvalidRepeatable.class.getName());
  }

  private static ThrowableTypeAssert<AnnotationConfigurationException> assertThatAnnotationConfigurationException() {
    return assertThatExceptionOfType(AnnotationConfigurationException.class);
  }

  @Retention(RetentionPolicy.RUNTIME)
  @interface NonRepeatable {

  }

  @Retention(RetentionPolicy.RUNTIME)
  @interface ContainerMissingValueAttribute {

    // InvalidRepeatable[] value();

  }

  @Retention(RetentionPolicy.RUNTIME)
  @interface ContainerWithNonArrayValueAttribute {

    InvalidRepeatable value();

  }

  @Retention(RetentionPolicy.RUNTIME)
  @interface ContainerWithArrayValueAttributeButWrongComponentType {

    String[] value();

  }

  @Retention(RetentionPolicy.RUNTIME)
  @interface InvalidRepeatable {

  }

  @Retention(RetentionPolicy.RUNTIME)
  @Inherited
  @interface PeteRepeats {

    PeteRepeat[] value();

  }

  @Retention(RetentionPolicy.RUNTIME)
  @Inherited
  @Repeatable(PeteRepeats.class)
  @interface PeteRepeat {

    String value();

  }

  @PeteRepeat("shadowed")
  @Target({ ElementType.METHOD, ElementType.TYPE })
  @Retention(RetentionPolicy.RUNTIME)
  @Inherited
  @interface ForPetesSake {

    @AliasFor(annotation = PeteRepeat.class)
    String value();

  }

  @PeteRepeat("shadowed")
  @Target({ ElementType.METHOD, ElementType.TYPE })
  @Retention(RetentionPolicy.RUNTIME)
  @Inherited
  @interface ForTheLoveOfFoo {

    @AliasFor(annotation = PeteRepeat.class)
    String value();

  }

  @PeteRepeats({ @PeteRepeat("B"), @PeteRepeat("C") })
  @Target({ ElementType.METHOD, ElementType.TYPE })
  @Retention(RetentionPolicy.RUNTIME)
  @Inherited
  @interface ComposedContainer {

  }

  @PeteRepeat("A")
  @PeteRepeats({ @PeteRepeat("B"), @PeteRepeat("C") })
  static class RepeatableClass {

  }

  static class SubRepeatableClass extends RepeatableClass {

  }

  @ForPetesSake("B")
  @ForTheLoveOfFoo("C")
  @PeteRepeat("A")
  static class ComposedRepeatableClass {

  }

  @ForPetesSake("C")
  @PeteRepeats(@PeteRepeat("A"))
  @PeteRepeat("B")
  static class ComposedRepeatableMixedWithContainerClass {

  }

  @PeteRepeat("A")
  @ComposedContainer
  static class ComposedContainerClass {

  }

  @Target(ElementType.TYPE)
  @Retention(RetentionPolicy.RUNTIME)
  @interface Noninheriteds {

    Noninherited[] value();

  }

  @Target(ElementType.TYPE)
  @Retention(RetentionPolicy.RUNTIME)
  @Repeatable(Noninheriteds.class)
  @interface Noninherited {

    @AliasFor("name")
    String value() default "";

    @AliasFor("value")
    String name() default "";

  }

  @Noninherited(name = "shadowed")
  @Target(ElementType.TYPE)
  @Retention(RetentionPolicy.RUNTIME)
  @interface ComposedNoninherited {

    @AliasFor(annotation = Noninherited.class)
    String name() default "";

  }

  @ComposedNoninherited(name = "C")
  @Noninheriteds({ @Noninherited(value = "A"), @Noninherited(name = "B") })
  static class NoninheritedRepeatableClass {

  }

  static class SubNoninheritedRepeatableClass extends NoninheritedRepeatableClass {

  }

  @Retention(RetentionPolicy.RUNTIME)
  @PeteRepeat("A")
  @PeteRepeat("B")
  @interface WithRepeatedMetaAnnotations {
  }

  @WithRepeatedMetaAnnotations
  @PeteRepeat("C")
  @Noninherited("X")
  @Noninherited("Y")
  static class WithRepeatedMetaAnnotationsClass {
  }

  @Retention(RetentionPolicy.RUNTIME)
  @interface ContainerWithMultipleAttributes {

    RepeatableWithContainerWithMultipleAttributes[] value();

    String name() default "";
  }

  @Retention(RetentionPolicy.RUNTIME)
  @Repeatable(ContainerWithMultipleAttributes.class)
  @interface RepeatableWithContainerWithMultipleAttributes {

    String value() default "";
  }

  @ContainerWithMultipleAttributes(name = "enigma", value = {
          @RepeatableWithContainerWithMultipleAttributes("A"),
          @RepeatableWithContainerWithMultipleAttributes("B")
  })
  @RepeatableWithContainerWithMultipleAttributes("C")
  static class StandardRepeatablesWithContainerWithMultipleAttributesTestCase {
  }

}
