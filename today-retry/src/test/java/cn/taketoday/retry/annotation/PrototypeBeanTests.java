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

package cn.taketoday.retry.annotation;

import org.junit.Test;
import org.junit.runner.RunWith;

import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.stereotype.Prototype;
import cn.taketoday.test.context.junit4.Runner;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * @author Gary Russell
 */
@RunWith(Runner.class)
public class PrototypeBeanTests {

  @Autowired
  private Bar bar1;

  @Autowired
  private Bar bar2;

  @Autowired
  private Foo foo;

  @Test
  public void testProtoBean() {
    this.bar1.foo("one");
    this.bar2.foo("two");
    assertThat(this.foo.recovered, equalTo("two"));
  }

  @Configuration
  @EnableRetry
  public static class Config {

    @Bean
    public Foo foo() {
      return new Foo();
    }

    @Bean
    @Prototype
    public Baz baz() {
      return new Baz();
    }

  }

  public static class Foo {

    private String recovered;

    void demoRun(Bar bar) {
      throw new RuntimeException();
    }

    void demoRecover(String instance) {
      this.recovered = instance;
    }

  }

  public interface Bar {

    @Retryable(backoff = @Backoff(0))
    void foo(String instance);

    @Recover
    void bar();

  }

  public static class Baz implements Bar {

    private String instance;

    @Autowired
    private Foo foo;

    @Override
    public void foo(String instance) {
      this.instance = instance;
      foo.demoRun(this);
    }

    @Override
    public void bar() {
      foo.demoRecover(this.instance);
    }

    @Override
    public String toString() {
      return "Baz [instance=" + this.instance + "]";
    }

  }

}
