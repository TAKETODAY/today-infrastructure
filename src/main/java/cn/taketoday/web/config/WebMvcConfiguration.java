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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */
package cn.taketoday.web.config;

import java.util.List;

import cn.taketoday.context.conversion.TypeConverter;
import cn.taketoday.context.io.Resource;
import cn.taketoday.web.annotation.Multipart;
import cn.taketoday.web.mapping.ResourceMappingRegistry;
import cn.taketoday.web.multipart.MultipartConfiguration;
import cn.taketoday.web.resolver.method.ParameterResolver;
import cn.taketoday.web.resolver.result.ResultResolver;
import cn.taketoday.web.view.AbstractViewResolver;
import cn.taketoday.web.view.ViewResolver;

/**
 * @author TODAY <br>
 *         2019-05-17 17:46
 */
public interface WebMvcConfiguration {

    /**
     * Configure {@link ParameterResolver}
     * 
     * @param parameterResolvers
     *            {@link ParameterResolver} registry
     */
    default void configureParameterResolver(List<ParameterResolver> parameterResolvers) {

    }

    /**
     * Configure {@link ResultResolver}
     * 
     * @param resultResolvers
     *            {@link ResultResolver} registry
     */
    default void configureResultResolver(List<ResultResolver> resultResolvers) {

    }

    /**
     * Configure {@link ViewResolver}
     * 
     * @param viewResolver
     *            {@link ViewResolver} instance
     */
    default void configureViewResolver(AbstractViewResolver viewResolver) {

    }

    /**
     * Configure static {@link Resource}
     * 
     * @param registry
     *            {@link ResourceMappingRegistry}
     */
    default void configureResourceMappings(ResourceMappingRegistry registry) {

    }

    /**
     * Configure {@link Multipart}
     * 
     * @param multipartConfiguration
     *            {@link MultipartConfiguration}
     */
    default void configureMultipart(MultipartConfiguration multipartConfiguration) {

    }

    /**
     * Use {@link TypeConverter}s to convert request parameters
     * 
     * @param typeConverters
     *            {@link TypeConverter} registry
     */
    default void configureTypeConverter(List<TypeConverter> typeConverters) {

    }

    /**
     * Configure WebApplicationInitializer
     * 
     * @param initializers
     *            WebApplicationInitializer register
     */
    default void configureInitializer(List<WebApplicationInitializer> initializers) {

    }

}
