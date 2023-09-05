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

package cn.taketoday.annotation.config.jmx;

import javax.management.MBeanServer;

import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.context.annotation.EnableMBeanExport;
import cn.taketoday.context.annotation.Primary;
import cn.taketoday.context.annotation.config.DisableDIAutoConfiguration;
import cn.taketoday.context.annotation.config.EnableAutoConfiguration;
import cn.taketoday.context.condition.ConditionalOnClass;
import cn.taketoday.context.condition.ConditionalOnMissingBean;
import cn.taketoday.context.condition.ConditionalOnProperty;
import cn.taketoday.context.condition.SearchStrategy;
import cn.taketoday.context.properties.EnableConfigurationProperties;
import cn.taketoday.jmx.export.MBeanExporter;
import cn.taketoday.jmx.export.annotation.AnnotationJmxAttributeSource;
import cn.taketoday.jmx.export.annotation.AnnotationMBeanExporter;
import cn.taketoday.jmx.export.naming.ObjectNamingStrategy;
import cn.taketoday.jmx.support.MBeanServerFactoryBean;
import cn.taketoday.stereotype.Component;
import cn.taketoday.util.StringUtils;

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

  @Primary
  @Component
  @ConditionalOnMissingBean(value = MBeanExporter.class, search = SearchStrategy.CURRENT)
  static AnnotationMBeanExporter mbeanExporter(JmxProperties properties, ObjectNamingStrategy namingStrategy, BeanFactory beanFactory) {
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
  static ParentAwareNamingStrategy objectNamingStrategy(JmxProperties properties) {
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
  static MBeanServer mbeanServer() {
    MBeanServerFactoryBean factory = new MBeanServerFactoryBean();
    factory.setLocateExistingServerIfPossible(true);
    factory.afterPropertiesSet();
    return factory.getObject();
  }

}
