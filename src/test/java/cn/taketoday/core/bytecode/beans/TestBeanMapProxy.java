/*
 * Copyright 2003,2004 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
