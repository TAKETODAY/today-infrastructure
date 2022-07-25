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

package example.scannable;

import java.util.concurrent.Future;

import cn.taketoday.context.annotation.Scope;
import cn.taketoday.scheduling.annotation.AsyncResult;

/**
 * @author Mark Fisher
 * @author Juergen Hoeller
 */
@Scope("myScope")
public class ScopedProxyTestBean implements FooService {

  @Override
  public String foo(int id) {
    return "bar";
  }

  @Override
  public Future<String> asyncFoo(int id) {
    return new AsyncResult<>("bar");
  }

  @Override
  public boolean isInitCalled() {
    return false;
  }

}
