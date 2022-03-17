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
package cn.taketoday.core.bytecode.beans;

import org.junit.jupiter.api.Test;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Chris Nokleberg
 * <a href="mailto:chris@nokleberg.com">chris@nokleberg.com</a>
 * @version $Id: TestBeanMapProxy.java,v 1.3 2004/06/24 21:15:17 herbyderby Exp
 * $
 */
public class TestBeanMapProxy {

  @Test
  public void testBeanMap() throws Exception {
    HashMap identity = new HashMap() { }; // use anonymous class for correct class loader
    Person person = (Person) BeanMapProxy.newInstance(identity, new Class[] { Person.class });
    person.setName("Chris");
    assertEquals("Chris", person.getName());
    assertEquals("Chris", identity.get("Name"));
  }

  public interface Person {
    public String getName();

    public void setName(String name);
  }

}
