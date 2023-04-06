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

package cn.taketoday.test.web.servlet.samples.spr;

import org.junit.jupiter.api.Test;

import cn.taketoday.http.MediaType;
import cn.taketoday.test.web.servlet.MockMvc;
import cn.taketoday.test.web.servlet.setup.MockMvcBuilders;
import cn.taketoday.web.annotation.PutMapping;
import cn.taketoday.web.annotation.RestController;
import cn.taketoday.web.servlet.filter.FormContentFilter;

import static cn.taketoday.test.web.servlet.request.MockMvcRequestBuilders.put;
import static cn.taketoday.test.web.servlet.result.MockMvcResultMatchers.content;

/**
 * Test for issues related to form content.
 *
 * @author Rossen Stoyanchev
 */
public class FormContentTests {

  @Test // SPR-15753
  public void formContentIsNotDuplicated() throws Exception {

    MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new Spr15753Controller())
            .addFilter(new FormContentFilter())
            .build();

    mockMvc.perform(put("/").content("d1=a&d2=s").contentType(MediaType.APPLICATION_FORM_URLENCODED))
            .andExpect(content().string("d1:a, d2:s."));
  }

  @RestController
  private static class Spr15753Controller {

    @PutMapping("/")
    public String test(Data d) {
      return String.format("d1:%s, d2:%s.", d.getD1(), d.getD2());
    }
  }

  @SuppressWarnings("unused")
  private static class Data {

    private String d1;

    private String d2;

    public Data() {
    }

    public String getD1() {
      return d1;
    }

    public void setD1(String d1) {
      this.d1 = d1;
    }

    public String getD2() {
      return d2;
    }

    public void setD2(String d2) {
      this.d2 = d2;
    }
  }

}
