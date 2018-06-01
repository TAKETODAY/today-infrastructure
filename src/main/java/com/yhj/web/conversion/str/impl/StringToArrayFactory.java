
package com.yhj.web.conversion.str.impl;

import com.yhj.web.conversion.Converter;
import com.yhj.web.conversion.ConverterFactory;
import com.yhj.web.utils.NumberUtils;

public final class StringToArrayFactory implements ConverterFactory<String[], Object> {

	@Override
	public <T> Converter<String[], T> getConverter(Class<T> targetClass) {
		return source -> {
			return NumberUtils.parseArray(source, targetClass);
		};
	}
	
//	@Override
//	public <T> Converter<String[], T> getConverter(Class<T> targetClass) {
//		return source -> {
//			return NumberUtils.parseArray(source, targetClass);
//		};
//	}

	

}



