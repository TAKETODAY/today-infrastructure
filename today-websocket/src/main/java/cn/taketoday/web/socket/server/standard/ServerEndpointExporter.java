/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.web.socket.server.standard;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import cn.taketoday.beans.factory.InitializingBean;
import cn.taketoday.beans.factory.SmartInitializingSingleton;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.web.servlet.WebApplicationObjectSupport;
import jakarta.servlet.ServletContext;
import jakarta.websocket.DeploymentException;
import jakarta.websocket.server.ServerContainer;
import jakarta.websocket.server.ServerEndpoint;
import jakarta.websocket.server.ServerEndpointConfig;

/**
 * Detects beans of type {@link ServerEndpointConfig} and registers
 * with the standard Jakarta WebSocket runtime. Also detects beans annotated with
 * {@link ServerEndpoint} and registers them as well. Although not required, it is likely
 * annotated endpoints should have their {@code configurator} property set to
 * {@link InfraConfigurator}.
 *
 * <p>When this class is used, by declaring it in Infra configuration, it should be
 * possible to turn off a Servlet container's scan for WebSocket endpoints. This can be
 * done with the help of the {@code <absolute-ordering>} element in {@code web.xml}.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @author Juergen Hoeller
 * @see ServerEndpointRegistration
 * @see InfraConfigurator
 * @see ServletServerContainerFactoryBean
 * @since 4.0
 */
public class ServerEndpointExporter extends WebApplicationObjectSupport
        implements InitializingBean, SmartInitializingSingleton {

  @Nullable
  private List<Class<?>> annotatedEndpointClasses;

  @Nullable
  private ServerContainer serverContainer;

  /**
   * Explicitly list annotated endpoint types that should be registered on startup. This
   * can be done if you wish to turn off a Servlet container's scan for endpoints, which
   * goes through all 3rd party jars in the classpath, and rely on configuration instead.
   *
   * @param annotatedEndpointClasses {@link ServerEndpoint}-annotated types
   */
  public void setAnnotatedEndpointClasses(Class<?>... annotatedEndpointClasses) {
    this.annotatedEndpointClasses = Arrays.asList(annotatedEndpointClasses);
  }

  /**
   * Set the JSR-356 {@link ServerContainer} to use for endpoint registration.
   * If not set, the container is going to be retrieved via the {@code ServletContext}.
   */
  public void setServerContainer(@Nullable ServerContainer serverContainer) {
    this.serverContainer = serverContainer;
  }

  /**
   * Return the JSR-356 {@link ServerContainer} to use for endpoint registration.
   */
  @Nullable
  protected ServerContainer getServerContainer() {
    return this.serverContainer;
  }

  @Override
  protected void initServletContext(ServletContext servletContext) {
    if (this.serverContainer == null) {
      this.serverContainer = (ServerContainer)
              servletContext.getAttribute("jakarta.websocket.server.ServerContainer");
    }
  }

  @Override
  protected boolean isContextRequired() {
    return false;
  }

  @Override
  public void afterPropertiesSet() {
    Assert.state(getServerContainer() != null, "jakarta.websocket.server.ServerContainer not available");
  }

  @Override
  public void afterSingletonsInstantiated() {
    registerEndpoints();
  }

  /**
   * Actually register the endpoints. Called by {@link #afterSingletonsInstantiated()}.
   */
  protected void registerEndpoints() {
    var endpointClasses = new LinkedHashSet<Class<?>>();
    if (this.annotatedEndpointClasses != null) {
      endpointClasses.addAll(this.annotatedEndpointClasses);
    }

    ApplicationContext context = getApplicationContext();
    if (context != null) {
      Set<String> endpointBeanNames = context.getBeanNamesForAnnotation(ServerEndpoint.class);
      for (String beanName : endpointBeanNames) {
        endpointClasses.add(context.getType(beanName));
      }
    }

    for (Class<?> endpointClass : endpointClasses) {
      registerEndpoint(endpointClass);
    }

    if (context != null) {
      var endpointConfigMap = context.getBeansOfType(ServerEndpointConfig.class);
      for (ServerEndpointConfig endpointConfig : endpointConfigMap.values()) {
        registerEndpoint(endpointConfig);
      }
    }
  }

  private void registerEndpoint(Class<?> endpointClass) {
    ServerContainer serverContainer = getServerContainer();
    Assert.state(serverContainer != null,
            "No ServerContainer set. Most likely the server's own WebSocket ServletContainerInitializer " +
                    "has not run yet. Was the Infra ApplicationContext refreshed through a " +
                    "cn.taketoday.web.servlet.ContextLoaderListener, " +
                    "i.e. after the ServletContext has been fully initialized?");
    try {
      if (log.isDebugEnabled()) {
        log.debug("Registering @ServerEndpoint class: {}", endpointClass);
      }
      serverContainer.addEndpoint(endpointClass);
    }
    catch (DeploymentException ex) {
      throw new IllegalStateException("Failed to register @ServerEndpoint class: " + endpointClass, ex);
    }
  }

  private void registerEndpoint(ServerEndpointConfig endpointConfig) {
    ServerContainer serverContainer = getServerContainer();
    Assert.state(serverContainer != null, "No ServerContainer set");
    try {
      if (log.isDebugEnabled()) {
        log.debug("Registering ServerEndpointConfig: {}", endpointConfig);
      }
      serverContainer.addEndpoint(endpointConfig);
    }
    catch (DeploymentException ex) {
      throw new IllegalStateException("Failed to register ServerEndpointConfig: " + endpointConfig, ex);
    }
  }

}
