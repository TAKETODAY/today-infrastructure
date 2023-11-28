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

package cn.taketoday.annotation.config.web.embedded;

import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.condition.JRE;

import cn.taketoday.framework.web.embedded.jetty.ConfigurableJettyWebServerFactory;
import cn.taketoday.framework.web.server.ServerProperties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.assertArg;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/7/1 23:36
 */
class JettyVirtualThreadsWebServerFactoryCustomizerTests {

  @Test
  @EnabledForJreRange(min = JRE.JAVA_21)
  void shouldConfigureVirtualThreads() {
    ServerProperties properties = new ServerProperties();
    var customizer = new JettyVirtualThreadsWebServerFactoryCustomizer(properties);
    ConfigurableJettyWebServerFactory factory = mock(ConfigurableJettyWebServerFactory.class);
    customizer.customize(factory);
    then(factory).should().setThreadPool(assertArg((threadPool) -> {
      assertThat(threadPool).isInstanceOf(QueuedThreadPool.class);
      QueuedThreadPool queuedThreadPool = (QueuedThreadPool) threadPool;
      assertThat(queuedThreadPool.getVirtualThreadsExecutor()).isNotNull();
    }));
  }

}