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

package infra.instrument;

import org.junit.jupiter.api.Test;

import java.lang.instrument.Instrumentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/3/16 23:05
 */
class InstrumentationSavingAgentTests {

  @Test
  void test() {
    assertThat(InstrumentationSavingAgent.getInstrumentation()).isNull();

    Instrumentation instrumentation = mock(Instrumentation.class);
    InstrumentationSavingAgent.agentmain("", instrumentation);
    assertThat(InstrumentationSavingAgent.getInstrumentation()).isSameAs(instrumentation);

    InstrumentationSavingAgent.agentmain("", null);

    InstrumentationSavingAgent.premain("", instrumentation);
    assertThat(InstrumentationSavingAgent.getInstrumentation()).isSameAs(instrumentation);

  }

}