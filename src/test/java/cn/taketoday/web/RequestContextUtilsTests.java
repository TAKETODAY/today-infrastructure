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

package cn.taketoday.web;

import org.junit.jupiter.api.Test;

import cn.taketoday.web.bind.RequestBindingException;
import cn.taketoday.web.mock.MockHttpServletRequest;
import cn.taketoday.web.servlet.ServletRequestContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/1/22 23:23
 */
class RequestContextUtilsTests {

  private final MockHttpServletRequest request = new MockHttpServletRequest();

  RequestContext context = new ServletRequestContext(null, request, null);

  @Test
  void testIntParameter() throws RequestBindingException {
    request.addParameter("param1", "5");
    request.addParameter("param2", "e");
    request.addParameter("paramEmpty", "");

    assertThat(RequestContextUtils.getIntParameter(context, "param1")).isEqualTo(5);
    assertThat(RequestContextUtils.getIntParameter(context, "param1", 6)).isEqualTo(5);
    assertThat(RequestContextUtils.getRequiredIntParameter(context, "param1")).isEqualTo(5);

    assertThat(RequestContextUtils.getIntParameter(context, "param2", 6)).isEqualTo(6);
    assertThatExceptionOfType(RequestBindingException.class).isThrownBy(() ->
            RequestContextUtils.getRequiredIntParameter(context, "param2"));

    assertThat(RequestContextUtils.getIntParameter(context, "param3")).isNull();
    assertThat(RequestContextUtils.getIntParameter(context, "param3", 6)).isEqualTo(6);
    assertThatExceptionOfType(RequestBindingException.class).isThrownBy(() ->
            RequestContextUtils.getRequiredIntParameter(context, "param3"));

    assertThatExceptionOfType(RequestBindingException.class).isThrownBy(() ->
            RequestContextUtils.getRequiredIntParameter(context, "paramEmpty"));
  }

  @Test
  void testIntParameters() throws RequestBindingException {
    request.addParameter("param", "1", "2", "3");

    request.addParameter("param2", "1");
    request.addParameter("param2", "2");
    request.addParameter("param2", "bogus");

    int[] array = new int[] { 1, 2, 3 };
    int[] values = RequestContextUtils.getRequiredIntParameters(context, "param");
    assertThat(values).hasSize(3);
    for (int i = 0; i < array.length; i++) {
      assertThat(array[i]).isEqualTo(values[i]);
    }

    assertThatExceptionOfType(RequestBindingException.class).isThrownBy(() ->
            RequestContextUtils.getRequiredIntParameters(context, "param2"));
  }

  @Test
  void testLongParameter() throws RequestBindingException {
    request.addParameter("param1", "5");
    request.addParameter("param2", "e");
    request.addParameter("paramEmpty", "");

    assertThat(RequestContextUtils.getLongParameter(context, "param1")).isEqualTo(Long.valueOf(5L));
    assertThat(RequestContextUtils.getLongParameter(context, "param1", 6L)).isEqualTo(5L);
    assertThat(RequestContextUtils.getRequiredIntParameter(context, "param1")).isEqualTo(5L);

    assertThat(RequestContextUtils.getLongParameter(context, "param2", 6L)).isEqualTo(6L);
    assertThatExceptionOfType(RequestBindingException.class).isThrownBy(() ->
            RequestContextUtils.getRequiredLongParameter(context, "param2"));

    assertThat(RequestContextUtils.getLongParameter(context, "param3")).isNull();
    assertThat(RequestContextUtils.getLongParameter(context, "param3", 6L)).isEqualTo(6L);
    assertThatExceptionOfType(RequestBindingException.class).isThrownBy(() ->
            RequestContextUtils.getRequiredLongParameter(context, "param3"));

    assertThatExceptionOfType(RequestBindingException.class).isThrownBy(() ->
            RequestContextUtils.getRequiredLongParameter(context, "paramEmpty"));
  }

