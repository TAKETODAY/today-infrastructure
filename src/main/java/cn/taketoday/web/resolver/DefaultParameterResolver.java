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
package cn.taketoday.web.resolver;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import cn.taketoday.context.conversion.Converter;
import cn.taketoday.context.exception.ConversionException;
import cn.taketoday.context.utils.NumberUtils;
import cn.taketoday.context.utils.StringUtils;
import cn.taketoday.web.core.Constant;
import cn.taketoday.web.exception.BadRequestException;
import cn.taketoday.web.mapping.MethodParameter;
import cn.taketoday.web.utils.ParamList;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Today
 * @date 2018年6月25日 下午8:35:04
 * @version 2.0.0
 */
@Slf4j
public final class DefaultParameterResolver extends AbstractParameterResolver implements Constant {

	private static final long serialVersionUID = 4394085271334581064L;

	/***
	 * resolve
	 */
	@Override
	public boolean resolveParameter(Object[] args, MethodParameter[] parameters, HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		// log.debug("set parameter start");
		try {
			for (int i = 0; i < parameters.length; i++) {
				args[i] = setParameter(request, response, parameters[i]);
			}
			return true;
		} catch (BadRequestException e) {
			return false; // bad request
		}
	}

	/**
	 * resolve parameter[]
	 * 
	 * @param request
	 * @param response
	 * @param methodParameter
	 *            method parameter
	 * @return
	 * @throws Exception
	 */
	private Object setParameter(HttpServletRequest request, HttpServletResponse response,
			final MethodParameter methodParameter) throws Exception {
		// 方法参数名
		final String methodParameterName = methodParameter.getParameterName();

		final Class<?> parameterClass = methodParameter.getParameterClass();

		switch (parameterClass.getName()) //
		{
			case HTTP_SESSION: 			return request.getSession();
			case HTTP_SERVLET_CONTEXT:  return request.getServletContext();
			case HTTP_SERVLET_REQUEST:  return request;
			case HTTP_SERVLET_RESPONSE: return response;
	
			// Collection parameter []
			case TYPE_SET:				return resolveSetParameter(request, methodParameterName, methodParameter);
			case TYPE_MAP:				return resolveMapParameter(request, methodParameterName, methodParameter);
			case TYPE_LIST:				return resolveListParameter(request, methodParameterName, methodParameter);
	
			case TYPE_OPTIONAL:			return resolveOptionalParameter(request, methodParameterName, methodParameter);

		}

		return resolve(request, methodParameter, methodParameterName, parameterClass);
	}


	/**
	 * resolve.
	 * 
	 * @param request
	 * @param methodParameter
	 * @param methodParameterName
	 * @param parameterClass
	 * @return
	 * @throws ConversionException
	 * @throws Exception
	 * @throws BadRequestException
	 */
	private final Object resolve(HttpServletRequest request, final MethodParameter methodParameter,
			final String methodParameterName, final Class<?> parameterClass)
			throws ConversionException, Exception, BadRequestException {
		// 
		if (this.supportsParameter(methodParameter)) {
			log.debug("set other support parameter -> {}", methodParameterName);

			Converter<String, ?> converter = supportParameterTypes.get(methodParameter.getParameterClass());
			return converter.doConvert(request.getParameter(methodParameterName));
		}

		// resolve annotation parameter expect @RequestParam
		if (methodParameter.hasAnnotation()) {
			log.debug("set annotation parameter -> [{}]", methodParameterName);
			return resolveAnnotationParameter(request, methodParameterName, methodParameter);
		}

		if (parameterClass.getSuperclass() == Number.class) {

			log.debug("set number parameter -> [{}]", methodParameterName);
			final String requestParameter = request.getParameter(methodParameterName);// request parameter value
			if (StringUtils.isEmpty(requestParameter)) {
				if (methodParameter.isRequired()) {
					log.debug("parameter -> [{}] is required", methodParameterName);
					throw new BadRequestException(); // bad request
				}
				return null; // default value
			} // parse number
			return NumberUtils.parseDigit(requestParameter, parameterClass);

		} else if (parameterClass.isArray()) { // array
			log.debug("set array parameter -> [{}]", methodParameterName);
			// parameter value[]
			final String[] parameterValues = request.getParameterValues(methodParameterName);
			if (parameterValues.length == 0) {
				if (methodParameter.isRequired()) {
					log.debug("array parameter -> [{}] is required", methodParameterName);
					throw new BadRequestException();
				}
				return null;
			}
			return NumberUtils.parseArray(parameterValues, parameterClass);
		}

		if (parameterClass == String.class) {
			// parameter value
			final String requestParameter = request.getParameter(methodParameterName);
			if (methodParameter.isRequired() && (requestParameter == null || "".equals(requestParameter))) {
				log.debug("parameter -> [{}] is required", methodParameterName);
				throw new BadRequestException("parameter is required");
			}
			return requestParameter;
		}
		// resolve pojo
		log.debug("set pojo parameter -> {}", methodParameterName);
		Object newInstance = null;
		try {
			newInstance = parameterClass.newInstance();
		} catch (InstantiationException | IllegalAccessException e) {
			throw new BadRequestException();
		}

		// pojo
		if (!setBean(request, parameterClass, newInstance, request.getParameterNames(), methodParameter)) {

			throw new BadRequestException("set pojo error");
		}
		return newInstance;
	}

