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

package cn.taketoday.framework.web.servlet;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import cn.taketoday.beans.BeansException;
import cn.taketoday.beans.factory.annotation.AnnotatedBeanDefinition;
import cn.taketoday.beans.factory.config.BeanDefinition;
import cn.taketoday.beans.factory.config.BeanFactoryPostProcessor;
import cn.taketoday.beans.factory.config.ConfigurableBeanFactory;
import cn.taketoday.beans.factory.support.BeanDefinitionRegistry;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.aware.ApplicationContextAware;
import cn.taketoday.context.loader.ClassPathScanningCandidateComponentProvider;
import cn.taketoday.web.servlet.WebServletApplicationContext;

/**
 * {@link BeanFactoryPostProcessor} that registers beans for Servlet components found via
 * package scanning.
 *
 * @author Andy Wilkinson
 * @see ServletComponentScan
 * @see ServletComponentScanRegistrar
 */
class ServletComponentRegisteringPostProcessor implements BeanFactoryPostProcessor, ApplicationContextAware {

  private static final List<ServletComponentHandler> HANDLERS = List.of(
          new WebServletHandler(), new WebFilterHandler(), new WebListenerHandler()
  );

  private final Set<String> packagesToScan;

  private ApplicationContext applicationContext;

  ServletComponentRegisteringPostProcessor(Set<String> packagesToScan) {
    this.packagesToScan = packagesToScan;
  }

  @Override
  public void postProcessBeanFactory(ConfigurableBeanFactory beanFactory) throws BeansException {
    if (isRunningInEmbeddedWebServer()) {
      ClassPathScanningCandidateComponentProvider componentProvider = createComponentProvider();
      for (String packageToScan : this.packagesToScan) {
        scanPackage(componentProvider, packageToScan);
      }
    }
  }

  private void scanPackage(ClassPathScanningCandidateComponentProvider componentProvider, String packageToScan) {
    for (BeanDefinition candidate : componentProvider.findCandidateComponents(packageToScan)) {
      if (candidate instanceof AnnotatedBeanDefinition) {
        for (ServletComponentHandler handler : HANDLERS) {
          handler.handle(((AnnotatedBeanDefinition) candidate),
                  (BeanDefinitionRegistry) this.applicationContext);
        }
      }
    }
  }

  private boolean isRunningInEmbeddedWebServer() {
    return this.applicationContext instanceof WebServletApplicationContext
            && ((WebServletApplicationContext) this.applicationContext).getServletContext() == null;
  }

  private ClassPathScanningCandidateComponentProvider createComponentProvider() {
    ClassPathScanningCandidateComponentProvider componentProvider = new ClassPathScanningCandidateComponentProvider(
            false, applicationContext.getEnvironment());
    componentProvider.setResourceLoader(this.applicationContext);
    for (ServletComponentHandler handler : HANDLERS) {
      componentProvider.addIncludeFilter(handler.getTypeFilter());
    }
    return componentProvider;
  }

  Set<String> getPackagesToScan() {
    return Collections.unmodifiableSet(this.packagesToScan);
  }

  @Override
  public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
    this.applicationContext = applicationContext;
  }

}
