/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.web.socket.server.support;

import cn.taketoday.aot.hint.MemberCategory;
import cn.taketoday.aot.hint.ReflectionHints;
import cn.taketoday.aot.hint.RuntimeHints;
import cn.taketoday.aot.hint.RuntimeHintsRegistrar;
import cn.taketoday.aot.hint.TypeReference;
import cn.taketoday.util.ClassUtils;

/**
 * {@link RuntimeHintsRegistrar} implementation that registers reflection entries
 * for {@link AbstractHandshakeHandler}.
 *
 * @author Sebastien Deleuze
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class HandshakeHandlerRuntimeHints implements RuntimeHintsRegistrar {

  private static final boolean tomcatWsPresent;

  private static final boolean jettyWsPresent;

  private static final boolean undertowWsPresent;

  static {
    ClassLoader classLoader = AbstractHandshakeHandler.class.getClassLoader();
    tomcatWsPresent = ClassUtils.isPresent("org.apache.tomcat.websocket.server.WsHttpUpgradeHandler", classLoader);
    jettyWsPresent = ClassUtils.isPresent("org.eclipse.jetty.ee10.websocket.server.JettyWebSocketServerContainer", classLoader);
    undertowWsPresent = ClassUtils.isPresent("io.undertow.websockets.jsr.ServerWebSocketContainer", classLoader);
  }

  @Override
  public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
    ReflectionHints reflectionHints = hints.reflection();
    if (tomcatWsPresent) {
      registerType(reflectionHints, "cn.taketoday.web.socket.server.standard.TomcatRequestUpgradeStrategy");
    }
    else if (jettyWsPresent) {
      registerType(reflectionHints, "cn.taketoday.web.socket.server.jetty.JettyRequestUpgradeStrategy");
    }
    else if (undertowWsPresent) {
      registerType(reflectionHints, "cn.taketoday.web.socket.server.standard.UndertowRequestUpgradeStrategy");
    }
  }

  private void registerType(ReflectionHints reflectionHints, String className) {
    reflectionHints.registerType(TypeReference.of(className),
            builder -> builder.withMembers(MemberCategory.INVOKE_DECLARED_CONSTRUCTORS));
  }
}
