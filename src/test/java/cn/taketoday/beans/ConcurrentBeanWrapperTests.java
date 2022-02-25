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

package cn.taketoday.beans;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Guillaume Poirier
 * @author Juergen Hoeller
 * @author Chris Beams
 * @since 08.03.2004
 */
class ConcurrentBeanWrapperTests {

  private final Logger logger = LoggerFactory.getLogger(getClass());

  private final Set<TestRun> set = ConcurrentHashMap.newKeySet();

  private Throwable ex = null;

  @RepeatedTest(100)
  void testSingleThread() {
    performSet();
  }

  @Test
  void testConcurrent() {
    for (int i = 0; i < 10; i++) {
      TestRun run = new TestRun(this);
      set.add(run);
      Thread t = new Thread(run);
      t.setDaemon(true);
      t.start();
    }
    logger.info("Thread creation over, " + set.size() + " still active.");
    synchronized(this) {
      while (!set.isEmpty() && ex == null) {
        try {
          wait();
        }
        catch (InterruptedException e) {
          logger.info(e.toString());
        }
        logger.info(set.size() + " threads still active.");
      }
    }
    if (ex != null) {
      throw new AssertionError("Unexpected exception", ex);
    }
  }

  private static void performSet() {
    TestBean bean = new TestBean();

    Properties p = (Properties) System.getProperties().clone();

    assertThat(p).as("The System properties must not be empty").isNotEmpty();

    for (Iterator<?> i = p.entrySet().iterator(); i.hasNext(); ) {
      i.next();
      if (Math.random() > 0.9) {
        i.remove();
      }
    }

    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    try {
      p.store(buffer, null);
    }
    catch (IOException e) {
      // ByteArrayOutputStream does not throw
      // any IOException
    }
    String value = buffer.toString();

    BeanWrapperImpl wrapper = new BeanWrapperImpl(bean);
    wrapper.setPropertyValue("properties", value);
    assertThat(bean.getProperties()).isEqualTo(p);
  }

  private static class TestRun implements Runnable {

    private final ConcurrentBeanWrapperTests test;

    TestRun(ConcurrentBeanWrapperTests test) {
      this.test = test;
    }

    @Override
    public void run() {
      try {
        for (int i = 0; i < 100; i++) {
          performSet();
        }
      }
      catch (Throwable e) {
        test.ex = e;
      }
      finally {
        synchronized(test) {
          test.set.remove(this);
          test.notifyAll();
        }
      }
    }
  }

  @SuppressWarnings("unused")
  private static class TestBean {

    private Properties properties;

    public Properties getProperties() {
      return properties;
    }

    public void setProperties(Properties properties) {
      this.properties = properties;
    }
  }

}
