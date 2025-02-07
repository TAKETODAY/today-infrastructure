/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.annotation.config.jmx;

import javax.management.MBeanServer;

import infra.beans.factory.BeanFactory;
import infra.context.annotation.EnableMBeanExport;
import infra.context.annotation.Primary;
import infra.context.annotation.config.DisableDIAutoConfiguration;
import infra.context.annotation.config.EnableAutoConfiguration;
import infra.context.condition.ConditionalOnClass;
import infra.context.condition.ConditionalOnMissingBean;
import infra.context.condition.ConditionalOnProperty;
import infra.context.condition.SearchStrategy;
import infra.context.properties.EnableConfigurationProperties;
import infra.jmx.export.MBeanExporter;
import infra.jmx.export.annotation.AnnotationJmxAttributeSource;
import infra.jmx.export.annotation.AnnotationMBeanExporter;
import infra.jmx.export.naming.ObjectNamingStrategy;
import infra.jmx.support.MBeanServerFactoryBean;
import infra.stereotype.Component;
import infra.util.StringUtils;

/**
 * {@link EnableAutoConfiguration Auto-configuration} to enable/disable Infra
 * {@link EnableMBeanExport @EnableMBeanExport} mechanism based on configuration
 * properties.
 * <p>
 * To enable auto export of annotation beans set {@code infra.jmx.enabled: true}.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @author Christian Dupuis
 * @author Madhura Bhave
 * @author Artsiom Yudovin
 * @author Scott Frederick
 * @since 4.0 2022/10/9 18:35
 */
@DisableDIAutoConfiguration
@ConditionalOnClass({ MBeanExporter.class })
@EnableConfigurationProperties(JmxProperties.class)
@ConditionalOnProperty(prefix = "infra.jmx", name = "enabled", havingValue = "true")
public class JmxAutoConfiguration {

  private JmxAutoConfiguration() {
  }

  @Primary
  @Component
  @ConditionalOnMissingBean(value = MBeanExporter.class, search = SearchStrategy.CURRENT)
  public static AnnotationMBeanExporter mbeanExporter(JmxProperties properties, ObjectNamingStrategy namingStrategy, BeanFactory beanFactory) {
    AnnotationMBeanExporter exporter = new AnnotationMBeanExporter();
    exporter.setRegistrationPolicy(properties.getRegistrationPolicy());
    exporter.setNamingStrategy(namingStrategy);
    String serverBean = properties.getServer();
    if (StringUtils.isNotEmpty(serverBean)) {
      exporter.setServer(beanFactory.getBean(serverBean, MBeanServer.class));
    }
    exporter.setEnsureUniqueRuntimeObjectNames(properties.isUniqueNames());
    return exporter;
  }

  @Component
  @ConditionalOnMissingBean(value = ObjectNamingStrategy.class, search = SearchStrategy.CURRENT)
  public static ParentAwareNamingStrategy objectNamingStrategy(JmxProperties properties) {
    var namingStrategy = new ParentAwareNamingStrategy(new AnnotationJmxAttributeSource());
    String defaultDomain = properties.getDefaultDomain();
    if (StringUtils.isNotEmpty(defaultDomain)) {
      namingStrategy.setDefaultDomain(defaultDomain);
    }
    namingStrategy.setEnsureUniqueRuntimeObjectNames(properties.isUniqueNames());
    return namingStrategy;
  }

  @Component
  @ConditionalOnMissingBean
  public static MBeanServer mbeanServer() {
    MBeanServerFactoryBean factory = new MBeanServerFactoryBean();
    factory.setLocateExistingServerIfPossible(true);
    factory.afterPropertiesSet();
    return factory.getObject();
  }

}
