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

package cn.taketoday.web.handler.method;

import java.lang.annotation.Annotation;

import cn.taketoday.core.annotation.MergedAnnotation;
import cn.taketoday.lang.Constant;
import cn.taketoday.lang.Nullable;

/**
 * Represents the information about a named value,
 * including name, whether it's required and a default value.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see cn.taketoday.web.annotation.RequestParam
 * @since 4.0 2022/1/19 21:50
 */
public class NamedValueInfo {

  public final String name;
  public final boolean required;

  @Nullable
  public final String defaultValue;

  public boolean containsPlaceHolder;

  public NamedValueInfo(MergedAnnotation<Annotation> annotation) {
    this.required = annotation.getValue("required", boolean.class).orElse(false);
    this.name = annotation.getValue("name", String.class).orElse("");
    this.defaultValue = annotation.getValue("defaultValue", String.class).orElse(Constant.DEFAULT_NONE);
  }

  public NamedValueInfo(String name) {
    this.name = name;
    this.required = true;
    this.defaultValue = null;
  }

  public NamedValueInfo(String name, boolean required, @Nullable String defaultValue) {
    this.name = name;
    this.required = required;
    this.defaultValue = defaultValue;
  }

}
