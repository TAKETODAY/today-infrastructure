/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.framework.context.config;

import java.util.ArrayList;
import java.util.List;

import cn.taketoday.beans.BeanUtils;
import cn.taketoday.context.ApplicationContextException;
import cn.taketoday.context.ApplicationContextInitializer;
import cn.taketoday.context.ConfigurableApplicationContext;
import cn.taketoday.core.GenericTypeResolver;
import cn.taketoday.core.Ordered;
import cn.taketoday.core.annotation.AnnotationAwareOrderComparator;
import cn.taketoday.core.env.ConfigurableEnvironment;
import cn.taketoday.lang.Assert;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.StringUtils;

/**
 * {@link ApplicationContextInitializer} that delegates to other initializers that are
 * specified under a {@literal context.initializer.classes} environment property.
 *
 * @author Dave Syer
 * @author Phillip Webb
 * @since 4.0
 */
public class DelegatingApplicationContextInitializer
        implements ApplicationContextInitializer, Ordered {

  // NOTE: Similar to cn.taketoday.web.context.ContextLoader

  private static final String PROPERTY_NAME = "context.initializer.classes";

  private int order = 0;

  @Override
  public void initialize(ConfigurableApplicationContext context) {
    ConfigurableEnvironment environment = context.getEnvironment();
    List<Class<?>> initializerClasses = getInitializerClasses(environment);
    if (!initializerClasses.isEmpty()) {
      applyInitializerClasses(context, initializerClasses);
    }
  }

  private List<Class<?>> getInitializerClasses(ConfigurableEnvironment env) {
    String classNames = env.getProperty(PROPERTY_NAME);
    List<Class<?>> classes = new ArrayList<>();
    if (StringUtils.isNotEmpty(classNames)) {
      for (String className : StringUtils.tokenizeToStringArray(classNames, ",")) {
        classes.add(getInitializerClass(className));
      }
    }
    return classes;
  }

  private Class<?> getInitializerClass(String className) throws LinkageError {
    try {
      Class<?> initializerClass = ClassUtils.forName(className, ClassUtils.getDefaultClassLoader());
      Assert.isAssignable(ApplicationContextInitializer.class, initializerClass);
      return initializerClass;
    }
    catch (ClassNotFoundException ex) {
      throw new ApplicationContextException("Failed to load context initializer class [" + className + "]", ex);
    }
  }

  private void applyInitializerClasses(ConfigurableApplicationContext context, List<Class<?>> initializerClasses) {
    Class<?> contextClass = context.getClass();
    List<ApplicationContextInitializer> initializers = new ArrayList<>();
    for (Class<?> initializerClass : initializerClasses) {
      initializers.add(instantiateInitializer(contextClass, initializerClass));
    }
    applyInitializers(context, initializers);
  }

  private ApplicationContextInitializer instantiateInitializer(Class<?> contextClass, Class<?> initializerClass) {
    Class<?> requireContextClass = GenericTypeResolver.resolveTypeArgument(initializerClass, ApplicationContextInitializer.class);
    Assert.isAssignable(requireContextClass, contextClass,
            () -> String.format(
                    "Could not add context initializer [%s] as its generic parameter [%s] is not assignable "
                            + "from the type of application context used by this context loader [%s]: ",
                    initializerClass.getName(), requireContextClass.getName(), contextClass.getName()));
    return (ApplicationContextInitializer) BeanUtils.newInstance(initializerClass);
  }

  private void applyInitializers(
          ConfigurableApplicationContext context, List<ApplicationContextInitializer> initializers) {
    initializers.sort(new AnnotationAwareOrderComparator());
    for (ApplicationContextInitializer initializer : initializers) {
      initializer.initialize(context);
    }
  }

  public void setOrder(int order) {
    this.order = order;
  }

  @Override
  public int getOrder() {
    return this.order;
  }

}
