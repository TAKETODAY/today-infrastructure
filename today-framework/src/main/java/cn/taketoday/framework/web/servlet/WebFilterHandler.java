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

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Map;

import cn.taketoday.beans.factory.annotation.AnnotatedBeanDefinition;
import cn.taketoday.beans.factory.config.BeanDefinition;
import cn.taketoday.beans.factory.support.BeanDefinitionBuilder;
import cn.taketoday.beans.factory.support.BeanDefinitionRegistry;
import cn.taketoday.util.StringUtils;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.annotation.WebFilter;

/**
 * Handler for {@link WebFilter @WebFilter}-annotated classes.
 *
 * @author Andy Wilkinson
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class WebFilterHandler extends ServletComponentHandler {

  WebFilterHandler() {
    super(WebFilter.class);
  }

  @Override
  public void doHandle(Map<String, Object> attributes, AnnotatedBeanDefinition beanDefinition, BeanDefinitionRegistry registry) {

    BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(FilterRegistrationBean.class);
    builder.addPropertyValue("asyncSupported", attributes.get("asyncSupported"));
    builder.addPropertyValue("dispatcherTypes", extractDispatcherTypes(attributes));
    builder.addPropertyValue("filter", beanDefinition);
    builder.addPropertyValue("initParameters", extractInitParameters(attributes));
    String name = determineName(attributes, beanDefinition);
    builder.addPropertyValue("name", name);
    builder.addPropertyValue("servletNames", attributes.get("servletNames"));
    builder.addPropertyValue("urlPatterns", extractUrlPatterns(attributes));
    registry.registerBeanDefinition(name, builder.getBeanDefinition());
  }

  private EnumSet<DispatcherType> extractDispatcherTypes(Map<String, Object> attributes) {
    DispatcherType[] dispatcherTypes = (DispatcherType[]) attributes.get("dispatcherTypes");
    if (dispatcherTypes.length == 0) {
      return EnumSet.noneOf(DispatcherType.class);
    }
    if (dispatcherTypes.length == 1) {
      return EnumSet.of(dispatcherTypes[0]);
    }
    return EnumSet.of(dispatcherTypes[0], Arrays.copyOfRange(dispatcherTypes, 1, dispatcherTypes.length));
  }

  private String determineName(Map<String, Object> attributes, BeanDefinition beanDefinition) {
    return (String) (StringUtils.hasText((String) attributes.get("filterName"))
                     ? attributes.get("filterName")
                     : beanDefinition.getBeanClassName());
  }

}
