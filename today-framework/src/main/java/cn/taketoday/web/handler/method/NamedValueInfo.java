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

import cn.taketoday.beans.factory.config.EmbeddedValueResolver;
import cn.taketoday.lang.Nullable;

/**
 * Represents the information about a named value,
 * including name, whether it's required and a default value.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see cn.taketoday.web.annotation.RequestParam
 * @since 4.0 2022/1/19 21:50
 */
public final class NamedValueInfo {

  public final String name;

  // default is true (required)
  public final boolean required;

  @Nullable
  public final String defaultValue;

  public final boolean nameEmbedded;
  public final boolean defaultValueEmbedded;

  public NamedValueInfo(String name) {
    this(name, true, null);
  }

  public NamedValueInfo(String name, boolean required, @Nullable String defaultValue) {
    this.name = name;
    this.required = required;
    this.defaultValue = defaultValue;
    this.nameEmbedded = EmbeddedValueResolver.isEmbedded(name);
    this.defaultValueEmbedded = EmbeddedValueResolver.isEmbedded(defaultValue);
  }

}
