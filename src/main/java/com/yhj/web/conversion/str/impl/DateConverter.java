package com.yhj.web.conversion.str.impl;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.yhj.web.conversion.Converter;
import com.yhj.web.exception.ConversionException;


public final class DateConverter implements Converter<String, Date> {
	
	@Override
	public Date doConvert(String source) throws ConversionException{

		if (source == null) {
			return null;
		}
		try {
			return new SimpleDateFormat("yyyy-MM-dd").parse(source);
		} catch (ParseException e) {
			throw new ConversionException();
		}
	}
}


