package cn.taketoday.test.service.impl;

import cn.taketoday.context.annotation.Autowired;
import cn.taketoday.context.annotation.Service;
import cn.taketoday.test.dao.UserDao;
import cn.taketoday.test.domain.User;
import cn.taketoday.test.service.UserService;


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











