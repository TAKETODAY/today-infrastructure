/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.context.annotation;

import org.junit.jupiter.api.Test;

import cn.taketoday.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Juergen Hoeller
 * @author Oliver Gierke
 */
public class Spr16179Tests {

  @Test
  public void repro() {
    AnnotationConfigApplicationContext bf =
            new AnnotationConfigApplicationContext(AssemblerConfig.class, AssemblerInjection.class);

    assertThat(bf.getBean(AssemblerInjection.class).assembler0).isSameAs(bf.getBean("someAssembler"));
    // assertNull(bf.getBean(AssemblerInjection.class).assembler1);  TODO: accidental match
    // assertNull(bf.getBean(AssemblerInjection.class).assembler2);
    assertThat(bf.getBean(AssemblerInjection.class).assembler3).isSameAs(bf.getBean("pageAssembler"));
    assertThat(bf.getBean(AssemblerInjection.class).assembler4).isSameAs(bf.getBean("pageAssembler"));
    assertThat(bf.getBean(AssemblerInjection.class).assembler5).isSameAs(bf.getBean("pageAssembler"));
    assertThat(bf.getBean(AssemblerInjection.class).assembler6).isSameAs(bf.getBean("pageAssembler"));
  }

  @Configuration
  static class AssemblerConfig {

    @Bean
    PageAssemblerImpl<?> pageAssembler() {
      return new PageAssemblerImpl<>();
    }

    @Bean
    Assembler<SomeType> someAssembler() {
      return new Assembler<SomeType>() { };
    }
  }

  public static class AssemblerInjection {

    @Autowired(required = false)
    Assembler<SomeType> assembler0;

    @Autowired(required = false)
    Assembler<SomeOtherType> assembler1;

    @Autowired(required = false)
    Assembler<Page<String>> assembler2;

    @Autowired(required = false)
    @SuppressWarnings("rawtypes")
    Assembler<Page> assembler3;

    @Autowired(required = false)
    Assembler<Page<?>> assembler4;

    @Autowired(required = false)
    PageAssembler<?> assembler5;

    @Autowired(required = false)
    PageAssembler<String> assembler6;
  }

  interface Assembler<T> { }

  interface PageAssembler<T> extends Assembler<Page<T>> { }

  static class PageAssemblerImpl<T> implements PageAssembler<T> { }

  interface Page<T> { }

  interface SomeType { }

  interface SomeOtherType { }

}
