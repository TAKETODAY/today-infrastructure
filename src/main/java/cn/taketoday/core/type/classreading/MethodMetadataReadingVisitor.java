/*
 * Copyright 2002-2020 the original author or authors.
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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cn.taketoday.core.DefaultMultiValueMap;
import cn.taketoday.core.MultiValueMap;
import cn.taketoday.core.annotation.AnnotationAttributes;
import cn.taketoday.core.annotation.MergedAnnotations;
import cn.taketoday.core.bytecode.AnnotationVisitor;
import cn.taketoday.core.bytecode.MethodVisitor;
import cn.taketoday.core.bytecode.Opcodes;
import cn.taketoday.core.bytecode.Type;
import cn.taketoday.core.type.MethodMetadata;
import cn.taketoday.lang.Nullable;

/**
 * ASM method visitor which looks for the annotations defined on a method,
 * exposing them through the {@link cn.taketoday.core.type.MethodMetadata}
 * interface.
 *
 * @author Juergen Hoeller
 * @author Mark Pollack
 * @author Costin Leau
 * @author Chris Beams
 * @author Phillip Webb
 * @since 3.0
 * @deprecated this class and related classes in this
 * package have been replaced by {@link SimpleAnnotationMetadataReadingVisitor}
 * and related classes for internal use within the framework.
 */
@Deprecated
public class MethodMetadataReadingVisitor extends MethodVisitor implements MethodMetadata {

  protected final String methodName;

  protected final int access;

  protected final String declaringClassName;

  protected final String returnTypeName;

  @Nullable
  protected final ClassLoader classLoader;

  protected final Set<MethodMetadata> methodMetadataSet;

  protected final Map<String, Set<String>> metaAnnotationMap = new LinkedHashMap<>(4);

  protected final DefaultMultiValueMap<String, AnnotationAttributes>
          attributesMap = new DefaultMultiValueMap<>(3);

  public MethodMetadataReadingVisitor(
          String methodName, int access, String declaringClassName,
          String returnTypeName, @Nullable ClassLoader classLoader, Set<MethodMetadata> methodMetadataSet) {

    this.methodName = methodName;
    this.access = access;
    this.declaringClassName = declaringClassName;
    this.returnTypeName = returnTypeName;
    this.classLoader = classLoader;
    this.methodMetadataSet = methodMetadataSet;
  }

  @Override
  public MergedAnnotations getAnnotations() {
    throw new UnsupportedOperationException();
  }

  @Override
  @Nullable
  public AnnotationVisitor visitAnnotation(final String desc, boolean visible) {
    if (!visible) {
      return null;
    }
    this.methodMetadataSet.add(this);
    String className = Type.fromDescriptor(desc).getClassName();
    return new AnnotationAttributesReadingVisitor(
            className, this.attributesMap, this.metaAnnotationMap, this.classLoader);
  }

  @Override
  public String getMethodName() {
    return this.methodName;
  }

  @Override
  public boolean isAbstract() {
    return ((this.access & Opcodes.ACC_ABSTRACT) != 0);
  }

  @Override
  public boolean isStatic() {
    return ((this.access & Opcodes.ACC_STATIC) != 0);
  }

  @Override
  public boolean isFinal() {
    return ((this.access & Opcodes.ACC_FINAL) != 0);
  }

  @Override
  public boolean isOverridable() {
    return (!isStatic() && !isFinal() && ((this.access & Opcodes.ACC_PRIVATE) == 0));
  }

  @Override
  public int getParameterCount() {
    return 0; // TODO
  }

  @Override
  public Type[] getArgumentTypes() {
    return new Type[0]; // TODO
  }

  @Override
  public Class<?>[] getParameterTypes() {
    return new Class[0];// TODO
  }

  @Override
  public boolean isAnnotated(String annotationName) {
    return this.attributesMap.containsKey(annotationName);
  }

  @Override
  @Nullable
  public AnnotationAttributes getAnnotationAttributes(String annotationName, boolean classValuesAsString) {
    AnnotationAttributes raw = AnnotationReadingVisitorUtils.getMergedAnnotationAttributes(
            this.attributesMap, this.metaAnnotationMap, annotationName);
    if (raw == null) {
      return null;
    }
    return AnnotationReadingVisitorUtils.convertClassValues(
            "method '" + getMethodName() + "'", this.classLoader, raw, classValuesAsString);
  }

  @Override
  @Nullable
  public MultiValueMap<String, Object> getAllAnnotationAttributes(
          String annotationName, boolean classValuesAsString) {
    if (!this.attributesMap.containsKey(annotationName)) {
      return null;
    }
    MultiValueMap<String, Object> allAttributes = new DefaultMultiValueMap<>();
    List<AnnotationAttributes> attributesList = this.attributesMap.get(annotationName);
    if (attributesList != null) {
      String annotatedElement = "method '" + getMethodName() + '\'';
      for (AnnotationAttributes annotationAttributes : attributesList) {
        AnnotationAttributes convertedAttributes = AnnotationReadingVisitorUtils.convertClassValues(
                annotatedElement, this.classLoader, annotationAttributes, classValuesAsString);
        convertedAttributes.forEach(allAttributes::add);
      }
    }
    return allAttributes;
  }

  @Override
  public String getDeclaringClassName() {
    return this.declaringClassName;
  }

  @Override
  public String getReturnTypeName() {
    return this.returnTypeName;
  }

}
