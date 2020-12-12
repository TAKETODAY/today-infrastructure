/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2020 All Rights Reserved.
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
package test.framework;

import org.junit.Test;

import java.io.IOException;
import java.util.Properties;

import cn.taketoday.framework.env.StandardWebEnvironment;

/**
 * @author TODAY <br>
 *         2019-06-18 09:16
 */
public class StandardWebServletEnvironmentTest {

  @Test
  public void testLoadProperties() throws IOException {

    StandardWebEnvironment environment = //
            new StandardWebEnvironment(StandardWebServletEnvironmentTest.class);

    environment.loadProperties();

    final Properties properties = environment.getProperties();

    assert "http://go.taketoday.cn".equals(properties.get("site.Access-Control-Allow-Origin"));

    properties.list(System.err);
  }

}
