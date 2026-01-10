/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.test.context.junit.jupiter.web;

import infra.test.context.junit.jupiter.comics.Person;
import infra.web.annotation.GetMapping;
import infra.web.annotation.PathVariable;
import infra.web.annotation.RestController;

/**
 * @author Sam Brannen
 * @since 4.0
 */
@RestController
class PersonController {

  @GetMapping("/person/{id}")
  Person getPerson(@PathVariable long id) {
    if (id == 42) {
      return new Person("Dilbert");
    }
    return new Person("Wally");
  }

}
