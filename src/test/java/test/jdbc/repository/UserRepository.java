package test.jdbc.repository;

import java.util.List;

import cn.taketoday.context.annotation.Repository;
import cn.taketoday.jdbc.annotation.Delete;
import cn.taketoday.jdbc.annotation.Insert;
import cn.taketoday.jdbc.annotation.Select;
import cn.taketoday.jdbc.annotation.Update;
import test.jdbc.model.User;

/**
 * @author TODAY 2021/3/17 17:30
 */
@Repository
public interface UserRepository {

  @Insert("insert into `t_user` (`id`,`name`,`age`) values (:id, :name, :age)")
  void save(User user);

  @Update("Update `t_user` set name = :name where id=:id")
  void update(User user);

  @Delete("DELETE FROM `t_user` WHERE id=:id")
  void deleteById(Integer id);

  @Select("SELECT * from `t_user`")
  List<User> findAll();

  @Select("SELECT * from `t_user` where id=:id")
  User findById(Integer id);
}
