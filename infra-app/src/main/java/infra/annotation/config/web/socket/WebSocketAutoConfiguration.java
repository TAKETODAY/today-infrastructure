/*
 * Copyright 2017 - 2026 the TODAY authors.
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

package infra.annotation.config.web.socket;

import infra.annotation.ConditionalOnWebApplication;
import infra.context.annotation.Lazy;
import infra.context.annotation.config.DisableDIAutoConfiguration;
import infra.context.annotation.config.EnableAutoConfiguration;
import infra.context.condition.ConditionalOnClass;
import infra.web.socket.config.EnableWebSocket;
import infra.web.socket.server.support.WebSocketHandlerMapping;

/**
 * {@link EnableAutoConfiguration Auto-configuration} WebSocket
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/2/5 21:52
 */
@Lazy
@EnableWebSocket
@DisableDIAutoConfiguration
@ConditionalOnWebApplication
@ConditionalOnClass(WebSocketHandlerMapping.class)
public class WebSocketAutoConfiguration {

}
