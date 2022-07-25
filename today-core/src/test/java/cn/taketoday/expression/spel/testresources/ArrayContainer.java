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

package cn.taketoday.expression.spel.testresources;

/**
 * Hold the various kinds of primitive array for access through the test evaluation context.
 *
 * @author Andy Clement
 */
public class ArrayContainer {

  public int[] ints = new int[3];
  public long[] longs = new long[3];
  public double[] doubles = new double[3];
  public byte[] bytes = new byte[3];
  public char[] chars = new char[3];
  public short[] shorts = new short[3];
  public boolean[] booleans = new boolean[3];
  public float[] floats = new float[3];

  public ArrayContainer() {
    // setup some values
    ints[0] = 42;
    longs[0] = 42L;
    doubles[0] = 42.0d;
    bytes[0] = 42;
    chars[0] = 42;
    shorts[0] = 42;
    booleans[0] = true;
    floats[0] = 42.0f;
  }
}
