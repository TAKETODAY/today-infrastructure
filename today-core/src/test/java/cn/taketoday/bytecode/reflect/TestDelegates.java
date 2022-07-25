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
package cn.taketoday.bytecode.reflect;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * @version $Id: TestDelegates.java,v 1.4 2004/06/24 21:15:16 herbyderby Exp $
 */
@DisabledIfSystemProperty(
        named = "coverage", matches = "true", disabledReason = "jacoco")
public class TestDelegates {

  public interface StringMaker {
    Object newInstance(char[] buf, int offset, int count);
  }

  @Test
  public void testConstructor() throws Throwable {
    StringMaker maker = ConstructorDelegate.create(String.class, StringMaker.class);
    assertEquals("nil", maker.newInstance("vanilla".toCharArray(), 2, 3));
  }

  public interface Substring {
    String substring(int start, int end);
  }

  public interface Substring2 {
    Object anyNameAllowed(int start, int end);
  }

  public interface IndexOf {
    int indexOf(String str, int fromIndex);

    default int indexOf(String str) {
      return indexOf(str, 0);
    }
  }

  public interface Format {
    String format(String format, Object... args);
  }

  @Test
  public void testFancy() throws Throwable {
    Substring delegate = MethodDelegate.create("CGLIB", "substring", Substring.class);
    assertEquals("LI", delegate.substring(2, 4));
  }

  @Test
  public void testFancyNames() throws Throwable {
    Substring2 delegate = MethodDelegate.create("CGLIB", "substring", Substring2.class);
    assertEquals("LI", delegate.anyNameAllowed(2, 4));
  }

  @Test
  void testFancyTypes() throws Throwable {
    String test = "abcabcabc";
    IndexOf delegate = MethodDelegate.create(test, "indexOf", IndexOf.class);
    assertEquals(delegate.indexOf("ab", 1), test.indexOf("ab", 1));
    assertEquals(delegate.indexOf("ab"), test.indexOf("ab"));
  }

  @Test
  public void testVarArgs() throws Throwable {
    String formatStr = "Time: %d";
    long time = System.currentTimeMillis();
    Format delegate = MethodDelegate.createStatic(String.class, "format", Format.class);
    assertEquals(delegate.format(formatStr, time), String.format(formatStr, time));
  }

  @SuppressWarnings("unlikely-arg-type")
  @Test
  public void testEquals() throws Throwable {
    String test = "abc";
    IndexOf mc1 = MethodDelegate.create(test, "indexOf", IndexOf.class);
    IndexOf mc2 = MethodDelegate.create(test, "indexOf", IndexOf.class);
    IndexOf mc3 = MethodDelegate.create("other", "indexOf", IndexOf.class);
    Substring mc4 = MethodDelegate.create(test, "substring", Substring.class);
    Substring2 mc5 = MethodDelegate.create(test, "substring", Substring2.class);
    assertEquals(mc1, mc2);
    assertNotEquals(mc1, mc3);
    assertNotEquals(mc1, mc4);
    assertEquals(mc4, mc5);
  }

  public static interface MainDelegate {
    int main(String[] args);
  }

  public static class MainTest {
    public static int alternateMain(String[] args) {
      return 7;
    }
  }

  @Test
  public void testStaticDelegate() throws Throwable {
    MainDelegate start = MethodDelegate.createStatic(
            MainTest.class, "alternateMain", MainDelegate.class);
    assertEquals(7, start.main(null));
  }

  public static interface Listener {
    public void onEvent();
  }

  public static class Publisher {
    public int test = 0;
    private MulticastDelegate event = MulticastDelegate.create(Listener.class);

    public void addListener(Listener listener) {
      event = event.add(listener);
    }

    public void removeListener(Listener listener) {
      event = event.remove(listener);
    }

    public void fireEvent() {
      ((Listener) event).onEvent();
    }
  }

  @Test
  public void testPublisher() throws Throwable {
    final Publisher p = new Publisher();
    Listener l1 = new Listener() {
      public void onEvent() {
        p.test++;
      }
    };
    p.addListener(l1);
    p.addListener(l1);
    p.fireEvent();
    assertEquals(2, p.test);
    p.removeListener(l1);
    p.fireEvent();
    assertEquals(3, p.test);
  }

  public static interface SuperSimple {
    public int execute();
  }

  @Test
  public void testMulticastReturnValue() {
    SuperSimple ss1 = new SuperSimple() {
      public int execute() {
        return 1;
      }
    };
    SuperSimple ss2 = new SuperSimple() {
      public int execute() {
        return 2;
      }
    };
    MulticastDelegate multi = MulticastDelegate.create(SuperSimple.class);
    multi = multi.add(ss1);
    multi = multi.add(ss2);
    assertEquals(2, ((SuperSimple) multi).execute());
    multi = multi.remove(ss1);
    multi = multi.add(ss1);
    assertEquals(1, ((SuperSimple) multi).execute());
  }

}
