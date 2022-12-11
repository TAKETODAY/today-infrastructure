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

package cn.taketoday.framework.test.mock.mockito;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;

import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.test.context.junit.jupiter.InfraExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

/**
 * Tests for a mock bean where the class being mocked uses field injection.
 *
 * @author Andy Wilkinson
 */
@ExtendWith(InfraExtension.class)
class MockBeanWithInjectedFieldIntegrationTests {

  @MockBean
  private MyService myService;

  @Test
  void fieldInjectionIntoMyServiceMockIsNotAttempted() {
    given(this.myService.getCount()).willReturn(5);
    assertThat(this.myService.getCount()).isEqualTo(5);
  }

  static class MyService {

    @Autowired
    private MyRepository repository;

    int getCount() {
      return this.repository.findAll().size();
    }

  }

  interface MyRepository {

    List<Object> findAll();

  }

}
