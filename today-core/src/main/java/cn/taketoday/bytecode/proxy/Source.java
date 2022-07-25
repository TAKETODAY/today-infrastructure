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
package cn.taketoday.bytecode.proxy;

public abstract class Source implements java.io.Serializable {

  private static final long serialVersionUID = 6257051574913595351L;

  public static class CheckedException extends Exception {

    private static final long serialVersionUID = 1L;
  }

  public static class UndeclaredException extends Exception {

    private static final long serialVersionUID = 1L;
  }

  public String toString() {
    return "";
  }

  public Source() { }

  public void callAll() {
    protectedMethod();
    packageMethod();
    abstractMethod();
    synchronizedMethod();
    finalMethod();
    intType(1);
    longType(1L);
    floatType(1f);
    doubleType(1.0);
    objectType("1");
    voidType();
    multiArg(1, 1, 1, 1, "", "", "");
  }

  protected void protectedMethod() { }

  void packageMethod() { }

  abstract void abstractMethod();

  public void throwChecked() throws CheckedException {
    throw new CheckedException();
  }

  public void throwIndexOutOfBoundsException() {
    throw new IndexOutOfBoundsException();
  }

  public void throwAbstractMethodError() {
    throw new AbstractMethodError();
  }

  public synchronized void synchronizedMethod() { }

  public final void finalMethod() { }

  public int intType(int val) {
    return val;
  }

  public long longType(long val) {
    return val;
  }

  public double doubleType(double val) {
    return val;
  }

  public float floatType(float val) {
    return val;
  }

  public boolean booleanType(boolean val) {
    return val;
  }

  public short shortType(short val) {
    return val;
  }

  public char charType(char val) {
    return val;
  }

  public byte byteType(byte val) {
    return val;
  }

  public int[] arrayType(int val[]) {
    return val;
  }

  public String[] arrayType(String val[]) {
    return val;
  }

  public Object objectType(Object val) {
    return val;
  }

  public void voidType() {

  }

  public void multiArg(int arg1, long arg2,
          double arg3, float arg4, Object arg5, Object arg6, Object arg7) {

  }

}
