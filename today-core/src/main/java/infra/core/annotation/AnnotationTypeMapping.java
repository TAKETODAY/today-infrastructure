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

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import infra.lang.Nullable;
import infra.util.CollectionUtils;
import infra.util.ObjectUtils;
import infra.util.StringUtils;

/**
 * Provides mapping information for a single annotation (or meta-annotation) in
 * the context of a root annotation type.
 *
 * @author Phillip Webb
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see AnnotationTypeMappings
 * @since 4.0
 */
final class AnnotationTypeMapping {

  private static final MirrorSets.MirrorSet[] EMPTY_MIRROR_SETS = new MirrorSets.MirrorSet[0];

  private static final int[] EMPTY_INT_ARRAY = new int[0];

  /**
   * Get the source of the mapping or {@code null}.
   *
   * <p> the source of the mapping
   */
  @Nullable
  public final AnnotationTypeMapping source;

  /**
   * Get the root mapping.
   *
   * <p> the root mapping
   */
  public final AnnotationTypeMapping root;

  /**
   * Get the distance of this mapping.
   * <p>the distance of the mapping
   */
  public final int distance;

  /**
   * Get the type of the mapped annotation.
   *
   * <p>the annotation type
   */
  public final Class<? extends Annotation> annotationType;

  public final List<Class<? extends Annotation>> metaTypes;

  /**
   * Get the source annotation for this mapping. This will be the
   * meta-annotation, or {@code null} if this is the root mapping.
   *
   * <p> the source annotation of the mapping
   */
  @Nullable
  public final Annotation annotation;

  /**
   * the annotation attributes for the mapping annotation type.
   * <p>
   * the attribute methods
   */
  public final AttributeMethods methods;

  /**
   * Get the mirror sets for this type mapping.
   *
   * <p>the attribute mirror sets
   */
  public final MirrorSets mirrorSets;

  private final int[] aliasMappings;

  private final int[] conventionMappings;

  private final int[] annotationValueMappings;

  private final AnnotationTypeMapping[] annotationValueSource;

  private final Map<Method, List<Method>> aliasedBy;

  /**
   * Determine if the mapped annotation is <em>synthesizable</em>.
   * <p>Consult the documentation for {@link MergedAnnotation#synthesize()}
   * for an explanation of what is considered synthesizable.
   *
   * <p> {@code true} if the mapped annotation is synthesizable
   */
  public final boolean synthesizable;

  private final HashSet<Method> claimedAliases = new HashSet<>();

  AnnotationTypeMapping(@Nullable AnnotationTypeMapping source, Class<? extends Annotation> annotationType,
          @Nullable Annotation annotation, Set<Class<? extends Annotation>> visitedAnnotationTypes) {

    this.source = source;
    this.annotation = annotation;
    this.annotationType = annotationType;
    this.root = (source != null ? source.root : this);
    this.distance = (source == null ? 0 : source.distance + 1);
    this.metaTypes = merge(source != null ? source.metaTypes : null, annotationType);
    this.methods = AttributeMethods.forAnnotationType(annotationType);
    this.mirrorSets = new MirrorSets(); // MUST init after methods
    this.aliasMappings = filledIntArray(methods.size());
    this.conventionMappings = filledIntArray(methods.size());
    this.annotationValueMappings = filledIntArray(methods.size());
    this.annotationValueSource = new AnnotationTypeMapping[methods.size()];
    this.aliasedBy = resolveAliasedForTargets();
    processAliases();
    addConventionMappings();
    addConventionAnnotationValues();
    this.synthesizable = computeSynthesizableFlag(visitedAnnotationTypes);
  }

  private static <T> List<T> merge(@Nullable List<T> existing, T element) {
    if (existing == null) {
      return Collections.singletonList(element);
    }
    ArrayList<T> merged = new ArrayList<>(existing.size() + 1);
    merged.addAll(existing);
    merged.add(element);
    return Collections.unmodifiableList(merged);
  }

