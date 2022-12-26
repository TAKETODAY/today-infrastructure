/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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
import java.util.List;

import cn.taketoday.web.socket.WebSocketExtension;
import jakarta.websocket.Extension;

/**
 * Adapt an instance of {@link WebSocketExtension} to
 * the {@link jakarta.websocket.Extension} interface.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class WebSocketToStandardExtensionAdapter implements Extension {

  private final String name;

  private final List<Parameter> parameters = new ArrayList<>();

  public WebSocketToStandardExtensionAdapter(final WebSocketExtension extension) {
    this.name = extension.getName();
    for (final String paramName : extension.getParameters().keySet()) {
      this.parameters.add(new Parameter() {
        @Override
        public String getName() {
          return paramName;
        }

        @Override
        public String getValue() {
          return extension.getParameters().get(paramName);
        }
      });
    }
  }

  @Override
  public String getName() {
    return this.name;
  }

  @Override
  public List<Parameter> getParameters() {
    return this.parameters;
  }

}
