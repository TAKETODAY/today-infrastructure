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

package cn.taketoday.framework.context;

import cn.taketoday.context.ApplicationListener;
import cn.taketoday.context.properties.bind.Binder;
import cn.taketoday.core.env.ConfigurableEnvironment;
import cn.taketoday.framework.ansi.AnsiOutput;
import cn.taketoday.framework.ansi.AnsiOutput.Enabled;
import cn.taketoday.framework.context.event.ApplicationEnvironmentPreparedEvent;

/**
 * An {@link ApplicationListener} that configures {@link AnsiOutput} depending on the
 * value of the property {@code spring.output.ansi.enabled}. See {@link Enabled} for valid
 * values.
 *
 * @author Raphael von der Grün
 * @author Madhura Bhave
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/12/7 22:25
 */
public class AnsiOutputApplicationListener
        implements ApplicationListener<ApplicationEnvironmentPreparedEvent> {

  @Override
  public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
    ConfigurableEnvironment environment = event.getEnvironment();
    Binder.get(environment)
            .bind("infra.output.ansi.enabled", Enabled.class)
            .ifBound(AnsiOutput::setEnabled);
    AnsiOutput.setConsoleAvailable(
            environment.getProperty("infra.output.ansi.console-available", Boolean.class));
  }

}
