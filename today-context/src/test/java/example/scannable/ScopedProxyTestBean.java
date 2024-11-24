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

package example.scannable;

import infra.context.annotation.Scope;
import infra.util.concurrent.Future;

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
    return Future.ok("bar");
  }

  @Override
  public boolean isInitCalled() {
    return false;
  }

}
