/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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
package cn.taketoday.retry.util.test;

import cn.taketoday.beans.DirectFieldAccessor;
import cn.taketoday.lang.Assert;

/**
 * See Infra Integration TestUtils.
 *
 * @author Mark Fisher
 * @author Iwein Fuld
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @since 1.2
 */
public class TestUtils {

  /**
   * Uses nested {@link cn.taketoday.beans.DirectFieldAccessor}s to obtain a
   * property using dotted notation to traverse fields; e.g. "foo.bar.baz" will obtain a
   * reference to the baz field of the bar field of foo. Adopted from Spring
   * Integration.
   *
   * @param root The object.
   * @param propertyPath The path.
   * @return The field.
   */
  public static Object getPropertyValue(Object root, String propertyPath) {
    Object value = null;
    DirectFieldAccessor accessor = new DirectFieldAccessor(root);
    String[] tokens = propertyPath.split("\\.");
    for (int i = 0; i < tokens.length; i++) {
      value = accessor.getPropertyValue(tokens[i]);
      if (value != null) {
        accessor = new DirectFieldAccessor(value);
      }
      else if (i == tokens.length - 1) {
        return null;
      }
      else {
        throw new IllegalArgumentException("intermediate property '" + tokens[i] + "' is null");
      }
    }
    return value;
  }

  @SuppressWarnings("unchecked")
  public static <T> T getPropertyValue(Object root, String propertyPath, Class<T> type) {
    Object value = getPropertyValue(root, propertyPath);
    if (value != null) {
      Assert.isAssignable(type, value.getClass());
    }
    return (T) value;
  }

}
