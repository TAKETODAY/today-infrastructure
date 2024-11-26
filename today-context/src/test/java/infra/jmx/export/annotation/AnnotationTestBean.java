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

import infra.jmx.IJmxTestBean;
import infra.jmx.support.MetricType;
import infra.stereotype.Service;

/**
 * @author Rob Harrop
 * @author Juergen Hoeller
 */
@Service("testBean")
@ManagedResource(objectName = "bean:name=testBean4", description = "My Managed Bean", log = true,
                 logFile = "build/jmx.log", currencyTimeLimit = 15, persistPolicy = "OnUpdate", persistPeriod = 200,
                 persistLocation = "./foo", persistName = "bar.jmx")
@ManagedNotification(name = "My Notification", notificationTypes = { "type.foo", "type.bar" })
public class AnnotationTestBean implements IJmxTestBean {

  private String name;

  private String nickName;

  private int age;

  private boolean isSuperman;

  @Override
  @ManagedAttribute(description = "The Age Attribute", currencyTimeLimit = 15)
  public int getAge() {
    return age;
  }

  @Override
  public void setAge(int age) {
    this.age = age;
  }

  @Override
  @ManagedOperation(currencyTimeLimit = 30)
  public long myOperation() {
    return 1L;
  }

  @Override
  @ManagedAttribute(description = "The Name Attribute",
                    currencyTimeLimit = 20,
                    defaultValue = "bar",
                    persistPolicy = "OnUpdate")
  public void setName(String name) {
    this.name = name;
  }

  @Override
  @ManagedAttribute(defaultValue = "foo", persistPeriod = 300)
  public String getName() {
    return name;
  }

  @ManagedAttribute(description = "The Nick Name Attribute")
  public void setNickName(String nickName) {
    this.nickName = nickName;
  }

  public String getNickName() {
    return this.nickName;
  }

  public void setSuperman(boolean superman) {
    this.isSuperman = superman;
  }

  @ManagedAttribute(description = "The Is Superman Attribute")
  public boolean isSuperman() {
    return isSuperman;
  }

  @Override
  @ManagedOperation(description = "Add Two Numbers Together")
  @ManagedOperationParameter(name = "x", description = "Left operand")
  @ManagedOperationParameter(name = "y", description = "Right operand")
  public int add(int x, int y) {
    return x + y;
  }

  /**
   * Test method that is not exposed by the MetadataAssembler.
   */
  @Override
  public void dontExposeMe() {
    throw new RuntimeException();
  }

  @ManagedMetric(description = "The QueueSize metric", currencyTimeLimit = 20, persistPolicy = "OnUpdate", persistPeriod = 300,
                 category = "utilization", metricType = MetricType.COUNTER, displayName = "Queue Size", unit = "messages")
  public long getQueueSize() {
    return 100L;
  }

  @ManagedMetric
  public int getCacheEntries() {
    return 3;
  }

}
