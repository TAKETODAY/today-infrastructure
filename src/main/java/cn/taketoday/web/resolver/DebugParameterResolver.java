/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2019 All Rights Reserved.
 * 
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cn.taketoday.web.resolver;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import cn.taketoday.context.annotation.Autowired;
import cn.taketoday.context.annotation.Singleton;
import cn.taketoday.context.bean.BeanDefinition;
import cn.taketoday.context.conversion.Converter;
import cn.taketoday.context.exception.ConfigurationException;
import cn.taketoday.context.factory.InitializingBean;
import cn.taketoday.context.utils.NumberUtils;
import cn.taketoday.context.utils.StringUtils;
import cn.taketoday.web.Constant;
import cn.taketoday.web.WebApplicationContext;
import cn.taketoday.web.WebApplicationContextAware;
import cn.taketoday.web.annotation.ParameterConverter;
import cn.taketoday.web.annotation.WebDebugMode;
import cn.taketoday.web.exception.BadRequestException;
import cn.taketoday.web.mapping.MethodParameter;
import cn.taketoday.web.multipart.MultipartResolver;
import cn.taketoday.web.ui.ModelAndView;
import cn.taketoday.web.ui.ModelAttributes;
import cn.taketoday.web.ui.RedirectModel;
import cn.taketoday.web.ui.RedirectModelAttributes;
import cn.taketoday.web.utils.ParamList;
import cn.taketoday.web.utils.WebUtils;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Today <br>
 * 
 *         2019-01-03 23:02
 */
@Slf4j
@WebDebugMode
@SuppressWarnings("serial")
@Singleton(Constant.PARAMETER_RESOLVER)
public class DebugParameterResolver implements ParameterResolver, Constant, InitializingBean, WebApplicationContextAware {

    private ServletContext servletContext;

    @Autowired(Constant.MULTIPART_RESOLVER)
    private MultipartResolver multipartResolver;

    private WebApplicationContext applicationContext;

    private final Map<Class<?>, Converter<String, Object>> supportParameterTypes = new HashMap<>(8, 1.0f);;

    /**
     * @param targetClass
     * @param converter
     *            converter instance
     */
    @SuppressWarnings("unchecked")
    public final void register(Class<?> targetClass, Object converter) {
        supportParameterTypes.put(targetClass, (Converter<String, Object>) converter);
    }

    @Override
    public void afterPropertiesSet() throws Exception {

        log.info("Loading ParameterConverter Extensions");
        try {

            WebApplicationContext applicationContext = this.applicationContext;

            for (Entry<String, BeanDefinition> entry : applicationContext.getBeanDefinitionsMap().entrySet()) {
                BeanDefinition beanDefinition = entry.getValue();
                Class<? extends Object> beanClass = beanDefinition.getBeanClass();
                ParameterConverter converter = beanClass.getAnnotation(ParameterConverter.class);
                if (converter == null) {
                    continue;
                }
                if (!Converter.class.isAssignableFrom(beanClass)) {
                    throw new ConfigurationException("Component: [" + entry.getKey() + "] which annotated '@ParameterConverter'" + //
                            " must be a [cn.taketoday.context.conversion.Converter]");
                }
                Object singleton = applicationContext.getBean(beanClass);
                Class<?>[] values = converter.value();
                if (values.length != 0 && values[0] != void.class) {
                    for (Class<?> value : values) {
                        register(value, singleton);
                        log.info("Mapped ParameterConverter : [{}] -> [{}].", value, beanClass.getName());
                    }
                    continue;
                }

                Class<?> returnType = //
                        beanClass.getMethod(Constant.CONVERT_METHOD, String.class).getReturnType(); // get method named 'doConvert'

                if (!supportParameterTypes.containsKey(returnType)) {
                    register(returnType, singleton);
                }
                log.info("Mapped ParameterConverter: [{}] -> [{}].", returnType, beanClass.getName());
            }
        }
        catch (NoSuchMethodException e) {
            throw new ConfigurationException(//
                    "The method of " + Constant.CONVERT_METHOD + "'s parameter only support [java.lang.String]", e//
            );
        }
        if (supportParameterTypes.size() < 1) {
            log.info("NO ParameterConverter Found");
        }
    }

    public boolean supportsParameter(MethodParameter parameter) {
        return supportParameterTypes.containsKey(parameter.getParameterClass());
    }

    /***
     * resolve
     */
    @Override
    public void resolveParameter(Object[] args, //
            MethodParameter[] parameters, HttpServletRequest request, HttpServletResponse response) throws Throwable //
    {
        log.debug("Resolve parameter start");
        for (int i = 0; i < parameters.length; i++) {
            args[i] = setParameter(request, response, parameters[i]);
        }
    }

