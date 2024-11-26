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

package infra.beans.factory.xml;

import infra.beans.testfixture.beans.TestBean;
import infra.beans.testfixture.beans.factory.DummyFactory;

public class DummyReferencer {

  private TestBean testBean1;

  private TestBean testBean2;

  private DummyFactory dummyFactory;

  public DummyReferencer() {
  }

  public DummyReferencer(DummyFactory dummyFactory) {
    this.dummyFactory = dummyFactory;
  }

  public void setDummyFactory(DummyFactory dummyFactory) {
    this.dummyFactory = dummyFactory;
  }

  public DummyFactory getDummyFactory() {
    return dummyFactory;
  }

  public void setTestBean1(TestBean testBean1) {
    this.testBean1 = testBean1;
  }

  public TestBean getTestBean1() {
    return testBean1;
  }

  public void setTestBean2(TestBean testBean2) {
    this.testBean2 = testBean2;
  }

  public TestBean getTestBean2() {
    return testBean2;
  }
}
