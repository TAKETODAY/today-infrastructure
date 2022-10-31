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

package cn.taketoday.annotation.config.jdbc;

import cn.taketoday.annotation.config.jdbc.DataSourceProperties.DataSourceBeanCreationException;
import cn.taketoday.core.env.Environment;
import cn.taketoday.framework.diagnostics.AbstractFailureAnalyzer;
import cn.taketoday.framework.diagnostics.FailureAnalysis;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.StringUtils;

/**
 * An {@link AbstractFailureAnalyzer} for failures caused by a
 * {@link DataSourceBeanCreationException}.
 *
 * @author Andy Wilkinson
 * @author Patryk Kostrzewa
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/10/31 11:42
 */
class DataSourceBeanCreationFailureAnalyzer extends AbstractFailureAnalyzer<DataSourceBeanCreationException> {

  private final Environment environment;

  DataSourceBeanCreationFailureAnalyzer(Environment environment) {
    this.environment = environment;
  }

  @Override
  protected FailureAnalysis analyze(Throwable rootFailure, DataSourceBeanCreationException cause) {
    return getFailureAnalysis(cause);
  }

  private FailureAnalysis getFailureAnalysis(DataSourceBeanCreationException cause) {
    String description = getDescription(cause);
    String action = getAction(cause);
    return new FailureAnalysis(description, action, cause);
  }

  private String getDescription(DataSourceBeanCreationException cause) {
    StringBuilder description = new StringBuilder();
    description.append("Failed to configure a DataSource: ");
    if (!StringUtils.hasText(cause.properties.getUrl())) {
      description.append("'url' attribute is not specified and ");
    }
    description.append(String.format("no embedded datasource could be configured.%n"));
    description.append(String.format("%nReason: %s%n", cause.getMessage()));
    return description.toString();
  }

  private String getAction(DataSourceBeanCreationException cause) {
    StringBuilder action = new StringBuilder();
    action.append(String.format("Consider the following:%n"));
    if (EmbeddedDatabaseConnection.NONE == cause.connection) {
      action.append(String.format(
              "\tIf you want an embedded database (H2, HSQL or Derby), please put it on the classpath.%n"));
    }
    else {
      action.append(String.format("\tReview the configuration of %s%n.", cause.connection));
    }
    action.append("\tIf you have database settings to be loaded from a particular "
            + "profile you may need to activate it").append(getActiveProfiles());
    return action.toString();
  }

  private String getActiveProfiles() {
    StringBuilder message = new StringBuilder();
    String[] profiles = this.environment.getActiveProfiles();
    if (ObjectUtils.isEmpty(profiles)) {
      message.append(" (no profiles are currently active).");
    }
    else {
      message.append(" (the profiles ");
      message.append(StringUtils.arrayToCommaDelimitedString(profiles));
      message.append(" are currently active).");
    }
    return message.toString();
  }

}
