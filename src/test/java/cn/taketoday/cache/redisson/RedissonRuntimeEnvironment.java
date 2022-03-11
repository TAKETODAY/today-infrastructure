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

package cn.taketoday.cache.redisson;

import java.util.Locale;

/**
 * @author Rui Gu (https://github.com/jackygurui)
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/3/11 11:24
 */
public class RedissonRuntimeEnvironment {

  public static final boolean isTravis = "true".equalsIgnoreCase(System.getProperty("travisEnv"));
  public static final String redisBinaryPath = System.getProperty("redisBinary", "C:\\redis\\redis-server.exe");
  public static final String tempDir = System.getProperty("java.io.tmpdir");
  public static final String OS;
  public static final boolean isWindows;

  static {
    OS = System.getProperty("os.name", "generic");
    isWindows = OS.toLowerCase(Locale.ENGLISH).contains("win");
  }
}