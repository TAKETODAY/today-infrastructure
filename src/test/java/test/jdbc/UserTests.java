package test.jdbc;

import org.junit.Test;

import java.util.List;

import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.StandardApplicationContext;
import test.jdbc.model.User;

/**
 * @author TODAY 2021/3/17 18:17
 */
public class UserTests {

  ApplicationContext context = new StandardApplicationContext("");

  {
    context.importBeans(RepositoryConfig.class);
    context.load("test.jdbc");

  }

  @Test
  public void test() {
    final UserService userService = context.getBean(UserService.class);

    final List<User> users = userService.getAll();
    System.out.println(users);


    final User byId = userService.getById(1);
    System.out.println(byId);
  }

}
