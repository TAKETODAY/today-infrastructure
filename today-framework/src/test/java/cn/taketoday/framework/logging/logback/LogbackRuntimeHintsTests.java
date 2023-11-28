/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.framework.logging.logback;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.Consumer;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.pattern.SyslogStartConverter;
import ch.qos.logback.core.rolling.helper.DateTokenConverter;
import ch.qos.logback.core.rolling.helper.IntegerTokenConverter;
import cn.taketoday.aot.hint.MemberCategory;
import cn.taketoday.aot.hint.ReflectionHints;
import cn.taketoday.aot.hint.RuntimeHints;
import cn.taketoday.aot.hint.TypeHint;
import cn.taketoday.logging.SLF4JBridgeHandler;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/7/3 22:42
 */
class LogbackRuntimeHintsTests {

  @Test
  void registersHintsForTypesCheckedByLogbackLoggingSystem() {
    ReflectionHints reflection = registerHints();
    assertThat(reflection.getTypeHint(LoggerContext.class)).isNotNull();
    assertThat(reflection.getTypeHint(SLF4JBridgeHandler.class)).isNotNull();
  }

  @Test
  void registersHintsForBuiltInLogbackConverters() {
    ReflectionHints reflection = registerHints();
    assertThat(logbackConverters()).allSatisfy(registeredForPublicConstructorInvocation(reflection));
  }

  @Test
  void registersHintsForInfraConverters() throws LinkageError {
    ReflectionHints reflection = registerHints();
    assertThat(List.of(ColorConverter.class, ExtendedWhitespaceThrowableProxyConverter.class,
            WhitespaceThrowableProxyConverter.class))
            .allSatisfy(registeredForPublicConstructorInvocation(reflection));
  }

  @Test
  void doesNotRegisterHintsWhenLoggerContextIsNotAvailable() {
    RuntimeHints hints = new RuntimeHints();
    new LogbackRuntimeHints().registerHints(hints, ClassLoader.getPlatformClassLoader());
    assertThat(hints.reflection().typeHints()).isEmpty();
  }

  private ReflectionHints registerHints() {
    RuntimeHints hints = new RuntimeHints();
    new LogbackRuntimeHints().registerHints(hints, getClass().getClassLoader());
    return hints.reflection();
  }

  private Consumer<Class<?>> registeredForPublicConstructorInvocation(ReflectionHints reflection) {
    return (converter) -> {
      TypeHint typeHint = reflection.getTypeHint(converter);
      assertThat(typeHint).isNotNull();
      assertThat(typeHint.getMemberCategories()).containsExactly(MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS);
    };
  }

  private List<Class<?>> logbackConverters() {
    return List.of(DateTokenConverter.class, IntegerTokenConverter.class, SyslogStartConverter.class);
  }

}