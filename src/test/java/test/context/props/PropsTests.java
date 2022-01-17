/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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
package test.context.props;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import cn.taketoday.context.support.ApplicationPropertySourcesProcessor;
import cn.taketoday.context.support.StandardApplicationContext;

/**
 * @author Today <br>
 *
 * 2018-10-09 15:06
 */
class PropsTests {

  @Test
  void props() throws IOException {
    try (StandardApplicationContext applicationContext = new StandardApplicationContext()) {
      ApplicationPropertySourcesProcessor processor = new ApplicationPropertySourcesProcessor(applicationContext);
      processor.setPropertiesLocation("info.properties");
      processor.postProcessEnvironment();
      applicationContext.scan("test.context.props");

      applicationContext.refresh();

      Config_ bean = applicationContext.getBean(Config_.class);
      assert "https://taketoday.cn".equals(bean.getHost());
    }
  }

  @Test
  void propsOnConstructor() {

    try (StandardApplicationContext applicationContext = new StandardApplicationContext()) {

      applicationContext.registerBean("testBean", PropsBean.class);
      applicationContext.refresh();

      PropsBean bean = applicationContext.getBean(PropsBean.class);

      assert bean != null : "@Props function error";

      System.err.println(bean);
    }
  }

}
