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
package cn.taketoday.context.conversion;

import cn.taketoday.context.utils.NumberUtils;


public final class StringToNumberFactory implements ConverterFactory<String, Number> {

	
	@Override
	public <T extends Number> Converter<String, T> getConverter(Class<T> targetType) {
		return source -> {
			return NumberUtils.parseNumber(source, targetType);
		};
	}

}

//@Override
//public <T extends Number> Converter<String, T> getConverter(Class<T> targetType) {
//	return new Converter<String, T> () {
//		@Override
//		public T doConvert(String source) throws ConversionException {
//			if (source.isEmpty()) {
//				return null;
//			}
//			return NumberUtils.parseNumber(source, targetType);
//		}
//	};
//}
