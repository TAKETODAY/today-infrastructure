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

import java.util.List;
import java.util.Locale;
import java.util.Map;

import cn.taketoday.util.LinkedCaseInsensitiveMap;
import cn.taketoday.web.socket.WebSocketExtension;
import jakarta.websocket.Extension;

/**
 * A subclass of {@link WebSocketExtension} that can be constructed from a
 * {@link jakarta.websocket.Extension}.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class StandardToWebSocketExtensionAdapter extends WebSocketExtension {

  public StandardToWebSocketExtensionAdapter(Extension extension) {
    super(extension.getName(), initParameters(extension));
  }

  private static Map<String, String> initParameters(Extension extension) {
    List<Extension.Parameter> parameters = extension.getParameters();
    Map<String, String> result = new LinkedCaseInsensitiveMap<>(parameters.size(), Locale.ENGLISH);
    for (Extension.Parameter parameter : parameters) {
      result.put(parameter.getName(), parameter.getValue());
    }
    return result;
  }

}