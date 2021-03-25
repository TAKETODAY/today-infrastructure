package test.jdbc;

import java.util.List;

import cn.taketoday.context.annotation.Autowired;
import cn.taketoday.context.annotation.Service;
import test.jdbc.model.User;
import test.jdbc.repository.UserRepository;

/**
 * @author TODAY 2021/3/17 18:13
 */
@Service
public class UserService {

  @Autowired
  private UserRepository repository;

  public void saveUser(User user) {
    repository.save(user);
  }

  public void deleteUser(User user) {
    repository.deleteById(user.getId());
  }

  public void updateUser(User user) {
    repository.update(user);
  }

  public List<User> getAll() {
    return repository.findAll();
  }

  public User getById(Integer id) {
    return repository.findById(id);
  }
}
