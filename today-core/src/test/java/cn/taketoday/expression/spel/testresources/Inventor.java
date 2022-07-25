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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import cn.taketoday.util.ObjectUtils;

///CLOVER:OFF
@SuppressWarnings("unused")
public class Inventor {

  private String name;
  public String _name;
  public String _name_;
  public String publicName;
  private PlaceOfBirth placeOfBirth;
  private Date birthdate;
  private int sinNumber;
  private String nationality;
  private String[] inventions;
  public String randomField;
  public Map<String, String> testMap;
  private boolean wonNobelPrize;
  private PlaceOfBirth[] placesLived;
  private List<PlaceOfBirth> placesLivedList = new ArrayList<>();
  public ArrayContainer arrayContainer;
  public boolean publicBoolean;
  private boolean accessedThroughGetSet;
  public List<Integer> listOfInteger = new ArrayList<>();
  public List<Boolean> booleanList = new ArrayList<>();
  public Map<String, Boolean> mapOfStringToBoolean = new LinkedHashMap<>();
  public Map<Integer, String> mapOfNumbersUpToTen = new LinkedHashMap<>();
  public List<Integer> listOfNumbersUpToTen = new ArrayList<>();
  public List<Integer> listOneFive = new ArrayList<>();
  public String[] stringArrayOfThreeItems = new String[] { "1", "2", "3" };
  private String foo;
  public int counter;

  public Inventor(String name, Date birthdate, String nationality) {
    this.name = name;
    this._name = name;
    this._name_ = name;
    this.birthdate = birthdate;
    this.nationality = nationality;
    this.arrayContainer = new ArrayContainer();
    testMap = new HashMap<>();
    testMap.put("monday", "montag");
    testMap.put("tuesday", "dienstag");
    testMap.put("wednesday", "mittwoch");
    testMap.put("thursday", "donnerstag");
    testMap.put("friday", "freitag");
    testMap.put("saturday", "samstag");
    testMap.put("sunday", "sonntag");
    listOneFive.add(1);
    listOneFive.add(5);
    booleanList.add(false);
    booleanList.add(false);
    listOfNumbersUpToTen.add(1);
    listOfNumbersUpToTen.add(2);
    listOfNumbersUpToTen.add(3);
    listOfNumbersUpToTen.add(4);
    listOfNumbersUpToTen.add(5);
    listOfNumbersUpToTen.add(6);
    listOfNumbersUpToTen.add(7);
    listOfNumbersUpToTen.add(8);
    listOfNumbersUpToTen.add(9);
    listOfNumbersUpToTen.add(10);
    mapOfNumbersUpToTen.put(1, "one");
    mapOfNumbersUpToTen.put(2, "two");
    mapOfNumbersUpToTen.put(3, "three");
    mapOfNumbersUpToTen.put(4, "four");
    mapOfNumbersUpToTen.put(5, "five");
    mapOfNumbersUpToTen.put(6, "six");
    mapOfNumbersUpToTen.put(7, "seven");
    mapOfNumbersUpToTen.put(8, "eight");
    mapOfNumbersUpToTen.put(9, "nine");
    mapOfNumbersUpToTen.put(10, "ten");
  }

  public void setPlaceOfBirth(PlaceOfBirth placeOfBirth2) {
    placeOfBirth = placeOfBirth2;
    this.placesLived = new PlaceOfBirth[] { placeOfBirth2 };
    this.placesLivedList.add(placeOfBirth2);
  }

  public String[] getInventions() {
    return inventions;
  }

  public void setInventions(String[] inventions) {
    this.inventions = inventions;
  }

  public PlaceOfBirth getPlaceOfBirth() {
    return placeOfBirth;
  }

  public int throwException(int valueIn) throws Exception {
    counter++;
    if (valueIn == 1) {
      throw new IllegalArgumentException("IllegalArgumentException for 1");
    }
    if (valueIn == 2) {
      throw new RuntimeException("RuntimeException for 2");
    }
    if (valueIn == 4) {
      throw new TestException();
    }
    return valueIn;
  }

  @SuppressWarnings("serial")
  static class TestException extends Exception { }

  public String throwException(PlaceOfBirth pob) {
    return pob.getCity();
  }

  public String getName() {
    return name;
  }

  public boolean getWonNobelPrize() {
    return wonNobelPrize;
  }

  public void setWonNobelPrize(boolean wonNobelPrize) {
    this.wonNobelPrize = wonNobelPrize;
  }

  public PlaceOfBirth[] getPlacesLived() {
    return placesLived;
  }

  public void setPlacesLived(PlaceOfBirth[] placesLived) {
    this.placesLived = placesLived;
  }

  public List<PlaceOfBirth> getPlacesLivedList() {
    return placesLivedList;
  }

  public void setPlacesLivedList(List<PlaceOfBirth> placesLivedList) {
    this.placesLivedList = placesLivedList;
  }

  public String echo(Object o) {
    return o.toString();
  }

  public String sayHelloTo(String person) {
    return "hello " + person;
  }

  public String printDouble(Double d) {
    return d.toString();
  }

  public String printDoubles(double[] d) {
    return ObjectUtils.nullSafeToString(d);
  }

  public List<String> getDoublesAsStringList() {
    List<String> result = new ArrayList<>();
    result.add("14.35");
    result.add("15.45");
    return result;
  }

  public String joinThreeStrings(String a, String b, String c) {
    return a + b + c;
  }

  public String aVarargsMethod(String... strings) {
    return Arrays.toString(strings);
  }

  public String aVarargsMethod2(int i, String... strings) {
    return i + "-" + Arrays.toString(strings);
  }

  @SuppressWarnings("unchecked")
  public String optionalVarargsMethod(Optional<String>... values) {
    return Arrays.toString(values);
  }

  public String aVarargsMethod3(String str1, String... strings) {
    if (ObjectUtils.isEmpty(strings)) {
      return str1;
    }
    return str1 + "-" + String.join("-", strings);
  }

  public Inventor(String... strings) {
  }

  public boolean getSomeProperty() {
    return accessedThroughGetSet;
  }

  public void setSomeProperty(boolean b) {
    this.accessedThroughGetSet = b;
  }

  public Date getBirthdate() { return birthdate; }

  public String getFoo() { return foo; }

  public void setFoo(String s) { foo = s; }

  public String getNationality() { return nationality; }
}
