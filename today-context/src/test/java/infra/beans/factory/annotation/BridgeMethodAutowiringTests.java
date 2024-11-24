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

package infra.beans.factory.annotation;

import org.junit.jupiter.api.Test;

import infra.context.annotation.AnnotationConfigApplicationContext;
import infra.stereotype.Component;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import static org.assertj.core.api.Assertions.assertThat;

public class BridgeMethodAutowiringTests {

  @Test
  public void SPR8434() {
    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
    ctx.register(UserServiceImpl.class, Foo.class);
    ctx.refresh();
    assertThat(ctx.getBean(UserServiceImpl.class).object).isNotNull();
  }

  static abstract class GenericServiceImpl<D> {

    public abstract void setObject(D object);
  }

  public static class UserServiceImpl extends GenericServiceImpl<Foo> {

    protected Foo object;

    @Override
    @Inject
    @Named("userObject")
    public void setObject(Foo object) {
      if (this.object != null) {
        throw new IllegalStateException("Already called");
      }
      this.object = object;
    }
  }

  @Component("userObject")
  public static class Foo {
  }

}
