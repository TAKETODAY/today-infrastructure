/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

package cn.taketoday.web.socket;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import cn.taketoday.core.AttributeAccessorSupport;
import jakarta.websocket.Decoder;
import jakarta.websocket.Encoder;
import jakarta.websocket.Extension;
import jakarta.websocket.server.ServerEndpointConfig;

/**
 * @author TODAY 2021/5/6 17:36
 * @since 3.0.1
 */
public class StandardServerEndpointConfig extends AttributeAccessorSupport implements ServerEndpointConfig {
  private final String path;
  private final Configurator configurator;

  public StandardServerEndpointConfig(String path, Configurator configurator) {
    this.path = path;
    this.configurator = configurator;
  }

  @Override
  public Class<?> getEndpointClass() {
    return StandardEndpoint.class;
  }

  @Override
  public String getPath() {
    return path;
  }

  @Override
  public List<String> getSubprotocols() {
    return Collections.emptyList();
  }

  @Override
  public List<Extension> getExtensions() {
    return Collections.emptyList();
  }

  @Override
  public Configurator getConfigurator() {
    return configurator;
  }

  @Override
  public List<Class<? extends Encoder>> getEncoders() {
    return Collections.emptyList();
  }

  @Override
  public List<Class<? extends Decoder>> getDecoders() {
    return Collections.emptyList();
  }

  @Override
  public Map<String, Object> getUserProperties() {
    return getAttributes();
  }
}
