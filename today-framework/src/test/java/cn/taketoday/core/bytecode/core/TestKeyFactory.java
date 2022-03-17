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
package cn.taketoday.core.bytecode.core;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Chris Nokleberg
 * <a href="mailto:chris@nokleberg.com">chris@nokleberg.com</a>
 * @version $Id: TestKeyFactory.java,v 1.7 2012/07/27 16:02:50 baliuka Exp $
 */
public class TestKeyFactory {
  public interface MyKey {
    public Object newInstance(int a, int[] b, boolean flag);
  }

  public interface MyKey2 {
    public Object newInstance(int[][] a);
  }

  public interface CharArrayKey {
    public Object newInstance(char[] a);
  }

  public interface BooleanArrayKey {
    public Object newInstance(boolean[] a);
  }

  public interface ClassArrayKey {
    public Object newInstance(Class[] a);
  }

  public interface MethodKey {
    public Object newInstance(Class returnType, Class[] parameterTypes);
  }

  public interface PrimitivesKey {
    public Object newInstance(boolean b, double d, float f, int i, long l);
  }

  public interface IntegerKey {
    public Object newInstance(int i);
  }

  public interface LongKey {
    public Object newInstance(long l);
  }

  public interface FloatKey {
    public Object newInstance(float f);
  }

  @Test
  public void testSimple() throws Exception {
    MyKey mykey = KeyFactory.create(MyKey.class);
    assertEquals(
            mykey.newInstance(5, new int[] { 6, 7 }, false).hashCode(),
            mykey.newInstance(5, new int[] { 6, 7 }, false).hashCode());
  }

  private Object helper(Class type) {
    KeyFactory.Generator gen = new KeyFactory.Generator();
    gen.setInterface(type);
    gen.setHashConstant(5);
    gen.setHashMultiplier(3);
    return gen.create();
  }

  @Test
  public void testPrimitives() throws Exception {
    PrimitivesKey factory = (PrimitivesKey) helper(PrimitivesKey.class);
    Object instance = factory.newInstance(true, 1.234d, 5.678f, 100, 200L);
    assertEquals(1525582882, instance.hashCode());
  }

  @Test
  public void testInteger() throws Exception {
    IntegerKey factory = (IntegerKey) helper(IntegerKey.class);
    Object instance = factory.newInstance(7);
    assertEquals(22, instance.hashCode());
  }

  @Test

  public void testLong() throws Exception {
    LongKey factory = (LongKey) helper(LongKey.class);
    Object instance = factory.newInstance(7L);
    assertEquals(22, instance.hashCode());
  }

  @Test

  public void testFloat() throws Exception {
    FloatKey factory = (FloatKey) helper(FloatKey.class);
    Object instance = factory.newInstance(7f);
    assertEquals(1088421903, instance.hashCode());
  }

  @Test
  public void testNested() throws Exception {
    KeyFactory.Generator gen = new KeyFactory.Generator();
    gen.setInterface(MyKey2.class);
    gen.setHashConstant(17);
    gen.setHashMultiplier(37);
    MyKey2 mykey2 = (MyKey2) gen.create();
    Object instance = mykey2.newInstance(new int[][] { { 1, 2 }, { 3, 4 } });
    assertEquals(31914243, instance.hashCode());
  }

  @Test
  public void testCharArray() throws Exception {
    CharArrayKey f = (CharArrayKey) KeyFactory.create(CharArrayKey.class);
    Object key1 = f.newInstance(new char[] { 'a', 'b' });
    Object key2 = f.newInstance(new char[] { 'a', '_' });
    assertFalse(key1.equals(key2));
  }

  @Test
  public void testBooleanArray() throws Exception {
    BooleanArrayKey f = (BooleanArrayKey) KeyFactory.create(BooleanArrayKey.class);
    Object key1 = f.newInstance(new boolean[] { true, false, true });
    Object key2 = f.newInstance(new boolean[] { true, false, true });
    assertEquals(key1, key2);
  }

  @Test
  public void testMethodKey() throws Exception {
    MethodKey factory = KeyFactory.create(MethodKey.class);
    Set methodSet = new HashSet();
    methodSet.add(factory.newInstance(Number.class, new Class[] { int.class }));
    assertTrue(methodSet.contains(factory.newInstance(Number.class, new Class[] { int.class })));
    assertFalse(methodSet.contains(factory.newInstance(Number.class, new Class[] { Integer.class })));
  }

  @Test
  public void testEqualOtherClass() throws Exception {
    MyKey mykey = (MyKey) KeyFactory.create(MyKey.class);
    assertNotEquals(mykey.newInstance(5, new int[] { 6, 7 }, false), new Object());
  }

  public void perform(ClassLoader loader) throws Throwable {
    KeyFactory.create(loader, MyKey.class, null);
  }

}
