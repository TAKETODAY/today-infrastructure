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

package cn.taketoday.orm.jpa.hibernate.beans;

public class NoDefinitionInSpringContextTestBean extends TestBean {

  @SuppressWarnings("unused")
  private NoDefinitionInSpringContextTestBean() {
    throw new AssertionError("Unexpected call to the default constructor. " +
            "Is Infra trying to instantiate this class by itself, even though it should delegate to the fallback producer?"
    );
  }

  /*
   * Expect instantiation through a non-default constructor, just to be sure that Infra will fail if it tries to instantiate it,
   * and will subsequently delegate to the fallback bean instance producer.
   */
  public NoDefinitionInSpringContextTestBean(String name, BeanSource source) {
    setName(name);
    setSource(source);
  }

}
