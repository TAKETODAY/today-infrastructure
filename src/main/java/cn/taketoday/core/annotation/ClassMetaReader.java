/*
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
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

package cn.taketoday.core.annotation;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import cn.taketoday.core.AnnotationAttributes;
import cn.taketoday.core.Constant;
import cn.taketoday.core.NonNull;
import cn.taketoday.core.Nullable;
import cn.taketoday.core.bytecode.ClassReader;
import cn.taketoday.core.bytecode.Type;
import cn.taketoday.core.bytecode.commons.MethodSignature;
import cn.taketoday.core.bytecode.tree.AnnotationNode;
import cn.taketoday.core.bytecode.tree.ClassNode;
import cn.taketoday.core.bytecode.tree.FieldNode;
import cn.taketoday.core.bytecode.tree.MethodNode;
import cn.taketoday.core.io.Resource;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.ReflectionUtils;

/**
 * @author TODAY 2021/8/1 18:00
 * @since 4.0
 */
public class ClassMetaReader {

  private static final HashMap<String, ClassNode> classNodeCache = new HashMap<>(128); // class-name to ClassNode
  //  private static final ConcurrentHashMap<String, AnnotationAttributes> annotationDefaultCache // class-name to AnnotationAttributes
//          = new ConcurrentHashMap<>(128);
  private static final ConcurrentHashMap<String, AnnotationDescriptor> annotationDefaultCache // class-name to AnnotationAttributes
          = new ConcurrentHashMap<>(128);
  private static final HashMap<Object, AnnotationAttributes[]> attributesMap = new HashMap<>(128);

  private static final HashMap<Resource, ClassNode> resourceClassNodeCache = new HashMap<>(128); // class-name to ClassNode

  public static ClassNode read(Class<?> classToRead) {
    return read(classToRead, ClassUtils.getClassLoader());
  }

  public static ClassNode read(Class<?> key, ClassLoader classLoader) {
    return read(key.getName(), classLoader);
  }

  public static ClassNode read(String className) {
    return read(className, ClassUtils.getClassLoader());
  }

  public static ClassNode read(String className, ClassLoader classLoader) {
    return classNodeCache.computeIfAbsent(className, classToRead -> {
      String classFile = getClassFile(classToRead);
      try (InputStream resourceAsStream = classLoader.getResourceAsStream(classFile)) {
        ClassReader classReader = new ClassReader(resourceAsStream);
        ClassNode classNode = new ClassNode();
        classReader.accept(classNode, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG);
        return classNode;
      }
      catch (IOException e) {
        throw new IllegalStateException("'" + classFile + "' read failed", e);
      }
    });
  }

  public static String getClassFile(Class<?> classToRead) {
    return getClassFile(classToRead.getName());
  }

  public static String getClassFile(String classNameToRead) {
    return classNameToRead
            .replace(Constant.PACKAGE_SEPARATOR, Constant.PATH_SEPARATOR)
            .concat(ClassUtils.CLASS_FILE_SUFFIX);
  }

  public static AnnotationAttributes readAnnotation(AnnotationNode annotationNode) {
    if (annotationNode != null) {
      String desc = annotationNode.desc;
      if (desc == null) {
        // exclude array
        return null;
      }

      String className = desc.substring(1, desc.length() - 1).replace('/', '.');
      AnnotationAttributes attributes = new AnnotationAttributes(className);
      // read default values
      applyDefaults(attributes, className);

      // override default values
      List<Object> values = annotationNode.values;
      if (values != null) {
        for (int i = 0, n = values.size(); i < n; i += 2) {
          String name = (String) values.get(i);
          Object value = values.get(i + 1);
          if (value instanceof AnnotationNode) {
            // nested Annotation
            value = readAnnotation((AnnotationNode) value);
          }
          attributes.put(name, value);
        }
      }
      return attributes;
    }
    return null;
  }

  private static void applyDefaults(AnnotationAttributes attributes, String aClass) {
    attributes.putAll(readDefaultAttributes(aClass));
  }

  public static AnnotationAttributes readDefaultAttributes(Class<?> aClass) {
    return readDefaultAttributes(aClass.getName());
  }

  public static AnnotationAttributes readDefaultAttributes(String className) {
    return readDefault(className).defaultAttributes;
  }

