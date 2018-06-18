
package com.yhj.web.conversion;

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



