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

package cn.taketoday.test.context.web.socket;

import cn.taketoday.context.ConfigurableApplicationContext;
import cn.taketoday.lang.Nullable;
import cn.taketoday.mock.api.MockContext;
import cn.taketoday.test.context.ContextCustomizer;
import cn.taketoday.test.context.MergedContextConfiguration;
import cn.taketoday.web.mock.WebApplicationContext;

/**
 * {@link ContextCustomizer} that instantiates a new {@link MockServerContainer}
 * and stores it in the {@code ServletContext} under the attribute named
 * {@code "jakarta.websocket.server.ServerContainer"}.
 *
 * @author Sam Brannen
 * @since 4.0
 */
class MockServerContainerContextCustomizer implements ContextCustomizer {

  @Override
  public void customizeContext(ConfigurableApplicationContext context, MergedContextConfiguration mergedConfig) {
    if (context instanceof WebApplicationContext wac) {
      MockContext sc = wac.getServletContext();
      if (sc != null) {
        sc.setAttribute("jakarta.websocket.server.ServerContainer", new MockServerContainer());
      }
    }
  }

  @Override
  public boolean equals(@Nullable Object other) {
    return (this == other || (other != null && getClass() == other.getClass()));
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }

}
