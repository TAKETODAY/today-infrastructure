/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.context.properties;

import java.util.Optional;

/**
 * Data object with a public method picked up by the {@code ObjectToObjectConverter}. Used
 * in {@link ConfigurationPropertiesTests}.
 *
 * @author Phillip Webb
 */
public class WithPublicObjectToObjectMethod {

  private final String value;

  WithPublicObjectToObjectMethod(String value) {
    this.value = value;
  }

  String getValue() {
    return this.value;
  }

  public static Optional<WithPublicObjectToObjectMethod> from(String value) {
    return Optional.of(new WithPublicObjectToObjectMethod(value));
  }

}
