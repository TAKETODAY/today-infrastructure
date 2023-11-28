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
import cn.taketoday.beans.factory.support.BeanDefinitionBuilder;
import cn.taketoday.beans.factory.support.BeanDefinitionRegistry;
import jakarta.servlet.annotation.WebListener;

/**
 * Handler for {@link WebListener @WebListener}-annotated classes.
 *
 * @author Andy Wilkinson
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class WebListenerHandler extends ServletComponentHandler {

  WebListenerHandler() {
    super(WebListener.class);
  }

  @Override
  protected void doHandle(Map<String, Object> attributes,
          AnnotatedBeanDefinition beanDefinition, BeanDefinitionRegistry registry) {
    BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(
            ServletComponentWebListenerRegistrar.class);

    builder.addConstructorArgValue(beanDefinition.getBeanClassName());
    registry.registerBeanDefinition(
            beanDefinition.getBeanClassName() + "Registrar", builder.getBeanDefinition());
  }

  static class ServletComponentWebListenerRegistrar implements WebListenerRegistrar {

    private final String listenerClassName;

    ServletComponentWebListenerRegistrar(String listenerClassName) {
      this.listenerClassName = listenerClassName;
    }

    @Override
    public void register(WebListenerRegistry registry) {
      registry.addWebListeners(this.listenerClassName);
    }

  }

}
