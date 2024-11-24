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

package infra.jmx.export.naming;

import org.junit.jupiter.api.Test;

import javax.management.ObjectName;

import infra.jmx.export.naming.ObjectNamingStrategy;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Rob Harrop
 */
public abstract class AbstractNamingStrategyTests {

  @Test
  public void naming() throws Exception {
    ObjectNamingStrategy strat = getStrategy();
    ObjectName objectName = strat.getObjectName(getManagedResource(), getKey());
    assertThat(getCorrectObjectName()).isEqualTo(objectName.getCanonicalName());
  }

  protected abstract ObjectNamingStrategy getStrategy() throws Exception;

  protected abstract Object getManagedResource() throws Exception;

  protected abstract String getKey();

  protected abstract String getCorrectObjectName();

}
