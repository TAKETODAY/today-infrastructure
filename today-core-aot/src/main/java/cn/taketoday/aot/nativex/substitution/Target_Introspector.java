/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.aot.nativex.substitution;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

import java.beans.Customizer;

/**
 * {@link java.beans.Introspector} substitution with a refined {@code findCustomizerClass} implementation
 * designed to avoid thousands of AWT classes to be included in the native image.
 *
 * TODO Remove once Infra requires GraalVM 23.0+, see <a href="https://github.com/oracle/graal/pull/5224">graal#5224</a>.
 *
 * @author Sebastien Deleuze
 * @since 4.0
 */
@TargetClass(className = "java.beans.Introspector")
final class Target_Introspector {

  @Substitute
  private static Class<?> findCustomizerClass(Class<?> type) {
    String name = type.getName() + "Customizer";
    try {
      type = Target_ClassFinder.findClass(name, type.getClassLoader());
      if (Customizer.class.isAssignableFrom(type)) {
        Class<?> c = type;
        do {
          c = c.getSuperclass();
          if (c.getName().equals("java.awt.Component")) {
            return type;
          }
        }
        while (!c.getName().equals("java.lang.Object"));
      }
    }
    catch (Exception exception) {
    }
    return null;
  }

}
