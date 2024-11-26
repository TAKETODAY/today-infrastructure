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

package infra.context.annotation;

import java.util.Map;

import javax.management.MBeanServer;

import infra.beans.factory.BeanFactory;
import infra.beans.factory.BeanFactoryAware;
import infra.beans.factory.config.BeanDefinition;
import infra.context.EnvironmentAware;
import infra.core.annotation.AnnotationAttributes;
import infra.core.env.Environment;
import infra.core.type.AnnotationMetadata;
import infra.jmx.export.annotation.AnnotationMBeanExporter;
import infra.jmx.support.RegistrationPolicy;
import infra.lang.Assert;
import infra.lang.Nullable;
import infra.stereotype.Component;
import infra.util.StringUtils;

/**
 * {@code @Configuration} class that registers a {@link AnnotationMBeanExporter} bean.
 *
 * <p>This configuration class is automatically imported when using the
 * {@link EnableMBeanExport} annotation. See its javadoc for complete usage details.
 *
 * @author Phillip Webb
 * @author Chris Beams
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see EnableMBeanExport
 * @since 4.0 2022/3/7 23:07
 */
@Configuration(proxyBeanMethods = false)
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
public class MBeanExportConfiguration implements ImportAware, EnvironmentAware, BeanFactoryAware {

  private static final String MBEAN_EXPORTER_BEAN_NAME = "mbeanExporter";

  @Nullable
  private AnnotationAttributes enableMBeanExport;

  @Nullable
  private Environment environment;

  @Nullable
  private BeanFactory beanFactory;

  @Override
  public void setImportMetadata(AnnotationMetadata importMetadata) {
    Map<String, Object> map = importMetadata.getAnnotationAttributes(EnableMBeanExport.class.getName());
    this.enableMBeanExport = AnnotationAttributes.fromMap(map);
    if (enableMBeanExport == null) {
      throw new IllegalArgumentException(
              "@EnableMBeanExport is not present on importing class " + importMetadata.getClassName());
    }
  }

  @Override
  public void setEnvironment(Environment environment) {
    this.environment = environment;
  }

  @Override
  public void setBeanFactory(BeanFactory beanFactory) {
    this.beanFactory = beanFactory;
  }

  @Component(name = MBEAN_EXPORTER_BEAN_NAME)
  @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
  public AnnotationMBeanExporter mbeanExporter() {
    AnnotationMBeanExporter exporter = new AnnotationMBeanExporter();
    Assert.state(enableMBeanExport != null, "No EnableMBeanExport annotation found");
    setupDomain(exporter, enableMBeanExport);
    setupServer(exporter, enableMBeanExport);
    setupRegistrationPolicy(exporter, enableMBeanExport);
    return exporter;
  }

  private void setupDomain(AnnotationMBeanExporter exporter, AnnotationAttributes enableMBeanExport) {
    String defaultDomain = enableMBeanExport.getString("defaultDomain");
    if (StringUtils.isNotEmpty(defaultDomain) && environment != null) {
      defaultDomain = environment.resolvePlaceholders(defaultDomain);
    }
    if (StringUtils.hasText(defaultDomain)) {
      exporter.setDefaultDomain(defaultDomain);
    }
  }

  private void setupServer(AnnotationMBeanExporter exporter, AnnotationAttributes enableMBeanExport) {
    String server = enableMBeanExport.getString("server");
    if (StringUtils.isNotEmpty(server) && environment != null) {
      server = environment.resolvePlaceholders(server);
    }
    if (StringUtils.hasText(server)) {
      Assert.state(beanFactory != null, "No BeanFactory set");
      exporter.setServer(beanFactory.getBean(server, MBeanServer.class));
    }
  }

  private void setupRegistrationPolicy(AnnotationMBeanExporter exporter, AnnotationAttributes enableMBeanExport) {
    RegistrationPolicy registrationPolicy = enableMBeanExport.getEnum("registration");
    exporter.setRegistrationPolicy(registrationPolicy);
  }

}