  private Map<Method, List<Method>> resolveAliasedForTargets() {
    HashMap<Method, List<Method>> aliasedBy = new HashMap<>();
    for (Method attribute : methods.attributes) {
      AliasFor aliasFor = AnnotationsScanner.getDeclaredAnnotation(attribute, AliasFor.class);
      if (aliasFor != null) {
        Method target = resolveAliasTarget(attribute, aliasFor);
        aliasedBy.computeIfAbsent(target, key -> new ArrayList<>()).add(attribute);
      }
    }
    return Collections.unmodifiableMap(aliasedBy);
  }

  private Method resolveAliasTarget(Method attribute, AliasFor aliasFor) {
    return resolveAliasTarget(attribute, aliasFor, true);
  }

  private Method resolveAliasTarget(Method attribute, AliasFor aliasFor, boolean checkAliasPair) {
    if (StringUtils.hasText(aliasFor.value()) && StringUtils.hasText(aliasFor.attribute())) {
      throw new AnnotationConfigurationException(String.format(
              "In @AliasFor declared on %s, attribute 'attribute' and its alias 'value' " +
                      "are present with values of '%s' and '%s', but only one is permitted.",
              AttributeMethods.describe(attribute), aliasFor.attribute(),
              aliasFor.value()));
    }
    Class<? extends Annotation> targetAnnotation = aliasFor.annotation();
    if (targetAnnotation == Annotation.class) {
      targetAnnotation = this.annotationType;
    }
    String targetAttributeName = aliasFor.attribute();
    if (StringUtils.isEmpty(targetAttributeName)) {
      targetAttributeName = aliasFor.value();
    }
    if (StringUtils.isEmpty(targetAttributeName)) {
      targetAttributeName = attribute.getName();
    }
    Method target = AttributeMethods.forAnnotationType(targetAnnotation).get(targetAttributeName);
    if (target == null) {
      if (targetAnnotation == this.annotationType) {
        throw new AnnotationConfigurationException(String.format(
                "@AliasFor declaration on %s declares an alias for '%s' which is not present.",
                AttributeMethods.describe(attribute), targetAttributeName));
      }
      throw new AnnotationConfigurationException(String.format(
              "%s is declared as an @AliasFor nonexistent %s.",
              StringUtils.capitalize(AttributeMethods.describe(attribute)),
              AttributeMethods.describe(targetAnnotation, targetAttributeName)));
    }
    if (target.equals(attribute)) {
      throw new AnnotationConfigurationException(String.format(
              "@AliasFor declaration on %s points to itself. " +
                      "Specify 'annotation' to point to a same-named attribute on a meta-annotation.",
              AttributeMethods.describe(attribute)));
    }
    if (!isCompatibleReturnType(attribute.getReturnType(), target.getReturnType())) {
      throw new AnnotationConfigurationException(String.format(
              "Misconfigured aliases: %s and %s must declare the same return type.",
              AttributeMethods.describe(attribute),
              AttributeMethods.describe(target)));
    }
    if (isAliasPair(target) && checkAliasPair) {
      AliasFor targetAliasFor = target.getAnnotation(AliasFor.class);
      if (targetAliasFor != null) {
        Method mirror = resolveAliasTarget(target, targetAliasFor, false);
        if (!mirror.equals(attribute)) {
          throw new AnnotationConfigurationException(String.format(
                  "%s must be declared as an @AliasFor %s, not %s.",
                  StringUtils.capitalize(AttributeMethods.describe(target)),
                  AttributeMethods.describe(attribute), AttributeMethods.describe(mirror)));
        }
      }
    }
    return target;
  }

  private boolean isAliasPair(Method target) {
    return (this.annotationType == target.getDeclaringClass());
  }

  private boolean isCompatibleReturnType(Class<?> attributeType, Class<?> targetType) {
    return (attributeType == targetType || attributeType == targetType.getComponentType());
  }

  private void processAliases() {
    ArrayList<Method> aliases = new ArrayList<>();
    Method[] attributes = this.methods.attributes;
    for (int i = 0; i < attributes.length; i++) {
      aliases.clear();
      aliases.add(attributes[i]);
      collectAliases(aliases);
      if (aliases.size() > 1) {
        processAliases(i, aliases);
      }
    }
  }

