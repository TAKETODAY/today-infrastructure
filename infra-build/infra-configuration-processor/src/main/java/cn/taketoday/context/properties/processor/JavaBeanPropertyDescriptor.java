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

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;

/**
 * A {@link PropertyDescriptor} for a standard JavaBean property.
 *
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class JavaBeanPropertyDescriptor extends PropertyDescriptor<ExecutableElement> {

  JavaBeanPropertyDescriptor(TypeElement ownerElement, ExecutableElement factoryMethod, ExecutableElement getter,
          String name, TypeMirror type, VariableElement field, ExecutableElement setter) {
    super(ownerElement, factoryMethod, getter, name, type, field, getter, setter);
  }

  @Override
  protected boolean isProperty(MetadataGenerationEnvironment env) {
    boolean isCollection = env.getTypeUtils().isCollectionOrMap(getType());
    return !env.isExcluded(getType()) && getGetter() != null && (getSetter() != null || isCollection);
  }

  @Override
  protected Object resolveDefaultValue(MetadataGenerationEnvironment environment) {
    return environment.getFieldDefaultValue(getOwnerElement(), getName());
  }

}
