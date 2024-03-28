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


import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.beans.factory.annotation.Qualifier;
import cn.taketoday.context.annotation.Lazy;
import cn.taketoday.util.concurrent.Future;
import jakarta.annotation.PostConstruct;

/**
 * @author Mark Fisher
 * @author Juergen Hoeller
 */
@Lazy
public class AutowiredQualifierFooService implements FooService {

  @Autowired
  @Qualifier("testing")
  private FooDao fooDao;

  private boolean initCalled = false;

  @PostConstruct
  private void init() {
    if (this.initCalled) {
      throw new IllegalStateException("Init already called");
    }
    this.initCalled = true;
  }

  @Override
  public String foo(int id) {
    return this.fooDao.findFoo(id);
  }

  @Override
  public Future<String> asyncFoo(int id) {
    return Future.ok(this.fooDao.findFoo(id));
  }

  @Override
  public boolean isInitCalled() {
    return this.initCalled;
  }

}
