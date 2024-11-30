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

package infra.app.context;

import infra.app.ApplicationStartupListener;
import infra.app.ConfigurableBootstrapContext;
import infra.context.properties.bind.Binder;
import infra.core.ansi.AnsiOutput;
import infra.core.ansi.AnsiOutput.Enabled;
import infra.core.env.ConfigurableEnvironment;

/**
 * An {@link ApplicationStartupListener} that configures {@link AnsiOutput} depending on the
 * value of the property {@code infra.output.ansi.enabled}. See {@link Enabled} for valid
 * values.
 *
 * @author Raphael von der Grün
 * @author Madhura Bhave
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/12/7 22:25
 */
public class AnsiOutputStartupListener implements ApplicationStartupListener {

  @Override
  public void environmentPrepared(ConfigurableBootstrapContext bootstrapContext, ConfigurableEnvironment environment) {
    Binder.get(environment)
            .bind("infra.output.ansi.enabled", Enabled.class)
            .ifBound(AnsiOutput::setEnabled);
    AnsiOutput.setConsoleAvailable(
            environment.getProperty("infra.output.ansi.console-available", Boolean.class));
  }

}
