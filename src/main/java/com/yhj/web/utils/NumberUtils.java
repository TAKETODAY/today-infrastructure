
package com.yhj.web.utils;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;

import com.yhj.web.core.Constant;
import com.yhj.web.exception.ConversionException;

public abstract class NumberUtils implements Constant{

	private static final long serialVersionUID = 3935474383079641578L;

	public static void main(String[] args) throws ClassNotFoundException {
		long start = System.currentTimeMillis();
		long nano = System.nanoTime();
		try {
			
			Object parseNumber = NumberUtils.stringToNumber("6273672", long.class);
			System.out.println(parseNumber.getClass());
			
//			Object parseArray = NumberUtils.parseArray(new String[] {"12","12222","12121" }, Class.forName("[Ljava.lang.Integer;"));
//			Long[] parseArray = NumberUtils.parseArray(new String[] {"12","12222","12121" }, Long[].class);

			int[] parseArray = NumberUtils.parseArray(new String[] {"12","12222","12121","56723562" }, int[].class);
//			Double[] parseArray = NumberUtils.parseArray(new String[] {"12","12222","12121","4789236426427676" }, Double[].class);

			
//			for(Integer i : parseArray) {
//				System.out.println(i);
//			}
			
			System.out.println(parseArray);
			
			System.out.println(Arrays.toString(parseArray));
			
		} catch (ConversionException e) {
			e.printStackTrace();
		}
		
		System.out.println(System.currentTimeMillis() - start + "ms");
		System.out.println(System.nanoTime() - nano + "ns");
	}
	
	@SuppressWarnings("unchecked")
	public final static <T> T parseArray(String source[], Class<T> targetClass) throws ConversionException{
		final int length = source.length;

		if(int[].class == targetClass) {
			int [] newInstance = new int[length];
			for(short j = 0 ; j < length ; j++)
				newInstance[j] = Integer.parseInt(source[j]);
			return (T) newInstance;
		} else if(Integer[].class == targetClass) {
			Integer [] newInstance = new Integer[length];
			for(short j = 0 ; j < length ; j++)
				newInstance[j] = Integer.parseInt(source[j]);
			return (T) newInstance;
		} else if(long[].class == targetClass) {
			long[] newInstance = new long[length];
			for(short j = 0 ; j < length ; j++)
				newInstance[j] = Long.parseLong(source[j]);
			return (T) newInstance;
		} else if(Long[].class == targetClass) {
			Long[] newInstance = new Long[length];
			for(short j = 0 ; j < length ; j++)
				newInstance[j] = Long.parseLong(source[j]);
			return (T) newInstance;
		} else if(short[].class == targetClass) {
			short[] newInstance = new short[length];
			for(short j = 0 ; j < length ; j++)
				newInstance[j] = Short.parseShort(source[j]);
			return (T) newInstance;
		} else if(Short[].class == targetClass) {
			Short[] newInstance = new Short[length];
			for(short j = 0 ; j < length ; j++)
				newInstance[j] = Short.parseShort(source[j]);
			return (T) newInstance;
		} else if(byte[].class == targetClass) {
			byte[] newInstance = new byte[length];
			for(short j = 0 ; j < length ; j++)
				newInstance[j] = Byte.parseByte(source[j]);
			return (T) newInstance;
		} else if(Byte[].class == targetClass) {
			Byte[] newInstance = new Byte[length];
			for(short j = 0 ; j < length ; j++)
				newInstance[j] = Byte.parseByte(source[j]);
			return (T) newInstance;
		} else if(float[].class == targetClass) {
			float[] newInstance = new float[length];
			for(short j = 0 ; j < length ; j++)
				newInstance[j] = Float.parseFloat(source[j]);
			return (T) newInstance;
		} else if(Float[].class == targetClass) {
			Float[] newInstance = new Float[length];
			for(short j = 0 ; j < length ; j++)
				newInstance[j] = Float.parseFloat(source[j]);
			return (T) newInstance;
		} else if(double[].class == targetClass) {
			double[] newInstance = new double[length];
			for(short j = 0 ; j < length ; j++)
				newInstance[j] = Double.parseDouble(source[j]);
			return (T) newInstance;
		} else if(Double[].class == targetClass) {
			Double[] newInstance = new Double[length];
			for(short j = 0 ; j < length ; j++)
				newInstance[j] = Double.parseDouble(source[j]);
			return (T) newInstance;
		}
		
		throw new ConversionException( "不能将字符串数组[" + source.toString() + "] 转换成 [" + targetClass.getName() + "]");
	}

	public final static Object stringToNumber(String text, Class<?> targetClass) throws ConversionException{
		
		if (Byte.class == targetClass || byte.class == targetClass) {
			return Byte.parseByte(text);
		} else if (Short.class == targetClass || short.class == targetClass) {
			return Short.parseShort(text);
		} else if (Integer.class == targetClass || int.class == targetClass) {
			return Integer.parseInt(text);
		} else if (Long.class == targetClass || long.class == targetClass) {
			return Long.parseLong(text);
		} else if (BigInteger.class == targetClass) {
			return new BigInteger(text);
		} else if (Float.class == targetClass || float.class == targetClass) {
			return Float.parseFloat(text);
		} else if (Double.class == targetClass || double.class == targetClass) {
			return Double.parseDouble(text);
		} else if (BigDecimal.class == targetClass || Number.class == targetClass) {
			return new BigDecimal(text);
		}
	
		throw new ConversionException("不能将字符串[" + text + "] 转换成 [" + targetClass.getName() + "]");
	
	}
	
	@SuppressWarnings("unchecked")
	public final static <T extends Number> T parseNumber(String text, Class<T> targetClass) throws ConversionException{

		if (Byte.class == targetClass || byte.class == targetClass) {
			return (T) Byte.valueOf(text);
		} else if (Short.class == targetClass || short.class == targetClass) {
			return (T) Short.valueOf(text);
		} else if (Integer.class == targetClass || int.class == targetClass) {
			return (T) Integer.valueOf(text);
		} else if (Long.class == targetClass || long.class == targetClass) {
			return (T)  Long.valueOf(text);
		} else if (BigInteger.class == targetClass) {
			return (T)  new BigInteger(text);
		} else if (Float.class == targetClass || float.class == targetClass) {
			return (T) Float.valueOf(text);
		} else if (Double.class == targetClass || double.class == targetClass) {
			return (T) Double.valueOf(text);
		} else if (BigDecimal.class == targetClass || Number.class == targetClass) {
			return (T) new BigDecimal(text);
		} else {
			throw new IllegalArgumentException("不能将字符串[" + text + "] 转换成 [" + targetClass.getName() + "]");
		}
	}

}
