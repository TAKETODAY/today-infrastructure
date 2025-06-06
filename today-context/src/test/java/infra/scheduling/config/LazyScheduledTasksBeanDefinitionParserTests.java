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

package infra.scheduling.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import infra.context.support.GenericXmlApplicationContext;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/3/7 22:00
 */
public class LazyScheduledTasksBeanDefinitionParserTests {

  @Test
  @Timeout(5)
  void checkTarget() {
    var applicationContext =
            new GenericXmlApplicationContext(getClass(), "lazyScheduledTasksContext.xml");

    Task task = applicationContext.getBean(Task.class);

    while (!task.executed) {
      try {
        Thread.sleep(10);
      }
      catch (Exception ex) {
        /* Do Nothing */
      }
    }
  }

  static class Task {

    volatile boolean executed = false;

    public void doWork() {
      executed = true;
    }
  }

}
