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

package cn.taketoday.context.support;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;

import cn.taketoday.asm.ClassReader;
import cn.taketoday.asm.Type;
import cn.taketoday.asm.tree.AnnotationNode;
import cn.taketoday.asm.tree.ClassNode;
import cn.taketoday.asm.tree.MethodNode;
import cn.taketoday.context.AnnotationAttributes;
import cn.taketoday.context.Constant;
import cn.taketoday.context.utils.ClassUtils;

/**
 * @author TODAY 2021/8/1 18:00
 * @since 4.0
 */
public class ClassMetaReader {
  private static final HashMap<Class<?>, ClassNode> classNodeCache = new HashMap<>(128);
  private static final HashMap<Class<?>, AnnotationAttributes> annotationDefaultCache = new HashMap<>(128);

  public static ClassNode read(Class<?> classToRead) {
    return read(classToRead, ClassUtils.getClassLoader());
  }

  public static ClassNode read(Class<?> key, ClassLoader classLoader) {
    return classNodeCache.computeIfAbsent(key, classToRead -> {
      String classFile = getClassFile(classToRead);
      try (InputStream resourceAsStream = classLoader.getResourceAsStream(classFile)) {
        final ClassReader classReader = new ClassReader(resourceAsStream);
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
    return classToRead.getName()
            .replace(Constant.PACKAGE_SEPARATOR, Constant.PATH_SEPARATOR)
            .concat(Constant.CLASS_FILE_SUFFIX);
  }

  public static AnnotationAttributes readAnnotation(AnnotationNode annotationNode) {
    if (annotationNode != null) {
      String desc = annotationNode.desc;
      Type type = Type.fromDescriptor(desc);
      // Annotation type
      Class aClass = ClassUtils.loadClass(type.getClassName());
      AnnotationAttributes attributes = new AnnotationAttributes(aClass);

      // read default values
      applyDefaults(attributes, aClass);

      // override default values
      List<Object> values = annotationNode.values;
      if (values != null) {
        for (int i = 0, n = values.size(); i < n; i += 2) {
          String name = (String) values.get(i);
          Object value = values.get(i + 1);
          if (value instanceof AnnotationNode) {
            value = readAnnotation((AnnotationNode) value);
          }
          attributes.put(name, value);
        }
      }

      return attributes;
    }
    return null;
  }

  private static void applyDefaults(AnnotationAttributes attributes, Class<?> aClass) {
    attributes.putAll(readDefaultAttributes(aClass));
  }

  public static AnnotationAttributes readDefaultAttributes(Class<?> aClass) {
    return annotationDefaultCache.computeIfAbsent(aClass, annotationType -> {
      ClassNode classNode = read(annotationType);
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
      }
      return defaultAttributes;
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
        AnnotationAttributes[] annotationAttributes = new AnnotationAttributes[visibleAnnotations.size()];
        int i = 0;
        for (final AnnotationNode visibleAnnotation : visibleAnnotations) {
          annotationAttributes[i++] = readAnnotation(visibleAnnotation);
        }
        return annotationAttributes;
      }
    }
    return null;
  }

}
