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

package cn.taketoday.http.codec.json;

import com.fasterxml.jackson.annotation.JsonView;

/**
 * @author Sebastien Deleuze
 */
@JsonView(JacksonViewBean.MyJacksonView3.class)
class JacksonViewBean {

  interface MyJacksonView1 { }

  interface MyJacksonView2 { }

  interface MyJacksonView3 { }

  @JsonView(MyJacksonView1.class)
  private String withView1;

  @JsonView(MyJacksonView2.class)
  private String withView2;

  private String withoutView;

  public String getWithView1() {
    return withView1;
  }

  public void setWithView1(String withView1) {
    this.withView1 = withView1;
  }

  public String getWithView2() {
    return withView2;
  }

  public void setWithView2(String withView2) {
    this.withView2 = withView2;
  }

  public String getWithoutView() {
    return withoutView;
  }

  public void setWithoutView(String withoutView) {
    this.withoutView = withoutView;
  }
}
