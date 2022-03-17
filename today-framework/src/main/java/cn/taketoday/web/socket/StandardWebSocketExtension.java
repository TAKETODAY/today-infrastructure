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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jakarta.websocket.Extension;

/**
 * Standard websocket extension
 *
 * @author TODAY 2021/5/4 20:14
 * @since 3.0.1
 */
public class StandardWebSocketExtension implements Extension {
  private final String name;
  private final List<Parameter> parameters = new ArrayList<>();

  public StandardWebSocketExtension(String name) {
    this.name = name;
  }

  public void addParameter(Parameter parameter) {
    parameters.add(parameter);
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public List<Parameter> getParameters() {
    return parameters;
  }

  public static StandardWebSocketExtension from(WebSocketExtension socketExtension) {
    final StandardWebSocketExtension extension = new StandardWebSocketExtension(socketExtension.getName());
    for (final Map.Entry<String, String> entry : socketExtension.getParameters().entrySet()) {
      final StandardExtensionParameter extensionParameter = new StandardExtensionParameter(entry.getKey(), entry.getValue());
      extension.addParameter(extensionParameter);
    }
    return extension;
  }

}