  public static AnnotationDescriptor readDefault(Class<?> aClass) {
    return readDefault(aClass.getName());
  }

  public static AnnotationDescriptor readDefault(String className) {
    return annotationDefaultCache.computeIfAbsent(className, annotationType -> {
      ClassNode classNode = read(annotationType);
      HashMap<String, String> annotationTypes = new HashMap<>();
      AnnotationAttributes defaultAttributes = new AnnotationAttributes();
      for (final MethodNode method : classNode.methods) {
        Object defaultValue = method.annotationDefault;
        if (defaultValue != null) {
          // must not be null
          if (defaultValue instanceof AnnotationNode) {
            defaultValue = readAnnotation((AnnotationNode) defaultValue);
          }
          defaultAttributes.put(method.name, defaultValue);
        }

        Type type = Type.forReturnType(method.desc);
        annotationTypes.put(method.name, type.getClassName());
      }
      return new AnnotationDescriptor(defaultAttributes, annotationTypes);
    });
  }

  /**
   * read class Annotations
   *
   * @param classNode
   *         class structure source
   */
  public static AnnotationAttributes[] readAnnotations(ClassNode classNode) {
    if (classNode != null) {
      List<AnnotationNode> visibleAnnotations = classNode.visibleAnnotations;
      if (visibleAnnotations != null) {
        return getAttributes(visibleAnnotations);
      }
    }
    return AnnotationMetaReader.EMPTY_ANNOTATION_ATTRIBUTES;
  }

  private static AnnotationAttributes[] getAttributes(List<AnnotationNode> visibleAnnotations) {
    if (CollectionUtils.isNotEmpty(visibleAnnotations)) {
      AnnotationAttributes[] annotationAttributes = new AnnotationAttributes[visibleAnnotations.size()];
      int i = 0;
      for (final AnnotationNode visibleAnnotation : visibleAnnotations) {
        annotationAttributes[i++] = readAnnotation(visibleAnnotation);
      }
      return annotationAttributes;
    }
    return AnnotationMetaReader.EMPTY_ANNOTATION_ATTRIBUTES;
  }

  /**
   * @param annotated
   *         AnnotatedElement or string (class-name)
   *
   * @return list of target input AnnotationNode
   */
  public static AnnotationAttributes[] readAnnotations(Object annotated) {
    return attributesMap.computeIfAbsent(annotated, target -> {
      List<AnnotationNode> annotationNode = getAnnotationNode(target);
      if (annotationNode == null) {
        // read from java reflect API
        if (target instanceof AnnotatedElement) {
          Annotation[] annotations = ((AnnotatedElement) target).getDeclaredAnnotations();
          if (ObjectUtils.isNotEmpty(annotations)) {
            AnnotationAttributes[] annotationAttributes = new AnnotationAttributes[annotations.length];
            int i = 0;
            for (final Annotation annotation : annotations) {
              annotationAttributes[i++] = AnnotationUtils.getAttributes(annotation);
            }
            return annotationAttributes;
          }
        }
        return AnnotationMetaReader.EMPTY_ANNOTATION_ATTRIBUTES;
      }
      if (annotationNode.isEmpty()) {
        return AnnotationMetaReader.EMPTY_ANNOTATION_ATTRIBUTES;
      }
      return getAttributes(annotationNode);
    });
  }

