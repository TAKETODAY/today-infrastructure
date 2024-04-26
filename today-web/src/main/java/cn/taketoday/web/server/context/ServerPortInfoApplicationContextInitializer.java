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

package cn.taketoday.web.server.context;

import java.util.HashMap;
import java.util.Map;

import cn.taketoday.beans.factory.annotation.Value;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.ApplicationContextInitializer;
import cn.taketoday.context.ApplicationListener;
import cn.taketoday.context.ConfigurableApplicationContext;
import cn.taketoday.core.env.ConfigurableEnvironment;
import cn.taketoday.core.env.Environment;
import cn.taketoday.core.env.MapPropertySource;
import cn.taketoday.core.env.PropertySource;
import cn.taketoday.core.env.PropertySources;
import cn.taketoday.util.StringUtils;
import cn.taketoday.web.server.WebServer;

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
