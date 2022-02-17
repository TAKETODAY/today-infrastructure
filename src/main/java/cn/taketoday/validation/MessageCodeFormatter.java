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

package cn.taketoday.validation;

import cn.taketoday.lang.Nullable;

/**
 * A strategy interface for formatting message codes.
 *
 * @author Chris Beams
 * @see DefaultMessageCodesResolver
 * @see DefaultMessageCodesResolver.Format
 * @since 4.0
 */
@FunctionalInterface
public interface MessageCodeFormatter {

  /**
   * Build and return a message code consisting of the given fields,
   * usually delimited by {@link DefaultMessageCodesResolver#CODE_SEPARATOR}.
   *
   * @param errorCode e.g.: "typeMismatch"
   * @param objectName e.g.: "user"
   * @param field e.g. "age"
   * @return concatenated message code, e.g.: "typeMismatch.user.age"
   * @see DefaultMessageCodesResolver.Format
   */
  String format(String errorCode, @Nullable String objectName, @Nullable String field);

}
