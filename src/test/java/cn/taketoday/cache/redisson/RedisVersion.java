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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Rui Gu (https://github.com/jackygurui)
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/3/11 11:25
 */
public class RedisVersion implements Comparable<RedisVersion> {

  private final String fullVersion;
  private final Integer majorVersion;
  private final Integer minorVersion;
  private final Integer patchVersion;

  public RedisVersion(String fullVersion) {
    this.fullVersion = fullVersion;
    Matcher matcher = Pattern.compile("^([\\d]+)\\.([\\d]+)\\.([\\d]+)").matcher(fullVersion);
    matcher.find();
    majorVersion = Integer.parseInt(matcher.group(1));
    minorVersion = Integer.parseInt(matcher.group(2));
    patchVersion = Integer.parseInt(matcher.group(3));
  }

  public String getFullVersion() {
    return fullVersion;
  }

  public int getMajorVersion() {
    return majorVersion;
  }

  public int getMinorVersion() {
    return minorVersion;
  }

  public int getPatchVersion() {
    return patchVersion;
  }

  @Override
  public int compareTo(RedisVersion o) {
    int ma = this.majorVersion.compareTo(o.majorVersion);
    int mi = this.minorVersion.compareTo(o.minorVersion);
    int pa = this.patchVersion.compareTo(o.patchVersion);
    return ma != 0 ? ma : mi != 0 ? mi : pa;
  }

  public int compareTo(String redisVersion) {
    return this.compareTo(new RedisVersion(redisVersion));
  }

  public static int compareTo(String redisVersion1, String redisVersion2) {
    return new RedisVersion(redisVersion1).compareTo(redisVersion2);
  }

}