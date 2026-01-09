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

package infra.app;

import org.jspecify.annotations.Nullable;

import java.util.List;

import infra.aot.hint.RuntimeHints;
import infra.aot.hint.RuntimeHintsRegistrar;
import infra.aot.hint.TypeReference;
import infra.core.ReactiveStreams;
import infra.lang.TodayStrategies;
import infra.util.ClassUtils;

/**
 * An enumeration of possible types of application.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/1/15 14:58
 */
public enum ApplicationType {

  /**
   * The application should not run as a web application and should not start an
   * embedded web server.
   */
  NORMAL,

  /**
   * The application should run as a reactive web application and should start an
   * embedded reactive web server.
   */
  REACTIVE_WEB,

  /**
   * The application should run as a netty web application and should start an
   * embedded netty web server.
   */
  NETTY_WEB;

  public static final String WEB_INDICATOR_CLASS = "infra.web.RequestContext";

  public static final String REACTOR_INDICATOR_CLASS = ReactiveStreams.REACTOR_INDICATOR_CLASS;

  public static final String NETTY_WEB_INDICATOR_CLASS = "io.netty.handler.codec.http.HttpRequest";

  /**
   * Determines the application type by checking for the presence of indicator classes.
   * If both the web indicator class ({@link #WEB_INDICATOR_CLASS}) and Netty web indicator class
   * ({@link #NETTY_WEB_INDICATOR_CLASS}) are present, returns {@link ApplicationType#NETTY_WEB}.
   * Otherwise, returns {@link ApplicationType#NORMAL}.
   *
   * @return the application type
   */
  public static ApplicationType forDefaults() {
    List<Resolver> resolvers = TodayStrategies.find(Resolver.class);
    for (Resolver resolver : resolvers) {
      ApplicationType resolved = resolver.resolve();
      if (resolved != null) {
        return resolved;
      }
    }

    ClassLoader classLoader = ApplicationType.class.getClassLoader();
    if (ClassUtils.isPresent(WEB_INDICATOR_CLASS, classLoader)
            && ClassUtils.isPresent(NETTY_WEB_INDICATOR_CLASS, classLoader)) {
      return ApplicationType.NETTY_WEB;
    }
    return ApplicationType.NORMAL;
  }

  /**
   * Strategy that may be implemented by a module that can deduce the
   * {@link ApplicationType}.
   *
   * @since 5.0
   */
  @FunctionalInterface
  public interface Resolver {

    /**
     * Deduce the application type.
     *
     * @return the application type or {@code null}
     */
    @Nullable
    ApplicationType resolve();

  }

  static class Hints implements RuntimeHintsRegistrar {

    @Override
    public void registerHints(RuntimeHints hints, @Nullable ClassLoader classLoader) {
      registerTypeIfPresent(WEB_INDICATOR_CLASS, classLoader, hints);
      registerTypeIfPresent(NETTY_WEB_INDICATOR_CLASS, classLoader, hints);
      registerTypeIfPresent(REACTOR_INDICATOR_CLASS, classLoader, hints);
    }

    private void registerTypeIfPresent(String typeName, @Nullable ClassLoader classLoader, RuntimeHints hints) {
      if (ClassUtils.isPresent(typeName, classLoader)) {
        hints.reflection().registerType(TypeReference.of(typeName));
      }
    }

  }

}
