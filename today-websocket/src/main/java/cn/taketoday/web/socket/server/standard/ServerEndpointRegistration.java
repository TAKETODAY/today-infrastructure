/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.beans.factory.BeanFactoryAware;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.web.socket.handler.BeanCreatingHandlerProvider;
import jakarta.websocket.Decoder;
import jakarta.websocket.Encoder;
import jakarta.websocket.Endpoint;
import jakarta.websocket.Extension;
import jakarta.websocket.HandshakeResponse;
import jakarta.websocket.server.HandshakeRequest;
import jakarta.websocket.server.ServerEndpointConfig;

/**
 * An implementation of {@link ServerEndpointConfig} for use in
 * Infra-based applications. A {@link ServerEndpointRegistration} bean is detected by
 * {@link ServerEndpointExporter} and registered with a Jakarta WebSocket runtime at startup.
 *
 * <p>Class constructors accept a singleton {@link Endpoint} instance
 * or an Endpoint specified by type {@link Class}. When specified by type, the endpoint
 * will be instantiated and initialized through the Infra ApplicationContext before
 * each client WebSocket connection.
 *
 * <p>This class also extends {@link Configurator} to make it easier
 * to override methods for customizing the handshake process.
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see ServerEndpointExporter
 * @since 4.0
 */
public class ServerEndpointRegistration
        extends ServerEndpointConfig.Configurator implements ServerEndpointConfig, BeanFactoryAware {

  private final String path;

  @Nullable
  private final Endpoint endpoint;

  @Nullable
  private final BeanCreatingHandlerProvider<Endpoint> endpointProvider;

  private List<String> subprotocols = new ArrayList<>(0);

  private List<Extension> extensions = new ArrayList<>(0);

  private List<Class<? extends Encoder>> encoders = new ArrayList<>(0);

  private List<Class<? extends Decoder>> decoders = new ArrayList<>(0);

  private final Map<String, Object> userProperties = new HashMap<>(4);

  /**
   * Create a new {@link ServerEndpointRegistration} instance from an
   * {@code jakarta.websocket.Endpoint} instance.
   *
   * @param path the endpoint path
   * @param endpoint the endpoint instance
   */
  public ServerEndpointRegistration(String path, Endpoint endpoint) {
    Assert.hasText(path, "Path must not be empty");
    Assert.notNull(endpoint, "Endpoint must not be null");
    this.path = path;
    this.endpoint = endpoint;
    this.endpointProvider = null;
  }

  /**
   * Create a new {@link ServerEndpointRegistration} instance from an
   * {@code jakarta.websocket.Endpoint} class.
   *
   * @param path the endpoint path
   * @param endpointClass the endpoint class
   */
  public ServerEndpointRegistration(String path, Class<? extends Endpoint> endpointClass) {
    Assert.hasText(path, "Path must not be empty");
    Assert.notNull(endpointClass, "Endpoint Class must not be null");
    this.path = path;
    this.endpoint = null;
    this.endpointProvider = new BeanCreatingHandlerProvider<>(endpointClass);
  }

  // ServerEndpointConfig implementation

  @Override
  public String getPath() {
    return this.path;
  }

  @Override
  public Class<? extends Endpoint> getEndpointClass() {
    if (this.endpoint != null) {
      return this.endpoint.getClass();
    }
    else {
      Assert.state(this.endpointProvider != null, "No endpoint set");
      return this.endpointProvider.getHandlerType();
    }
  }

  public Endpoint getEndpoint() {
    if (this.endpoint != null) {
      return this.endpoint;
    }
    else {
      Assert.state(this.endpointProvider != null, "No endpoint set");
      return this.endpointProvider.getHandler();
    }
  }

  public void setSubprotocols(List<String> subprotocols) {
    this.subprotocols = subprotocols;
  }

  @Override
  public List<String> getSubprotocols() {
    return this.subprotocols;
  }

  public void setExtensions(List<Extension> extensions) {
    this.extensions = extensions;
  }

  @Override
  public List<Extension> getExtensions() {
    return this.extensions;
  }

  public void setEncoders(List<Class<? extends Encoder>> encoders) {
    this.encoders = encoders;
  }

  @Override
  public List<Class<? extends Encoder>> getEncoders() {
    return this.encoders;
  }

  public void setDecoders(List<Class<? extends Decoder>> decoders) {
    this.decoders = decoders;
  }

  @Override
  public List<Class<? extends Decoder>> getDecoders() {
    return this.decoders;
  }

  public void setUserProperties(Map<String, Object> userProperties) {
    this.userProperties.clear();
    this.userProperties.putAll(userProperties);
  }

  @Override
  public Map<String, Object> getUserProperties() {
    return this.userProperties;
  }

  @Override
  public Configurator getConfigurator() {
    return this;
  }

  // ServerEndpointConfig.Configurator implementation

  @SuppressWarnings("unchecked")
  @Override
  public final <T> T getEndpointInstance(Class<T> clazz) {
    return (T) getEndpoint();
  }

  @Override
  public void modifyHandshake(ServerEndpointConfig sec, HandshakeRequest request, HandshakeResponse response) {
    super.modifyHandshake(this, request, response);
  }

  // Remaining methods

  @Override
  public void setBeanFactory(BeanFactory beanFactory) {
    if (this.endpointProvider != null) {
      this.endpointProvider.setBeanFactory(beanFactory);
    }
  }

  @Override
  public String toString() {
    return "ServerEndpointRegistration for path '" + getPath() + "': " + getEndpointClass();
  }

}
