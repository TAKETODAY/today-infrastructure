/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.test.context.junit4.orm.service.impl;

import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.stereotype.Service;
import cn.taketoday.test.context.junit4.orm.domain.Person;
import cn.taketoday.test.context.junit4.orm.repository.PersonRepository;
import cn.taketoday.test.context.junit4.orm.service.PersonService;
import cn.taketoday.transaction.annotation.Transactional;

/**
 * Standard implementation of the {@link PersonService} API.
 *
 * @author Sam Brannen
 * @since 3.0
 */
@Service
@Transactional(readOnly = true)
public class StandardPersonService implements PersonService {

	private final PersonRepository personRepository;


	@Autowired
	public StandardPersonService(PersonRepository personRepository) {
		this.personRepository = personRepository;
	}

	@Override
	public Person findByName(String name) {
		return this.personRepository.findByName(name);
	}

	@Override
	@Transactional(readOnly = false)
	public Person save(Person person) {
		return this.personRepository.save(person);
	}

}