    /**
     * Resolve parameter[]
     *
     * @param request
     * @param response
     * @param methodParameter
     *            method parameter
     * @return
     * @throws Exception
     */
    private final Object setParameter(HttpServletRequest request, //
            HttpServletResponse response, final MethodParameter methodParameter) throws Throwable //
    {
        // method parameter name
        final String methodParameterName = methodParameter.getParameterName();
        // resolve annotation parameter expect @RequestParam
        if (methodParameter.hasAnnotation()) {
            return resolveAnnotationParameter(methodParameterName, request, methodParameter);
        }
        // @off
		switch (methodParameter.getParameterType())
		{
			case TYPE_HTTP_SESSION 			: 	return request.getSession();
			case TYPE_SERVLET_CONTEXT 		:	return request.getServletContext();
			case TYPE_HTTP_SERVLET_REQUEST 	: 	return request;
			case TYPE_HTTP_SERVLET_RESPONSE :	return response;
			case TYPE_READER				:  	return request.getReader();
			case TYPE_LOCALE				: 	return request.getLocale();
			case TYPE_WRITER				:  	return response.getWriter();
			case TYPE_OUT_STREAM			:  	return response.getOutputStream();
			case TYPE_INPUT_STREAM			:  	return request.getInputStream();
			case TYPE_PRINCIPAL				:  	return request.getUserPrincipal();
			
			case TYPE_MODEL 	: 	return new ModelAttributes(request);
			case TYPE_SET 		:	return resolveSetParameter(request, methodParameterName, methodParameter);
			case TYPE_MAP 		:	return resolveMapParameter(request, methodParameterName, methodParameter);
			case TYPE_LIST 		:	return resolveListParameter(request, methodParameterName, methodParameter);
			case TYPE_ARRAY 	:	return resolveArrayParameter(request, methodParameterName, methodParameter);
			case TYPE_STRING 	:	return resolveStringParameter(request, methodParameterName, methodParameter);
			
			case TYPE_BYTE 		:	return resolveParameter(request, methodParameterName, methodParameter, Byte::parseByte);
			case TYPE_LONG 		:	return resolveParameter(request, methodParameterName, methodParameter, Long::parseLong);
			case TYPE_SHORT 	:	return resolveParameter(request, methodParameterName, methodParameter, Short::parseShort);
			case TYPE_FLOAT 	:	return resolveParameter(request, methodParameterName, methodParameter, Float::parseFloat);
			case TYPE_INT 		:	return resolveParameter(request, methodParameterName, methodParameter, Integer::parseInt);
			case TYPE_DOUBLE 	:	return resolveParameter(request, methodParameterName, methodParameter, Double::parseDouble);
			case TYPE_BOOLEAN 	:	return resolveParameter(request, methodParameterName, methodParameter, Boolean::parseBoolean);
			
			case TYPE_MODEL_AND_VIEW : {
				final ModelAndView modelAndView = new ModelAndView();
				request.setAttribute(KEY_MODEL_AND_VIEW, modelAndView);
				return modelAndView;
			}
			case TYPE_REDIRECT_MODEL : {
				final RedirectModel redirectModel = new RedirectModelAttributes();
				request.getSession().setAttribute(KEY_REDIRECT_MODEL, redirectModel);
				return redirectModel;
			}
		}
		//@on
        return resolve(request, methodParameter, methodParameterName, methodParameter.getParameterClass());
    }

