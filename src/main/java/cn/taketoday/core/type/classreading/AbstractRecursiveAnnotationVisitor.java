/*
 * Copyright 2002-2021 the original author or authors.
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

package cn.taketoday.core.type.classreading;

import cn.taketoday.core.annotation.AnnotationAttributes;
import cn.taketoday.core.bytecode.AnnotationVisitor;
import cn.taketoday.core.bytecode.Type;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.ReflectionUtils;

import java.lang.reflect.Field;

/**
 * {@link AnnotationVisitor} to recursively visit annotations.
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @author Phillip Webb
 * @author Sam Brannen
 * @since 3.1.1
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
