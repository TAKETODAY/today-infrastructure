/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.test.web.servlet.setup;

import java.util.Arrays;
import java.util.List;

import cn.taketoday.beans.factory.support.StaticListableBeanFactory;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.core.io.PatternResourceLoader;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.web.servlet.WebApplicationContext;
import cn.taketoday.web.servlet.support.GenericWebApplicationContext;
import cn.taketoday.web.servlet.support.ServletContextResourcePatternLoader;
import jakarta.servlet.ServletContext;

/**
 * A stub WebApplicationContext that accepts registrations of object instances.
 *
 * <p>As registered object instances are instantiated and initialized externally,
 * there is no wiring, bean initialization, lifecycle events, as well as no
 * pre-processing and post-processing hooks typically associated with beans
 * managed by an {@link ApplicationContext}. Just a simple lookup into a
 * {@link StaticListableBeanFactory}.
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @since 4.0
 */
class StubWebApplicationContext extends GenericWebApplicationContext implements WebApplicationContext {

  private final PatternResourceLoader resourcePatternResolver;

  public StubWebApplicationContext(ServletContext servletContext) {
    setServletContext(servletContext);
    this.resourcePatternResolver = new ServletContextResourcePatternLoader(servletContext);
  }

  @Override
  protected PatternResourceLoader getPatternResourceLoader() {
    return resourcePatternResolver;
  }

  public void addBean(String name, Object bean) {
    this.beanFactory.registerSingleton(name, bean);
  }

  public void addBeans(@Nullable List<?> beans) {
    if (beans != null) {
      for (Object bean : beans) {
        String name = bean.getClass().getName() + "#" + ObjectUtils.getIdentityHexString(bean);
        this.beanFactory.registerSingleton(name, bean);
      }
    }
  }

  public void addBeans(Object... beans) {
    addBeans(Arrays.asList(beans));
  }

}
