/*
 * Copyright 2017 - 2024 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package infra.app.context.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import infra.beans.BeanUtils;
import infra.context.ApplicationContextException;
import infra.context.ApplicationContextInitializer;
import infra.context.ConfigurableApplicationContext;
import infra.core.Ordered;
import infra.core.annotation.AnnotationAwareOrderComparator;
import infra.core.env.ConfigurableEnvironment;
import infra.lang.Assert;
import infra.util.ClassUtils;
import infra.util.StringUtils;

/**
 * {@link ApplicationContextInitializer} that delegates to other initializers that are
 * specified under a {@literal context.initializer.classes} environment property.
 *
 * @author Dave Syer
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class DelegatingApplicationContextInitializer implements ApplicationContextInitializer, Ordered {

  // NOTE: Similar to infra.web.mock.ContextLoader

  private static final String PROPERTY_NAME = "context.initializer.classes";

  private int order = 0;

  @Override
  public void initialize(ConfigurableApplicationContext context) {
    ConfigurableEnvironment environment = context.getEnvironment();
    List<Class<ApplicationContextInitializer>> initializerClasses = getInitializerClasses(environment);
    if (!initializerClasses.isEmpty()) {
      applyInitializerClasses(context, initializerClasses);
    }
  }

  private List<Class<ApplicationContextInitializer>> getInitializerClasses(ConfigurableEnvironment env) {
    String classNames = env.getProperty(PROPERTY_NAME);
    if (StringUtils.hasText(classNames)) {
      var classes = new ArrayList<Class<ApplicationContextInitializer>>();
      for (String className : StringUtils.tokenizeToStringArray(classNames, ",")) {
        classes.add(getInitializerClass(className));
      }
      return classes;
    }
    return Collections.emptyList();
  }

  private Class<ApplicationContextInitializer> getInitializerClass(String className) throws LinkageError {
    try {
      Class<ApplicationContextInitializer> initializerClass = ClassUtils.forName(className, ClassUtils.getDefaultClassLoader());
      Assert.isAssignable(ApplicationContextInitializer.class, initializerClass);
      return initializerClass;
    }
    catch (ClassNotFoundException ex) {
      throw new ApplicationContextException("Failed to load context initializer class [" + className + "]", ex);
    }
  }

  private void applyInitializerClasses(ConfigurableApplicationContext context, List<Class<ApplicationContextInitializer>> initializerClasses) {
    var initializers = new ArrayList<ApplicationContextInitializer>();
    for (Class<ApplicationContextInitializer> initializerClass : initializerClasses) {
      initializers.add(BeanUtils.newInstance(initializerClass));
    }

    AnnotationAwareOrderComparator.sort(initializers);

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