  @Test
  void testLongParameters() throws RequestBindingException {
    request.setParameter("param", new String[] { "1", "2", "3" });

    request.setParameter("param2", "0");
    request.setParameter("param2", "1");
    request.addParameter("param2", "2");
    request.addParameter("param2", "bogus");

    long[] values = RequestContextUtils.getRequiredLongParameters(context, "param");
    assertThat(values).containsExactly(1, 2, 3);

    assertThatExceptionOfType(RequestBindingException.class).isThrownBy(() ->
            RequestContextUtils.getRequiredLongParameters(context, "param2"));

    request.setParameter("param2", new String[] { "1", "2" });
    values = RequestContextUtils.getRequiredLongParameters(context, "param2");
    assertThat(values).containsExactly(1, 2);

    request.removeParameter("param2");
    assertThatExceptionOfType(RequestBindingException.class).isThrownBy(() ->
            RequestContextUtils.getRequiredLongParameters(context, "param2"));
  }

  @Test
  void testFloatParameter() throws RequestBindingException {
    request.addParameter("param1", "5.5");
    request.addParameter("param2", "e");
    request.addParameter("paramEmpty", "");

    assertThat(RequestContextUtils.getFloatParameter(context, "param1")).isEqualTo(Float.valueOf(5.5f));
    assertThat(RequestContextUtils.getFloatParameter(context, "param1", 6.5f) == 5.5f).isTrue();
    assertThat(RequestContextUtils.getRequiredFloatParameter(context, "param1") == 5.5f).isTrue();

    assertThat(RequestContextUtils.getFloatParameter(context, "param2", 6.5f) == 6.5f).isTrue();
    assertThatExceptionOfType(RequestBindingException.class).isThrownBy(() ->
            RequestContextUtils.getRequiredFloatParameter(context, "param2"));

    assertThat(RequestContextUtils.getFloatParameter(context, "param3")).isNull();
    assertThat(RequestContextUtils.getFloatParameter(context, "param3", 6.5f) == 6.5f).isTrue();
    assertThatExceptionOfType(RequestBindingException.class).isThrownBy(() ->
            RequestContextUtils.getRequiredFloatParameter(context, "param3"));

    assertThatExceptionOfType(RequestBindingException.class).isThrownBy(() ->
            RequestContextUtils.getRequiredFloatParameter(context, "paramEmpty"));
  }

  @Test
  void testFloatParameters() throws RequestBindingException {
    request.addParameter("param", new String[] { "1.5", "2.5", "3" });

    request.addParameter("param2", "1.5");
    request.addParameter("param2", "2");
    request.addParameter("param2", "bogus");

    float[] values = RequestContextUtils.getRequiredFloatParameters(context, "param");
    assertThat(values).containsExactly(1.5F, 2.5F, 3F);

    assertThatExceptionOfType(RequestBindingException.class).isThrownBy(() ->
            RequestContextUtils.getRequiredFloatParameters(context, "param2"));
  }

  @Test
  void testDoubleParameter() throws RequestBindingException {
    request.addParameter("param1", "5.5");
    request.addParameter("param2", "e");
    request.addParameter("paramEmpty", "");

    assertThat(RequestContextUtils.getDoubleParameter(context, "param1")).isEqualTo(Double.valueOf(5.5));
    assertThat(RequestContextUtils.getDoubleParameter(context, "param1", 6.5) == 5.5).isTrue();
    assertThat(RequestContextUtils.getRequiredDoubleParameter(context, "param1") == 5.5).isTrue();

    assertThat(RequestContextUtils.getDoubleParameter(context, "param2", 6.5) == 6.5).isTrue();
    assertThatExceptionOfType(RequestBindingException.class).isThrownBy(() ->
            RequestContextUtils.getRequiredDoubleParameter(context, "param2"));

    assertThat(RequestContextUtils.getDoubleParameter(context, "param3")).isNull();
    assertThat(RequestContextUtils.getDoubleParameter(context, "param3", 6.5) == 6.5).isTrue();
    assertThatExceptionOfType(RequestBindingException.class).isThrownBy(() ->
            RequestContextUtils.getRequiredDoubleParameter(context, "param3"));

    assertThatExceptionOfType(RequestBindingException.class).isThrownBy(() ->
            RequestContextUtils.getRequiredDoubleParameter(context, "paramEmpty"));
  }

