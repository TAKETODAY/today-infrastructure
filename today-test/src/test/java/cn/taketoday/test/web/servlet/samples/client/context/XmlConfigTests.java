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

package cn.taketoday.test.web.servlet.samples.client.context;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.http.MediaType;
import cn.taketoday.test.context.ContextConfiguration;
import cn.taketoday.test.context.ContextHierarchy;
import cn.taketoday.test.context.junit.jupiter.InfraExtension;
import cn.taketoday.test.context.web.WebAppConfiguration;
import cn.taketoday.test.web.Person;
import cn.taketoday.test.web.reactive.server.WebTestClient;
import cn.taketoday.test.web.servlet.client.MockMvcWebTestClient;
import cn.taketoday.test.web.servlet.samples.context.PersonDao;
import cn.taketoday.web.servlet.WebApplicationContext;

import static org.mockito.BDDMockito.given;

/**
 * {@link MockMvcWebTestClient} equivalent of the MockMvc
 * {@link cn.taketoday.test.web.servlet.samples.context.XmlConfigTests}.
 *
 * @author Rossen Stoyanchev
 */
@ExtendWith(InfraExtension.class)
@WebAppConfiguration("src/test/resources/META-INF/web-resources")
@ContextHierarchy({
        @ContextConfiguration("../../context/root-context.xml"),
        @ContextConfiguration("../../context/servlet-context.xml")
})
public class XmlConfigTests {

  @Autowired
  private WebApplicationContext wac;

  @Autowired
  private PersonDao personDao;

  private WebTestClient testClient;

  @BeforeEach
  public void setup() {
    this.testClient = MockMvcWebTestClient.bindToApplicationContext(this.wac).build();
    given(this.personDao.getPerson(5L)).willReturn(new Person("Joe"));
  }

  @Test
  public void person() {
    testClient.get().uri("/person/5")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isOk()
            .expectBody().json("{\"name\":\"Joe\",\"someDouble\":0.0,\"someBoolean\":false}");
  }

}
