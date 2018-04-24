package com.yhj.web.utils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * 反射工具类
 * 
 * @huangbaoyuan
 * @version 1.0.0
 */
public final class ReflectionUtil {

	/**
	 * 创建实例
	 */
	public final static Object newInstance(Class<?> cls) {
		Object instance;
		try {
			instance = cls.newInstance();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		return instance;
	}

	/**
	 * 调用方法
	 */
	public final static Object invokeMethod(Object obj, Method method, Object... args) {
		Object result;
		try {
			method.setAccessible(true);
			result = method.invoke(obj, args);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		return result;
	}

	/**
	 * 设置成员变量的值
	 * 
	 * @throws IllegalAccessException
	 */
	public final static void setField(Object obj, Field field, Object value) throws IllegalAccessException {
		field.setAccessible(true);
		field.set(obj, value);
	}
}
