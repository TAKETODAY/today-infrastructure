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
package cn.taketoday.orm.mybatis.submitted.xa;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.test.context.junit.jupiter.InfraExtension;
import cn.taketoday.test.context.junit.jupiter.JUnitConfig;
import jakarta.transaction.UserTransaction;

import static org.assertj.core.api.Assertions.assertThat;

@Disabled
@ExtendWith(InfraExtension.class)
@JUnitConfig(locations = "classpath:cn/taketoday/orm/mybatis/submitted/xa/applicationContext.xml")
class UserServiceTest {

  @Autowired
  UserTransaction userTransaction;

  @Autowired
  private UserService userService;

  @Test
  void testCommit() {
    User user = new User(1, "Pocoyo");
    userService.saveWithNoFailure(user);
    assertThat(userService.checkUserExists(user.getId())).isTrue();
  }

  @Test
  void testRollback() {
    User user = new User(2, "Pocoyo");
    try {
      userService.saveWithFailure(user);
    }
    catch (RuntimeException ignore) {
      // ignored
    }
    assertThat(userService.checkUserExists(user.getId())).isFalse();
  }

  @Test
  void testCommitWithExistingTx() throws Exception {
    userTransaction.begin();
    User user = new User(3, "Pocoyo");
    userService.saveWithNoFailure(user);
    userTransaction.commit();
    assertThat(userService.checkUserExists(user.getId())).isTrue();
  }

  // TODO when the outer JTA tx is rolledback,
  // SqlSession should be rolledback but it is committed
  // because Infra calls beforeCommmit from its TX interceptor
  // then, the JTA TX may be rolledback.
  @Test
  void testRollbackWithExistingTx() throws Exception {
    userTransaction.begin();
    User user = new User(5, "Pocoyo");
    userService.saveWithNoFailure(user);
    userTransaction.rollback();
    assertThat(userService.checkUserExists(user.getId())).isFalse();
  }

}
