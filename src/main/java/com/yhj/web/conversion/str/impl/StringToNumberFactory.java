
package com.yhj.web.conversion.str.impl;

import com.yhj.web.conversion.Converter;
import com.yhj.web.conversion.ConverterFactory;
import com.yhj.web.utils.NumberUtils;


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
//	return new Converter<String, T> (){
//		@Override
//		public T doConvert(String source) throws ConversionException {
//			if (source.isEmpty()) {
//				return null;
//			}
//			return NumberUtils.parseNumber(source, targetType);
//		}
//	};
//}
