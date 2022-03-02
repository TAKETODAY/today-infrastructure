/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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
package cn.taketoday.web.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.Import;
import cn.taketoday.context.annotation.MissingBean;
import cn.taketoday.core.Ordered;
import cn.taketoday.web.handler.ViewControllerHandlerAdapter;
import cn.taketoday.web.handler.method.ParameterResolvingRegistryResolvableParameterFactory;
import cn.taketoday.web.registry.ViewControllerHandlerRegistry;
import cn.taketoday.web.bind.resolver.ParameterResolvingRegistry;

/**
 * @author TODAY 2020-03-30 21:38
 */
@Retention(RetentionPolicy.RUNTIME)
@Import(ViewControllerConfig.class)
@Target({ ElementType.TYPE, ElementType.METHOD })
public @interface EnableViewController {

}

/**
 * @since 3.0
 */
@Configuration(proxyBeanMethods = false)
class ViewControllerConfig {

  @MissingBean
  ViewControllerHandlerRegistry viewControllerHandlerRegistry(ParameterResolvingRegistry registry) {
    return new ViewControllerHandlerRegistry(new ParameterResolvingRegistryResolvableParameterFactory(registry));
  }

  @MissingBean
  ViewControllerHandlerAdapter viewControllerHandlerAdapter() {
    return new ViewControllerHandlerAdapter(Ordered.HIGHEST_PRECEDENCE + 2);
  }
}
