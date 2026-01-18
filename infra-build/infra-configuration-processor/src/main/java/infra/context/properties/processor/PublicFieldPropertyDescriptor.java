/*
 * Copyright 2012-present the original author or authors.
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

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.context.properties.processor;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;

import infra.context.properties.processor.metadata.ItemDeprecation;

/**
 * for public field
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2024/3/13 23:35
 */
class PublicFieldPropertyDescriptor extends PropertyDescriptor {

  private final VariableElement field;

  PublicFieldPropertyDescriptor(String name, TypeMirror type, TypeElement declaringElement, ExecutableElement getter, VariableElement field) {
    super(name, type, declaringElement, getter);
    this.field = field;
  }

  @Override
  protected boolean isProperty(MetadataGenerationEnvironment env) {
    return !env.isExcluded(getType()) && !field.getModifiers().contains(Modifier.STATIC);
  }

  @Override
  protected boolean isMarkedAsNested(MetadataGenerationEnvironment environment) {
    return environment.getNestedConfigurationPropertyAnnotation(field) != null;
  }

  @Override
  protected String resolveDescription(MetadataGenerationEnvironment environment) {
    return environment.getTypeUtils().getJavaDoc(this.field);
  }

  @Override
  protected Object resolveDefaultValue(MetadataGenerationEnvironment environment) {
    return environment.getFieldDefaultValue(getDeclaringElement(), field);
  }

  @Override
  protected ItemDeprecation resolveItemDeprecation(MetadataGenerationEnvironment environment) {
    return resolveItemDeprecation(environment, getGetter(), this.field);
  }

}
