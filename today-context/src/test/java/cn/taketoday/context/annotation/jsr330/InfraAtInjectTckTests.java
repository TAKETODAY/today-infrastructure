/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.context.annotation.jsr330;

import junit.framework.Test;

import org.atinject.tck.Tck;
import org.atinject.tck.auto.Car;
import org.atinject.tck.auto.Convertible;
import org.atinject.tck.auto.Drivers;
import org.atinject.tck.auto.DriversSeat;
import org.atinject.tck.auto.FuelTank;
import org.atinject.tck.auto.Seat;
import org.atinject.tck.auto.Tire;
import org.atinject.tck.auto.V8Engine;
import org.atinject.tck.auto.accessories.Cupholder;
import org.atinject.tck.auto.accessories.SpareTire;

import cn.taketoday.context.annotation.AnnotatedBeanDefinitionReader;
import cn.taketoday.context.annotation.Jsr330ScopeMetadataResolver;
import cn.taketoday.context.annotation.Primary;
import cn.taketoday.context.support.GenericApplicationContext;

/**
 * @author Juergen Hoeller
 * @since 3.0
 */
public class InfraAtInjectTckTests {

  @SuppressWarnings("unchecked")
  public static Test suite() {
    GenericApplicationContext ac = new GenericApplicationContext();
    AnnotatedBeanDefinitionReader bdr = new AnnotatedBeanDefinitionReader(ac);
    bdr.setScopeMetadataResolver(new Jsr330ScopeMetadataResolver());

    bdr.registerBean(Convertible.class);
    bdr.registerBean(DriversSeat.class, Drivers.class);
    bdr.registerBean(Seat.class, Primary.class);
    bdr.registerBean(V8Engine.class);
    bdr.registerBean(SpareTire.class, "spare");
    bdr.registerBean(Cupholder.class);
    bdr.registerBean(Tire.class, Primary.class);
    bdr.registerBean(FuelTank.class);

    ac.refresh();
    Car car = ac.getBean(Car.class);

    return Tck.testsFor(car, false, true);
  }

}
