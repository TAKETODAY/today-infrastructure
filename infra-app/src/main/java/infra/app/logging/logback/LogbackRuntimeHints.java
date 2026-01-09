/*
 * Copyright 2012-present the original author or authors.
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

package infra.app.logging.logback;

import org.jspecify.annotations.Nullable;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.pattern.SyslogStartConverter;
import ch.qos.logback.core.rolling.helper.DateTokenConverter;
import ch.qos.logback.core.rolling.helper.IntegerTokenConverter;
import infra.aot.hint.MemberCategory;
import infra.aot.hint.ReflectionHints;
import infra.aot.hint.RuntimeHints;
import infra.aot.hint.RuntimeHintsRegistrar;
import infra.aot.hint.TypeReference;
import infra.util.ClassUtils;

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
            "infra.logging.SLF4JBridgeHandler", (typeHint) -> {

            });
  }

  private void registerHintsForBuiltInLogbackConverters(ReflectionHints reflection) {
    registerForPublicConstructorInvocation(reflection, DateTokenConverter.class, IntegerTokenConverter.class,
            SyslogStartConverter.class);
  }

  private void registerHintsForInfraConverters(ReflectionHints reflection) {
    registerForPublicConstructorInvocation(reflection, ColorConverter.class,
            EnclosedInSquareBracketsConverter.class, ExtendedWhitespaceThrowableProxyConverter.class,
            WhitespaceThrowableProxyConverter.class, CorrelationIdConverter.class);
  }

  private void registerForPublicConstructorInvocation(ReflectionHints reflection, Class<?>... classes) {
    reflection.registerTypes(TypeReference.listOf(classes),
            (hint) -> hint.withMembers(MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS));
  }

}
