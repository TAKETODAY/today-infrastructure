/*
 * Copyright 2012-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

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

  @SuppressWarnings("NullAway")
  @Component
  @ConditionalOnMissingBean
  public static MBeanServer mbeanServer() {
    MBeanServerFactoryBean factory = new MBeanServerFactoryBean();
    factory.setLocateExistingServerIfPossible(true);
    factory.afterPropertiesSet();
    return factory.getObject();
  }

}