  private void collectAliases(List<Method> aliases) {
    AnnotationTypeMapping mapping = this;
    while (mapping != null) {
      int size = aliases.size();
      for (int j = 0; j < size; j++) {
        List<Method> additional = mapping.aliasedBy.get(aliases.get(j));
        if (additional != null) {
          aliases.addAll(additional);
        }
      }
      mapping = mapping.source;
    }
  }

  private void processAliases(int attributeIndex, ArrayList<Method> aliases) {
    int rootAttributeIndex = getFirstRootAttributeIndex(aliases);
    AnnotationTypeMapping mapping = this;
    while (mapping != null) {
      if (rootAttributeIndex != -1 && mapping != this.root) {
        Method[] attributes = mapping.methods.attributes;
        for (int i = 0; i < attributes.length; i++) {
          if (aliases.contains(attributes[i])) {
            mapping.aliasMappings[i] = rootAttributeIndex;
          }
        }
      }
      mapping.mirrorSets.updateFrom(aliases);
      mapping.claimedAliases.addAll(aliases);
      if (mapping.annotation != null) {
        int[] resolvedMirrors = mapping.mirrorSets.resolve(
                null, mapping.annotation, AnnotationUtils::invokeAnnotationMethod);
        Method[] attributes = mapping.methods.attributes;
        for (int i = 0; i < attributes.length; i++) {
          if (aliases.contains(attributes[i])) {
            this.annotationValueMappings[attributeIndex] = resolvedMirrors[i];
            this.annotationValueSource[attributeIndex] = mapping;
          }
        }
      }
      mapping = mapping.source;
    }
  }

  private int getFirstRootAttributeIndex(ArrayList<Method> aliases) {
    Method[] rootAttributes = this.root.methods.attributes;
    for (int i = 0; i < rootAttributes.length; i++) {
      if (aliases.contains(rootAttributes[i])) {
        return i;
      }
    }
    return -1;
  }

  private void addConventionMappings() {
    if (this.distance == 0) {
      return;
    }
    int[] mappings = this.conventionMappings;
    Method[] attributes = this.methods.attributes;
    for (int i = 0; i < mappings.length; i++) {
      String name = attributes[i].getName();
      int mapped = this.root.methods.indexOf(name);
      if (!MergedAnnotation.VALUE.equals(name) && mapped != -1) {
        MirrorSets.MirrorSet mirrors = mirrorSets.getAssigned(i);
        mappings[i] = mapped;
        if (mirrors != null) {
          for (int j = 0; j < mirrors.size; j++) {
            mappings[mirrors.getAttributeIndex(j)] = mapped;
          }
        }
      }
    }
  }

  private void addConventionAnnotationValues() {
    Method[] attributes = this.methods.attributes;
    for (int i = 0; i < attributes.length; i++) {
      Method attribute = attributes[i];
      boolean isValueAttribute = MergedAnnotation.VALUE.equals(attribute.getName());
      AnnotationTypeMapping mapping = this;
      while (mapping != null && mapping.distance > 0) {
        int mapped = mapping.methods.indexOf(attribute.getName());
        if (mapped != -1 && isBetterConventionAnnotationValue(i, isValueAttribute, mapping)) {
          this.annotationValueMappings[i] = mapped;
          this.annotationValueSource[i] = mapping;
        }
        mapping = mapping.source;
      }
    }
  }

  private boolean isBetterConventionAnnotationValue(
          int index, boolean isValueAttribute, AnnotationTypeMapping mapping) {

    if (this.annotationValueMappings[index] == -1) {
      return true;
    }
    int existingDistance = this.annotationValueSource[index].distance;
    return !isValueAttribute && existingDistance > mapping.distance;
  }

