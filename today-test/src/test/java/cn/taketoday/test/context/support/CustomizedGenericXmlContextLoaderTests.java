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

package cn.taketoday.test.context.support;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import cn.taketoday.context.support.GenericApplicationContext;
import cn.taketoday.test.context.MergedContextConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit test which verifies that extensions of
 * {@link AbstractGenericContextLoader} are able to <em>customize</em> the
 * newly created {@code ApplicationContext}. Specifically, this test
 * addresses the issues raised in <a
 * href="https://opensource.atlassian.com/projects/spring/browse/SPR-4008"
 * target="_blank">SPR-4008</a>: <em>Supply an opportunity to customize context
 * before calling refresh in ContextLoaders</em>.
 *
 * @author Sam Brannen
 * @since 4.0
 */
class CustomizedGenericXmlContextLoaderTests {

  @Test
  void customizeContext() throws Exception {
    AtomicBoolean customizeInvoked = new AtomicBoolean(false);

    GenericXmlContextLoader customLoader = new GenericXmlContextLoader() {
      @Override
      protected void customizeContext(GenericApplicationContext context) {
        assertThat(context.isActive()).as("The context should not yet have been refreshed.").isFalse();
        customizeInvoked.set(true);
      }
    };

    MergedContextConfiguration mergedConfig =
            new MergedContextConfiguration(getClass(), null, null, null, null);
    customLoader.loadContext(mergedConfig);

    assertThat(customizeInvoked).as("customizeContext() should have been invoked").isTrue();
  }

}
