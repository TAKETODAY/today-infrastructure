package test.service;

import test.domain.User;

public interface UserService {

	public boolean register(User user);

	public User login(User user);

}
