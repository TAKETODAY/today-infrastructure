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

package infra.app.test.mock.mockito.example;

/**
 * Example bean for mocking tests that calls {@link ExampleGenericService}.
 *
 * @author Phillip Webb
 */
public class ExampleGenericServiceCaller {

  private final ExampleGenericService<Integer> integerService;

  private final ExampleGenericService<String> stringService;

  public ExampleGenericServiceCaller(ExampleGenericService<Integer> integerService,
          ExampleGenericService<String> stringService) {
    this.integerService = integerService;
    this.stringService = stringService;
  }

  public ExampleGenericService<Integer> getIntegerService() {
    return this.integerService;
  }

  public ExampleGenericService<String> getStringService() {
    return this.stringService;
  }

  public String sayGreeting() {
    return "I say " + this.integerService.greeting() + " " + this.stringService.greeting();
  }

}
