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

package cn.taketoday.core.style;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;

import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.ObjectUtils;

/**
 * Converts objects to String form, generally for debugging purposes,
 * using default {@code toString} styling conventions.
 *
 * <p>Uses the reflective visitor pattern underneath the hood to nicely
 * encapsulate styling algorithms for each type of styled object.
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 * @since 4.0
 */
public class DefaultValueStyler implements ValueStyler {

  private static final String EMPTY = "[[empty]]";
  private static final String NULL = "[null]";
  private static final String COLLECTION = "collection";
  private static final String SET = "set";
  private static final String LIST = "list";
  private static final String MAP = "map";
  private static final String EMPTY_MAP = MAP + EMPTY;
  private static final String ARRAY = "array";

  @Override
  public String style(@Nullable Object value) {
    if (value == null) {
      return NULL;
    }
    else if (value instanceof String) {
      return "'" + value + "'";
    }
    else if (value instanceof Class) {
      return ClassUtils.getShortName((Class<?>) value);
    }
    else if (value instanceof Method method) {
      return method.getName() + "@" + ClassUtils.getShortName(method.getDeclaringClass());
    }
    else if (value instanceof Map) {
      return style((Map<?, ?>) value);
    }
    else if (value instanceof Map.Entry) {
      return style((Map.Entry<?, ?>) value);
    }
    else if (value instanceof Collection) {
      return style((Collection<?>) value);
    }
    else if (value.getClass().isArray()) {
      return styleArray(ObjectUtils.toObjectArray(value));
    }
    else {
      return String.valueOf(value);
    }
  }

  private <K, V> String style(Map<K, V> value) {
    if (value.isEmpty()) {
      return EMPTY_MAP;
    }

    StringJoiner result = new StringJoiner(", ", "[", "]");
    for (Map.Entry<K, V> entry : value.entrySet()) {
      result.add(style(entry));
    }
    return MAP + result;
  }

  private String style(Map.Entry<?, ?> value) {
    return style(value.getKey()) + " -> " + style(value.getValue());
  }

  private String style(Collection<?> value) {
    String collectionType = getCollectionTypeString(value);

    if (value.isEmpty()) {
      return collectionType + EMPTY;
    }

    StringJoiner result = new StringJoiner(", ", "[", "]");
    for (Object o : value) {
      result.add(style(o));
    }
    return collectionType + result;
  }

  private String getCollectionTypeString(Collection<?> value) {
    if (value instanceof List) {
      return LIST;
    }
    else if (value instanceof Set) {
      return SET;
    }
    else {
      return COLLECTION;
    }
  }

  private String styleArray(Object[] array) {
    if (array.length == 0) {
      return ARRAY + '<' + ClassUtils.getShortName(array.getClass().getComponentType()) + '>' + EMPTY;
    }

    StringJoiner result = new StringJoiner(", ", "[", "]");
    for (Object o : array) {
      result.add(style(o));
    }
    return ARRAY + '<' + ClassUtils.getShortName(array.getClass().getComponentType()) + '>' + result;
  }

}
