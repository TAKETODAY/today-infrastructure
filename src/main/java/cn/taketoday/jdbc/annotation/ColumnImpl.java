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
package cn.taketoday.jdbc.annotation;

import java.lang.annotation.Annotation;

import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 *
 * @author Today <br>
 *         2018-08-30 09:41
 */
@Setter
@NoArgsConstructor
@SuppressWarnings("all")
public final class ColumnImpl implements Column {

  private String value;

  /**
   * foreign key name
   */
  private String foreignKey;

  /**
   * the comment in database
   */
  private String comment;
  /**
   * the column type
   */
  private String type;

  private String defaultValue;

  private int length;

  private int decimal;

  private boolean notNull;

  private boolean zerofill;

  private boolean unsigned;

  private boolean increment;

  @Override
  public Class<? extends Annotation> annotationType() {
    return Column.class;
  }

  @Override
  public String value() {
    return value;
  }

  @Override
  public String foreignKey() {
    return foreignKey;
  }

  @Override
  public boolean notNull() {
    return notNull;
  }

  @Override
  public int length() {
    return length;
  }

  @Override
  public int decimal() {
    return decimal;
  }

  @Override
  public String comment() {
    return comment;
  }

  @Override
  public String type() {
    return type;
  }

  @Override
  public String defaultValue() {
    return defaultValue;
  }

  @Override
  public boolean zerofill() {
    return zerofill;
  }

  @Override
  public boolean unsigned() {
    return unsigned;
  }

  @Override
  public boolean increment() {
    return increment;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("{\"value\":\"").append(value).append("\",\"foreignKey\":\"").append(foreignKey).append("\",\"comment\":\"").append(
            comment).append("\",\"type\":\"").append(type).append("\",\"defaultValue\":\"").append(defaultValue).append(
            "\",\"length\":\"").append(length).append("\",\"decimal\":\"").append(decimal).append("\",\"notNull\":\"").append(
            notNull).append("\",\"zerofill\":\"").append(zerofill).append("\",\"unsigned\":\"").append(unsigned).append(
            "\",\"increment\":\"").append(increment).append("\"}");
    return builder.toString();
  }

}
