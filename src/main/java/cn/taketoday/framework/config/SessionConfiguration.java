/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright ©  TODAY & 2017 - 2021 All Rights Reserved.
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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cn.taketoday.framework.config;

import java.io.File;
import java.io.IOException;
import java.time.Duration;

import cn.taketoday.context.annotation.Autowired;
import cn.taketoday.context.annotation.Props;
import cn.taketoday.context.exception.ConfigurationException;
import cn.taketoday.context.io.Resource;
import cn.taketoday.context.logger.LoggerFactory;
import cn.taketoday.framework.utils.ApplicationUtils;
import lombok.Getter;
import lombok.Setter;

/**
 * Session Configuration.
 *
 * @author TODAY(taketoday(AT)foxmail.com) https://taketoday.cn <br>
 * 2019-01-26 17:11
 */
@Setter
@Getter
@Props(prefix = "server.session.")
public class SessionConfiguration {

  private boolean enable = true;
  private boolean persistent = true;

  /** Directory used to store session data. */
  private Resource storeDirectory;
  private TrackingMode[] trackingModes;
  private Duration timeout = Duration.ofMinutes(30);

  @Autowired
  private SessionCookieConfiguration cookieConfiguration;

  public File getStoreDirectory(Class<?> startupClass) throws IOException {

    if (this.storeDirectory == null || !this.storeDirectory.exists()) {
      return ApplicationUtils.getTemporalDirectory(startupClass, "web-app-sessions");
    }

    if (storeDirectory.isDirectory()) {

      LoggerFactory.getLogger(getClass())
              .info("Use directory: [{}] to store sessions", storeDirectory);
      return storeDirectory.getFile();
    }

    throw new ConfigurationException("Store directory must be a 'directory'");
  }

  public enum TrackingMode {
    COOKIE,
    URL,
    SSL
  }
}
