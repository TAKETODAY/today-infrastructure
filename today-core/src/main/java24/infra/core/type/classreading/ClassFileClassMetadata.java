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

package infra.core.type.classreading;

import org.jspecify.annotations.Nullable;

import java.lang.classfile.AccessFlags;
import java.lang.classfile.ClassModel;
import java.lang.classfile.Interfaces;
import java.lang.classfile.MethodModel;
import java.lang.classfile.Superclass;
import java.lang.classfile.attribute.InnerClassInfo;
import java.lang.classfile.attribute.InnerClassesAttribute;
import java.lang.classfile.attribute.NestHostAttribute;
import java.lang.classfile.attribute.RuntimeVisibleAnnotationsAttribute;
import java.lang.classfile.constantpool.ClassEntry;
import java.lang.reflect.AccessFlag;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import infra.core.annotation.MergedAnnotations;
import infra.core.type.AnnotationMetadata;
import infra.core.type.MethodMetadata;
import infra.util.ClassUtils;
import infra.util.StringUtils;

/**
 * {@link AnnotationMetadata} implementation that leverages
 * the {@link java.lang.classfile.ClassFile} API.
 *
 * @author Brian Clozel
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 */
class ClassFileClassMetadata implements AnnotationMetadata {

  private final String className;

  private final AccessFlags accessFlags;

  @Nullable
  private final String enclosingClassName;

  @Nullable
  private final String superClassName;

  private final boolean independentInnerClass;

  private final Set<String> interfaceNames;

  private final Set<String> memberClassNames;

  private final Set<MethodMetadata> declaredMethods;

  private final MergedAnnotations mergedAnnotations;

  @Nullable
  private Set<String> annotationTypes;

  ClassFileClassMetadata(String className, AccessFlags accessFlags, @Nullable String enclosingClassName,
          @Nullable String superClassName, boolean independentInnerClass, Set<String> interfaceNames,
          Set<String> memberClassNames, Set<MethodMetadata> declaredMethods, MergedAnnotations mergedAnnotations) {
    this.className = className;
    this.accessFlags = accessFlags;
    this.enclosingClassName = enclosingClassName;
    this.superClassName = (!className.endsWith(".package-info")) ? superClassName : null;
    this.independentInnerClass = independentInnerClass;
    this.interfaceNames = interfaceNames;
    this.memberClassNames = memberClassNames;
    this.declaredMethods = declaredMethods;
    this.mergedAnnotations = mergedAnnotations;
  }

  @Override
  public String getClassName() {
    return this.className;
  }

  @Override
  public boolean isInterface() {
    return this.accessFlags.has(AccessFlag.INTERFACE);
  }

  @Override
  public boolean isAnnotation() {
    return this.accessFlags.has(AccessFlag.ANNOTATION);
  }

  @Override
  public boolean isAbstract() {
    return this.accessFlags.has(AccessFlag.ABSTRACT);
  }

  @Override
  public boolean isFinal() {
    return this.accessFlags.has(AccessFlag.FINAL);
  }

  @Override
  public boolean isIndependent() {
    return (this.enclosingClassName == null || this.independentInnerClass);
  }

  @Nullable
  @Override
  public String getEnclosingClassName() {
    return this.enclosingClassName;
  }

  @Nullable
  @Override
  public String getSuperClassName() {
    return this.superClassName;
  }

  @Override
  public String[] getInterfaceNames() {
    return StringUtils.toStringArray(this.interfaceNames);
  }

  @Override
  public String[] getMemberClassNames() {
    return StringUtils.toStringArray(this.memberClassNames);
  }

  @Override
  public MergedAnnotations getAnnotations() {
    return this.mergedAnnotations;
  }

  @Override
  public Set<String> getAnnotationTypes() {
    Set<String> annotationTypes = this.annotationTypes;
    if (annotationTypes == null) {
      annotationTypes = Collections.unmodifiableSet(
              AnnotationMetadata.super.getAnnotationTypes());
      this.annotationTypes = annotationTypes;
    }
    return annotationTypes;
  }

  @Override
  public Set<MethodMetadata> getAnnotatedMethods(String annotationName) {
    Set<MethodMetadata> result = new LinkedHashSet<>(4);
    for (MethodMetadata annotatedMethod : this.declaredMethods) {
      if (annotatedMethod.isAnnotated(annotationName)) {
        result.add(annotatedMethod);
      }
    }
    return Collections.unmodifiableSet(result);
  }

  @Override
  public Set<MethodMetadata> getDeclaredMethods() {
    return Collections.unmodifiableSet(this.declaredMethods);
  }

  @Override
  public boolean equals(@Nullable Object other) {
    return (this == other || (other instanceof ClassFileClassMetadata that && this.className.equals(that.className)));
  }

