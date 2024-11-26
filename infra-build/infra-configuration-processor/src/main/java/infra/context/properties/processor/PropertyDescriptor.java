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

package infra.context.properties.processor;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;

import infra.context.properties.processor.metadata.ConfigurationMetadata;
import infra.context.properties.processor.metadata.ItemDeprecation;
import infra.context.properties.processor.metadata.ItemMetadata;

/**
 * Description of a property that can be candidate for metadata generation.
 *
 * @param <S> the type of the source element that determines the property
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
abstract class PropertyDescriptor<S extends Element> {

  private final TypeElement ownerElement;

  private final ExecutableElement factoryMethod;

  private final S source;

  private final String name;

  private final TypeMirror type;

  private final VariableElement field;

  private final ExecutableElement getter;

  private final ExecutableElement setter;

  protected PropertyDescriptor(TypeElement ownerElement, ExecutableElement factoryMethod, S source, String name,
          TypeMirror type, VariableElement field, ExecutableElement getter, ExecutableElement setter) {
    this.ownerElement = ownerElement;
    this.factoryMethod = factoryMethod;
    this.source = source;
    this.name = name;
    this.type = type;
    this.field = field;
    this.getter = getter;
    this.setter = setter;
  }

  TypeElement getOwnerElement() {
    return this.ownerElement;
  }

  ExecutableElement getFactoryMethod() {
    return this.factoryMethod;
  }

  S getSource() {
    return this.source;
  }

  String getName() {
    return this.name;
  }

  TypeMirror getType() {
    return this.type;
  }

  VariableElement getField() {
    return this.field;
  }

  ExecutableElement getGetter() {
    return this.getter;
  }

  ExecutableElement getSetter() {
    return this.setter;
  }

  protected abstract boolean isProperty(MetadataGenerationEnvironment environment);

  protected abstract Object resolveDefaultValue(MetadataGenerationEnvironment environment);

  protected ItemDeprecation resolveItemDeprecation(MetadataGenerationEnvironment environment) {
    boolean deprecated = environment.isDeprecated(getGetter()) || environment.isDeprecated(getSetter())
            || environment.isDeprecated(getField()) || environment.isDeprecated(getFactoryMethod());
    return deprecated ? environment.resolveItemDeprecation(getGetter()) : null;
  }

  protected boolean isNested(MetadataGenerationEnvironment environment) {
    Element typeElement = environment.getTypeUtils().asElement(getType());
    if (!(typeElement instanceof TypeElement) || typeElement.getKind() == ElementKind.ENUM) {
      return false;
    }
    if (environment.getConfigurationPropertiesAnnotation(getGetter()) != null) {
      return false;
    }
    if (environment.getNestedConfigurationPropertyAnnotation(getField()) != null) {
      return true;
    }
    if (isCyclePresent(typeElement, getOwnerElement())) {
      return false;
    }
    return isParentTheSame(environment, typeElement, getOwnerElement());
  }

  ItemMetadata resolveItemMetadata(String prefix, MetadataGenerationEnvironment environment) {
    if (isNested(environment)) {
      return resolveItemMetadataGroup(prefix, environment);
    }
    else if (isProperty(environment)) {
      return resolveItemMetadataProperty(prefix, environment);
    }
    return null;
  }

  private ItemMetadata resolveItemMetadataProperty(String prefix, MetadataGenerationEnvironment environment) {
    String dataType = resolveType(environment);
    String ownerType = environment.getTypeUtils().getQualifiedName(getOwnerElement());
    String description = resolveDescription(environment);
    Object defaultValue = resolveDefaultValue(environment);
    ItemDeprecation deprecation = resolveItemDeprecation(environment);
    return ItemMetadata.newProperty(prefix, getName(), dataType, ownerType, null, description, defaultValue,
            deprecation);
  }

  private ItemMetadata resolveItemMetadataGroup(String prefix, MetadataGenerationEnvironment environment) {
    Element propertyElement = environment.getTypeUtils().asElement(getType());
    String nestedPrefix = ConfigurationMetadata.nestedPrefix(prefix, getName());
    String dataType = environment.getTypeUtils().getQualifiedName(propertyElement);
    String ownerType = environment.getTypeUtils().getQualifiedName(getOwnerElement());
    String sourceMethod = (getGetter() != null) ? getGetter().toString() : null;
    return ItemMetadata.newGroup(nestedPrefix, dataType, ownerType, sourceMethod);
  }

  private String resolveType(MetadataGenerationEnvironment environment) {
    return environment.getTypeUtils().getType(getOwnerElement(), getType());
  }

  private String resolveDescription(MetadataGenerationEnvironment environment) {
    return environment.getTypeUtils().getJavaDoc(getField());
  }

  private boolean isCyclePresent(Element returnType, Element element) {
    if (!(element.getEnclosingElement() instanceof TypeElement)) {
      return false;
    }
    if (element.getEnclosingElement().equals(returnType)) {
      return true;
    }
    return isCyclePresent(returnType, element.getEnclosingElement());
  }

  private boolean isParentTheSame(MetadataGenerationEnvironment environment, Element returnType,
          TypeElement element) {
    if (returnType == null || element == null) {
      return false;
    }
    returnType = getTopLevelType(returnType);
    Element candidate = element;
    while (candidate != null && candidate instanceof TypeElement) {
      if (returnType.equals(getTopLevelType(candidate))) {
        return true;
      }
      candidate = environment.getTypeUtils().asElement(((TypeElement) candidate).getSuperclass());
    }
    return false;
  }

  private Element getTopLevelType(Element element) {
    if (!(element.getEnclosingElement() instanceof TypeElement)) {
      return element;
    }
    return getTopLevelType(element.getEnclosingElement());
  }

}
