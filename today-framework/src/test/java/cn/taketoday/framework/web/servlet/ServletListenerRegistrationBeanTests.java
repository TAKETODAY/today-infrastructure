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

package cn.taketoday.framework.web.servlet;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.EventListener;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextListener;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

/**
 * Tests for {@link ServletListenerRegistrationBean}.
 *
 * @author Dave Syer
 */
@ExtendWith(MockitoExtension.class)
class ServletListenerRegistrationBeanTests {

  @Mock
  private ServletContextListener listener;

  @Mock
  private ServletContext servletContext;

  @Test
  void startupWithDefaults() throws Exception {
    ServletListenerRegistrationBean<ServletContextListener> bean = new ServletListenerRegistrationBean<>(
            this.listener);
    bean.onStartup(this.servletContext);
    then(this.servletContext).should().addListener(this.listener);
  }

  @Test
  void disable() throws Exception {
    ServletListenerRegistrationBean<ServletContextListener> bean = new ServletListenerRegistrationBean<>(
            this.listener);
    bean.setEnabled(false);
    bean.onStartup(this.servletContext);
    then(this.servletContext).should(never()).addListener(any(ServletContextListener.class));
  }

  @Test
  void cannotRegisterUnsupportedType() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> new ServletListenerRegistrationBean<>(new EventListener() {

            })).withMessageContaining("Listener is not of a supported type");
  }

}
