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

package cn.taketoday.test.context.junit4.orm;

import org.assertj.core.api.Assertions;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.Before;
import org.junit.Test;
import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.test.context.ContextConfiguration;
import cn.taketoday.test.context.junit.jupiter.orm.JpaEntityListenerTests;
import cn.taketoday.test.context.junit4.AbstractTransactionalJUnit4ContextTests;
import cn.taketoday.test.context.junit4.orm.domain.DriversLicense;
import cn.taketoday.test.context.junit4.orm.domain.Person;
import cn.taketoday.test.context.junit4.orm.service.PersonService;
import cn.taketoday.test.transaction.TransactionAssert;
import cn.taketoday.transaction.annotation.Transactional;

import cn.taketoday.w.test.context.junit.jupiter.orm.JpaEntityListenerTests;
import jakarta.persistence.PersistenceException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Transactional integration tests regarding <i>manual</i> session flushing with
 * Hibernate.
 *
 * @author Sam Brannen
 * @author Juergen Hoeller
 * @author Vlad Mihalcea
 * @since 3.0
 * @see JpaEntityListenerTests
 */
@ContextConfiguration
public class HibernateSessionFlushingTests extends AbstractTransactionalJUnit4ContextTests {

	private static final String SAM = "Sam";
	private static final String JUERGEN = "Juergen";

	@Autowired
	private PersonService personService;

	@Autowired
	private SessionFactory sessionFactory;


	@Before
	public void setup() {
		TransactionAssert.assertThatTransaction().isActive();
		assertThat(personService).as("PersonService should have been autowired.").isNotNull();
		assertThat(sessionFactory).as("SessionFactory should have been autowired.").isNotNull();
	}


	@Test
	public void findSam() {
		Person sam = personService.findByName(SAM);
		assertThat(sam).as("Should be able to find Sam").isNotNull();
		DriversLicense driversLicense = sam.getDriversLicense();
		assertThat(driversLicense).as("Sam's driver's license should not be null").isNotNull();
		assertThat(driversLicense.getNumber()).as("Verifying Sam's driver's license number").isEqualTo(Long.valueOf(1234));
	}

	@Test  // SPR-16956
	@Transactional(readOnly = true)
	public void findSamWithReadOnlySession() {
		Person sam = personService.findByName(SAM);
		sam.setName("Vlad");
		// By setting setDefaultReadOnly(true), the user can no longer modify any entity...
		Session session = sessionFactory.getCurrentSession();
		session.flush();
		session.refresh(sam);
		assertThat(sam.getName()).isEqualTo("Sam");
	}

	@Test
	public void saveJuergenWithDriversLicense() {
		DriversLicense driversLicense = new DriversLicense(2L, 2222L);
		Person juergen = new Person(JUERGEN, driversLicense);
		int numRows = countRowsInTable("person");
		personService.save(juergen);
		assertThat(countRowsInTable("person")).as("Verifying number of rows in the 'person' table.").isEqualTo((numRows + 1));
		Assertions.assertThat(personService.findByName(JUERGEN)).as("Should be able to save and retrieve Juergen").isNotNull();
		assertThat(juergen.getId()).as("Juergen's ID should have been set").isNotNull();
	}

	@Test
	public void saveJuergenWithNullDriversLicense() {
		assertThatExceptionOfType(ConstraintViolationException.class).isThrownBy(() ->
				personService.save(new Person(JUERGEN)));
	}

	@Test
	// no expected exception!
	public void updateSamWithNullDriversLicenseWithoutSessionFlush() {
		updateSamWithNullDriversLicense();
		// False positive, since an exception will be thrown once the session is
		// finally flushed (i.e., in production code)
	}

	@Test
	public void updateSamWithNullDriversLicenseWithSessionFlush() throws Throwable {
		updateSamWithNullDriversLicense();
		assertThatExceptionOfType(ConstraintViolationException.class).isThrownBy(() -> {
			// Manual flush is required to avoid false positive in test
			try {
				sessionFactory.getCurrentSession().flush();
			}
			catch (PersistenceException ex) {
				// Wrapped in Hibernate 5.2, with the constraint violation as cause
				throw ex.getCause();
			}
		});
	}

	private void updateSamWithNullDriversLicense() {
		Person sam = personService.findByName(SAM);
		assertThat(sam).as("Should be able to find Sam").isNotNull();
		sam.setDriversLicense(null);
		personService.save(sam);
	}

}
