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

package infra.web.server.context;

import java.util.HashMap;
import java.util.Map;

import infra.beans.factory.annotation.Value;
import infra.context.ApplicationContext;
import infra.context.ApplicationContextInitializer;
import infra.context.ApplicationListener;
import infra.context.ConfigurableApplicationContext;
import infra.core.env.ConfigurableEnvironment;
import infra.core.env.Environment;
import infra.core.env.MapPropertySource;
import infra.core.env.PropertySource;
import infra.core.env.PropertySources;
import infra.util.StringUtils;
import infra.web.server.WebServer;

/**
 * {@link ApplicationContextInitializer} that sets {@link Environment} properties for the
 * ports that {@link WebServer} servers are actually listening on. The property
 * {@literal "local.server.port"} can be injected directly into tests using
 * {@link Value @Value} or obtained via the {@link Environment}.
 * <p>
 * If the {@link WebServerInitializedEvent} has a
 * {@link WebServerApplicationContext#getServerNamespace() server namespace} , it will be
 * used to construct the property name. For example, the "management" actuator context
 * will have the property name {@literal "local.management.port"}.
 * <p>
 * Properties are automatically propagated up to any parent context.
 *
 * @author Dave Syer
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class ServerPortInfoApplicationContextInitializer implements ApplicationContextInitializer, ApplicationListener<WebServerInitializedEvent> {

  @Override
  public void initialize(ConfigurableApplicationContext applicationContext) {
    applicationContext.addApplicationListener(this);
  }

  @Override
  public void onApplicationEvent(WebServerInitializedEvent event) {
    String propertyName = "local." + getName(event.getApplicationContext()) + ".port";
    setPortProperty(event.getApplicationContext(), propertyName, event.getWebServer().getPort());
  }

  private String getName(WebServerApplicationContext context) {
    String name = context.getServerNamespace();
    return StringUtils.hasText(name) ? name : "server";
  }

  private void setPortProperty(ApplicationContext context, String propertyName, int port) {
    if (context instanceof ConfigurableApplicationContext) {
      setPortProperty(((ConfigurableApplicationContext) context).getEnvironment(), propertyName, port);
    }
    if (context.getParent() != null) {
      setPortProperty(context.getParent(), propertyName, port);
    }
  }

  @SuppressWarnings("unchecked")
  private void setPortProperty(ConfigurableEnvironment environment, String propertyName, int port) {
    PropertySources sources = environment.getPropertySources();
    PropertySource<?> source = sources.get("server.ports");
    if (source == null) {
      source = new MapPropertySource("server.ports", new HashMap<>());
      sources.addFirst(source);
    }
    ((Map<String, Object>) source.getSource()).put(propertyName, port);
  }

}
