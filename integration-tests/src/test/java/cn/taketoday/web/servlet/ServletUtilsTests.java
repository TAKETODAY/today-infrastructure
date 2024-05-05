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

package cn.taketoday.web.servlet;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import cn.taketoday.web.mock.ServletUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/1/26 15:07
 */
class ServletUtilsTests {

  @Test
  public void findParameterValue() {
    Map<String, Object> params = new HashMap<>();
    params.put("myKey1", "myValue1");
    params.put("myKey2_myValue2", "xxx");
    params.put("myKey3_myValue3.x", "xxx");
    params.put("myKey4_myValue4.y", new String[] { "yyy" });

    assertThat(ServletUtils.findParameterValue(params, "myKey0")).isNull();
    assertThat(ServletUtils.findParameterValue(params, "myKey1")).isEqualTo("myValue1");
    assertThat(ServletUtils.findParameterValue(params, "myKey2")).isEqualTo("myValue2");
    assertThat(ServletUtils.findParameterValue(params, "myKey3")).isEqualTo("myValue3");
    assertThat(ServletUtils.findParameterValue(params, "myKey4")).isEqualTo("myValue4");
  }

}