  @SuppressWarnings("unchecked")
  private boolean computeSynthesizableFlag(Set<Class<? extends Annotation>> visitedAnnotationTypes) {
    visitedAnnotationTypes.add(this.annotationType);
    // Uses @AliasFor for local aliases?
    for (int index : this.aliasMappings) {
      if (index != -1) {
        return true;
      }
    }

    // Uses @AliasFor for attribute overrides in meta-annotations?
    if (!this.aliasedBy.isEmpty()) {
      return true;
    }

    // Uses convention-based attribute overrides in meta-annotations?
    for (int index : this.conventionMappings) {
      if (index != -1) {
        return true;
      }
    }

    // Has nested annotations or arrays of annotations that are synthesizable?
    if (methods.hasNestedAnnotation) {
      for (Method attribute : methods.attributes) {
        Class<?> type = attribute.getReturnType();
        if (type.isAnnotation() || (type.isArray() && type.getComponentType().isAnnotation())) {
          var annotationType = (Class<? extends Annotation>) (type.isAnnotation() ? type : type.getComponentType());
          // Ensure we have not yet visited the current nested annotation type, in order
          // to avoid infinite recursion for JVM languages other than Java that support
          // recursive annotation definitions.
          if (annotationType != this.annotationType) {
            if (visitedAnnotationTypes.add(annotationType)) {
              var mapping = AnnotationTypeMappings.forAnnotationType(annotationType, visitedAnnotationTypes).get(0);
              if (mapping.synthesizable) {
                return true;
              }
            }
          }
        }
      }
    }

    return false;
  }

  /**
   * Method called after all mappings have been set. At this point no further
   * lookups from child mappings will occur.
   */
  void afterAllMappingsSet() {
    validateAllAliasesClaimed();
    for (var mirrorSet : mirrorSets.mirrorSets) {
      validateMirrorSet(mirrorSet);
    }
    this.claimedAliases.clear();
  }

  private void validateAllAliasesClaimed() {
    for (Method attribute : methods.attributes) {
      AliasFor aliasFor = AnnotationsScanner.getDeclaredAnnotation(attribute, AliasFor.class);
      if (aliasFor != null && !claimedAliases.contains(attribute)) {
        Method target = resolveAliasTarget(attribute, aliasFor);
        throw new AnnotationConfigurationException(String.format(
                "@AliasFor declaration on %s declares an alias for %s which is not meta-present.",
                AttributeMethods.describe(attribute), AttributeMethods.describe(target)));
      }
    }
  }

  private void validateMirrorSet(MirrorSets.MirrorSet mirrorSet) {
    Method firstAttribute = mirrorSet.get(0);
    Object firstDefaultValue = firstAttribute.getDefaultValue();
    for (int i = 1; i <= mirrorSet.size - 1; i++) {
      Method mirrorAttribute = mirrorSet.get(i);
      Object mirrorDefaultValue = mirrorAttribute.getDefaultValue();
      if (firstDefaultValue == null || mirrorDefaultValue == null) {
        throw new AnnotationConfigurationException(String.format(
                "Misconfigured aliases: %s and %s must declare default values.",
                AttributeMethods.describe(firstAttribute), AttributeMethods.describe(mirrorAttribute)));
      }
      if (!ObjectUtils.nullSafeEquals(firstDefaultValue, mirrorDefaultValue)) {
        throw new AnnotationConfigurationException(String.format(
                "Misconfigured aliases: %s and %s must declare the same default value.",
                AttributeMethods.describe(firstAttribute), AttributeMethods.describe(mirrorAttribute)));
      }
    }
  }

  /**
   * Get the type of the mapped annotation.
   *
   * @return the annotation type
   */
  Class<? extends Annotation> getAnnotationType() {
    return this.annotationType;
  }

  /**
   * Get the related index of an alias mapped attribute, or {@code -1} if
   * there is no mapping. The resulting value is the index of the attribute on
   * the root annotation that can be invoked in order to obtain the actual
   * value.
   *
   * @param attributeIndex the attribute index of the source attribute
   * @return the mapped attribute index or {@code -1}
   */
  int getAliasMapping(int attributeIndex) {
    return this.aliasMappings[attributeIndex];
  }

  /**
   * Get the related index of a convention mapped attribute, or {@code -1}
   * if there is no mapping. The resulting value is the index of the attribute
   * on the root annotation that can be invoked in order to obtain the actual
   * value.
   *
   * @param attributeIndex the attribute index of the source attribute
   * @return the mapped attribute index or {@code -1}
   */
  int getConventionMapping(int attributeIndex) {
    return this.conventionMappings[attributeIndex];
  }

