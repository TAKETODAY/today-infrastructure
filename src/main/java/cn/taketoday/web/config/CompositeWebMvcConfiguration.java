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
import java.util.Set;

import javax.servlet.ServletRegistration;

import cn.taketoday.web.mapping.ResourceMappingRegistry;
import cn.taketoday.web.multipart.AbstractMultipartResolver;
import cn.taketoday.web.view.AbstractViewResolver;

/**
 * @author TODAY <br>
 *         2019-05-17 17:46
 */
final class CompositeWebMvcConfiguration implements WebMvcConfiguration {

    private final List<WebMvcConfiguration> webMvcConfigurations;

    public CompositeWebMvcConfiguration(List<WebMvcConfiguration> webMvcConfigurations) {
        this.webMvcConfigurations = webMvcConfigurations;
    }

    @Override
    public void configureViewResolver(AbstractViewResolver viewResolver) {
        for (WebMvcConfiguration webMvcConfiguration : webMvcConfigurations) {
            webMvcConfiguration.configureViewResolver(viewResolver);
        }
    }

    @Override
    public void configureResourceMappings(ResourceMappingRegistry registry) {
        for (WebMvcConfiguration webMvcConfiguration : webMvcConfigurations) {
            webMvcConfiguration.configureResourceMappings(registry);
        }
    }

    @Override
    public void configureResourceServletUrlMappings(Set<String> urlMappings) {
        for (WebMvcConfiguration webMvcConfiguration : webMvcConfigurations) {
            webMvcConfiguration.configureResourceServletUrlMappings(urlMappings);
        }
    }

    @Override
    public void configureDefaultServlet(ServletRegistration servletRegistration) {
        for (WebMvcConfiguration webMvcConfiguration : webMvcConfigurations) {
            webMvcConfiguration.configureDefaultServlet(servletRegistration);
        }
    }

    public void configureMultipartResolver(AbstractMultipartResolver multipartResolver) {

        for (WebMvcConfiguration webMvcConfiguration : webMvcConfigurations) {
            webMvcConfiguration.configureMultipartResolver(multipartResolver);
        }
    }

}
