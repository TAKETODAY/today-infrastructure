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

package cn.taketoday.context.expression;

import cn.taketoday.core.annotation.AnnotationAttributes;
import cn.taketoday.core.annotation.MergedAnnotation;
import cn.taketoday.lang.Constant;
import cn.taketoday.lang.Env;
import cn.taketoday.lang.Value;

/**
 * @author TODAY 2021/10/14 21:58
 * @since 4.0
 */
public class ExpressionInfo {
  private String expression;
  private boolean required;
  private String defaultValue;
  private boolean placeholderOnly;

  public ExpressionInfo(Value value) {
    this.expression = value.value();
    this.required = value.required();
    this.defaultValue = value.defaultValue();
    this.placeholderOnly = false;
  }

  public ExpressionInfo(Env env) {
    this.expression = env.value();
    this.required = env.required();
    this.defaultValue = env.defaultValue();
    this.placeholderOnly = true;
  }

  public ExpressionInfo(AnnotationAttributes attributes, boolean placeholderOnly) {
    this.required = attributes.getBoolean("required");
    this.expression = attributes.getString(Constant.VALUE);
    this.defaultValue = attributes.getString("defaultValue");
    this.placeholderOnly = placeholderOnly;
  }

  public ExpressionInfo(MergedAnnotation<?> attributes, boolean placeholderOnly) {
    this.required = attributes.getBoolean("required");
    this.expression = attributes.getString(Constant.VALUE);
    this.defaultValue = attributes.getString("defaultValue");
    this.placeholderOnly = placeholderOnly;
  }

  /**
   * get default value expression or fallback expression
   */
  public String getDefaultValue() {
    return defaultValue;
  }

  public String getExpression() {
    return expression;
  }

  public boolean isRequired() {
    return required;
  }

  public boolean isPlaceholderOnly() {
    return placeholderOnly;
  }

  public void setDefaultValue(String defaultValue) {
    this.defaultValue = defaultValue;
  }

  public void setExpression(String expression) {
    this.expression = expression;
  }

  public void setPlaceholderOnly(boolean placeholderOnly) {
    this.placeholderOnly = placeholderOnly;
  }

  public void setRequired(boolean required) {
    this.required = required;
  }
}