  @Test
  void testDoubleParameters() throws RequestBindingException {
    request.addParameter("param", new String[] { "1.5", "2.5", "3" });

    request.addParameter("param2", "1.5");
    request.addParameter("param2", "2");
    request.addParameter("param2", "bogus");

    double[] values = RequestContextUtils.getRequiredDoubleParameters(context, "param");
    assertThat(values).containsExactly(1.5, 2.5, 3);
    assertThatExceptionOfType(RequestBindingException.class).isThrownBy(() ->
            RequestContextUtils.getRequiredDoubleParameters(context, "param2"));
  }

  @Test
  void testBooleanParameter() throws RequestBindingException {
    request.addParameter("param1", "true");
    request.addParameter("param2", "e");
    request.addParameter("param4", "yes");
    request.addParameter("param5", "1");
    request.addParameter("paramEmpty", "");

    assertThat(RequestContextUtils.getBooleanParameter(context, "param1").equals(Boolean.TRUE)).isTrue();
    assertThat(RequestContextUtils.getBooleanParameter(context, "param1", false)).isTrue();
    assertThat(RequestContextUtils.getRequiredBooleanParameter(context, "param1")).isTrue();

    assertThat(RequestContextUtils.getBooleanParameter(context, "param2", true)).isFalse();
    assertThat(RequestContextUtils.getRequiredBooleanParameter(context, "param2")).isFalse();

    assertThat(RequestContextUtils.getBooleanParameter(context, "param3")).isNull();
    assertThat(RequestContextUtils.getBooleanParameter(context, "param3", true)).isTrue();
    assertThatExceptionOfType(RequestBindingException.class).isThrownBy(() ->
            RequestContextUtils.getRequiredBooleanParameter(context, "param3"));

    assertThat(RequestContextUtils.getBooleanParameter(context, "param4", false)).isTrue();
    assertThat(RequestContextUtils.getRequiredBooleanParameter(context, "param4")).isTrue();

    assertThat(RequestContextUtils.getBooleanParameter(context, "param5", false)).isTrue();
    assertThat(RequestContextUtils.getRequiredBooleanParameter(context, "param5")).isTrue();
    assertThat(RequestContextUtils.getRequiredBooleanParameter(context, "paramEmpty")).isFalse();
  }

  @Test
  void testBooleanParameters() throws RequestBindingException {
    request.addParameter("param", new String[] { "true", "yes", "off", "1", "bogus" });

    request.addParameter("param2", "false");
    request.addParameter("param2", "true");
    request.addParameter("param2", "");

    boolean[] array = new boolean[] { true, true, false, true, false };
    boolean[] values = RequestContextUtils.getRequiredBooleanParameters(context, "param");
    assertThat(array).hasSameSizeAs(values);
    for (int i = 0; i < array.length; i++) {
      assertThat(array[i]).isEqualTo(values[i]);
    }

    array = new boolean[] { false, true, false };
    values = RequestContextUtils.getRequiredBooleanParameters(context, "param2");
    assertThat(array).hasSameSizeAs(values);
    for (int i = 0; i < array.length; i++) {
      assertThat(array[i]).isEqualTo(values[i]);
    }
  }

  @Test
  void testStringParameter() throws RequestBindingException {
    request.addParameter("param1", "str");
    request.addParameter("paramEmpty", "");

    assertThat(RequestContextUtils.getStringParameter(context, "param1")).isEqualTo("str");
    assertThat(RequestContextUtils.getStringParameter(context, "param1", "string")).isEqualTo("str");
    assertThat(RequestContextUtils.getRequiredStringParameter(context, "param1")).isEqualTo("str");

    assertThat(RequestContextUtils.getStringParameter(context, "param3")).isNull();
    assertThat(RequestContextUtils.getStringParameter(context, "param3", "string")).isEqualTo("string");
    assertThat(RequestContextUtils.getStringParameter(context, "param3", null)).isNull();
    assertThatExceptionOfType(RequestBindingException.class).isThrownBy(() ->
            RequestContextUtils.getRequiredStringParameter(context, "param3"));

    assertThat(RequestContextUtils.getStringParameter(context, "paramEmpty")).isEmpty();
    assertThat(RequestContextUtils.getRequiredStringParameter(context, "paramEmpty")).isEmpty();
  }

}
