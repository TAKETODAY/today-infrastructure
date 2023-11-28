/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.framework.web.servlet;

import java.util.Map;

import cn.taketoday.beans.factory.annotation.AnnotatedBeanDefinition;
import cn.taketoday.beans.factory.config.BeanDefinition;
import cn.taketoday.beans.factory.support.BeanDefinitionBuilder;
import cn.taketoday.beans.factory.support.BeanDefinitionRegistry;
import cn.taketoday.util.StringUtils;
import jakarta.servlet.MultipartConfigElement;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;

/**
 * Handler for {@link WebServlet @WebServlet}-annotated classes.
 *
 * @author Andy Wilkinson
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class WebServletHandler extends ServletComponentHandler {

  WebServletHandler() {
    super(WebServlet.class);
  }

  @Override
  public void doHandle(Map<String, Object> attributes,
          AnnotatedBeanDefinition beanDefinition, BeanDefinitionRegistry registry) {

    BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(ServletRegistrationBean.class);
    builder.addPropertyValue("asyncSupported", attributes.get("asyncSupported"));
    builder.addPropertyValue("initParameters", extractInitParameters(attributes));
    builder.addPropertyValue("loadOnStartup", attributes.get("loadOnStartup"));
    String name = determineName(attributes, beanDefinition);
    builder.addPropertyValue("name", name);
    builder.addPropertyValue("servlet", beanDefinition);
    builder.addPropertyValue("urlMappings", extractUrlPatterns(attributes));
    builder.addPropertyValue("multipartConfig", determineMultipartConfig(beanDefinition));
    registry.registerBeanDefinition(name, builder.getBeanDefinition());
  }

  private String determineName(Map<String, Object> attributes, BeanDefinition beanDefinition) {
    return (String) (StringUtils.hasText((String) attributes.get("name")) ? attributes.get("name")
                                                                          : beanDefinition.getBeanClassName());
  }

  private MultipartConfigElement determineMultipartConfig(AnnotatedBeanDefinition beanDefinition) {
    Map<String, Object> attributes = beanDefinition.getMetadata().getAnnotationAttributes(MultipartConfig.class.getName());
    if (attributes == null) {
      return null;
    }
    return new MultipartConfigElement((String) attributes.get("location"), (Long) attributes.get("maxFileSize"),
            (Long) attributes.get("maxRequestSize"), (Integer) attributes.get("fileSizeThreshold"));
  }

}