  /**
   * @param annotated
   *         AnnotatedElement or string (class-name)
   *
   * @return list of target input AnnotationNode
   */
  public static List<AnnotationNode> getAnnotationNode(Object annotated) {
    if (annotated instanceof String) {
      final ClassNode node = read((String) annotated);
      return node.visibleAnnotations;
    }
    if (annotated instanceof Class) {
      ClassNode classNode = read((Class<?>) annotated);
      return warpEmpty(classNode.visibleAnnotations);
    }
    if (annotated instanceof Executable) {
      MethodNode methodNode = getMethodNode((Executable) annotated);
      return warpEmpty(methodNode.visibleAnnotations);
    }
    if (annotated instanceof Field) {
      // java reflect field
      Field field = (Field) annotated;
      Class<?> declaringClass = field.getDeclaringClass();
      String descriptor = null;
      ClassNode classNode = read(declaringClass);
      for (final FieldNode fieldNode : classNode.fields) {
        if (Objects.equals(field.getName(), fieldNode.name)) {
          if (descriptor == null) {
            descriptor = Type.getDescriptor(field.getType());
          }
          if (Objects.equals(fieldNode.desc, descriptor)) {
            return warpEmpty(fieldNode.visibleAnnotations);
          }
        }
      }
    }
    if (annotated instanceof Parameter) {
      Parameter parameter = (Parameter) annotated;
      MethodNode methodNode = getMethodNode(parameter.getDeclaringExecutable());
      List<AnnotationNode>[] annotations = methodNode.visibleParameterAnnotations;
      if (ObjectUtils.isNotEmpty(annotations)) {
        return warpEmpty(annotations[ReflectionUtils.getParameterIndex(parameter)]);
      }
    }
    return null;
  }

  private static List<AnnotationNode> warpEmpty(List<AnnotationNode> visibleAnnotations) {
    return visibleAnnotations == null ? Collections.emptyList() : visibleAnnotations;
  }

  private static MethodNode getMethodNode(Executable executable) {
    ClassNode classNode = read(executable.getDeclaringClass());
    boolean isConstructor = executable instanceof Constructor;
    String name = isConstructor ? MethodSignature.CONSTRUCTOR_NAME : executable.getName();
    for (final MethodNode method : classNode.methods) {
      if (Objects.equals(name, method.name)) {
        String descriptor = getDescriptor(executable, isConstructor);
        if (Objects.equals(method.desc, descriptor)) {
          return method;
        }
      }
    }
    throw new IllegalStateException("cannot read executable annotations");
  }

  private static String getDescriptor(AnnotatedElement annotated, boolean isConstructor) {
    if (isConstructor) {
      return Type.getConstructorDescriptor((Constructor<?>) annotated);
    }
    else {
      return Type.getMethodDescriptor((Method) annotated);
    }
  }

  @Nullable
  public static <T extends Annotation> AnnotationAttributes selectAttributes(
          AnnotatedElement element, Class<T> targetClass) {
    return selectAttributes(readAnnotations(element), targetClass);
  }

  @Nullable
  public static <T extends Annotation> AnnotationAttributes selectAttributes(
          AnnotationAttributes[] attributes, Class<T> targetClass) {
    for (final AnnotationAttributes attribute : attributes) {
      if (attribute.annotationType() == targetClass) {
        return attribute;
      }
    }
    return null;
  }

  @Nullable
  public static <T extends Annotation> AnnotationAttributes selectAttributes(
          ClassNode classNode, Class<T> targetClass) {
    AnnotationAttributes[] annotations = ClassMetaReader.readAnnotations(classNode);
    return selectAttributes(annotations, targetClass);
  }
  // proxy

  public static <T extends Annotation> T getAnnotation(Class<T> type, AnnotatedElement annotated) {
    AnnotationAttributes[] annotationAttributes = readAnnotations(annotated);
    AnnotationAttributes attributes = null;
    for (final AnnotationAttributes attribute : annotationAttributes) {
      if (attribute.annotationType() == type) {
        attributes = attribute;
        break;
      }
    }
    if (attributes == null) {
      return null;
    }
    return getAnnotation(type, attributes);
  }

  @SuppressWarnings("unchecked")
  @NonNull
  public static <T extends Annotation> T getAnnotation(Class<T> type, AnnotationAttributes attributes) {
    return (T) Proxy.newProxyInstance(
            type.getClassLoader(), new Class<?>[] { type },
            new AnnotationInvocationHandler(type, attributes));
  }

  public static void clearCache() {
    classNodeCache.clear();
    annotationDefaultCache.clear();
  }

  //

  public static ClassNode read(Resource classResource) {
    return resourceClassNodeCache.computeIfAbsent(classResource, key -> {
      try (InputStream inputStream = key.getInputStream()) {
        ClassReader classReader = new ClassReader(inputStream);
        ClassNode classNode = new ClassNode();
        classReader.accept(classNode, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG);
        return classNode;
      }
      catch (IOException e) {
        throw new IllegalStateException("'" + key + "' read failed", e);
      }
    });
  }

}
