/*
 * Copyright 2017 - 2026 the TODAY authors.
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

  private InfraApplicationJmxAutoConfiguration() {
  }

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
  public static InfraApplicationMXBeanRegistrar infraApplicationRegistrar(
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
