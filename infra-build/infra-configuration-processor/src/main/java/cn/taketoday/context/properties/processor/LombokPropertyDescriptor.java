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

package cn.taketoday.context.properties.processor;

import java.util.Map;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;

import cn.taketoday.context.properties.processor.metadata.ItemDeprecation;

/**
 * A {@link PropertyDescriptor} for a Lombok field.
 *
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class LombokPropertyDescriptor extends PropertyDescriptor<VariableElement> {

  private static final String LOMBOK_DATA_ANNOTATION = "lombok.Data";

  private static final String LOMBOK_VALUE_ANNOTATION = "lombok.Value";

  private static final String LOMBOK_GETTER_ANNOTATION = "lombok.Getter";

  private static final String LOMBOK_SETTER_ANNOTATION = "lombok.Setter";

  private static final String LOMBOK_ACCESS_LEVEL_PUBLIC = "PUBLIC";

  LombokPropertyDescriptor(TypeElement typeElement, ExecutableElement factoryMethod, VariableElement field,
          String name, TypeMirror type, ExecutableElement getter, ExecutableElement setter) {
    super(typeElement, factoryMethod, field, name, type, field, getter, setter);
  }

  @Override
  protected boolean isProperty(MetadataGenerationEnvironment env) {
    if (!hasLombokPublicAccessor(env, true)) {
      return false;
    }
    boolean isCollection = env.getTypeUtils().isCollectionOrMap(getType());
    return !env.isExcluded(getType()) && (hasSetter(env) || isCollection);
  }

  @Override
  protected Object resolveDefaultValue(MetadataGenerationEnvironment environment) {
    return environment.getFieldDefaultValue(getOwnerElement(), getName());
  }

  @Override
  protected boolean isNested(MetadataGenerationEnvironment environment) {
    if (!hasLombokPublicAccessor(environment, true)) {
      return false;
    }
    return super.isNested(environment);
  }

  @Override
  protected ItemDeprecation resolveItemDeprecation(MetadataGenerationEnvironment environment) {
    boolean deprecated = environment.isDeprecated(getField()) || environment.isDeprecated(getGetter())
            || environment.isDeprecated(getFactoryMethod());
    return deprecated ? environment.resolveItemDeprecation(getGetter()) : null;
  }

  private boolean hasSetter(MetadataGenerationEnvironment env) {
    boolean nonFinalPublicField = !getField().getModifiers().contains(Modifier.FINAL)
            && hasLombokPublicAccessor(env, false);
    return getSetter() != null || nonFinalPublicField;
  }

  /**
   * Determine if the current {@link #getField() field} defines a public accessor using
   * lombok annotations.
   *
   * @param env the {@link MetadataGenerationEnvironment}
   * @param getter {@code true} to look for the read accessor, {@code false} for the
   * write accessor
   * @return {@code true} if this field has a public accessor of the specified type
   */
  private boolean hasLombokPublicAccessor(MetadataGenerationEnvironment env, boolean getter) {
    String annotation = (getter ? LOMBOK_GETTER_ANNOTATION : LOMBOK_SETTER_ANNOTATION);
    AnnotationMirror lombokMethodAnnotationOnField = env.getAnnotation(getField(), annotation);
    if (lombokMethodAnnotationOnField != null) {
      return isAccessLevelPublic(env, lombokMethodAnnotationOnField);
    }
    AnnotationMirror lombokMethodAnnotationOnElement = env.getAnnotation(getOwnerElement(), annotation);
    if (lombokMethodAnnotationOnElement != null) {
      return isAccessLevelPublic(env, lombokMethodAnnotationOnElement);
    }
    return (env.hasAnnotation(getOwnerElement(), LOMBOK_DATA_ANNOTATION)
            || env.hasAnnotation(getOwnerElement(), LOMBOK_VALUE_ANNOTATION));
  }

  private boolean isAccessLevelPublic(MetadataGenerationEnvironment env, AnnotationMirror lombokAnnotation) {
    Map<String, Object> values = env.getAnnotationElementValues(lombokAnnotation);
    Object value = values.get("value");
    return (value == null || value.toString().equals(LOMBOK_ACCESS_LEVEL_PUBLIC));
  }

}
