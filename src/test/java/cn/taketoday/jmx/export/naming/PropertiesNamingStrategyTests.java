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

package cn.taketoday.jmx.export.naming;

import java.util.Properties;

/**
 * @author Rob Harrop
 * @author Juergen Hoeller
 */
public class PropertiesNamingStrategyTests extends AbstractNamingStrategyTests {

  private static final String OBJECT_NAME = "bean:name=namingTest";

  @Override
  protected ObjectNamingStrategy getStrategy() throws Exception {
    KeyNamingStrategy strat = new KeyNamingStrategy();
    Properties mappings = new Properties();
    mappings.setProperty("namingTest", "bean:name=namingTest");
    strat.setMappings(mappings);
    strat.afterPropertiesSet();
    return strat;
  }

  @Override
  protected Object getManagedResource() {
    return new Object();
  }

  @Override
  protected String getKey() {
    return "namingTest";
  }

  @Override
  protected String getCorrectObjectName() {
    return OBJECT_NAME;
  }

}
