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

package cn.taketoday.aop.aspectj.autoproxy.spr3064;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import cn.taketoday.context.support.ClassPathXmlApplicationContext;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Adrian Colyer
 * @author Chris Beams
 */
public class SPR3064Tests {

  private Service service;

  @Test
  public void testServiceIsAdvised() {
    ClassPathXmlApplicationContext ctx =
            new ClassPathXmlApplicationContext(getClass().getSimpleName() + ".xml", getClass());

    service = (Service) ctx.getBean("service");
    assertThatExceptionOfType(RuntimeException.class).isThrownBy(
                    this.service::serveMe)
            .withMessageContaining("advice invoked");
  }

}

@Retention(RetentionPolicy.RUNTIME)
@interface Transaction {
}

@Aspect
class TransactionInterceptor {

  @Around(value = "execution(* *..Service.*(..)) && @annotation(transaction)")
  public Object around(ProceedingJoinPoint pjp, Transaction transaction) throws Throwable {
    throw new RuntimeException("advice invoked");
    //return pjp.proceed();
  }
}

interface Service {

  void serveMe();
}

class ServiceImpl implements Service {

  @Override
  @Transaction
  public void serveMe() {
  }
}
