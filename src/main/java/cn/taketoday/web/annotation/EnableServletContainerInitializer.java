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
package cn.taketoday.web.annotation;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;
import javax.servlet.annotation.HandlesTypes;

import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.annotation.Import;
import cn.taketoday.context.utils.ClassUtils;
import cn.taketoday.web.WebApplicationContextSupport;
import cn.taketoday.web.servlet.initializer.ServletContextInitializer;

/**
 * @author TODAY <br>
 *         2020-03-30 21:38
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
@Import(WebApplicationServletContainerInitializer.class)
public @interface EnableServletContainerInitializer {

}

class WebApplicationServletContainerInitializer
        extends WebApplicationContextSupport implements ServletContextInitializer {

  @Override
  @SuppressWarnings("unchecked")
  public void onStartup(final ServletContext servletContext) throws Throwable {

    final ApplicationContext context = getApplicationContext();

    for (final ServletContainerInitializer initializer : context.getBeans(ServletContainerInitializer.class)) {
      final HandlesTypes handles = ClassUtils.getAnnotation(initializer, HandlesTypes.class);

      Set<Class<?>> c = null;
      if (handles != null) {
        c = new HashSet<>();
        for (final Class<?> handlesType : handles.value()) {
          if (handlesType.isAnnotation()) {
            c.addAll(context.getCandidateComponentScanner().getAnnotatedClasses((Class<? extends Annotation>) handlesType));
          }
          else if (handlesType.isInterface()) {
            c.addAll(context.getCandidateComponentScanner().getImplementationClasses(handlesType));
          }
          else {
            c.add(handlesType);
          }
        }
      }
      initializer.onStartup(c, servletContext);
    }
  }

}
