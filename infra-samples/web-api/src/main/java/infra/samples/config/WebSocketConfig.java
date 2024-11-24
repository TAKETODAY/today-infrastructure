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

package infra.samples.config;

import java.io.IOException;

import infra.context.annotation.Configuration;
import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.web.socket.TextMessage;
import infra.web.socket.WebSocketHandler;
import infra.web.socket.WebSocketSession;
import infra.web.socket.config.EnableWebSocket;
import infra.web.socket.config.WebSocketConfigurer;
import infra.web.socket.config.WebSocketHandlerRegistry;
import infra.web.socket.handler.TextWebSocketHandler;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/2/6 11:19
 */
@EnableWebSocket
@Configuration(proxyBeanMethods = false)
public class WebSocketConfig implements WebSocketConfigurer {
  private final Logger log = LoggerFactory.getLogger(getClass());

  @Override
  public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
    registry.addHandler(echoWebSocketHandler(), "/echo")
            .setAllowedOrigins("*");
  }

  public WebSocketHandler echoWebSocketHandler() {
    return new TextWebSocketHandler() {

      @Override
      protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
          session.sendMessage(message);
        }
        catch (IOException e) {
          log.error("消息 '{}' 发送失败", message, e);
          throw new RuntimeException(e);
        }
      }
    };
  }

}
