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

package infra.context.testfixture.context.annotation;

import java.util.ArrayList;
import java.util.List;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

public class InitDestroyComponent {

  public final List<String> events = new ArrayList<>();

  @PostConstruct
  public void init() {
    this.events.add("init");
  }

  public void customInit() {
    this.events.add("customInit");
  }

  @PreDestroy
  public void destroy() {
    this.events.add("destroy");
  }

  public void customDestroy() {
    this.events.add("customDestroy");
  }

}
