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

package cn.taketoday.framework.web.server;

import cn.taketoday.lang.Nullable;

/**
 * Simple server-independent abstraction for HTTP/2 configuration.
 *
 * @author Brian Clozel
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class Http2 {

  private boolean enabled = false;

  /**
   * Return whether to enable HTTP/2 support, if the current environment supports it.
   *
   * @return {@code true} to enable HTTP/2 support
   */
  public boolean isEnabled() {
    return this.enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  /**
   * Returns if Http2 is enabled for the given instance.
   *
   * @param http2 the {@link Http2} instance or {@code null}
   * @return {@code true} is Http2 is enabled
   */
  public static boolean isEnabled(@Nullable Http2 http2) {
    return http2 != null && http2.enabled;
  }

}
