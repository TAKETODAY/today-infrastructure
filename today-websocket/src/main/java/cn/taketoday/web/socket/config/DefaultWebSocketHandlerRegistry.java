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

package cn.taketoday.web.socket.config;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import cn.taketoday.core.MultiValueMap;
import cn.taketoday.web.HttpRequestHandler;
import cn.taketoday.web.handler.AbstractHandlerMapping;
import cn.taketoday.web.handler.SimpleUrlHandlerMapping;
import cn.taketoday.web.socket.WebSocketHandler;
import cn.taketoday.web.socket.server.support.WebSocketHandlerMapping;

/**
 * {@link WebSocketHandlerRegistry} with MVC handler mappings for the
 * handshake requests.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class DefaultWebSocketHandlerRegistry implements WebSocketHandlerRegistry {

  private final List<DefaultWebSocketHandlerRegistration> registrations = new ArrayList<>(4);

  private int order = 1;

  public DefaultWebSocketHandlerRegistry() { }

  @Override
  public WebSocketHandlerRegistration addHandler(WebSocketHandler handler, String... paths) {
    DefaultWebSocketHandlerRegistration registration = new DefaultWebSocketHandlerRegistration();
    registration.addHandler(handler, paths);
    this.registrations.add(registration);
    return registration;
  }

  /**
   * Set the order for the resulting {@link SimpleUrlHandlerMapping} relative to
   * other handler mappings configured in Spring MVC.
   * <p>The default value is 1.
   */
  public void setOrder(int order) {
    this.order = order;
  }

  public int getOrder() {
    return this.order;
  }

  public AbstractHandlerMapping getHandlerMapping() {
    LinkedHashMap<String, Object> urlMap = new LinkedHashMap<>();
    for (DefaultWebSocketHandlerRegistration registration : this.registrations) {
      MultiValueMap<HttpRequestHandler, String> mappings = registration.getMappings();
      for (Map.Entry<HttpRequestHandler, List<String>> entry : mappings.entrySet()) {
        for (String pattern : entry.getValue()) {
          urlMap.put(pattern, entry.getKey());
        }
      }
    }

    WebSocketHandlerMapping hm = new WebSocketHandlerMapping();
    hm.setUrlMap(urlMap);
    hm.setOrder(this.order);
    return hm;
  }

}
