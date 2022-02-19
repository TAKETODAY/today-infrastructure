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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package cn.taketoday.context.properties.bind;

/**
 * Internal utility to help when dealing with data object property names.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 * @see DataObjectBinder
 * @since 4.0
 */
public abstract class DataObjectPropertyName {

  private DataObjectPropertyName() {
  }

  /**
   * Return the specified Java Bean property name in dashed form.
   *
   * @param name the source name
   * @return the dashed from
   */
  public static String toDashedForm(String name) {
    StringBuilder result = new StringBuilder(name.length());
    boolean inIndex = false;
    for (int i = 0; i < name.length(); i++) {
      char ch = name.charAt(i);
      if (inIndex) {
        result.append(ch);
        if (ch == ']') {
          inIndex = false;
        }
      }
      else {
        if (ch == '[') {
          inIndex = true;
          result.append(ch);
        }
        else {
          ch = (ch != '_') ? ch : '-';
          if (Character.isUpperCase(ch) && result.length() > 0 && result.charAt(result.length() - 1) != '-') {
            result.append('-');
          }
          result.append(Character.toLowerCase(ch));
        }
      }
    }
    return result.toString();
  }

}
