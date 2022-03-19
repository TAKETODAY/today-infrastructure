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

package cn.taketoday.beans.testfixture.beans;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.beans.factory.BeanFactoryAware;
import cn.taketoday.beans.factory.BeanNameAware;
import cn.taketoday.util.ObjectUtils;

/**
 * Simple test bean used for testing bean factories, the AOP framework etc.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2021/11/30 15:31
 */
public class TestBean implements BeanNameAware, BeanFactoryAware, ITestBean, IOther, Comparable<Object> {

  private String beanName;

  private String country;

  private BeanFactory beanFactory;

  private boolean postProcessed;

  private String name;

  private String sex;

  private int age;

  private boolean jedi;

  private ITestBean spouse;

  private String touchy;

  private String[] stringArray;

  private Integer[] someIntegerArray;

  private Integer[][] nestedIntegerArray;

  private int[] someIntArray;

  private int[][] nestedIntArray;

  private Date date = new Date();

  private Float myFloat = Float.valueOf(0.0f);

  private Collection<? super Object> friends = new ArrayList<>();

  private Set<?> someSet = new HashSet<>();

  private Map<?, ?> someMap = new HashMap<>();

  private List<?> someList = new ArrayList<>();

  private Properties someProperties = new Properties();

  private boolean destroyed;

  private Number someNumber;

  private Boolean someBoolean;

  private List<?> otherColours;

  private List<?> pets;

  public TestBean() {
  }

  public TestBean(String name) {
    this.name = name;
  }

  public TestBean(ITestBean spouse) {
    this.spouse = spouse;
  }

  public TestBean(String name, int age) {
    this.name = name;
    this.age = age;
  }

  public TestBean(ITestBean spouse, Properties someProperties) {
    this.spouse = spouse;
    this.someProperties = someProperties;
  }

  public TestBean(List<?> someList) {
    this.someList = someList;
  }

  public TestBean(Set<?> someSet) {
    this.someSet = someSet;
  }

  public TestBean(Map<?, ?> someMap) {
    this.someMap = someMap;
  }

  public TestBean(Properties someProperties) {
    this.someProperties = someProperties;
  }

  @Override
  public void setBeanName(String beanName) {
    this.beanName = beanName;
  }

  public String getBeanName() {
    return beanName;
  }

  @Override
  public void setBeanFactory(BeanFactory beanFactory) {
    this.beanFactory = beanFactory;
  }

  public BeanFactory getBeanFactory() {
    return beanFactory;
  }

  public void setPostProcessed(boolean postProcessed) {
    this.postProcessed = postProcessed;
  }

  public boolean isPostProcessed() {
    return postProcessed;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public void setName(String name) {
    this.name = name;
  }

  public String getSex() {
    return sex;
  }

  public void setSex(String sex) {
    this.sex = sex;
    if (this.name == null) {
      this.name = sex;
    }
  }

  @Override
  public int getAge() {
    return age;
  }

  @Override
  public void setAge(int age) {
    this.age = age;
  }

  public boolean isJedi() {
    return jedi;
  }

  public void setJedi(boolean jedi) {
    this.jedi = jedi;
  }

  @Override
  public ITestBean getSpouse() {
    return this.spouse;
  }

  @Override
  public void setSpouse(ITestBean spouse) {
    this.spouse = spouse;
  }

  @Override
  public ITestBean[] getSpouses() {
    return (spouse != null ? new ITestBean[] { spouse } : null);
  }

  public String getTouchy() {
    return touchy;
  }

  public void setTouchy(String touchy) throws Exception {
    if (touchy.indexOf('.') != -1) {
      throw new Exception("Can't contain a .");
    }
    if (touchy.indexOf(',') != -1) {
      throw new NumberFormatException("Number format exception: contains a ,");
    }
    this.touchy = touchy;
  }

  public String getCountry() {
    return country;
  }

  public void setCountry(String country) {
    this.country = country;
  }

  @Override
  public String[] getStringArray() {
    return stringArray;
  }

  @Override
  public void setStringArray(String[] stringArray) {
    this.stringArray = stringArray;
  }

  @Override
  public Integer[] getSomeIntegerArray() {
    return someIntegerArray;
  }

  @Override
  public void setSomeIntegerArray(Integer[] someIntegerArray) {
    this.someIntegerArray = someIntegerArray;
  }

  @Override
  public Integer[][] getNestedIntegerArray() {
    return nestedIntegerArray;
  }

  @Override
  public void setNestedIntegerArray(Integer[][] nestedIntegerArray) {
    this.nestedIntegerArray = nestedIntegerArray;
  }

  @Override
  public int[] getSomeIntArray() {
    return someIntArray;
  }

  @Override
  public void setSomeIntArray(int[] someIntArray) {
    this.someIntArray = someIntArray;
  }

  @Override
  public int[][] getNestedIntArray() {
    return nestedIntArray;
  }

  @Override
  public void setNestedIntArray(int[][] nestedIntArray) {
    this.nestedIntArray = nestedIntArray;
  }

  public Date getDate() {
    return date;
  }

  public void setDate(Date date) {
    this.date = date;
  }

  public Float getMyFloat() {
    return myFloat;
  }

  public void setMyFloat(Float myFloat) {
    this.myFloat = myFloat;
  }

  public Collection<? super Object> getFriends() {
    return friends;
  }

  public void setFriends(Collection<? super Object> friends) {
    this.friends = friends;
  }

  public Set<?> getSomeSet() {
    return someSet;
  }

  public void setSomeSet(Set<?> someSet) {
    this.someSet = someSet;
  }

  public Map<?, ?> getSomeMap() {
    return someMap;
  }

  public void setSomeMap(Map<?, ?> someMap) {
    this.someMap = someMap;
  }

  public List<?> getSomeList() {
    return someList;
  }

  public void setSomeList(List<?> someList) {
    this.someList = someList;
  }

  public Properties getSomeProperties() {
    return someProperties;
  }

  public void setSomeProperties(Properties someProperties) {
    this.someProperties = someProperties;
  }

  public Number getSomeNumber() {
    return someNumber;
  }

  public void setSomeNumber(Number someNumber) {
    this.someNumber = someNumber;
  }

  public Boolean getSomeBoolean() {
    return someBoolean;
  }

  public void setSomeBoolean(Boolean someBoolean) {
    this.someBoolean = someBoolean;
  }

  public List<?> getOtherColours() {
    return otherColours;
  }

  public void setOtherColours(List<?> otherColours) {
    this.otherColours = otherColours;
  }

  public List<?> getPets() {
    return pets;
  }

  public void setPets(List<?> pets) {
    this.pets = pets;
  }

  @Override
  public void exceptional(Throwable t) throws Throwable {
    if (t != null) {
      throw t;
    }
  }

  @Override
  public void unreliableFileOperation() throws IOException {
    throw new IOException();
  }

  @Override
  public Object returnsThis() {
    return this;
  }

  @Override
  public void absquatulate() {
  }

  @Override
  public int haveBirthday() {
    return age++;
  }

  public void destroy() {
    this.destroyed = true;
  }

  public boolean wasDestroyed() {
    return destroyed;
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof TestBean)) {
      return false;
    }
    TestBean tb2 = (TestBean) other;
    return (ObjectUtils.nullSafeEquals(this.name, tb2.name) && this.age == tb2.age);
  }

  @Override
  public int hashCode() {
    return this.age;
  }

  @Override
  public int compareTo(Object other) {
    if (this.name != null && other instanceof TestBean) {
      return this.name.compareTo(((TestBean) other).getName());
    }
    else {
      return 1;
    }
  }

  @Override
  public String toString() {
    return this.name;
  }

}
