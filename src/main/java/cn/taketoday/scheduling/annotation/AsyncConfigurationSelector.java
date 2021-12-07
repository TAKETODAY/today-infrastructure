/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

package cn.taketoday.scheduling.annotation;

import cn.taketoday.context.annotation.AdviceMode;
import cn.taketoday.context.annotation.AdviceModeImportSelector;
import cn.taketoday.lang.Nullable;

/**
 * Selects which implementation of {@link AbstractAsyncConfiguration} should
 * be used based on the value of {@link EnableAsync#mode} on the importing
 * {@code @Configuration} class.
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @see EnableAsync
 * @see ProxyAsyncConfiguration
 * @since 4.0
 */
public class AsyncConfigurationSelector extends AdviceModeImportSelector<EnableAsync> {

  private static final String ASYNC_EXECUTION_ASPECT_CONFIGURATION_CLASS_NAME =
          "cn.taketoday.scheduling.aspectj.AspectJAsyncConfiguration";

  /**
   * Returns {@link ProxyAsyncConfiguration} or {@code AspectJAsyncConfiguration}
   * for {@code PROXY} and {@code ASPECTJ} values of {@link EnableAsync#mode()},
   * respectively.
   */
  @Override
  @Nullable
  public String[] selectImports(AdviceMode adviceMode) {
    return switch (adviceMode) {
      case PROXY -> new String[] { ProxyAsyncConfiguration.class.getName() };
      case ASPECTJ -> new String[] { ASYNC_EXECUTION_ASPECT_CONFIGURATION_CLASS_NAME };
    };
  }

}