  /**
   * Get a mapped attribute value from the most suitable
   * {@link #annotation meta-annotation}.
   * <p>The resulting value is obtained from the closest meta-annotation,
   * taking into consideration both convention and alias based mapping rules.
   * For root mappings, this method will always return {@code null}.
   *
   * @param attributeIndex the attribute index of the source attribute
   * @param metaAnnotationsOnly if only meta annotations should be considered.
   * If this parameter is {@code false} then aliases within the annotation will
   * also be considered.
   * @return the mapped annotation value, or {@code null}
   */
  @Nullable
  Object getMappedAnnotationValue(int attributeIndex, boolean metaAnnotationsOnly) {
    int mappedIndex = this.annotationValueMappings[attributeIndex];
    if (mappedIndex == -1) {
      return null;
    }
    AnnotationTypeMapping source = this.annotationValueSource[attributeIndex];
    if (source == this && metaAnnotationsOnly) {
      return null;
    }
    return AnnotationUtils.invokeAnnotationMethod(source.methods.get(mappedIndex), source.annotation);
  }

  /**
   * Determine if the specified value is equivalent to the default value of the
   * attribute at the given index.
   *
   * @param attributeIndex the attribute index of the source attribute
   * @param value the value to check
   * @param valueExtractor the value extractor used to extract values from any
   * nested annotations
   * @return {@code true} if the value is equivalent to the default value
   */
  boolean isEquivalentToDefaultValue(int attributeIndex, Object value, ValueExtractor valueExtractor) {

    Method attribute = this.methods.get(attributeIndex);
    return isEquivalentToDefaultValue(attribute, value, valueExtractor);
  }

  private static int[] filledIntArray(int size) {
    if (size == 0) {
      return EMPTY_INT_ARRAY;
    }
    int[] array = new int[size];
    Arrays.fill(array, -1);
    return array;
  }

  private static boolean isEquivalentToDefaultValue(
          Method attribute, Object value, ValueExtractor valueExtractor) {

    return areEquivalent(attribute.getDefaultValue(), value, valueExtractor);
  }

  private static boolean areEquivalent(@Nullable Object value, @Nullable Object extractedValue, ValueExtractor valueExtractor) {
    if (ObjectUtils.nullSafeEquals(value, extractedValue)) {
      return true;
    }
    if (value instanceof Class && extractedValue instanceof String) {
      return areEquivalent((Class<?>) value, (String) extractedValue);
    }
    if (value instanceof Class[] && extractedValue instanceof String[]) {
      return areEquivalent((Class<?>[]) value, (String[]) extractedValue);
    }
    if (value instanceof Annotation) {
      return areEquivalent((Annotation) value, extractedValue, valueExtractor);
    }
    return false;
  }

  private static boolean areEquivalent(Class<?>[] value, String[] extractedValue) {
    if (value.length != extractedValue.length) {
      return false;
    }
    for (int i = 0; i < value.length; i++) {
      if (!areEquivalent(value[i], extractedValue[i])) {
        return false;
      }
    }
    return true;
  }

  private static boolean areEquivalent(Class<?> value, String extractedValue) {
    return value.getName().equals(extractedValue);
  }

  private static boolean areEquivalent(Annotation annotation, @Nullable Object extractedValue, ValueExtractor valueExtractor) {
    AttributeMethods methods = AttributeMethods.forAnnotationType(annotation.annotationType());
    for (Method attribute : methods.attributes) {
      Object value1 = AnnotationUtils.invokeAnnotationMethod(attribute, annotation);
      Object value2;
      if (extractedValue instanceof TypeMappedAnnotation<?> typeMappedAnno) {
        value2 = typeMappedAnno.getValue(attribute.getName());
      }
      else {
        value2 = valueExtractor.extract(attribute, extractedValue);
      }
      if (!areEquivalent(value1, value2, valueExtractor)) {
        return false;
      }
    }
    return true;
  }

  /**
   * A collection of {@link MirrorSet} instances that provides details of all
   * defined mirrors.
   */
  final class MirrorSets {

