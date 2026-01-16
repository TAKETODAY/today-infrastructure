/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.web.socket.config;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import infra.context.annotation.Import;

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
