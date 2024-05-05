/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.test.web.mock;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;

import cn.taketoday.mock.web.HttpMockRequestImpl;

import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Test fixture for {@link DefaultMvcResult}.
 *
 * @author Rossen Stoyanchev
 */
public class DefaultMvcResultTests {

  private final DefaultMvcResult mvcResult = new DefaultMvcResult(
          new HttpMockRequestImpl(), null, null);

  @Test
  public void getAsyncResultSuccess() {
    this.mvcResult.setAsyncResult("Foo");
    this.mvcResult.setAsyncDispatchLatch(new CountDownLatch(0));
    this.mvcResult.getAsyncResult();
  }

  @Test
  public void getAsyncResultFailure() {
    assertThatIllegalStateException().isThrownBy(() ->
            this.mvcResult.getAsyncResult(0));
  }

}
