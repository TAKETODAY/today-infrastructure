/*
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
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
package cn.taketoday.context.conversion.support;

import cn.taketoday.context.conversion.ConversionService;
import cn.taketoday.context.conversion.Converter;

/**
 * Calls {@link Enum#ordinal()} to convert a source Enum to a Integer.
 * This converter will not match enums with interfaces that can be converted.
 *
 * @author Yanming Zhou
 * @since 3.0
 */
final class EnumToIntegerConverter implements Converter<Enum<?>, Integer> {

	@Override
	public Integer convert(Enum<?> source) {
		return source.ordinal();
	}

}
