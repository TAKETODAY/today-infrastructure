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

package infra.jmx;

import java.io.IOException;

/**
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @@infra.jmx.export.metadata.ManagedResource (description = " My Managed Bean ", objectName = " spring : bean = test ",
 *log = true, logFile = " build / jmx.log ", currencyTimeLimit = 15, persistPolicy = " OnUpdate ",
 *persistPeriod = 200, persistLocation = " . / foo ", persistName = " bar.jmx ")
 * @@infra.jmx.export.metadata.ManagedNotification (name = " My Notification ", description = " A Notification ", notificationType = " type.foo, type.bar ")
 */
public class JmxTestBean implements IJmxTestBean {

  private String name;

  private String nickName;

  private int age;

  private boolean isSuperman;

  /**
   * @@infra.jmx.export.metadata.ManagedAttribute (description = " The Age Attribute ", currencyTimeLimit = 15)
   */
  @Override
  public int getAge() {
    return age;
  }

  @Override
  public void setAge(int age) {
    this.age = age;
  }

  /**
   * @@infra.jmx.export.metadata.ManagedOperation(currencyTimeLimit=30)
   */
  @Override
  public long myOperation() {
    return 1L;
  }

  /**
   * @@infra.jmx.export.metadata.ManagedAttribute (description = " The Name Attribute ", currencyTimeLimit = 20,
   *defaultValue = " bar ", persistPolicy = " OnUpdate ")
   */
  @Override
  public void setName(String name) throws Exception {
    if ("Juergen".equals(name)) {
      throw new IllegalArgumentException("Juergen");
    }
    if ("Juergen Class".equals(name)) {
      throw new ClassNotFoundException("Juergen");
    }
    if ("Juergen IO".equals(name)) {
      throw new IOException("Juergen");
    }
    this.name = name;
  }

  /**
   * @@infra.jmx.export.metadata.ManagedAttribute (defaultValue = " foo ", persistPeriod = 300)
   */
  @Override
  public String getName() {
    return name;
  }

  /**
   * @@infra.jmx.export.metadata.ManagedAttribute(description="The Nick
   * Name
   * Attribute")
   */
  public void setNickName(String nickName) {
    this.nickName = nickName;
  }

  public String getNickName() {
    return this.nickName;
  }

  public void setSuperman(boolean superman) {
    this.isSuperman = superman;
  }

  /**
   * @@infra.jmx.export.metadata.ManagedAttribute(description="The Is
   * Superman
   * Attribute")
   */
  public boolean isSuperman() {
    return isSuperman;
  }

  /**
   * @@infra.jmx.export.metadata.ManagedOperation(description="Add Two
   * Numbers
   * Together")
   * @@infra.jmx.export.metadata.ManagedOperationParameter(index=0, name="x", description="Left operand")
   * @@infra.jmx.export.metadata.ManagedOperationParameter(index=1, name="y", description="Right operand")
   */
  @Override
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

  protected void someProtectedMethod() {
  }

  @SuppressWarnings("unused")
  private void somePrivateMethod() {
  }

  protected void getSomething() {
  }

  @SuppressWarnings("unused")
  private void getSomethingElse() {
  }

}
