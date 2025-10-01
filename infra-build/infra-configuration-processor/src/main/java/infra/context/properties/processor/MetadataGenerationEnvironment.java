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

package infra.context.properties.processor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;

import infra.context.properties.processor.fieldvalues.FieldValuesParser;
import infra.context.properties.processor.fieldvalues.javac.JavaCompilerFieldValuesParser;
import infra.context.properties.processor.metadata.ItemDeprecation;

/**
 * Provide utilities to detect and validate configuration properties.
 *
 * @author Stephane Nicoll
 * @author Scott Frederick
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class MetadataGenerationEnvironment {

  private static final String NULLABLE_ANNOTATION = "org.jspecify.annotations.Nullable";

  private static final Set<String> TYPE_EXCLUDES = Set.of(
          "com.zaxxer.hikari.IConnectionCustomizer",
          "groovy.lang.MetaClass", "groovy.text.markup.MarkupTemplateEngine",
          "java.io.Writer", "java.io.PrintWriter",
          "java.lang.ClassLoader", "java.util.concurrent.ThreadFactory",
          "jakarta.jms.XAConnectionFactory", "javax.sql.DataSource", "javax.sql.XADataSource",
          "org.apache.tomcat.jdbc.pool.PoolConfiguration", "org.apache.tomcat.jdbc.pool.Validator",
          "org.flywaydb.core.api.callback.FlywayCallback", "org.flywaydb.core.api.resolver.MigrationResolver");

  private final TypeUtils typeUtils;

  private final Elements elements;

  private final Messager messager;

  private final FieldValuesParser fieldValuesParser;

  private final Map<TypeElement, Map<String, Object>> defaultValues = new HashMap<>();

  private final String configurationPropertiesAnnotation;

  private final String nestedConfigurationPropertyAnnotation;

  private final String deprecatedConfigurationPropertyAnnotation;

  private final String constructorBindingAnnotation;

  private final String defaultValueAnnotation;

  private final Set<String> endpointAnnotations;

  private final String readOperationAnnotation;

  private final String nameAnnotation;

  private final String autowiredAnnotation;

  MetadataGenerationEnvironment(ProcessingEnvironment environment, String configurationPropertiesAnnotation,
          String nestedConfigurationPropertyAnnotation, String deprecatedConfigurationPropertyAnnotation,
          String constructorBindingAnnotation, String autowiredAnnotation, String defaultValueAnnotation,
          Set<String> endpointAnnotations, String readOperationAnnotation, String nameAnnotation) {
    this.typeUtils = new TypeUtils(environment);
    this.elements = environment.getElementUtils();
    this.messager = environment.getMessager();
    this.fieldValuesParser = resolveFieldValuesParser(environment);
    this.configurationPropertiesAnnotation = configurationPropertiesAnnotation;
    this.nestedConfigurationPropertyAnnotation = nestedConfigurationPropertyAnnotation;
    this.deprecatedConfigurationPropertyAnnotation = deprecatedConfigurationPropertyAnnotation;
    this.constructorBindingAnnotation = constructorBindingAnnotation;
    this.autowiredAnnotation = autowiredAnnotation;
    this.defaultValueAnnotation = defaultValueAnnotation;
    this.endpointAnnotations = endpointAnnotations;
    this.readOperationAnnotation = readOperationAnnotation;
    this.nameAnnotation = nameAnnotation;
  }

  private static FieldValuesParser resolveFieldValuesParser(ProcessingEnvironment env) {
    try {
      return new JavaCompilerFieldValuesParser(env);
    }
    catch (Throwable ex) {
      return FieldValuesParser.NONE;
    }
  }

  TypeUtils getTypeUtils() {
    return this.typeUtils;
  }

  Messager getMessager() {
    return this.messager;
  }

  /**
   * Return the default value of the field with the specified {@code name}.
   *
   * @param type the type to consider
   * @param name the name of the field
   * @return the default value or {@code null} if the field does not exist or no default
   * value has been detected
   */
  Object getFieldDefaultValue(TypeElement type, String name) {
    return this.defaultValues.computeIfAbsent(type, this::resolveFieldValues).get(name);
  }

  boolean isExcluded(TypeMirror type) {
    if (type == null) {
      return false;
    }
    String typeName = type.toString();
    if (typeName.endsWith("[]")) {
      typeName = typeName.substring(0, typeName.length() - 2);
    }
    return TYPE_EXCLUDES.contains(typeName);
  }

  boolean isDeprecated(Element element) {
    if (isElementDeprecated(element)) {
      return true;
    }
    if (element instanceof VariableElement || element instanceof ExecutableElement) {
      return isElementDeprecated(element.getEnclosingElement());
    }
    return false;
  }

  ItemDeprecation resolveItemDeprecation(Element element) {
    AnnotationMirror annotation = getAnnotation(element, this.deprecatedConfigurationPropertyAnnotation);
    String reason = null;
    String replacement = null;
    String since = null;
    if (annotation != null) {
      reason = getAnnotationElementStringValue(annotation, "reason");
      replacement = getAnnotationElementStringValue(annotation, "replacement");
      since = getAnnotationElementStringValue(annotation, "since");
    }
    return new ItemDeprecation(reason, replacement, since);
  }

  boolean hasConstructorBindingAnnotation(ExecutableElement element) {
    return hasAnnotation(element, this.constructorBindingAnnotation, true);
  }

  boolean hasAutowiredAnnotation(ExecutableElement element) {
    return hasAnnotation(element, this.autowiredAnnotation);
  }

  boolean hasAnnotation(Element element, String type) {
    return hasAnnotation(element, type, false);
  }

  boolean hasAnnotation(Element element, String type, boolean considerMetaAnnotations) {
    if (element != null) {
      for (AnnotationMirror annotation : element.getAnnotationMirrors()) {
        if (type.equals(annotation.getAnnotationType().toString())) {
          return true;
        }
      }
      if (considerMetaAnnotations) {
        Set<Element> seen = new HashSet<>();
        for (AnnotationMirror annotation : element.getAnnotationMirrors()) {
          if (hasMetaAnnotation(annotation.getAnnotationType().asElement(), type, seen)) {
            return true;
          }
        }

      }
    }
    return false;
  }

  private boolean hasMetaAnnotation(Element annotationElement, String type, Set<Element> seen) {
    if (seen.add(annotationElement)) {
      for (AnnotationMirror annotation : annotationElement.getAnnotationMirrors()) {
        DeclaredType annotationType = annotation.getAnnotationType();
        if (type.equals(annotationType.toString())
                || hasMetaAnnotation(annotationType.asElement(), type, seen)) {
          return true;
        }
      }
    }
    return false;
  }

  AnnotationMirror getAnnotation(Element element, String type) {
    if (element != null) {
      for (AnnotationMirror annotation : element.getAnnotationMirrors()) {
        if (type.equals(annotation.getAnnotationType().toString())) {
          return annotation;
        }
      }
    }
    return null;
  }

  /**
   * Collect the annotations that are annotated or meta-annotated with the specified
   * {@link TypeElement annotation}.
   *
   * @param element the element to inspect
   * @param annotationType the annotation to discover
   * @return the annotations that are annotated or meta-annotated with this annotation
   */
  List<Element> getElementsAnnotatedOrMetaAnnotatedWith(Element element, TypeElement annotationType) {
    LinkedList<Element> stack = new LinkedList<>();
    stack.push(element);
    collectElementsAnnotatedOrMetaAnnotatedWith(annotationType, stack);
    stack.removeFirst();
    return Collections.unmodifiableList(stack);
  }

  private boolean collectElementsAnnotatedOrMetaAnnotatedWith(TypeElement annotationType, LinkedList<Element> stack) {
    Element element = stack.peekLast();
    for (AnnotationMirror annotation : this.elements.getAllAnnotationMirrors(element)) {
      Element annotationElement = annotation.getAnnotationType().asElement();
      if (!stack.contains(annotationElement)) {
        stack.addLast(annotationElement);
        if (annotationElement.equals(annotationType)) {
          return true;
        }
        if (!collectElementsAnnotatedOrMetaAnnotatedWith(annotationType, stack)) {
          stack.removeLast();
        }
      }
    }
    return false;
  }

  Map<String, Object> getAnnotationElementValues(AnnotationMirror annotation) {
    Map<String, Object> values = new LinkedHashMap<>();
    annotation.getElementValues()
            .forEach((name, value) -> values.put(name.getSimpleName().toString(), getAnnotationValue(value)));
    return values;
  }

  String getAnnotationElementStringValue(AnnotationMirror annotation, String name) {
    return annotation.getElementValues()
            .entrySet()
            .stream()
            .filter((element) -> element.getKey().getSimpleName().toString().equals(name))
            .map((element) -> asString(getAnnotationValue(element.getValue())))
            .findFirst()
            .orElse(null);
  }

  private Object getAnnotationValue(AnnotationValue annotationValue) {
    Object value = annotationValue.getValue();
    if (value instanceof List) {
      List<Object> values = new ArrayList<>();
      ((List<?>) value).forEach((v) -> values.add(((AnnotationValue) v).getValue()));
      return values;
    }
    return value;
  }

  private String asString(Object value) {
    return (value == null || value.toString().isEmpty()) ? null : (String) value;
  }

  TypeElement getConfigurationPropertiesAnnotationElement() {
    return this.elements.getTypeElement(this.configurationPropertiesAnnotation);
  }

  AnnotationMirror getConfigurationPropertiesAnnotation(Element element) {
    return getAnnotation(element, this.configurationPropertiesAnnotation);
  }

  AnnotationMirror getNestedConfigurationPropertyAnnotation(Element element) {
    return getAnnotation(element, this.nestedConfigurationPropertyAnnotation);
  }

  AnnotationMirror getDefaultValueAnnotation(Element element) {
    return getAnnotation(element, this.defaultValueAnnotation);
  }

  Set<TypeElement> getEndpointAnnotationElements() {
    return this.endpointAnnotations.stream()
            .map(this.elements::getTypeElement)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
  }

  AnnotationMirror getReadOperationAnnotation(Element element) {
    return getAnnotation(element, this.readOperationAnnotation);
  }

  AnnotationMirror getNameAnnotation(Element element) {
    return getAnnotation(element, this.nameAnnotation);
  }

  boolean hasNullableAnnotation(Element element) {
    return getAnnotation(element, NULLABLE_ANNOTATION) != null;
  }

  private boolean isElementDeprecated(Element element) {
    return hasAnnotation(element, "java.lang.Deprecated")
            || hasAnnotation(element, this.deprecatedConfigurationPropertyAnnotation);
  }

  private Map<String, Object> resolveFieldValues(TypeElement element) {
    Map<String, Object> values = new LinkedHashMap<>();
    resolveFieldValuesFor(values, element);
    return values;
  }

  private void resolveFieldValuesFor(Map<String, Object> values, TypeElement element) {
    try {
      this.fieldValuesParser.getFieldValues(element).forEach((name, value) -> {
        if (!values.containsKey(name)) {
          values.put(name, value);
        }
      });
    }
    catch (Exception ex) {
      // continue
    }
    Element superType = this.typeUtils.asElement(element.getSuperclass());
    if (superType instanceof TypeElement && superType.asType().getKind() != TypeKind.NONE) {
      resolveFieldValuesFor(values, (TypeElement) superType);
    }
  }

}