    /**
     * Resolve parameter with given converter
     *
     * @param request
     *            current request
     * @param methodParameterName
     *            parameter name
     * @param methodParameter
     *            method parameter
     * @param converter
     *            the parameter converter
     * @return
     * @throws BadRequestException
     */
    private Object resolveParameter(HttpServletRequest request, String methodParameterName, //
            MethodParameter methodParameter, Converter<String, Object> converter) throws BadRequestException //
    {
        final String requestParameter = request.getParameter(methodParameterName);
        if (StringUtils.isEmpty(requestParameter)) {
            if (methodParameter.isRequired()) {
                throw WebUtils.newBadRequest(null, methodParameterName, null);
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
    private Object resolveStringParameter(HttpServletRequest request, String methodParameterName, //
            MethodParameter methodParameter) throws BadRequestException //
    {
        // parameter value
        final String requestParameter = request.getParameter(methodParameterName);

        if (StringUtils.isEmpty(requestParameter)) {
            if (methodParameter.isRequired()) {
                throw WebUtils.newBadRequest(null, methodParameterName, null);
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
    private Object resolveArrayParameter(HttpServletRequest request, String methodParameterName, //
            MethodParameter methodParameter) throws BadRequestException //
    {
        // parameter value[]
        String[] parameterValues = request.getParameterValues(methodParameterName);
        if (parameterValues == null || parameterValues.length == 0) {
            if (methodParameter.isRequired()) {
                throw WebUtils.newBadRequest(null, methodParameterName, null);
            }
            return null;
        }
        return NumberUtils.parseArray(parameterValues, methodParameter.getParameterClass());
    }

    /**
     * resolve
     *
     * @param request
     * @param methodParameter
     * @param methodParameterName
     * @param parameterClass
     * @return
     * @throws Throwable
     */
    private final Object resolve(HttpServletRequest request, final MethodParameter methodParameter, //
            final String methodParameterName, final Class<?> parameterClass) throws Throwable //
    {
        if (this.supportsParameter(methodParameter)) {
            log.debug("Resolve custom parameter: [{}]", methodParameterName);
            return supportParameterTypes.get(methodParameter.getParameterClass())//
                    .doConvert(request.getParameter(methodParameterName));
        }
        // resolve pojo
        Object newInstance = null;
        try {

            newInstance = parameterClass.getConstructor().newInstance();
        } //
        catch (Throwable e) {
            throw WebUtils.newBadRequest("Can't resolve pojo", methodParameterName, null);
        }

        log.debug("Resolve pojo parameter: [{}]", methodParameterName);
        // pojo
        if (!setBean(request, parameterClass, newInstance, request.getParameterNames())) {
            throw WebUtils.newBadRequest("Can't resolve pojo", methodParameterName, null);
        }
        return newInstance;
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
    private final Object resolveAnnotationParameter(String methodParameterName, HttpServletRequest request, //
            MethodParameter methodParameter) throws Throwable //
    {
        log.debug("Resolve Annotation Parameter: [{}]", methodParameterName);
        switch (methodParameter.getAnnotation()) //
        {
            case ANNOTATION_COOKIE : { // cookie
                return cookie(request, methodParameterName, methodParameter);
            }
            case ANNOTATION_SESSION : {
                return request.getSession().getAttribute(methodParameterName);
            }
            case ANNOTATION_MULTIPART : { // resolve multi part
                try {

                    if (multipartResolver.isMultipart(request)) {
                        return multipartResolver.resolveMultipart(request, methodParameterName, methodParameter);
                    }
                    throw new BadRequestException("This isn't multipart request, bad request.");
                } finally {
                    multipartResolver.cleanupMultipart(request);
                }
            }
            case ANNOTATION_HEADER : {// request header
                final String header = request.getHeader(methodParameterName);
                if (StringUtils.isEmpty(header)) {
                    if (methodParameter.isRequired()) {
                        throw WebUtils.newBadRequest("Header", methodParameterName, null);
                    }
                    return methodParameter.getDefaultValue();
                }
                return header;
            }
            case ANNOTATION_PATH_VARIABLE : { // path variable
                return pathVariable(request, methodParameterName, methodParameter);
            }
            case ANNOTATION_REQUEST_BODY : { // request body
                log.debug("Resolve request body: [{}]", methodParameterName);
                final Object requestBody = request.getAttribute(KEY_REQUEST_BODY);
                if (requestBody != null) {
                    return toJavaObject(methodParameterName, methodParameter, requestBody);
                }
                try {
                    // fixed #2 JSONObject could be null
                    final String formData = request.getReader().readLine();
                    if (StringUtils.isEmpty(formData)) {
                        throw WebUtils.newBadRequest("Request body", methodParameterName, null);
                    }
                    // fix
                    final Object parsedJson = JSON.parse(formData);

                    request.setAttribute(KEY_REQUEST_BODY, parsedJson);

                    return toJavaObject(methodParameterName, methodParameter, parsedJson);
                }
                catch (IOException e) {
                    throw WebUtils.newBadRequest("Request body", methodParameterName, e);
                }
            }
            case ANNOTATION_SERVLET_CONTEXT : { // servlet context attribute
                return servletContext.getAttribute(methodParameterName);
            }
            case ANNOTATION_REQUEST_ATTRIBUTE : {
                return request.getAttribute(methodParameterName);
            }
        }
        throw new BadRequestException("Annotation Parameter: [" + methodParameterName + "] not supported, bad request.");
    }

    /**
     * @param methodParameterName
     * @param methodParameter
     * @param requestBody
     * @return
     * @since 2.3.7
     */
    private static Object toJavaObject(final String methodParameterName, //
            final MethodParameter methodParameter, final Object parsedJson) //
    {
        if (parsedJson instanceof JSONArray) {// array
            // style: [{'name':'today','age':21},{'name':"YHJ",'age':22}]
            final Class<?> parameterClass = methodParameter.getParameterClass();

            if (Collection.class.isAssignableFrom(parameterClass)) {
                return ((JSONArray) parsedJson).toJavaList(methodParameter.getGenericityClass());
            }
        }
        else if (parsedJson instanceof JSONObject) {
            final JSONObject requestBody = (JSONObject) parsedJson;

            final Class<?> parameterClass = methodParameter.getParameterClass();

            if (Collection.class.isAssignableFrom(parameterClass)) {
                final JSONArray array = requestBody.getJSONArray(methodParameterName);
                if (array == null) { // style: {'users':[{'name':'today','age':21},{'name':"YHJ",'age':22}],...}
                    throw WebUtils.newBadRequest("Request body", methodParameterName, null);
                }
                return array.toJavaList(methodParameter.getGenericityClass());
            }
            else {
                final JSONObject obj = requestBody.getJSONObject(methodParameterName);
                if (obj == null) { // only one request body
                    return requestBody.toJavaObject(parameterClass); // style: {'name':'today','age':21}
                }
                return obj.toJavaObject(parameterClass); // style: {'user':{'name':'today','age':21},...}
            }
        }

        throw WebUtils.newBadRequest("Request body", methodParameterName, null);
    }

    /**
	 * Resolve Path Variable parameter.
	 *
	 * @param request
	 *            current request
	 * @param methodParameterName
	 *            request parameter or method parameter name
	 * @param methodParameter
	 *            current method parameter instance
	 * @off
	 * @return
	 * @throws BadRequestException
	 */
	private Object pathVariable(HttpServletRequest request, //
			String methodParameterName, MethodParameter methodParameter) throws BadRequestException //
	{

		try {
			
			final Object attribute = request.getAttribute(Constant.KEY_REPLACED);
			String pathVariable;
			if (attribute == null) {
				String requestURI = request.getRequestURI();
				log.debug("Resolve Path variable: [{}], With request Uri: [{}]", methodParameterName, requestURI);
				for (String regex : methodParameter.getSplitMethodUrl()) {
					log.debug("replace: [{}]", regex);
					requestURI = requestURI.replace(regex, Constant.REPLACE_SPLIT_METHOD_URL);
					log.debug("replaced: [{}]", requestURI);
				}
				final String[] split = requestURI.split(Constant.REPLACE_REGEXP);
				request.setAttribute(Constant.KEY_REPLACED, split);
				pathVariable = split[methodParameter.getPathIndex()];
			}
			else {
				pathVariable = ((String[]) attribute)[methodParameter.getPathIndex()];
			}

			log.debug("value: [{}]", pathVariable);
			switch (methodParameter.getParameterType())
			{
				case TYPE_STRING :	return pathVariable;
				case TYPE_BYTE :	return Byte.parseByte(pathVariable);
				case TYPE_INT :		return Integer.parseInt(pathVariable);
				case TYPE_SHORT :	return Short.parseShort(pathVariable);
				case TYPE_LONG :	return Long.parseLong(pathVariable);
				case TYPE_DOUBLE :	return Double.parseDouble(pathVariable);
				case TYPE_FLOAT :	return Float.parseFloat(pathVariable);
				default: 			{
					if (this.supportsParameter(methodParameter)) {
						return supportParameterTypes.get(methodParameter.getParameterClass()).doConvert(pathVariable);
					}
				}
			}
		}
		catch (Throwable e) {
			throw WebUtils.newBadRequest("Path variable", methodParameterName, e);
		}
		throw WebUtils.newBadRequest("Path variable", methodParameterName, null);
	}
	//@on

    /**
     * get cookie
     *
     * @param request
     * @param methodParameterName
     * @param methodParameter
     * @return
     * @throws BadRequestException
     */
    private final Object cookie(HttpServletRequest request, //
            String methodParameterName, MethodParameter methodParameter) throws BadRequestException //
    {
        log.debug("Resolve Cookie: [{}]", methodParameterName);
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
            throw WebUtils.newBadRequest("Cookie", methodParameterName, null);
        }
        return methodParameter.getDefaultValue(); // return default value.
    }

    /**
     * resolve pojo
     *
     * @param request
     * @param parameterClass
     * @param bean
     * @param parameterNames
     * @param methodParameter
     * @return
     * @throws BadRequestException
     */
    private boolean setBean(HttpServletRequest request, //
            Class<?> parameterClass, Object bean, Enumeration<String> parameterNames) throws Throwable //
    {
        log.debug("Resolve Pojo: [{}]", parameterClass.getName());
        try {

            while (parameterNames.hasMoreElements()) {
                // 遍历参数
                final String parameterName = parameterNames.nextElement();
                // 寻找参数
                if (!resolvePojoParameter(request, parameterName, bean, //
                        parameterClass.getDeclaredField(parameterName))) {

                    return false;
                }
            }
        }
        catch (NoSuchFieldException e) {
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
    private boolean resolvePojoParameter(HttpServletRequest request, //
            String parameterName, Object bean, Field field) throws Throwable //
    {
        Object property = null;

        final Class<?> type = field.getType();
        if (type.isArray()) {
            property = NumberUtils.toArrayObject(request.getParameterValues(parameterName), type);
        }
        else {
            String parameter = request.getParameter(parameterName);
            if (StringUtils.isEmpty(parameter)) {
                return true;
            }
            if (NumberUtils.isNumber(type)) {
                property = NumberUtils.parseDigit(parameter, type);
            }
            else if (type == String.class) {
                property = parameter;
            }
            else {
                // if has supported type
                Converter<String, Object> converter = supportParameterTypes.get(type);
                if (converter != null) {
                    property = converter.doConvert(parameter);
                }
                else {
                    // not supported
                    throw new BadRequestException("Parameter: [" + parameterName + "] not supported, bad request.");
                }
            }
        }
        if (property == null) {
            return true;
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
     * @throws Throwable
     */
    private List<?> resolveListParameter(HttpServletRequest request, //
            String parameterName, MethodParameter methodParameter) throws Throwable //
    {
        log.debug("Resolve List Parameter: [{}]", parameterName);
        // https://taketoday.cn/today/user/list?user%5b0%5d.userId=90&user%5b2%5d.userId=98&user%5b1%5d.userName=Today

        Enumeration<String> parameterNames = request.getParameterNames();// all request parameter name
        List<Object> list = new ParamList<>();

        Class<?> clazz = methodParameter.getGenericityClass();
        while (parameterNames.hasMoreElements()) {
            String requestParameter = parameterNames.nextElement();
            if (requestParameter.startsWith(parameterName)) {// users[0].userName=TODAY&users[0].age=20
                String[] split = requestParameter.split(Constant.COLLECTION_PARAM_REGEXP);// [users, 1,, userName]
                int index = Integer.parseInt(split[1]);// get index
                Object newInstance = list.get(index);
                if (newInstance == null) {
                    newInstance = clazz.getConstructor().newInstance();
                }

                if (!resolvePojoParameter(request, requestParameter, newInstance, //
                        clazz.getDeclaredField(split[3]))) {// 得到Field准备注入

                    return list;
                }
                list.set(index, newInstance);
            }
        }
        return list;
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
    private final Set<?> resolveSetParameter(HttpServletRequest request, //
            String methodParameterName, MethodParameter methodParameter) throws Throwable //
    {
        log.debug("Resolve Set Parameter: [{}]", methodParameterName);
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
    private Map<String, Object> resolveMapParameter(HttpServletRequest request, //
            String methodParameterName, MethodParameter methodParameter) throws Throwable//
    {
        log.debug("Resolve Map Parameter: [{}]", methodParameterName);
        Enumeration<String> parameterNames = request.getParameterNames();// all parameter
        Map<String, Object> map = new HashMap<>();

        // parameter class
        Class<?> clazz = methodParameter.getGenericityClass();
        while (parameterNames.hasMoreElements()) {
            // users%5B%27today_1%27%5D.userId=434&users%5B%27today%27%5D.age=43&users%5B%27today%27%5D.userName=434&users%5B%27today%27%5D.sex=%E7%94%B7&users%5B%27today%27%5D.passwd=4343
            String requestParameter = parameterNames.nextElement();
            if (requestParameter.startsWith(methodParameterName)) { // users['today'].userName=TODAY&users['today'].age=20

                String[] keyList = requestParameter.split(Constant.MAP_PARAM_REGEXP); // [users, today, , userName]

                String key = keyList[1];// get key
                Object newInstance = map.get(key);// 没有就是空值
                if (newInstance == null) {
                    newInstance = clazz.getConstructor().newInstance();// default constructor
                }
                if (!resolvePojoParameter(request, requestParameter, //
                        newInstance, clazz.getDeclaredField(keyList[3]))) {// 得到Field准备注入

                    return map;
                }
                map.put(key, newInstance);// put directly
            }
        }
        return map;
    }

    @Override
    public void setWebApplicationContext(WebApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
        this.servletContext = applicationContext.getServletContext();
    }

}
