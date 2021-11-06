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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cn.taketoday.core.MultiValueMap;
import cn.taketoday.core.annotation.AnnotationAttributes;
import cn.taketoday.core.annotation.AnnotationUtils;
import cn.taketoday.core.bytecode.Type;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.util.ObjectUtils;

/**
 * Internal utility class used when reading annotations via ASM.
 *
 * @author Juergen Hoeller
 * @author Mark Fisher
 * @author Costin Leau
 * @author Phillip Webb
 * @author Sam Brannen
 * @since 4.0
 */
@Deprecated
abstract class AnnotationReadingVisitorUtils {

  public static AnnotationAttributes convertClassValues(
          Object annotatedElement,
          @Nullable ClassLoader classLoader, AnnotationAttributes original, boolean classValuesAsString) {

    AnnotationAttributes result = new AnnotationAttributes(original);
    AnnotationUtils.postProcessAnnotationAttributes(annotatedElement, result, classValuesAsString);

    for (Map.Entry<String, Object> entry : result.entrySet()) {
      try {
        Object value = entry.getValue();
        if (value instanceof AnnotationAttributes) {
          value = convertClassValues(
                  annotatedElement, classLoader, (AnnotationAttributes) value, classValuesAsString);
        }
        else if (value instanceof AnnotationAttributes[]) {
          AnnotationAttributes[] values = (AnnotationAttributes[]) value;
          for (int i = 0; i < values.length; i++) {
            values[i] = convertClassValues(annotatedElement, classLoader, values[i], classValuesAsString);
          }
          value = values;
        }
        else if (value instanceof Type) {
          value = (classValuesAsString ? ((Type) value).getClassName() :
                   ClassUtils.forName(((Type) value).getClassName(), classLoader));
        }
        else if (value instanceof Type[]) {
          Type[] array = (Type[]) value;
          Object[] convArray =
                  (classValuesAsString ? new String[array.length] : new Class<?>[array.length]);
          for (int i = 0; i < array.length; i++) {
            convArray[i] = (classValuesAsString ? array[i].getClassName() :
                            ClassUtils.forName(array[i].getClassName(), classLoader));
          }
          value = convArray;
        }
        else if (classValuesAsString) {
          if (value instanceof Class) {
            value = ((Class<?>) value).getName();
          }
          else if (value instanceof Class[]) {
            Class<?>[] clazzArray = (Class<?>[]) value;
            String[] newValue = new String[clazzArray.length];
            for (int i = 0; i < clazzArray.length; i++) {
              newValue[i] = clazzArray[i].getName();
            }
            value = newValue;
          }
        }
        entry.setValue(value);
      }
      catch (Throwable ex) {
        // Class not found - can't resolve class reference in annotation attribute.
        result.put(entry.getKey(), ex);
      }
    }

    return result;
  }

  /**
   * Retrieve the merged attributes of the annotation of the given type,
   * if any, from the supplied {@code attributesMap}.
   * <p>Annotation attribute values appearing <em>lower</em> in the annotation
   * hierarchy (i.e., closer to the declaring class) will override those
   * defined <em>higher</em> in the annotation hierarchy.
   *
   * @param attributesMap the map of annotation attribute lists, keyed by
   * annotation type name
   * @param metaAnnotationMap the map of meta annotation relationships,
   * keyed by annotation type name
   * @param annotationName the fully qualified class name of the annotation
   * type to look for
   * @return the merged annotation attributes, or {@code null} if no
   * matching annotation is present in the {@code attributesMap}
   */
  @Nullable
  public static AnnotationAttributes getMergedAnnotationAttributes(
          MultiValueMap<String, AnnotationAttributes> attributesMap,
          Map<String, Set<String>> metaAnnotationMap, String annotationName) {

    // Get the unmerged list of attributes for the target annotation.
    List<AnnotationAttributes> attributesList = attributesMap.get(annotationName);
    if (CollectionUtils.isEmpty(attributesList)) {
      return null;
    }

    // To start with, we populate the result with a copy of all attribute values
    // from the target annotation. A copy is necessary so that we do not
    // inadvertently mutate the state of the metadata passed to this method.
    AnnotationAttributes result = new AnnotationAttributes(attributesList.get(0));

    Set<String> overridableAttributeNames = new HashSet<>(result.keySet());
    overridableAttributeNames.remove(AnnotationUtils.VALUE);

    // Since the map is a DefaultMultiValueMap, we depend on the ordering of
    // elements in the map and reverse the order of the keys in order to traverse
    // "down" the annotation hierarchy.
    List<String> annotationTypes = new ArrayList<>(attributesMap.keySet());
    Collections.reverse(annotationTypes);

    // No need to revisit the target annotation type:
    annotationTypes.remove(annotationName);

    for (String currentAnnotationType : annotationTypes) {
      List<AnnotationAttributes> currentAttributesList = attributesMap.get(currentAnnotationType);
      if (!ObjectUtils.isEmpty(currentAttributesList)) {
        Set<String> metaAnns = metaAnnotationMap.get(currentAnnotationType);
        if (metaAnns != null && metaAnns.contains(annotationName)) {
          AnnotationAttributes currentAttributes = currentAttributesList.get(0);
          for (String overridableAttributeName : overridableAttributeNames) {
            Object value = currentAttributes.get(overridableAttributeName);
            if (value != null) {
              // Store the value, potentially overriding a value from an attribute
              // of the same name found higher in the annotation hierarchy.
              result.put(overridableAttributeName, value);
            }
          }
        }
      }
    }

    return result;
  }

}
