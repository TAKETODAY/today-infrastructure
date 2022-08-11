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

package cn.taketoday.bytecode.core;

import java.util.Arrays;
import java.util.List;

public class ClassesKey {

  record Key(List<Object> array) {

  }

  private ClassesKey() { }

  public static Object create(Object[] array) {
    return new Key(classNames(array));
  }

  private static List<Object> classNames(Object[] objects) {
    if (objects == null) {
      return null;
    }
    String[] classNames = new String[objects.length];
    for (int i = 0; i < objects.length; i++) {
      Object object = objects[i];
      if (object != null) {
        Class<?> aClass = object.getClass();
        classNames[i] = aClass == null ? null : aClass.getName();
      }
    }
    return Arrays.asList(classNames);
  }
}