    public MirrorSet[] mirrorSets;

    public final MirrorSet[] assigned;

    MirrorSets() {
      this.assigned = methods.size() > 0 ? new MirrorSet[methods.size()] : EMPTY_MIRROR_SETS;
      this.mirrorSets = EMPTY_MIRROR_SETS;
    }

    void updateFrom(Collection<Method> aliases) {
      MirrorSet mirrorSet = null;
      int size = 0;
      int last = -1;
      Method[] attributes = methods.attributes;
      MirrorSet[] assigned = this.assigned;
      for (int i = 0; i < attributes.length; i++) {
        Method attribute = attributes[i];
        if (aliases.contains(attribute)) {
          size++;
          if (size > 1) {
            if (mirrorSet == null) {
              mirrorSet = new MirrorSet();
              assigned[last] = mirrorSet;
            }
            assigned[i] = mirrorSet;
          }
          last = i;
        }
      }
      if (mirrorSet != null) {
        mirrorSet.update();
        LinkedHashSet<MirrorSet> unique = new LinkedHashSet<>();
        CollectionUtils.addAll(unique, assigned);
        unique.remove(null);
        this.mirrorSets = unique.toArray(EMPTY_MIRROR_SETS);
      }
    }

    int size() {
      return mirrorSets.length;
    }

    MirrorSet get(int index) {
      return mirrorSets[index];
    }

    @Nullable
    MirrorSet getAssigned(int attributeIndex) {
      return assigned[attributeIndex];
    }

    int[] resolve(@Nullable Object source, @Nullable Object annotation, ValueExtractor valueExtractor) {
      if (methods.size() == 0) {
        return EMPTY_INT_ARRAY;
      }
      int[] result = new int[methods.size()];
      for (int i = 0; i < result.length; i++) {
        result[i] = i;
      }
      for (MirrorSet mirrorSet : mirrorSets) {
        int resolved = mirrorSet.resolve(source, annotation, valueExtractor);
        for (int j = 0; j < mirrorSet.size; j++) {
          result[mirrorSet.indexes[j]] = resolved;
        }
      }
      return result;
    }

    /**
     * A single set of mirror attributes.
     */
    final class MirrorSet {

      public int size;
      public final int[] indexes = new int[methods.size()];

      void update() {
        this.size = 0;
        Arrays.fill(this.indexes, -1);
        MirrorSet[] assigned = MirrorSets.this.assigned;
        for (int i = 0; i < assigned.length; i++) {
          if (assigned[i] == this) {
            this.indexes[this.size] = i;
            this.size++;
          }
        }
      }

      <A> int resolve(@Nullable Object source, @Nullable A annotation, ValueExtractor valueExtractor) {
        int result = -1;
        Object lastValue = null;
        int[] indexes = this.indexes;
        Method[] attributes = methods.attributes;
        for (int i = 0; i < this.size; i++) {
          Method attribute = attributes[indexes[i]];
          Object value = valueExtractor.extract(attribute, annotation);
          boolean isDefaultValue = (value == null || isEquivalentToDefaultValue(attribute, value, valueExtractor));
          if (isDefaultValue || ObjectUtils.nullSafeEquals(lastValue, value)) {
            if (result == -1) {
              result = indexes[i];
            }
            continue;
          }
          if (lastValue != null && !ObjectUtils.nullSafeEquals(lastValue, value)) {
            String on = (source != null) ? " declared on " + source : "";
            throw new AnnotationConfigurationException(String.format(
                    "Different @AliasFor mirror values for annotation [%s]%s; attribute '%s' " +
                            "and its alias '%s' are declared with values of [%s] and [%s].",
                    annotationType.getName(), on,
                    attributes[result].getName(),
                    attribute.getName(),
                    ObjectUtils.nullSafeToString(lastValue),
                    ObjectUtils.nullSafeToString(value)));
          }
          result = indexes[i];
          lastValue = value;
        }
        return result;
      }

      Method get(int index) {
        int attributeIndex = this.indexes[index];
        return methods.get(attributeIndex);
      }

      int getAttributeIndex(int index) {
        return this.indexes[index];
      }
    }
  }

}
