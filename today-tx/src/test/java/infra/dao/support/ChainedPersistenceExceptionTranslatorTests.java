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

package infra.dao.support;

import org.junit.jupiter.api.Test;

import infra.dao.InvalidDataAccessApiUsageException;
import infra.dao.OptimisticLockingFailureException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Rod Johnson
 * @since 2.0
 */
public class ChainedPersistenceExceptionTranslatorTests {

  @Test
  public void empty() {
    ChainedPersistenceExceptionTranslator pet = new ChainedPersistenceExceptionTranslator();
    //MapPersistenceExceptionTranslator mpet = new MapPersistenceExceptionTranslator();
    RuntimeException in = new RuntimeException("in");
    assertThat(DataAccessUtils.translateIfNecessary(in, pet)).isSameAs(in);
  }

  @Test
  public void exceptionTranslationWithTranslation() {
    MapPersistenceExceptionTranslator mpet1 = new MapPersistenceExceptionTranslator();
    RuntimeException in1 = new RuntimeException("in");
    InvalidDataAccessApiUsageException out1 = new InvalidDataAccessApiUsageException("out");
    InvalidDataAccessApiUsageException out2 = new InvalidDataAccessApiUsageException("out");
    mpet1.addTranslation(in1, out1);

    ChainedPersistenceExceptionTranslator chainedPet1 = new ChainedPersistenceExceptionTranslator();
    assertThat(DataAccessUtils.translateIfNecessary(in1, chainedPet1)).as("Should not translate yet").isSameAs(in1);
    chainedPet1.addDelegate(mpet1);
    assertThat(DataAccessUtils.translateIfNecessary(in1, chainedPet1)).as("Should now translate").isSameAs(out1);

    // Now add a new translator and verify it wins
    MapPersistenceExceptionTranslator mpet2 = new MapPersistenceExceptionTranslator();
    mpet2.addTranslation(in1, out2);
    chainedPet1.addDelegate(mpet2);
    assertThat(DataAccessUtils.translateIfNecessary(in1, chainedPet1)).as("Should still translate the same due to ordering").isSameAs(out1);

    ChainedPersistenceExceptionTranslator chainedPet2 = new ChainedPersistenceExceptionTranslator();
    chainedPet2.addDelegate(mpet2);
    chainedPet2.addDelegate(mpet1);
    assertThat(DataAccessUtils.translateIfNecessary(in1, chainedPet2)).as("Should translate differently due to ordering").isSameAs(out2);

    RuntimeException in2 = new RuntimeException("in2");
    OptimisticLockingFailureException out3 = new OptimisticLockingFailureException("out2");
    assertThat(chainedPet2.translateExceptionIfPossible(in2)).isNull();
    MapPersistenceExceptionTranslator mpet3 = new MapPersistenceExceptionTranslator();
    mpet3.addTranslation(in2, out3);
    chainedPet2.addDelegate(mpet3);
    assertThat(chainedPet2.translateExceptionIfPossible(in2)).isSameAs(out3);
  }

}
