/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.app.context.config;

import org.jspecify.annotations.Nullable;

import infra.app.diagnostics.AbstractFailureAnalyzer;
import infra.app.diagnostics.FailureAnalysis;
import infra.origin.Origin;

/**
 * An implementation of {@link AbstractFailureAnalyzer} to analyze failures caused by
 * {@link ConfigDataNotFoundException}.
 *
 * @author Michal Mlak
 * @author Phillip Webb
 */
class ConfigDataNotFoundFailureAnalyzer extends AbstractFailureAnalyzer<ConfigDataNotFoundException> {

  @Override
  protected FailureAnalysis analyze(Throwable rootFailure, ConfigDataNotFoundException cause) {
    ConfigDataLocation location = getLocation(cause);
    Origin origin = Origin.from(location);
    String message = String.format("Config data %s does not exist", cause.getReferenceDescription());
    StringBuilder action = new StringBuilder("Check that the value ");
    if (location != null) {
      action.append(String.format("'%s' ", location));
    }
    if (origin != null) {
      action.append(String.format("at %s ", origin));
    }
    action.append("is correct");
    if (location != null && !location.isOptional()) {
      action.append(String.format(", or prefix it with '%s'", ConfigDataLocation.OPTIONAL_PREFIX));
    }
    return new FailureAnalysis(message, action.toString(), cause);
  }

  @Nullable
  private ConfigDataLocation getLocation(ConfigDataNotFoundException cause) {
    if (cause instanceof ConfigDataLocationNotFoundException) {
      return ((ConfigDataLocationNotFoundException) cause).getLocation();
    }
    if (cause instanceof ConfigDataResourceNotFoundException) {
      return ((ConfigDataResourceNotFoundException) cause).getLocation();
    }
    return null;
  }

}
