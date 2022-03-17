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

package cn.taketoday.context.annotation.spr16756;

import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.context.annotation.Scope;
import cn.taketoday.lang.Component;

@Component
public class ScannedComponent {

  @Autowired
  private State state;

  public String iDoAnything() {
    return state.anyMethod();
  }

  public interface State {

    String anyMethod();
  }

  @Component
  @Scope(/*proxyMode = ScopedProxyMode.INTERFACES,*/ value = "prototype")
  public static class StateImpl implements State {

    @Override
    public String anyMethod() {
      return "anyMethod called";
    }
  }

}
