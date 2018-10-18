/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn Copyright
 * © Today & 2017 - 2018 All Rights Reserved.
 * 
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU Affero General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package cn.taketoday.web.resolver;

import cn.taketoday.context.conversion.Converter;
import cn.taketoday.context.exception.ConversionException;
import cn.taketoday.context.utils.NumberUtils;
import cn.taketoday.context.utils.StringUtils;
import cn.taketoday.web.Constant;
import cn.taketoday.web.exception.BadRequestException;
import cn.taketoday.web.mapping.MethodParameter;
import cn.taketoday.web.ui.ModelMap;
import cn.taketoday.web.utils.ParamList;

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

import lombok.extern.slf4j.Slf4j;

/**
 * 
 * @author Today <br>
 * @version 2.0.0<br>
 *          2018-06-25 20:35:04 <br>
 *          2018-08-21 21:05 <b>change:</b> add default value feature.
 */
@Slf4j
public final class DefaultParameterResolver extends AbstractParameterResolver implements Constant {

	private static final long serialVersionUID = 4394085271334581064L;

	/***
	 * resolve
	 */
	@Override
	public void resolveParameter(Object[] args, MethodParameter[] parameters, HttpServletRequest request,
			HttpServletResponse response) throws Throwable {
		// log.debug("set parameter start");
		for (int i = 0; i < parameters.length; i++) {
			args[i] = setParameter(request, response, parameters[i]);
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
			final MethodParameter methodParameter) throws Throwable {

		// 方法参数名
		final String methodParameterName = methodParameter.getParameterName();
		// resolve annotation parameter expect @RequestParam
		if (methodParameter.hasAnnotation()) {
			return resolveAnnotationParameter(methodParameterName, request, methodParameter);
		}
		switch (methodParameter.getParameterType())
		{
			case TYPE_HTTP_SESSION :
				return request.getSession();
			case TYPE_SERVLET_CONTEXT :
				return request.getServletContext();
			case TYPE_HTTP_SERVLET_REQUEST :
				return request;
			case TYPE_HTTP_SERVLET_RESPONSE :
				return response;
			case TYPE_SET :
				return resolveSetParameter(request, methodParameterName, methodParameter);
			case TYPE_MAP :
				return resolveMapParameter(request, methodParameterName, methodParameter);
			case TYPE_LIST :
				return resolveListParameter(request, methodParameterName, methodParameter);
			case TYPE_OPTIONAL :
				return resolveOptionalParameter(request, methodParameterName, methodParameter);
			case TYPE_ARRAY :
				return resolveArrayParameter(request, methodParameterName, methodParameter);
			case TYPE_STRING :
				return resolveStringParameter(request, methodParameterName, methodParameter);

			case TYPE_BYTE :
				return resolveParameter(//
						request, methodParameterName, methodParameter, //
						parameter -> Byte.parseByte(parameter)//
				);
			case TYPE_INT :
				return resolveParameter(//
						request, methodParameterName, methodParameter, //
						parameter -> Integer.parseInt(parameter)//
				);
			case TYPE_SHORT :
				return resolveParameter(//
						request, methodParameterName, methodParameter, //
						parameter -> Short.parseShort(parameter)//
				);
			case TYPE_LONG :
				return resolveParameter(//
						request, methodParameterName, methodParameter, //
						parameter -> Long.parseLong(parameter)//
				);

			case TYPE_DOUBLE :
				return resolveParameter(//
						request, methodParameterName, methodParameter, //
						parameter -> Double.parseDouble(parameter)//
				);
			case TYPE_FLOAT :
				return resolveParameter(//
						request, methodParameterName, methodParameter, //
						parameter -> Float.parseFloat(parameter)//
				);
			case TYPE_BOOLEAN :
				return resolveParameter(//
						request, methodParameterName, methodParameter, //
						parameter -> Boolean.parseBoolean(parameter)//
				);

			case TYPE_MODEL :
				return new ModelMap(request);
		}

		return resolve(request, methodParameter, methodParameterName, methodParameter.getParameterClass());
	}

	/**
	 * 
	 * @param request
	 * @param methodParameterName
	 * @param methodParameter
	 * @param converter
	 * @return
	 * @throws BadRequestException
	 */
	private Object resolveParameter(HttpServletRequest request, String methodParameterName,
			MethodParameter methodParameter, Converter<String, Object> converter) throws BadRequestException {

		String requestParameter = request.getParameter(methodParameterName);
		if (StringUtils.isEmpty(requestParameter)) {
			if (methodParameter.isRequired()) {
				log.debug("parameter -> [{}] is required, bad request.", methodParameterName);
				throw new BadRequestException("parameter is required, bad request.");
			}
			return converter.doConvert(methodParameter.getDefaultValue());
		}
		return converter.doConvert(requestParameter);
	}

	/**
	 * 
	 * @param request
	 * @param methodParameterName
	 * @param methodParameter
	 * @return
	 * @throws BadRequestException
	 */
	private Object resolveStringParameter(HttpServletRequest request, String methodParameterName,
			MethodParameter methodParameter) throws BadRequestException {
		// parameter value
		String requestParameter = request.getParameter(methodParameterName);

		if (StringUtils.isEmpty(requestParameter)) {
			if (methodParameter.isRequired()) {
				log.debug("parameter -> [{}] is required, bad request.", methodParameterName);
				throw new BadRequestException("parameter is required, bad request.");
			}
			return methodParameter.getDefaultValue();
		}
		return requestParameter;
	}

	/**
	 * 
	 * @param request
	 * @param methodParameterName
	 * @param methodParameter
	 * @return
	 * @throws BadRequestException
	 */
	private Object resolveArrayParameter(HttpServletRequest request, String methodParameterName,
			MethodParameter methodParameter) throws BadRequestException {

//		log.debug("set array parameter -> [{}]", methodParameterName);
		// parameter value[]
		final String[] parameterValues = request.getParameterValues(methodParameterName);
		if (parameterValues.length == 0) {
			if (methodParameter.isRequired()) {
				log.debug("array parameter -> [{}] is required, bad request.", methodParameterName);
				throw new BadRequestException(methodParameterName + " is required, bad request.");
			}
			return null;
		}
		return NumberUtils.parseArray(parameterValues, methodParameter.getParameterClass());
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
			final String methodParameterName, final Class<?> parameterClass) throws Throwable {

		if (this.supportsParameter(methodParameter)) {
			// log.debug("set other support parameter -> {}", methodParameterName);
			Converter<String, ?> converter = supportParameterTypes.get(methodParameter.getParameterClass());
			return converter.doConvert(request.getParameter(methodParameterName));
		}

		// resolve pojo
		log.debug("set pojo parameter -> {}", methodParameterName);
		Object newInstance = null;
		try {

			newInstance = objectFactory.create(parameterClass);
		} //
		catch (Throwable e) {
			throw new BadRequestException("Can't resolve pojo, bad request.");
		}

		// pojo
		if (!setBean(request, parameterClass, newInstance, request.getParameterNames(), methodParameter)) {
			throw new BadRequestException("Can't resolve pojo, bad request.");
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
	private final Object resolveOptionalParameter(HttpServletRequest request, String methodParameterName,
			MethodParameter methodParameter) throws Throwable {

		return Optional.ofNullable(//
				resolve(request, methodParameter, methodParameterName, methodParameter.getGenericityClass())//
		);
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
	private final Object resolveAnnotationParameter(String methodParameterName, HttpServletRequest request,
			MethodParameter methodParameter) throws Throwable {

//		log.debug("Set annotation parameter -> [{}]", methodParameterName);

		switch (methodParameter.getAnnotation()) //
		{
			case ANNOTATION_COOKIE : { // cookie
				return cookie(request, methodParameterName, methodParameter);
			}
			case ANNOTATION_SESSION : {
				return request.getSession().getAttribute(methodParameterName);
			}
			case ANNOTATION_MULTIPART : { // resolve multipart
				try {
					if (multipartResolver.isMultipart(request)) {
						return multipartResolver.resolveMultipart(request, methodParameterName, methodParameter);
					}
					throw new BadRequestException("This isn't multipart request, bad request.");
				} finally {
					multipartResolver.cleanupMultipart(request);
				}
			}
			case ANNOTATION_HEADER : {

				final String header = request.getHeader(methodParameterName);

				if (methodParameter.isRequired() && (header == null || "".equals(header))) {
					throw new BadRequestException("Header: [" + methodParameterName + "] can't be null, bad request");
				}

				return header == null ? methodParameter.getDefaultValue() : header;
			}
			case ANNOTATION_PATH_VARIABLE : {
				return pathVariable(request, methodParameterName, methodParameter);
			}
			case ANNOTATION_REQUESTBODY : {
				try {

					return JSON.parseObject(request.getReader().readLine(), methodParameter.getParameterClass());
				} //
				catch (IOException e) {
					log.error("Request body read error.", e);
					throw new BadRequestException("request body read error.", e);
				}
			}
			case ANNOTATION_SERVLET_CONTEXT : {
				return servletContext.getAttribute(methodParameterName);
			}
		}
		return null;
	}

	/**
	 * Set Path Variable parameter.
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

			for (String regex : methodParameter.getSplitMethodUrl()) {
				requestURI = requestURI.replace(regex, "\\");
			}
			final String value = requestURI.split(Constant.REPLACE_REGEXP)[methodParameter.getPathIndex()];
			switch (methodParameter.getParameterType())
			{
				case TYPE_STRING :
					return value;
				case TYPE_BYTE :
					return Byte.parseByte(value);
				case TYPE_INT :
					return Integer.parseInt(value);
				case TYPE_SHORT :
					return Short.parseShort(value);
				case TYPE_LONG :
					return Long.parseLong(value);
				case TYPE_DOUBLE :
					return Double.parseDouble(value);
				case TYPE_FLOAT :
					return Float.parseFloat(value);
			}
			throw new BadRequestException(
					"Path variable: '" + methodParameterName + "' can't be resolve, bad request.");
		} catch (Throwable e) {
			log.error("Path variable: '{}' can't be resolve, bad request.", methodParameterName, e);
			throw new BadRequestException(
					"Path variable: '" + methodParameterName + "' can't be resolve, bad request.");
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
	private final Object cookie(HttpServletRequest request, String methodParameterName, MethodParameter methodParameter)
			throws BadRequestException {

		final Cookie[] cookies = request.getCookies();

		if (cookies == null) {
			return null;
		}
		for (Cookie cookie : cookies) {
			if (methodParameterName.equals(cookie.getName())) {
				return cookie.getValue();
			}
		}
		// no cookie
		if (methodParameter.isRequired()) {
			throw new BadRequestException("cookie: [" + methodParameterName + "] can't be null, bad request.");
		}
		return methodParameter.getDefaultValue(); // return default value.
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
			Enumeration<String> parameterNames, MethodParameter methodParameter) throws Throwable {

		try {
			while (parameterNames.hasMoreElements()) {
				// 遍历参数
				final String parameterName = parameterNames.nextElement();
				// 寻找参数

				if (!resolvePojoParameter(request, parameterName, bean, forName.getDeclaredField(parameterName),
						methodParameter)) {
					return false;
				}
			}
		} catch (NoSuchFieldException e) {
			// continue;
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
			Field field, MethodParameter methodParameter) throws Throwable {

		Object property = null;

		final Class<?> type = field.getType();
		if (type.isArray()) {
			property = NumberUtils.toArrayObject(request.getParameterValues(parameterName), type);

		} else {
			String parameter = request.getParameter(parameterName);
			if (StringUtils.isEmpty(parameter)) {
				return true;
			}
			if (type.getSuperclass() == Number.class || type == byte.class || type == short.class || type == float.class
					|| type == int.class || type == long.class || type == double.class) {
				
				property = NumberUtils.parseDigit(parameter, type);
			} else if (type == String.class) {
				property = parameter;
			} else {
				// 除开普通参数注入的其他参注入
				Converter<String, Object> converter = supportParameterTypes.get(type);
				if (converter != null) {
					property = converter.doConvert(parameter);
				} else { // 不支持

					throw new BadRequestException("parameter not supported, bad request.");
				}
			}
		}

		field.setAccessible(true);
		field.set(bean, property);
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
			MethodParameter methodParameter) throws Throwable {

		if (methodParameter.isRequestBody()) {
			try {

				return JSONArray.parseArray(
						JSONObject.parseObject(request.getReader().readLine()).get(parameterName).toString(),
						methodParameter.getGenericityClass());
			} catch (IOException e) {
				log.error("List request body read error.", e);
				throw new BadRequestException("List request body read error.");
			}
		}

		try {

			// https://taketoday.cn/today/user/list?user%5b0%5d.userId=90&user%5b2%5d.userId=98&user%5b1%5d.userName=Today
			Enumeration<String> parameterNames = request.getParameterNames();// 所有参数名
			List<Object> list = new ParamList<>();
			while (parameterNames.hasMoreElements()) {

				String requestParameter = parameterNames.nextElement();

				if (requestParameter.startsWith(parameterName)) {
					String[] split = requestParameter.split(Constant.COLLECTION_PARAM_REGEXP);// [use_i&&``981_r, 65651,
																								// , userName]
					Class<?> clazz = methodParameter.getGenericityClass();

					int index = Integer.parseInt(split[1]);// 得到索引
					Object newInstance = list.get(index);// 没有就是空值
					if (newInstance == null) {
						newInstance = clazz.getConstructor().newInstance();
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
			MethodParameter methodParameter) throws Throwable {
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
			MethodParameter methodParameter) throws Throwable {

		Enumeration<String> parameterNames = request.getParameterNames();// 所有参数名
		Map<String, Object> map = new HashMap<>();
		while (parameterNames.hasMoreElements()) {
			// users%5B%27today_1%27%5D.userId=434&users%5B%27today%27%5D.age=43&users%5B%27today%27%5D.userName=434&users%5B%27today%27%5D.sex=%E7%94%B7&users%5B%27today%27%5D.passwd=4343
			String requestParameter = parameterNames.nextElement();
			if (requestParameter.startsWith(methodParameterName)) {

				final String[] split = requestParameter.split(Constant.MAP_PARAM_REGEXP); // [users, today, , userName]
				Class<?> clazz = methodParameter.getGenericityClass();

				String key = split[1];// 得到key
				Object newInstance = map.get(key);// 没有就是空值
				if (newInstance == null) {
					newInstance = clazz.getConstructor().newInstance();
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
