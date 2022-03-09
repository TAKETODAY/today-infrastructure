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

import java.util.Arrays;

import cn.taketoday.core.bytecode.Type;
import cn.taketoday.core.bytecode.core.CodeGenerationException;
import cn.taketoday.core.bytecode.transform.ClassFilter;
import cn.taketoday.core.bytecode.transform.ClassTransformer;
import cn.taketoday.core.bytecode.transform.ClassTransformerChain;
import cn.taketoday.core.bytecode.transform.ClassTransformerFactory;
import cn.taketoday.core.bytecode.transform.TransformingClassLoader;

/**
 * @author baliuka
 */
public class TransformDemo {
  public static void println(String x) {
//    System.out.println(x);
  }

  public static void register(Class cls) {
    println("register " + cls);
  }

  public static void start() {

    MA ma = new MA();
    makePersistent(ma);
    ma.setCharP('A');
    ma.getCharP();
    ma.setDoubleP(554);
    ma.setDoubleP(1.2);
    ma.getFloatP();
    ma.setName("testName");
    ma.publicField = "set value";
    ma.publicField = ma.publicField + " append value";
    ma.setBaseTest("base test field");
    ma.getBaseTest();

  }

  public static void makePersistent(Object obj) {
    println("makePersistent " + obj.getClass() + " " + Arrays.asList(obj.getClass().getInterfaces()));
    InterceptFieldEnabled t = (InterceptFieldEnabled) obj;
    t.setInterceptFieldCallback(new StateManager());
    PersistenceCapable pc = (PersistenceCapable) obj;
    pc.setPersistenceManager("Manager");

  }

  public static void main(String args[]) throws Exception {

    ClassTransformerFactory transformation =

            new ClassTransformerFactory() {

              public ClassTransformer newTransformer() {
                try {
                  InterceptFieldTransformer t1 = new InterceptFieldTransformer(new Filter());

                  AddStaticInitTransformer t2 = new AddStaticInitTransformer(
                          TransformDemo.class.getMethod("register", Class.class));

                  AddDelegateTransformer t3 = new AddDelegateTransformer(
                          new Class[] { PersistenceCapable.class }, PersistenceCapableImpl.class);

                  return new ClassTransformerChain(new ClassTransformer[] { t1, t2, t3 });
                }
                catch (Exception e) {
                  throw new CodeGenerationException(e);
                }
              }
            };

    TransformingClassLoader loader = new TransformingClassLoader(
            TransformDemo.class.getClassLoader(), new ClassFilter() {
      public boolean accept(String name) {
        println("load : " + name);
        boolean f = Base.class.getName().equals(name)
                || MA.class.getName().equals(name)
                || TransformDemo.class.getName().equals(name);
        if (f) {
          println("transforming " + name);
        }
        return f;
      }
    }, transformation);

    loader.loadClass(TransformDemo.class.getName())
            .getMethod("start", new Class[] {}).invoke(null, (Object[]) null);

  }

  public static class Filter implements InterceptFieldFilter {

    public boolean acceptRead(Type owner, String name) {
      return true;
    }

    public boolean acceptWrite(Type owner, String name) {
      return true;
    }

  }

  public static class StateManager implements InterceptFieldCallback {

    public boolean readBoolean(Object _this, String name, boolean oldValue) {
      println("read " + name + " = " + oldValue);
      return oldValue;
    }

    public byte readByte(Object _this, String name, byte oldValue) {
      println("read " + name + " = " + oldValue);
      return oldValue;
    }

    public char readChar(Object _this, String name, char oldValue) {
      println("read " + name + " = " + oldValue);
      return oldValue;
    }

    public double readDouble(Object _this, String name, double oldValue) {
      println("read " + name + " = " + oldValue);
      return oldValue;
    }

    public float readFloat(Object _this, String name, float oldValue) {
      println("read " + name + " = " + oldValue);
      return oldValue;
    }

    public int readInt(Object _this, String name, int oldValue) {
      println("read " + name + " = " + oldValue);
      return oldValue;
    }

    public long readLong(Object _this, String name, long oldValue) {
      println("read " + name + " = " + oldValue);
      return oldValue;
    }

    public Object readObject(Object _this, String name, Object oldValue) {
      println("read " + name + " = " + oldValue);
      return oldValue;
    }

    public short readShort(Object _this, String name, short oldValue) {
      println("read " + name + " = " + oldValue);
      return oldValue;
    }

    public boolean writeBoolean(Object _this, String name, boolean oldValue, boolean newValue) {
      println("write " + name + " = " + newValue);
      return newValue;
    }

    public byte writeByte(Object _this, String name, byte oldValue, byte newValue) {
      println("write " + name + " = " + newValue);
      return newValue;
    }

    public char writeChar(Object _this, String name, char oldValue, char newValue) {
      println("write " + name + " = " + newValue);
      return newValue;
    }

    public double writeDouble(Object _this, String name, double oldValue, double newValue) {
      println("write " + name + " = " + newValue);
      return newValue;
    }

    public float writeFloat(Object _this, String name, float oldValue, float newValue) {
      println("write " + name + " = " + newValue);
      return newValue;
    }

    public int writeInt(Object _this, String name, int oldValue, int newValue) {
      println("write " + name + " = " + newValue);
      return newValue;
    }

    public long writeLong(Object _this, String name, long oldValue, long newValue) {
      println("write " + name + " = " + newValue);
      return newValue;
    }

    public Object writeObject(Object _this, String name, Object oldValue, Object newValue) {
      println("write " + name + " = " + newValue);
      return newValue;
    }

    public short writeShort(Object _this, String name, short oldValue, short newValue) {
      println("write " + name + " = " + newValue);
      return newValue;
    }

  }

}
