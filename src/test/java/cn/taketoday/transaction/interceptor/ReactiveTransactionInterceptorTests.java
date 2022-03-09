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

package cn.taketoday.transaction.interceptor;

import cn.taketoday.aop.framework.ProxyFactory;
import cn.taketoday.transaction.ReactiveTransactionManager;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link TransactionInterceptor} with reactive methods.
 *
 * @author Mark Paluch
 */
public class ReactiveTransactionInterceptorTests extends AbstractReactiveTransactionAspectTests {

  @Override
  protected Object advised(Object target, ReactiveTransactionManager ptm, TransactionAttributeSource[] tas) {
    TransactionInterceptor ti = new TransactionInterceptor();
    ti.setTransactionManager(ptm);
    ti.setTransactionAttributeSources(tas);

    ProxyFactory pf = new ProxyFactory(target);
    pf.addAdvice(0, ti);
    return pf.getProxy();
  }

  /**
   * Template method to create an advised object given the
   * target object and transaction setup.
   * Creates a TransactionInterceptor and applies it.
   */
  @Override
  protected Object advised(Object target, ReactiveTransactionManager ptm, TransactionAttributeSource tas) {
    TransactionInterceptor ti = new TransactionInterceptor();
    ti.setTransactionManager(ptm);

    assertThat(ti.getTransactionManager()).isEqualTo(ptm);
    ti.setTransactionAttributeSource(tas);
    assertThat(ti.getTransactionAttributeSource()).isEqualTo(tas);

    ProxyFactory pf = new ProxyFactory(target);
    pf.addAdvice(0, ti);
    return pf.getProxy();
  }

}
