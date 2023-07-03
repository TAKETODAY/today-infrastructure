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

package cn.taketoday.context.properties.scan.valid.b;

import cn.taketoday.context.properties.ConfigurationProperties;

/**
 * @author Madhura Bhave
 * @author Stephane Nicoll
 */
public class BScanConfiguration {

  public interface BProperties {

  }

  @ConfigurationProperties(prefix = "b.first")
  public static class BFirstProperties implements BProperties {

    private final String name;

    public BFirstProperties(String name) {
      this.name = name;
    }

    public String getName() {
      return this.name;
    }

  }

  @ConfigurationProperties(prefix = "b.second")
  public static class BSecondProperties implements BProperties {

    private int number;

    public int getNumber() {
      return this.number;
    }

    public void setNumber(int number) {
      this.number = number;
    }

  }

}
