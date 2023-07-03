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

package cn.taketoday.framework.web.embedded.undertow;

import org.junit.jupiter.api.Test;

import cn.taketoday.aot.hint.RuntimeHints;
import cn.taketoday.aot.hint.predicate.ReflectionHintsPredicates.FieldHintPredicate;
import cn.taketoday.aot.hint.predicate.RuntimeHintsPredicates;
import cn.taketoday.framework.web.embedded.undertow.UndertowWebServer.UndertowWebServerRuntimeHints;
import cn.taketoday.util.ReflectionUtils;
import io.undertow.Undertow;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link UndertowWebServerRuntimeHints}.
 *
 * @author Andy Wilkinson
 */
class UndertowWebServerRuntimeHintsTests {

  @Test
  void registersHints() throws ClassNotFoundException {
    RuntimeHints runtimeHints = new RuntimeHints();
    new UndertowWebServerRuntimeHints().registerHints(runtimeHints, getClass().getClassLoader());
    assertThat(RuntimeHintsPredicates.reflection().onField(Undertow.class, "listeners")).accepts(runtimeHints);
    assertThat(RuntimeHintsPredicates.reflection().onField(Undertow.class, "channels")).accepts(runtimeHints);
    assertThat(reflectionOnField("io.undertow.Undertow$ListenerConfig", "type")).accepts(runtimeHints);
    assertThat(reflectionOnField("io.undertow.Undertow$ListenerConfig", "port")).accepts(runtimeHints);
    assertThat(reflectionOnField("io.undertow.protocols.ssl.UndertowAcceptingSslChannel", "ssl"))
            .accepts(runtimeHints);
  }

  private FieldHintPredicate reflectionOnField(String className, String fieldName) throws ClassNotFoundException {
    return RuntimeHintsPredicates.reflection()
            .onField(ReflectionUtils.findField(Class.forName(className), fieldName));
  }

}
