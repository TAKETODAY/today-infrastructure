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

package cn.taketoday.format.support;

/**
 * Converts from a String to a {@link Enum} with lenient conversion rules.
 * Specifically:
 * <ul>
 * <li>Uses a case insensitive search</li>
 * <li>Does not consider {@code '_'}, {@code '$'} or other special characters</li>
 * <li>Allows mapping of {@code "false"} and {@code "true"} to enums {@code ON} and
 * {@code OFF}</li>
 * </ul>
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
final class LenientStringToEnumConverterFactory extends LenientObjectToEnumConverterFactory<String> {

}
