package cn.taketoday.test.service;

import cn.taketoday.test.domain.User;

public interface UserService {

	public boolean register(User user);

	public User login(User user);

}