  @Override
  public int hashCode() {
    return this.className.hashCode();
  }

  @Override
  public String toString() {
    return this.className;
  }

  static ClassFileClassMetadata of(ClassModel classModel, @Nullable ClassLoader classLoader) {
    Builder builder = new Builder(classLoader);
    builder.classEntry(classModel.thisClass());
    String currentClassName = classModel.thisClass().name().stringValue();
    classModel.elementStream().forEach(classElement -> {
      switch (classElement) {
        case AccessFlags flags -> {
          builder.accessFlags(flags);
        }
        case NestHostAttribute _ -> {
          builder.enclosingClass(classModel.thisClass());
        }
        case InnerClassesAttribute innerClasses -> {
          builder.nestMembers(currentClassName, innerClasses);
        }
        case RuntimeVisibleAnnotationsAttribute annotationsAttribute -> {
          builder.mergedAnnotations(ClassFileAnnotationMetadata.createMergedAnnotations(
                  ClassUtils.convertResourcePathToClassName(currentClassName), annotationsAttribute, classLoader));
        }
        case Superclass superclass -> {
          builder.superClass(superclass);
        }
        case Interfaces interfaces -> {
          builder.interfaces(interfaces);
        }
        case MethodModel method -> {
          builder.method(method);
        }
        default -> {
          // ignore class element
        }
      }
    });
    return builder.build();
  }

  static class Builder {

    @Nullable
    private final ClassLoader clasLoader;

    private String className;

    private AccessFlags accessFlags;

    private Set<AccessFlag> innerAccessFlags;

    @Nullable
    private String enclosingClassName;

    @Nullable
    private String superClassName;

    private Set<String> interfaceNames = new LinkedHashSet<>(4);

    private Set<String> memberClassNames = new LinkedHashSet<>(4);

    private Set<MethodMetadata> declaredMethods = new LinkedHashSet<>(4);

    private MergedAnnotations mergedAnnotations = MergedAnnotations.valueOf(Collections.emptySet());

    public Builder(@Nullable ClassLoader classLoader) {
      this.clasLoader = classLoader;
    }

    void classEntry(ClassEntry classEntry) {
      this.className = ClassUtils.convertResourcePathToClassName(classEntry.name().stringValue());
    }

    void accessFlags(AccessFlags accessFlags) {
      this.accessFlags = accessFlags;
    }

    void enclosingClass(ClassEntry thisClass) {
      String thisClassName = thisClass.name().stringValue();
      int currentClassIndex = thisClassName.lastIndexOf('$');
      this.enclosingClassName = ClassUtils.convertResourcePathToClassName(thisClassName.substring(0, currentClassIndex));
    }

    void superClass(Superclass superClass) {
      this.superClassName = ClassUtils.convertResourcePathToClassName(superClass.superclassEntry().name().stringValue());
    }

    void interfaces(Interfaces interfaces) {
      for (ClassEntry entry : interfaces.interfaces()) {
        this.interfaceNames.add(ClassUtils.convertResourcePathToClassName(entry.name().stringValue()));
      }
    }

    void nestMembers(String currentClassName, InnerClassesAttribute innerClasses) {
      for (InnerClassInfo classInfo : innerClasses.classes()) {
        String innerClassName = classInfo.innerClass().name().stringValue();
        if (currentClassName.equals(innerClassName)) {
          // the current class is an inner class
          this.innerAccessFlags = classInfo.flags();
        }
        classInfo.outerClass().ifPresent(outerClass -> {
          if (outerClass.name().stringValue().equals(currentClassName)) {
            // collecting data about actual inner classes
            this.memberClassNames.add(ClassUtils.convertResourcePathToClassName(innerClassName));
          }
        });
      }
    }

    void mergedAnnotations(MergedAnnotations mergedAnnotations) {
      this.mergedAnnotations = mergedAnnotations;
    }

    void method(MethodModel method) {
      ClassFileMethodMetadata classFileMethodMetadata = ClassFileMethodMetadata.of(method, this.clasLoader);
      if (!classFileMethodMetadata.isSynthetic() && !classFileMethodMetadata.isDefaultConstructor()) {
        this.declaredMethods.add(classFileMethodMetadata);
      }
    }

    ClassFileClassMetadata build() {
      boolean independentInnerClass = (this.enclosingClassName != null) && this.innerAccessFlags.contains(AccessFlag.STATIC);
      return new ClassFileClassMetadata(this.className, this.accessFlags, this.enclosingClassName, this.superClassName,
              independentInnerClass, this.interfaceNames, this.memberClassNames, this.declaredMethods, this.mergedAnnotations);
    }

  }

}