	/**
	 * resolve Optional Parameter
	 * 
	 * @param request
	 * @param methodParameterName
	 * @param methodParameter
	 * @return
	 * @throws Exception
	 */
	private Object resolveOptionalParameter(HttpServletRequest request, String methodParameterName,
			MethodParameter methodParameter) throws Exception {

		return Optional.ofNullable(
				resolve(request, methodParameter, methodParameterName, methodParameter.getGenericityClass()));
	}

	/**
	 * resolve annotation parameter
	 * 
	 * @param request
	 * @param methodParameterName
	 * @param methodParameter
	 * @return
	 * @throws BadRequestException
	 */
	private final Object resolveAnnotationParameter(HttpServletRequest request, String methodParameterName,
			MethodParameter methodParameter) throws Exception {

		switch (methodParameter.getAnnotation()) //
		{
		case ANNOTATION_COOKIE: {
			return cookie(request, methodParameterName, methodParameter);
		}
		case ANNOTATION_SESSION: {
			return request.getSession().getAttribute(methodParameterName);
		}
		case ANNOTATION_MULTIPART: { // resolve multipart
			try {
				if (multipartResolver.isMultipart(request)) {
					return multipartResolver.resolveMultipart(request, methodParameterName, methodParameter);
				}
				throw new BadRequestException("this isn't multipart request.");
			} finally {
				multipartResolver.cleanupMultipart(request);
			}
		}
		case ANNOTATION_HEADER: {

			final String header = request.getHeader(methodParameterName);
			if (methodParameter.isRequired() && (header == null || "".equals(header.trim()))) {
				throw new BadRequestException("header -> " + methodParameterName + " can't be null");
			}
			return header;
		}
		case ANNOTATION_PATH_VARIABLE: {
			return pathVariable(request, methodParameterName, methodParameter);
		}
		case ANNOTATION_REQUESTBODY: {

			String body = null;
			try {
				body = request.getReader().readLine();
			} catch (IOException e) {
				log.error("request body read error.", e);
				throw new BadRequestException("request body read error.", e);
			}
			return JSON.parseObject(body, methodParameter.getParameterClass());
		}
		}
		return null;
	}

	/**
	 * set Path Variable arg
	 * 
	 * @param request
	 * @param methodParameterName
	 * @param methodParameter
	 * @return
	 * @throws BadRequestException
	 */
	private Object pathVariable(HttpServletRequest request, String methodParameterName, MethodParameter methodParameter)
			throws BadRequestException {
		try {
			String requestURI = request.getRequestURI();
			final String[] splitRegex = ((String) request.getAttribute("REGEX")).split(Constant.PATH_VARIABLE_REGEXP);

			for (String reg : splitRegex) {
				requestURI = requestURI.replace(reg, "\\");
			}
			String requestParameter = requestURI.split("\\\\")[methodParameter.getPathIndex()];
			// get parameter class
			final Class<?> parameterClass = methodParameter.getParameterClass();
			if (parameterClass.getSuperclass() == Number.class) {
				log.debug("number path variable -> [{}]", methodParameterName);
				// -> parse number
				return NumberUtils.parseDigit(requestParameter, parameterClass);
			}
			return requestParameter;
		} catch (ConversionException e) {
			log.error("path variable error", e);
			throw new BadRequestException("path variable -> " + methodParameterName + " can't be resolve.");
		}
	}

	/**
	 * get cookie
	 * 
	 * @param request
	 * @param methodParameterName
	 * @param methodParameter
	 * @return
	 * @throws BadRequestException
	 */
	private Object cookie(HttpServletRequest request, String methodParameterName, MethodParameter methodParameter)
			throws BadRequestException {
		final Cookie[] cookies = request.getCookies();
		for (Cookie cookie : cookies) {
			if (methodParameterName.equals(cookie.getName())) {
				return cookie.getValue();
			}
		}
		// no cookie
		if (methodParameter.isRequired()) {
			throw new BadRequestException("cookie -> " + methodParameterName + " can't be null");
		}
		return null;
	}

	/**
	 * resolve pojo
	 * 
	 * @param request
	 * @param forName
	 * @param bean
	 * @param parameterNames
	 * @param methodParameter
	 * @return
	 * @throws BadRequestException
	 */
	private final boolean setBean(HttpServletRequest request, Class<?> forName, Object bean,
			Enumeration<String> parameterNames, MethodParameter methodParameter) throws Exception {

		while (parameterNames.hasMoreElements()) {
			// 遍历参数
			final String parameterName = parameterNames.nextElement();
			// 寻找参数
			if (!resolvePojoParameter(request, parameterName, bean, forName.getDeclaredField(parameterName),
					methodParameter)) {

				return false;
			}
		}
		return true;
	}

