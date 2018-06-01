
package com.yhj.web.conversion;

import com.yhj.web.exception.ConversionException;

public interface Converter<S, T> {

	public T doConvert(S source) throws ConversionException;


}
