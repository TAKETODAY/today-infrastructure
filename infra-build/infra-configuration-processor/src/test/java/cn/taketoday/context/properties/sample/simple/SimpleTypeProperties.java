/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.context.properties.sample.simple;

import cn.taketoday.context.properties.sample.ConfigurationProperties;

/**
 * Expose simple types to make sure these are detected properly.
 *
 * @author Stephane Nicoll
 */
@ConfigurationProperties(prefix = "simple.type")
public class SimpleTypeProperties {

  private String myString;

  private Byte myByte;

  private byte myPrimitiveByte;

  private Character myChar;

  private char myPrimitiveChar;

  private Boolean myBoolean;

  private boolean myPrimitiveBoolean;

  private Short myShort;

  private short myPrimitiveShort;

  private Integer myInteger;

  private int myPrimitiveInteger;

  private Long myLong;

  private long myPrimitiveLong;

  private Double myDouble;

  private double myPrimitiveDouble;

  private Float myFloat;

  private float myPrimitiveFloat;

  public String getMyString() {
    return this.myString;
  }

  public void setMyString(String myString) {
    this.myString = myString;
  }

  public Byte getMyByte() {
    return this.myByte;
  }

  public void setMyByte(Byte myByte) {
    this.myByte = myByte;
  }

  public byte getMyPrimitiveByte() {
    return this.myPrimitiveByte;
  }

  public void setMyPrimitiveByte(byte myPrimitiveByte) {
    this.myPrimitiveByte = myPrimitiveByte;
  }

  public Character getMyChar() {
    return this.myChar;
  }

  public void setMyChar(Character myChar) {
    this.myChar = myChar;
  }

  public char getMyPrimitiveChar() {
    return this.myPrimitiveChar;
  }

  public void setMyPrimitiveChar(char myPrimitiveChar) {
    this.myPrimitiveChar = myPrimitiveChar;
  }

  public Boolean getMyBoolean() {
    return this.myBoolean;
  }

  public void setMyBoolean(Boolean myBoolean) {
    this.myBoolean = myBoolean;
  }

  public boolean isMyPrimitiveBoolean() {
    return this.myPrimitiveBoolean;
  }

  public void setMyPrimitiveBoolean(boolean myPrimitiveBoolean) {
    this.myPrimitiveBoolean = myPrimitiveBoolean;
  }

  public Short getMyShort() {
    return this.myShort;
  }

  public void setMyShort(Short myShort) {
    this.myShort = myShort;
  }

  public short getMyPrimitiveShort() {
    return this.myPrimitiveShort;
  }

  public void setMyPrimitiveShort(short myPrimitiveShort) {
    this.myPrimitiveShort = myPrimitiveShort;
  }

  public Integer getMyInteger() {
    return this.myInteger;
  }

  public void setMyInteger(Integer myInteger) {
    this.myInteger = myInteger;
  }

  public int getMyPrimitiveInteger() {
    return this.myPrimitiveInteger;
  }

  public void setMyPrimitiveInteger(int myPrimitiveInteger) {
    this.myPrimitiveInteger = myPrimitiveInteger;
  }

  public Long getMyLong() {
    return this.myLong;
  }

  public void setMyLong(Long myLong) {
    this.myLong = myLong;
  }

  public long getMyPrimitiveLong() {
    return this.myPrimitiveLong;
  }

  public void setMyPrimitiveLong(long myPrimitiveLong) {
    this.myPrimitiveLong = myPrimitiveLong;
  }

  public Double getMyDouble() {
    return this.myDouble;
  }

  public void setMyDouble(Double myDouble) {
    this.myDouble = myDouble;
  }

  public double getMyPrimitiveDouble() {
    return this.myPrimitiveDouble;
  }

  public void setMyPrimitiveDouble(double myPrimitiveDouble) {
    this.myPrimitiveDouble = myPrimitiveDouble;
  }

  public Float getMyFloat() {
    return this.myFloat;
  }

  public void setMyFloat(Float myFloat) {
    this.myFloat = myFloat;
  }

  public float getMyPrimitiveFloat() {
    return this.myPrimitiveFloat;
  }

  public void setMyPrimitiveFloat(float myPrimitiveFloat) {
    this.myPrimitiveFloat = myPrimitiveFloat;
  }

}