	/**
	 * 设置POJO属性
	 * 
	 * @param request
	 * @param parameterName
	 * @param bean
	 * @param field
	 * @return
	 */
	private final boolean resolvePojoParameter(HttpServletRequest request, String parameterName, Object bean,
			Field field, MethodParameter methodParameter) throws Exception {

		Object parseDigit = null;

		final Class<?> type = field.getType();
		if (type.isArray()) {
			parseDigit = NumberUtils.toArrayObject(request.getParameterValues(parameterName), type);
		} else {
			String parameter = request.getParameter(parameterName);
			if (StringUtils.isEmpty(parameter)) {
				return true;
			}
			if (type.getSuperclass() == Number.class) {

				parseDigit = NumberUtils.parseDigit(parameter, type);
			} else if (type == String.class) {
				parseDigit = parameter;
			} else {
				// 除开普通参数注入的其他参注入
				Converter<String, Object> converter = supportParameterTypes.get(field.getType());
				if (converter != null) {
					parseDigit = converter.doConvert(parameter);
				} else { // 不支持
					throw new BadRequestException("parameter not supported");
				}
			}
		}

		field.setAccessible(true);
		field.set(bean, parseDigit);
		return true;
	}

	/**
	 * resolve list parameter
	 * 
	 * @param request
	 * @param parameterName
	 * @param methodParameter
	 * @return
	 * @throws BadRequestException
	 */
	private final List<?> resolveListParameter(HttpServletRequest request, String parameterName,
			MethodParameter methodParameter) throws Exception {

		if (methodParameter.isRequestBody()) {
			String body = null;
			try {
				body = request.getReader().readLine();
			} catch (IOException e) {
				log.error("list request body read error.", e);
				throw new BadRequestException("list request body read error.");
			}

			return JSONArray.parseArray(JSONObject.parseObject(body).get(parameterName).toString(),
					methodParameter.getGenericityClass());
		}

		try {
			// http://www.yhj.com/today/user/list?user%5b0%5d.userId=90&user%5b2%5d.userId=98&user%5b1%5d.userName=Today

			Enumeration<String> parameterNames = request.getParameterNames();// 所有参数名
			List<Object> list = new ParamList<>();
			while (parameterNames.hasMoreElements()) {

				String requestParameter = parameterNames.nextElement();

				if (requestParameter.startsWith(parameterName)) {
					String[] split = requestParameter.split("(\\[|\\]|\\.)");// [use_i&&``981_r, 65651, , userName]
					Class<?> clazz = methodParameter.getGenericityClass();

					int index = Integer.parseInt(split[1]);// 得到索引
					Object newInstance = list.get(index);// 没有就是空值
					if (newInstance == null) {
						newInstance = clazz.newInstance();
					}

					if (!resolvePojoParameter(request, requestParameter, newInstance, clazz.getDeclaredField(split[3]),
							methodParameter)) {// 得到Field准备注入
						return list;
					}
					list.set(index, newInstance);
				}
			}
			return list;
		} catch (Exception ex) {
			log.error("ERROR -> [{}] caused by {}", ex.getMessage(), ex.getCause(), ex);
		}
		return null;
	}

	/**
	 * resolve set parameter
	 * 
	 * @param request
	 * @param methodParameterName
	 *            -> parameter name
	 * @param methodParameter
	 *            -> method parameter
	 * @return
	 * @throws Exception
	 */
	private final Set<?> resolveSetParameter(HttpServletRequest request, String methodParameterName,
			MethodParameter methodParameter) throws Exception {
		return new HashSet<>(resolveListParameter(request, methodParameterName, methodParameter));
	}

	/**
	 * resolve map parameter
	 * 
	 * @param request
	 * @param methodParameterName
	 *            -> parameter name
	 * @param methodParameter
	 *            -> method parameter
	 * @return method parameter type -> Map
	 * @throws Exception
	 */
	private final Map<String, Object> resolveMapParameter(HttpServletRequest request, String methodParameterName,
			MethodParameter methodParameter) throws Exception {

		Enumeration<String> parameterNames = request.getParameterNames();// 所有参数名
		Map<String, Object> map = new HashMap<>();
		while (parameterNames.hasMoreElements()) {
			// users%5B%27today_1%27%5D.userId=434&users%5B%27today%27%5D.age=43&users%5B%27today%27%5D.userName=434&users%5B%27today%27%5D.sex=%E7%94%B7&users%5B%27today%27%5D.passwd=4343
			String requestParameter = parameterNames.nextElement();
			if (requestParameter.startsWith(methodParameterName)) {

				final String[] split = requestParameter.split("(\\['|\\']|\\.)"); // [users, today, , userName]
				Class<?> clazz = methodParameter.getGenericityClass();

				String key = split[1];// 得到key
				Object newInstance = map.get(key);// 没有就是空值
				if (newInstance == null) {
					newInstance = clazz.newInstance();
				}

				if (!resolvePojoParameter(request, requestParameter, newInstance, clazz.getDeclaredField(split[3]),
						methodParameter)) {// 得到Field准备注入
					return map;
				}

				map.put(key, newInstance);
			}
		}
		return map;
	}

}
