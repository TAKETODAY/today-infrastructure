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

package infra.test.web.mock.samples.context;

import infra.test.web.Person;
import infra.web.annotation.GetMapping;
import infra.web.annotation.PathVariable;
import infra.web.annotation.RequestMapping;
import infra.web.annotation.RestController;

@RestController
@RequestMapping("/person")
public class PersonController {

  private final PersonDao personDao;

  public PersonController(PersonDao personDao) {
    this.personDao = personDao;
  }

  @GetMapping("/{id}")
  public Person getPerson(@PathVariable long id) {
    return this.personDao.getPerson(id);
  }

}