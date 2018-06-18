package com.yhj.web.mapping;

import java.lang.reflect.Field;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.yhj.web.conversion.Converter;
import com.yhj.web.core.Constant;
import com.yhj.web.exception.ConversionException;
import com.yhj.web.utils.NumberUtils;
import com.yhj.web.utils.ParamList;
import com.yhj.web.utils.StringUtil;


public class DefaultParameterResolver extends AbstractParameterResolver implements Constant {

	
	private static final long serialVersionUID = 4394085271334581064L;

	/***
	 * 解析参数
	 */
	@Override
	public boolean resolveParameter(Object[] args, MethodParameter[] parameters, HttpServletRequest request, HttpServletResponse response) throws Exception {

		try {
			for (int i = 0; i < parameters.length; i++) {
				MethodParameter methodParameter = parameters[i];
				args[i] = setParameter(request, response, methodParameter);
			}
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	private final Object setParameter(HttpServletRequest request, HttpServletResponse response, final MethodParameter methodParameter) throws Exception {
		System.out.print("\t普通参数注入：");
		//方法参数名
		final String methodParameterName =  methodParameter.getParameterName();
		//请求参数值
		final String requestParameter = request.getParameter(methodParameterName);
		final Class<?> parameterClass = methodParameter.getParameterClass();
		
		if(parameterClass.getSuperclass() == Number.class) {
			if (methodParameter.isRequired() && StringUtil.isEmpty(requestParameter)) {
				throw new Exception();
			}
			return NumberUtils.parseDigit(requestParameter, parameterClass);
		} else if(parameterClass.isArray()) {//判断是否是数组
			System.out.println("数组参数注入");
			
			if(StringUtil.isEmpty(requestParameter)) {
				if (methodParameter.isRequired()) {
					System.out.println("数组必须");
					throw new Exception();
				}
				return null;
			}
			return NumberUtils.parseArray(request.getParameterValues(methodParameterName), parameterClass);
		}
		
		switch (parameterClass.getName())
		{
			case TYPE_STRING:
				
				if (methodParameter.isRequired() && StringUtil.isEmpty(requestParameter)) {
					throw new Exception();
				}
				return requestParameter;
			
			case HTTP_SESSION: 			return request.getSession();
			case HTTP_SERVLET_REQUEST: 	return request;
			case HTTP_SERVLET_RESPONSE: return response;
			
			case TYPE_SET:				return resolveSetParameter(request, methodParameterName, methodParameter);
			case TYPE_MAP:				return resolveMapParameter(request, methodParameterName, methodParameter);
			case TYPE_LIST:				return resolveListParameter(request, methodParameterName, methodParameter);			

			default:
				System.out.print("\t没有普通参数注入：");
				
				// 除开普通参数注入的其他参注入
				if(this.supportsParameter(methodParameter)) {
					Converter<String, ?> converter = supportParameterTypes.get(methodParameter.getParameterClass());
					return converter.doConvert(request.getParameter(methodParameterName));
				}
				// 注入含有注解的参数 不包含 @RequestParam
				if(methodParameter.hasAnnotation()) {
					System.out.println();
					
				}
				
				Object newInstance = parameterClass.newInstance();

				if(!setBean(request, parameterClass, newInstance, request.getParameterNames(), methodParameter)) {
					throw new Exception();
				}
				return newInstance;
		}
	}


	private final boolean setBean(HttpServletRequest request , Class<?> forName, Object bean, Enumeration<String> parameterNames, MethodParameter methodParameter)
			throws Exception {
		
		System.out.print("\tPOJO参数注入\t");
		while (parameterNames.hasMoreElements()) {
			//遍历参数
			final String parameterName =  parameterNames.nextElement();
			try {
				//寻找参数
				if(!resolvePojoParameter(request, parameterName, bean, forName.getDeclaredField(parameterName), methodParameter)) {
					return false;
				}
			} catch (NoSuchFieldException e) {
				return false;
			}
		}
		return true;
	}

	/**
	 * 设置POJO属性
	 * @param request
	 * @param parameterName
	 * @param bean
	 * @param field
	 * @return
	 */
	private final boolean resolvePojoParameter(HttpServletRequest request, String parameterName, Object bean, Field field ,MethodParameter methodParameter)  {
		
		Object parseDigit = null;
		
		try {
			final Class<?> type = field.getType();
			if(type.isArray()) {
				parseDigit = NumberUtils.stringToArray(request.getParameterValues(parameterName), type);
			} else if(type.getSuperclass() == Number.class) {
				parseDigit = NumberUtils.parseDigit(request.getParameter(parameterName), type);
			} else if (type == String.class) {
				parseDigit = request.getParameter(parameterName);
			} else {
				//除开普通参数注入的其他参注入
				Converter<String, Object> converter = supportParameterTypes.get(field.getType());
				if(converter != null){
					parseDigit = converter.doConvert(request.getParameter(parameterName));
				} else {	//不支持
					return false;//Bad request.
				}
			}
			
			field.setAccessible(true);
			field.set(bean, parseDigit);
			return true;
		} catch (IllegalArgumentException | IllegalAccessException | ConversionException e) {
			return false;
		}
		
	}
	
	private final List<?> resolveListParameter(HttpServletRequest request , String parameterName, MethodParameter methodParameter) throws Exception {
		
		//http://www.yhj.com/today/user/list?user%5b0%5d.userId=90&user%5b2%5d.userId=98&user%5b1%5d.userName=Today
		
		try {
			Enumeration<String> parameterNames = request.getParameterNames();//所有参数名
			List<Object> list = new ParamList<>();
			while (parameterNames.hasMoreElements()) {
				String requestParameter = parameterNames.nextElement();

				if(requestParameter.startsWith(parameterName)){
					String[] split = requestParameter.split("(\\[|\\]|\\.)");//[use_i&&``981_r, 65651, , userN1ame]
					Class<?> clazz = methodParameter.getGenericityClass();
					
					int index = Integer.parseInt(split[1]);//得到索引
					Object newInstance = list.get(index);//没有就是空值
					if(newInstance == null) {
						newInstance= clazz.newInstance();
					}
					
					if(!resolvePojoParameter(request, requestParameter, newInstance, clazz.getDeclaredField(split[3]), methodParameter)) {//得到Field准备注入
						return list;
					}
					list.set(index, newInstance);
				}
			}
			return list;
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
	}
	
	private final Set<?> resolveSetParameter(HttpServletRequest request, String methodParameterName, MethodParameter methodParameter) throws Exception {
		return new HashSet<>(resolveListParameter(request, methodParameterName, methodParameter));
	}
	
	
	private final Map<String, Object> resolveMapParameter(HttpServletRequest request, String methodParameterName, MethodParameter methodParameter) throws Exception{
		
		try {
			Enumeration<String> parameterNames = request.getParameterNames();//所有参数名
			Map<String, Object> map = new HashMap<>();
			while (parameterNames.hasMoreElements()) {
				//users%5B%27today_1%27%5D.userId=434&users%5B%27today%27%5D.age=43&users%5B%27today%27%5D.userName=434&users%5B%27today%27%5D.sex=%E7%94%B7&users%5B%27today%27%5D.passwd=4343
				String requestParameter = parameterNames.nextElement();
				if(requestParameter.startsWith(methodParameterName)){
					
					final String[] split = requestParameter.split("(\\['|\\']|\\.)");	//[users, today, , userName]
					Class<?> clazz = methodParameter.getGenericityClass();
					
					String key = split[1];//得到key
					Object newInstance = map.get(key);//没有就是空值
					if(newInstance == null) {
						newInstance= clazz.newInstance();
					}
					
					if(!resolvePojoParameter(request, requestParameter, newInstance, clazz.getDeclaredField(split[3]), methodParameter)) {//得到Field准备注入
						return map;
					}
					
					map.put(key, newInstance);
				}
			}
			return map;
			
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
	}

}


