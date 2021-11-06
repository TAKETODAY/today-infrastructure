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

import java.lang.reflect.Field;

import cn.taketoday.core.annotation.AnnotationAttributes;
import cn.taketoday.core.bytecode.AnnotationVisitor;
import cn.taketoday.core.bytecode.Type;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.ReflectionUtils;

/**
 * {@link AnnotationVisitor} to recursively visit annotations.
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @author Phillip Webb
 * @author Sam Brannen
 * @since 4.0
 * @deprecated this class and related classes in this
 * package have been replaced by {@link SimpleAnnotationMetadataReadingVisitor}
 * and related classes for internal use within the framework.
 */
@Deprecated
abstract class AbstractRecursiveAnnotationVisitor extends AnnotationVisitor {

  protected final Logger logger = LoggerFactory.getLogger(getClass());

  protected final AnnotationAttributes attributes;

  @Nullable
  protected final ClassLoader classLoader;

  public AbstractRecursiveAnnotationVisitor(
          @Nullable ClassLoader classLoader, AnnotationAttributes attributes) {
    this.classLoader = classLoader;
    this.attributes = attributes;
  }

  @Override
  public void visit(String attributeName, Object attributeValue) {
    this.attributes.put(attributeName, attributeValue);
  }

  @Override
  public AnnotationVisitor visitAnnotation(String attributeName, String asmTypeDescriptor) {
    String annotationType = Type.fromDescriptor(asmTypeDescriptor).getClassName();
    AnnotationAttributes nestedAttributes = new AnnotationAttributes(annotationType, this.classLoader);
    this.attributes.put(attributeName, nestedAttributes);
    return new RecursiveAnnotationAttributesVisitor(annotationType, nestedAttributes, this.classLoader);
  }

  @Override
  public AnnotationVisitor visitArray(String attributeName) {
    return new RecursiveAnnotationArrayVisitor(attributeName, this.attributes, this.classLoader);
  }

  @Override
  public void visitEnum(String attributeName, String asmTypeDescriptor, String attributeValue) {
    Object newValue = getEnumValue(asmTypeDescriptor, attributeValue);
    visit(attributeName, newValue);
  }

  protected Object getEnumValue(String asmTypeDescriptor, String attributeValue) {
    Object valueToUse = attributeValue;
    try {
      Class<?> enumType = ClassUtils.forName(Type.fromDescriptor(asmTypeDescriptor).getClassName(), this.classLoader);
      Field enumConstant = ReflectionUtils.findField(enumType, attributeValue);
      if (enumConstant != null) {
        ReflectionUtils.makeAccessible(enumConstant);
        valueToUse = enumConstant.get(null);
      }
    }
    catch (ClassNotFoundException | NoClassDefFoundError ex) {
      logger.debug("Failed to classload enum type while reading annotation metadata", ex);
    }
    catch (IllegalAccessException ex) {
      logger.debug("Could not access enum value while reading annotation metadata", ex);
    }
    return valueToUse;
  }

}
