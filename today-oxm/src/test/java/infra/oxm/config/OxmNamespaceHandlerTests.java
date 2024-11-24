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

package infra.oxm.config;

import org.junit.jupiter.api.Test;

import infra.context.ApplicationContext;
import infra.context.support.ClassPathXmlApplicationContext;
import infra.oxm.jaxb.Jaxb2Marshaller;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the {@link OxmNamespaceHandler} class.
 *
 * @author Arjen Poustma
 * @author Jakub Narloch
 * @author Sam Brannen
 */
public class OxmNamespaceHandlerTests {

  private final ApplicationContext applicationContext =
          new ClassPathXmlApplicationContext("oxmNamespaceHandlerTest.xml", getClass());

  @Test
  public void jaxb2ContextPathMarshaller() {
    Jaxb2Marshaller jaxb2Marshaller = applicationContext.getBean("jaxb2ContextPathMarshaller", Jaxb2Marshaller.class);
    assertThat(jaxb2Marshaller).isNotNull();
  }

  @Test
  public void jaxb2ClassesToBeBoundMarshaller() {
    Jaxb2Marshaller jaxb2Marshaller = applicationContext.getBean("jaxb2ClassesMarshaller", Jaxb2Marshaller.class);
    assertThat(jaxb2Marshaller).isNotNull();
  }

}
