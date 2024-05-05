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

package cn.taketoday.mock.api.http;

/**
 * This interface encapsulates the upgrade protocol processing. A HttpUpgradeHandler implementation would allow the
 * servlet container to communicate with it.
 *
 * @since Servlet 3.1
 */
public interface HttpUpgradeHandler {
  /**
   * It is called once the HTTP Upgrade process has been completed and the upgraded connection is ready to start using the
   * new protocol.
   *
   * @param wc the WebConnection object associated to this upgrade request
   */
  public void init(WebConnection wc);

  /**
   * It is called when the client is disconnected.
   */
  public void destroy();
}
