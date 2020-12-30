/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Today & 2017 - 2018 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cn.taketoday.jdbc.mapping;

import java.lang.reflect.Field;

import lombok.Getter;
import lombok.Setter;

/**
 * @author TODAY <br>
 *         2018-09-11 16:04
 */
@Setter
@Getter
public final class ParameterMapping {

  private Integer genericClass;

  private Integer parameterClass;

  private String name;

  private int index = 1;

  private Field field;

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("{\n\t\"genericClass\":\"").append(genericClass).append("\",\n\t\"parameterClass\":\"").append(
            parameterClass).append("\",\n\t\"name\":\"").append(name).append("\",\n\t\"index\":\"").append(index).append(
            "\",\n\t\"field\":\"").append(field).append("\"\n}");
    return builder.toString();
  }

}
