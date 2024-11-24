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

package infra.annotation.config.jmx;

import infra.beans.factory.ObjectProvider;
import infra.context.annotation.config.DisableDIAutoConfiguration;
import infra.context.condition.ConditionalOnMissingBean;
import infra.context.condition.ConditionalOnProperty;
import infra.core.env.Environment;
import infra.jmx.export.MBeanExporter;
import infra.stereotype.Component;

/**
 * Register a JMX component that allows to administer the current application. Intended
 * for internal use only.
 *
 * @author Stephane Nicoll
 * @author Andy Wilkinson
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see InfraApplicationMXBean
 * @since 4.0
 */
@DisableDIAutoConfiguration(after = JmxAutoConfiguration.class)
@ConditionalOnProperty(prefix = "app.admin", value = "enabled", havingValue = "true", matchIfMissing = false)
public class InfraApplicationJmxAutoConfiguration {

  /**
   * The property to use to customize the {@code ObjectName} of the application admin
   * mbean.
   */
  private static final String JMX_NAME_PROPERTY = "app.admin.jmx-name";

  /**
   * The default {@code ObjectName} of the application admin mbean.
   */
  private static final String DEFAULT_JMX_NAME = "infra.app:type=Admin,name=InfraApplication";

  @Component
  @ConditionalOnMissingBean
  static InfraApplicationMXBeanRegistrar infraApplicationRegistrar(
          ObjectProvider<MBeanExporter> mbeanExporters, Environment environment) throws Exception {
    String jmxName = environment.getProperty(JMX_NAME_PROPERTY, DEFAULT_JMX_NAME);
    if (mbeanExporters != null) { // Make sure to not register that MBean twice
      for (MBeanExporter mbeanExporter : mbeanExporters) {
        mbeanExporter.addExcludedBean(jmxName);
      }
    }
    return new InfraApplicationMXBeanRegistrar(jmxName);
  }

}
