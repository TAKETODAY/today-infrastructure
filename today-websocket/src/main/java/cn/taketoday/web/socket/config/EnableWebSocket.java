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

package cn.taketoday.web.socket.config;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import cn.taketoday.context.annotation.Import;

/**
 * Add this annotation to an {@code @Configuration} class to configure
 * processing WebSocket requests. A typical configuration would look like this:
 *
 * <pre>{@code
 * @Configuration
 * @EnableWebSocket
 * public class AppConfig {
 *
 * }
 * }</pre>
 *
 * <p>Customize the imported configuration by implementing the
 * {@link WebSocketConfigurer} interface:
 *
 * <pre>{@code
 *
 * @Configuration
 * @EnableWebSocket
 * public class AppConfig implements WebSocketConfigurer {
 *
 *     @Override
 *     public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
 *         registry.addHandler(echoWebSocketHandler(), "/echo");
 *     }
 *
 *     public WebSocketHandler echoWebSocketHandler() {
 *         return new EchoWebSocketHandler();
 *     }
 * }
 * }</pre>
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@Import(WebSocketConfiguration.class)
public @interface EnableWebSocket {
}
