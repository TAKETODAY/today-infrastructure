package test.service.impl;

import cn.taketoday.context.annotation.Autowired;
import cn.taketoday.context.annotation.Service;
import test.dao.UserDao;
import test.domain.User;
import test.service.UserService;


@Service
public final class UserServiceImpl implements UserService {

	@Autowired
	private UserDao userDao;

	
	@Override
	public User login(User user) {
		return userDao.login(user);
	}

	@Override
	public boolean register(User user) {
		return userDao.save(user);
	}


}











