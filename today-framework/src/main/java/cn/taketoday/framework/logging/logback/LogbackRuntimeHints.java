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

package cn.taketoday.framework.logging.logback;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.pattern.SyslogStartConverter;
import ch.qos.logback.core.rolling.helper.DateTokenConverter;
import ch.qos.logback.core.rolling.helper.IntegerTokenConverter;
import cn.taketoday.aot.hint.MemberCategory;
import cn.taketoday.aot.hint.ReflectionHints;
import cn.taketoday.aot.hint.RuntimeHints;
import cn.taketoday.aot.hint.RuntimeHintsRegistrar;
import cn.taketoday.aot.hint.TypeReference;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ClassUtils;

/**
 * {@link RuntimeHintsRegistrar} for Logback.
 *
 * @author Andy Wilkinson
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class LogbackRuntimeHints implements RuntimeHintsRegistrar {

  @Override
  public void registerHints(RuntimeHints hints, @Nullable ClassLoader classLoader) {
    if (!ClassUtils.isPresent("ch.qos.logback.classic.LoggerContext", classLoader)) {
      return;
    }
    ReflectionHints reflection = hints.reflection();
    registerHintsForLogbackLoggingSystemTypeChecks(reflection, classLoader);
    registerHintsForBuiltInLogbackConverters(reflection);
    registerHintsForInfraConverters(reflection);
  }

  private void registerHintsForLogbackLoggingSystemTypeChecks(ReflectionHints reflection,
          @Nullable ClassLoader classLoader) {
    reflection.registerType(LoggerContext.class);
    reflection.registerTypeIfPresent(classLoader,
            "cn.taketoday.logging.SLF4JBridgeHandler", (typeHint) -> {

            });
  }

  private void registerHintsForBuiltInLogbackConverters(ReflectionHints reflection) {
    registerForPublicConstructorInvocation(reflection, DateTokenConverter.class, IntegerTokenConverter.class, SyslogStartConverter.class);
  }

  private void registerHintsForInfraConverters(ReflectionHints reflection) {
    registerForPublicConstructorInvocation(reflection, ApplicationNameConverter.class, ColorConverter.class,
            ExtendedWhitespaceThrowableProxyConverter.class, WhitespaceThrowableProxyConverter.class,
            CorrelationIdConverter.class);
  }

  private void registerForPublicConstructorInvocation(ReflectionHints reflection, Class<?>... classes) {
    reflection.registerTypes(TypeReference.listOf(classes),
            (hint) -> hint.withMembers(MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS));
  }

}
