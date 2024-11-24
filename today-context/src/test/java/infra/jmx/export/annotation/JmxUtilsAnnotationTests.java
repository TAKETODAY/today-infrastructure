/*
 * Copyright 2017 - 2024 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package infra.jmx.export.annotation;

import org.junit.jupiter.api.Test;

import javax.management.MXBean;

import infra.jmx.support.JmxUtils;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

/**
 * @author Juergen Hoeller
 */
public class JmxUtilsAnnotationTests {

  @Test
  public void notMXBean() throws Exception {
    assertThat(JmxUtils.isMBean(FooNotX.class)).as("MXBean annotation not detected correctly").isFalse();
  }

  @Test
  public void annotatedMXBean() throws Exception {
    assertThat(JmxUtils.isMBean(FooX.class)).as("MXBean annotation not detected correctly").isTrue();
  }

  @MXBean(false)
  public interface FooNotMXBean {
    String getName();
  }

  public static class FooNotX implements FooNotMXBean {

    @Override
    public String getName() {
      return "Rob Harrop";
    }
  }

  @MXBean(true)
  public interface FooIfc {
    String getName();
  }

  public static class FooX implements FooIfc {

    @Override
    public String getName() {
      return "Rob Harrop";
    }
  }

}
