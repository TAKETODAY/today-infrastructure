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

import java.util.Set;

import javax.servlet.Registration;
import javax.servlet.ServletRegistration;

import cn.taketoday.context.io.Resource;
import cn.taketoday.web.mapping.ResourceMappingRegistry;
import cn.taketoday.web.multipart.AbstractMultipartResolver;
import cn.taketoday.web.multipart.MultipartResolver;
import cn.taketoday.web.servlet.ResourceServlet;
import cn.taketoday.web.view.AbstractViewResolver;
import cn.taketoday.web.view.ViewResolver;

/**
 * @author TODAY <br>
 *         2019-05-17 17:46
 */
public interface WebMvcConfiguration {

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
     * Configure {@link ResourceServlet}'s mappings
     * 
     * @param urlMappings
     *            {@link ResourceServlet} url mappings
     */
    default void configureResourceServletUrlMappings(Set<String> urlMappings) {

    }

    /**
     * Configure default servlet
     * 
     * @param servletRegistration
     *            default servlet {@link Registration}
     */
    default void configureDefaultServlet(ServletRegistration servletRegistration) {

    }

    /**
     * Configure {@link MultipartResolver}
     * 
     * @param multipartResolver
     *            {@link AbstractMultipartResolver}
     */
    default void configureMultipartResolver(AbstractMultipartResolver multipartResolver) {

    }

}
