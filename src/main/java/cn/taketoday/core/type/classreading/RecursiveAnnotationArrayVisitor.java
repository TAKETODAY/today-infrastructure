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

package cn.taketoday.core.type.classreading;

import cn.taketoday.core.annotation.AnnotationAttributes;
import cn.taketoday.core.bytecode.AnnotationVisitor;
import cn.taketoday.core.bytecode.Type;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ObjectUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

/**
 * {@link AnnotationVisitor} to recursively visit annotation arrays.
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @since 4.0
 * @deprecated this class and related classes in this
 * package have been replaced by {@link SimpleAnnotationMetadataReadingVisitor}
 * and related classes for internal use within the framework.
 */
@Deprecated
class RecursiveAnnotationArrayVisitor extends AbstractRecursiveAnnotationVisitor {

  private final String attributeName;

  private final List<AnnotationAttributes> allNestedAttributes = new ArrayList<>();

  public RecursiveAnnotationArrayVisitor(
          String attributeName, AnnotationAttributes attributes, @Nullable ClassLoader classLoader) {

    super(classLoader, attributes);
    this.attributeName = attributeName;
  }

  @Override
  public void visit(String attributeName, Object attributeValue) {
    Object newValue = attributeValue;
    Object existingValue = this.attributes.get(this.attributeName);
    if (existingValue != null) {
      newValue = ObjectUtils.addObjectToArray((Object[]) existingValue, newValue);
    }
    else {
      Class<?> arrayClass = newValue.getClass();
      if (Enum.class.isAssignableFrom(arrayClass)) {
        while (arrayClass.getSuperclass() != null && !arrayClass.isEnum()) {
          arrayClass = arrayClass.getSuperclass();
        }
      }
      Object[] newArray = (Object[]) Array.newInstance(arrayClass, 1);
      newArray[0] = newValue;
      newValue = newArray;
    }
    this.attributes.put(this.attributeName, newValue);
  }

  @Override
  public AnnotationVisitor visitAnnotation(String attributeName, String asmTypeDescriptor) {
    String annotationType = Type.fromDescriptor(asmTypeDescriptor).getClassName();
    AnnotationAttributes nestedAttributes = new AnnotationAttributes(annotationType, this.classLoader);
    this.allNestedAttributes.add(nestedAttributes);
    return new RecursiveAnnotationAttributesVisitor(annotationType, nestedAttributes, this.classLoader);
  }

  @Override
  public void visitEnd() {
    if (!this.allNestedAttributes.isEmpty()) {
      this.attributes.put(this.attributeName, this.allNestedAttributes.toArray(new AnnotationAttributes[0]));
    }
    else if (!this.attributes.containsKey(this.attributeName)) {
      Class<? extends Annotation> annotationType = this.attributes.annotationType();
      if (annotationType != null) {
        try {
          Class<?> attributeType = annotationType.getMethod(this.attributeName).getReturnType();
          if (attributeType.isArray()) {
            Class<?> elementType = attributeType.getComponentType();
            if (elementType.isAnnotation()) {
              elementType = AnnotationAttributes.class;
            }
            this.attributes.put(this.attributeName, Array.newInstance(elementType, 0));
          }
        }
        catch (NoSuchMethodException ex) {
          // Corresponding attribute method not found: cannot expose empty array.
        }
      }
    }
  }

}
