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

import cn.taketoday.context.annotation.ParameterConverter;
import cn.taketoday.context.aware.ObjectFactoryAware;
import cn.taketoday.context.conversion.Converter;
import cn.taketoday.context.exception.ConfigurationException;
import cn.taketoday.context.exception.NoSuchBeanDefinitionException;
import cn.taketoday.context.factory.ObjectFactory;
import cn.taketoday.context.utils.ClassUtils;
import cn.taketoday.web.Constant;
import cn.taketoday.web.WebApplicationContext;
import cn.taketoday.web.mapping.MethodParameter;
import cn.taketoday.web.multipart.AbstractMultipartResolver;
import cn.taketoday.web.multipart.MultipartResolver;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import lombok.extern.slf4j.Slf4j;

/**
 * 
 * @author Today <br>
 *         2018-06-25 20:32:16
 */
@Slf4j
public abstract class AbstractParameterResolver implements ParameterResolver, ObjectFactoryAware {

	protected ServletContext servletContext;

	protected ObjectFactory objectFactory;
	/** multipart */
	protected MultipartResolver multipartResolver;

	protected Map<Class<?>, Converter<String, Object>> supportParameterTypes;

	/**
	 * 
	 * @param targetClass
	 * @param converter
	 */
	public void register(Class<?> targetClass, Converter<String, Object> converter) {
		supportParameterTypes.put(targetClass, converter);
	}

	@Override
	@SuppressWarnings("unchecked")
	public final void doInit(WebApplicationContext applicationContext) throws ConfigurationException {

		log.info("Loading ParameterConverter Extensions");
		try {

			Collection<Class<?>> classes = ClassUtils.getAnnotatedClasses(ParameterConverter.class);
			
			supportParameterTypes = new HashMap<>(classes.size());
			
			this.multipartResolver = applicationContext.getBean(Constant.MULTIPART_RESOLVER,
					AbstractMultipartResolver.class);

			for (Class<?> clazz : classes) { // get converter

				if (clazz.isInterface()) {
					continue;
				}

				// converter instance
				Converter<String, Object> create = (Converter<String, Object>) objectFactory.create(clazz);

				ParameterConverter converter = clazz.getAnnotation(ParameterConverter.class);
				if (converter.value().length != 0 && converter.value()[0] != void.class) {
					for (Class<?> class_ : converter.value()) {
						register(class_, create);
						log.info("Mapped ParameterConverter : [{}] -> [{}].", class_, clazz.getName());
					}
					continue;
				}
				Method method = clazz.getMethod(Constant.CONVERT_METHOD, String.class); // get method named
				Class<?> returnType = method.getReturnType();
				if (!supportParameterTypes.containsKey(returnType)) {
					register(returnType, create);
				}
				log.info("Mapped ParameterConverter : [{}] -> [{}].", returnType, clazz.getName());
			}
		} //
		catch (IllegalArgumentException e) {
			throw new ConfigurationException("Illegal Arguments of Constructor", e);
		} //
		catch (SecurityException e) {
			throw new ConfigurationException("Modifier Configuration Error", e);
		} //
		catch (NoSuchMethodException e) {
			throw new ConfigurationException("The method of {}'s parameter only support [java.lang.String]",
					Constant.CONVERT_METHOD, e);
		} //
		catch (NoSuchBeanDefinitionException e) {
			throw new ConfigurationException("The Context does not exist multipart resolver named -> {}.",
					Constant.MULTIPART_RESOLVER, e);
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
	public void setObjectFactory(ObjectFactory objectFactory) {
		this.objectFactory = objectFactory;
	}

	@Override
	public abstract boolean resolveParameter(Object[] args, MethodParameter[] parameters, HttpServletRequest request,
			HttpServletResponse response) throws Exception;

}
