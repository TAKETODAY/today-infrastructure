/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.framework.diagnostics;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.framework.ApplicationType;
import cn.taketoday.framework.builder.ApplicationBuilder;
import cn.taketoday.framework.test.system.CapturedOutput;
import cn.taketoday.framework.test.system.OutputCaptureExtension;
import cn.taketoday.framework.web.server.PortInUseException;
import jakarta.annotation.PostConstruct;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Integration tests for {@link FailureAnalyzers}.
 *
 * @author Andy Wilkinson
 */
@Execution(ExecutionMode.SAME_THREAD)
@ExtendWith(OutputCaptureExtension.class)
class FailureAnalyzersIntegrationTests {

  @Test
  void analysisIsPerformed(CapturedOutput output) {
    assertThatExceptionOfType(Exception.class)
            .isThrownBy(() -> ApplicationBuilder.from(TestConfiguration.class).type(ApplicationType.NONE_WEB).run());
    assertThat(output).contains("APPLICATION FAILED TO START");
  }

  @Configuration(proxyBeanMethods = false)
  static class TestConfiguration {

    @PostConstruct
    void fail() {
      throw new PortInUseException(8080);
    }

  }

}
