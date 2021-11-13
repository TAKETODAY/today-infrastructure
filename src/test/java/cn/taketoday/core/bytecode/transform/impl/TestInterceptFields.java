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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.core.bytecode.transform.impl;

import junit.framework.Test;
import junit.framework.TestSuite;

import cn.taketoday.core.bytecode.Type;
import cn.taketoday.core.bytecode.transform.AbstractTransformTest;
import cn.taketoday.core.bytecode.transform.ClassTransformer;
import cn.taketoday.core.bytecode.transform.ClassTransformerFactory;

/**
 * @author baliuka
 */
public class TestInterceptFields extends AbstractTransformTest implements InterceptFieldCallback {

  static Object TEST_VALUE = "test1";

  String field;

  /** Creates a new instance of TestInterceptFields */
  public TestInterceptFields() { }

  /** Creates a new instance of TestInterceptFields */
  public TestInterceptFields(String name) {
    super(name);
  }

  public void test() {

    InterceptFieldEnabled e = (InterceptFieldEnabled) this;
    e.setInterceptFieldCallback(this);
    field = "test";
    assertEquals(TEST_VALUE, field);

  }

  protected ClassTransformerFactory getTransformer() throws Exception {

    return new ClassTransformerFactory() {

      public ClassTransformer newTransformer() {

        return new InterceptFieldTransformer(

                new InterceptFieldFilter() {
                  public boolean acceptRead(Type owner, String name) {
                    return true;
                  }

                  public boolean acceptWrite(Type owner, String name) {
                    return true;
                  }

                }

        );

      }

    };

  }

  public boolean readBoolean(Object _this, String name, boolean oldValue) {

    return oldValue;
  }

  public byte readByte(Object _this, String name, byte oldValue) {

    return oldValue;
  }

  public char readChar(Object _this, String name, char oldValue) {

    return oldValue;
  }

  public double readDouble(Object _this, String name, double oldValue) {

    return oldValue;
  }

  public float readFloat(Object _this, String name, float oldValue) {

    return oldValue;
  }

  public int readInt(Object _this, String name, int oldValue) {

    return oldValue;
  }

  public long readLong(Object _this, String name, long oldValue) {

    return oldValue;
  }

  public Object readObject(Object _this, String name, Object oldValue) {

    return TEST_VALUE;
  }

  public short readShort(Object _this, String name, short oldValue) {

    return oldValue;
  }

  public boolean writeBoolean(Object _this, String name, boolean oldValue, boolean newValue) {

    return newValue;
  }

  public byte writeByte(Object _this, String name, byte oldValue, byte newValue) {

    return newValue;
  }

  public char writeChar(Object _this, String name, char oldValue, char newValue) {

    return newValue;
  }

  public double writeDouble(Object _this, String name, double oldValue, double newValue) {

    return newValue;
  }

  public float writeFloat(Object _this, String name, float oldValue, float newValue) {

    return newValue;
  }

  public int writeInt(Object _this, String name, int oldValue, int newValue) {

    return newValue;
  }

  public long writeLong(Object _this, String name, long oldValue, long newValue) {

    return newValue;
  }

  public Object writeObject(Object _this, String name, Object oldValue, Object newValue) {

    return newValue;
  }

  public short writeShort(Object _this, String name, short oldValue, short newValue) {

    return newValue;
  }

  public static void main(String[] args) throws Exception {
    junit.textui.TestRunner.run(suite());
  }

  public static Test suite() throws Exception {
    return new TestSuite(new TestInterceptFields().transform());
  }

}
