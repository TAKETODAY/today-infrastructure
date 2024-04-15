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

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.pattern.SyslogStartConverter;
import ch.qos.logback.core.pattern.Converter;
import ch.qos.logback.core.rolling.helper.DateTokenConverter;
import ch.qos.logback.core.rolling.helper.IntegerTokenConverter;
import cn.taketoday.aot.hint.MemberCategory;
import cn.taketoday.aot.hint.ReflectionHints;
import cn.taketoday.aot.hint.RuntimeHints;
import cn.taketoday.aot.hint.TypeHint;
import cn.taketoday.core.io.PathMatchingPatternResourceLoader;
import cn.taketoday.core.io.Resource;
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
  void registersHintsForSpringBootConverters() throws IOException {
    ReflectionHints reflection = registerHints();
    assertThat(converterClasses()).allSatisfy(registeredForPublicConstructorInvocation(reflection));
  }

  @SuppressWarnings("unchecked")
  private Stream<Class<Converter<?>>> converterClasses() throws IOException {
    PathMatchingPatternResourceLoader resolver = new PathMatchingPatternResourceLoader();
    return resolver.getResources("classpath:cn/taketoday/framework/logging/logback/*.class").stream()
            .filter(Resource::isFile)
            .map(this::loadClass)
            .filter(Converter.class::isAssignableFrom)
            .map((type) -> (Class<Converter<?>>) type);
  }

  private Class<?> loadClass(Resource resource) {
    try {
      return getClass().getClassLoader()
              .loadClass("cn.taketoday.framework.logging.logback." + resource.getName().replace(".class", ""));
    }
    catch (ClassNotFoundException ex) {
      throw new RuntimeException(ex);
    }
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