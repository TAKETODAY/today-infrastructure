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

package cn.taketoday.http.converter.json;

import com.google.gson.Gson;

import org.junit.jupiter.api.Test;

import java.util.Calendar;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link GsonFactoryBean} tests.
 *
 * @author Roy Clarkson
 */
public class GsonFactoryBeanTests {

  private static final String DATE_FORMAT = "yyyy-MM-dd";

  private GsonFactoryBean factory = new GsonFactoryBean();

  @Test
  public void serializeNulls() throws Exception {
    this.factory.setSerializeNulls(true);
    this.factory.afterPropertiesSet();
    Gson gson = this.factory.getObject();
    StringBean bean = new StringBean();
    String result = gson.toJson(bean);
    assertThat(result).isEqualTo("{\"name\":null}");
  }

  @Test
  public void serializeNullsFalse() throws Exception {
    this.factory.setSerializeNulls(false);
    this.factory.afterPropertiesSet();
    Gson gson = this.factory.getObject();
    StringBean bean = new StringBean();
    String result = gson.toJson(bean);
    assertThat(result).isEqualTo("{}");
  }

  @Test
  public void prettyPrinting() throws Exception {
    this.factory.setPrettyPrinting(true);
    this.factory.afterPropertiesSet();
    Gson gson = this.factory.getObject();
    StringBean bean = new StringBean();
    bean.setName("Jason");
    String result = gson.toJson(bean);
    assertThat(result.contains("  \"name\": \"Jason\"")).isTrue();
  }

  @Test
  public void prettyPrintingFalse() throws Exception {
    this.factory.setPrettyPrinting(false);
    this.factory.afterPropertiesSet();
    Gson gson = this.factory.getObject();
    StringBean bean = new StringBean();
    bean.setName("Jason");
    String result = gson.toJson(bean);
    assertThat(result).isEqualTo("{\"name\":\"Jason\"}");
  }

  @Test
  public void disableHtmlEscaping() throws Exception {
    this.factory.setDisableHtmlEscaping(true);
    this.factory.afterPropertiesSet();
    Gson gson = this.factory.getObject();
    StringBean bean = new StringBean();
    bean.setName("Bob=Bob");
    String result = gson.toJson(bean);
    assertThat(result).isEqualTo("{\"name\":\"Bob=Bob\"}");
  }

  @Test
  public void disableHtmlEscapingFalse() throws Exception {
    this.factory.setDisableHtmlEscaping(false);
    this.factory.afterPropertiesSet();
    Gson gson = this.factory.getObject();
    StringBean bean = new StringBean();
    bean.setName("Bob=Bob");
    String result = gson.toJson(bean);
    assertThat(result).isEqualTo("{\"name\":\"Bob\\u003dBob\"}");
  }

  @Test
  public void customizeDateFormatPattern() throws Exception {
    this.factory.setDateFormatPattern(DATE_FORMAT);
    this.factory.afterPropertiesSet();
    Gson gson = this.factory.getObject();
    DateBean bean = new DateBean();
    Calendar cal = Calendar.getInstance();
    cal.clear();
    cal.set(Calendar.YEAR, 2014);
    cal.set(Calendar.MONTH, Calendar.JANUARY);
    cal.set(Calendar.DATE, 1);
    Date date = cal.getTime();
    bean.setDate(date);
    String result = gson.toJson(bean);
    assertThat(result).isEqualTo("{\"date\":\"2014-01-01\"}");
  }

  @Test
  public void customizeDateFormatNone() throws Exception {
    this.factory.afterPropertiesSet();
    Gson gson = this.factory.getObject();
    DateBean bean = new DateBean();
    Calendar cal = Calendar.getInstance();
    cal.clear();
    cal.set(Calendar.YEAR, 2014);
    cal.set(Calendar.MONTH, Calendar.JANUARY);
    cal.set(Calendar.DATE, 1);
    Date date = cal.getTime();
    bean.setDate(date);
    String result = gson.toJson(bean);
    assertThat(result.startsWith("{\"date\":\"Jan 1, 2014")).isTrue();
    assertThat(result.endsWith("12:00:00 AM\"}")).isTrue();
  }

  @Test
  public void base64EncodeByteArrays() throws Exception {
    this.factory.setBase64EncodeByteArrays(true);
    this.factory.afterPropertiesSet();
    Gson gson = this.factory.getObject();
    ByteArrayBean bean = new ByteArrayBean();
    bean.setBytes(new byte[] { 0x1, 0x2 });
    String result = gson.toJson(bean);
    assertThat(result).isEqualTo("{\"bytes\":\"AQI\\u003d\"}");
  }

  @Test
  public void base64EncodeByteArraysDisableHtmlEscaping() throws Exception {
    this.factory.setBase64EncodeByteArrays(true);
    this.factory.setDisableHtmlEscaping(true);
    this.factory.afterPropertiesSet();
    Gson gson = this.factory.getObject();
    ByteArrayBean bean = new ByteArrayBean();
    bean.setBytes(new byte[] { 0x1, 0x2 });
    String result = gson.toJson(bean);
    assertThat(result).isEqualTo("{\"bytes\":\"AQI=\"}");
  }

  @Test
  public void base64EncodeByteArraysFalse() throws Exception {
    this.factory.setBase64EncodeByteArrays(false);
    this.factory.afterPropertiesSet();
    Gson gson = this.factory.getObject();
    ByteArrayBean bean = new ByteArrayBean();
    bean.setBytes(new byte[] { 0x1, 0x2 });
    String result = gson.toJson(bean);
    assertThat(result).isEqualTo("{\"bytes\":[1,2]}");
  }

  private static class StringBean {

    private String name;

    @SuppressWarnings("unused")
    public String getName() {
      return this.name;
    }

    public void setName(String name) {
      this.name = name;
    }
  }

  private static class DateBean {

    private Date date;

    @SuppressWarnings("unused")
    public Date getDate() {
      return this.date;
    }

    public void setDate(Date date) {
      this.date = date;
    }
  }

  private static class ByteArrayBean {

    private byte[] bytes;

    @SuppressWarnings("unused")
    public byte[] getBytes() {
      return this.bytes;
    }

    public void setBytes(byte[] bytes) {
      this.bytes = bytes;
    }
  }

}
