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

package cn.taketoday.context.properties.processor;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;

/**
 * for public field
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2024/3/13 23:35
 */
class PublicFieldPropertyDescriptor extends PropertyDescriptor<VariableElement> {

  PublicFieldPropertyDescriptor(TypeElement ownerElement, ExecutableElement factoryMethod, String name, TypeMirror type, VariableElement field) {
    super(ownerElement, factoryMethod, null, name, type, field, null, null);
  }

  @Override
  protected boolean isProperty(MetadataGenerationEnvironment env) {
    return !env.isExcluded(getType());
  }

  @Override
  protected Object resolveDefaultValue(MetadataGenerationEnvironment environment) {
    return environment.getFieldDefaultValue(getOwnerElement(), getName());
  }

}
