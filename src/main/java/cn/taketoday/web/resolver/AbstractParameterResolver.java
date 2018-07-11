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

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import cn.taketoday.context.annotation.ParameterConverter;
import cn.taketoday.context.conversion.Converter;
import cn.taketoday.context.exception.NoSuchBeanDefinitionException;
import cn.taketoday.web.core.Constant;
import cn.taketoday.web.core.WebApplicationContext;
import cn.taketoday.web.mapping.MethodParameter;
import cn.taketoday.web.multipart.AbstractMultipartResolver;
import cn.taketoday.web.multipart.MultipartResolver;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Today
 * @date 2018年6月25日 下午8:32:16
 */
@Slf4j
public abstract class AbstractParameterResolver implements ParameterResolver {

	protected MultipartResolver							multipartResolver;

	protected Map<Class<?>, Converter<String, Object>>	supportParameterTypes	= new HashMap<>(1);

	// protected WebApplicationContext applicationContext;

	@Override
	@SuppressWarnings("unchecked")
	public final void doInit(WebApplicationContext applicationContext) {

		log.info("Load ParameterConverter Extensions");
		try {
			Set<Class<?>> actions = applicationContext.getActions();
			
			this.multipartResolver = applicationContext.getBean(Constant.MULTIPART_RESOLVER,
					AbstractMultipartResolver.class);
			
			for (Class<?> clazz : actions) { // get converter
				if (clazz.isInterface()) { // basic
					continue;
				}

				ParameterConverter converter = clazz.getAnnotation(ParameterConverter.class);
				if (converter == null) { // basic
					continue;
				}
				try {
					Method method = clazz.getMethod("doConvert", String.class); // get method named 'doConvert'

					supportParameterTypes.put(method.getReturnType(), (Converter<String, Object>) clazz.newInstance()); // put

					log.info("Mapped ParameterConverter : {} -> [{}]", method.getReturnType(), clazz.getName());
				} catch (NoSuchMethodException e) {
					log.error("doConvert's method parameter only support [String]", e);
				}
			}
		} catch (InstantiationException | IllegalAccessException | NoSuchBeanDefinitionException ex) {
			log.error("Initialized ERROR -> [{}] caused by {}", ex.getMessage(), ex.getCause(), ex);
			System.exit(0);
		}

		if (supportParameterTypes.size() < 1) {
			log.info("NO ParameterConverter FOUND");
		}
	}

	@Override
	public final boolean supportsParameter(MethodParameter parameter) {
		return supportParameterTypes.containsKey(parameter.getParameterClass());
	}

	@Override
	public abstract boolean resolveParameter(Object[] args, MethodParameter[] parameters, HttpServletRequest request,
			HttpServletResponse response) throws Exception;

}
