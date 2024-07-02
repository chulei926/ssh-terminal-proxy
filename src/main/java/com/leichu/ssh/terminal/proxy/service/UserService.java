package com.leichu.ssh.terminal.proxy.service;

import com.leichu.ssh.terminal.proxy.model.User;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

@Service("userService")
public class UserService {

	private static final Map<String, User> TMP_USER = new HashMap<>();

	@PostConstruct
	public void init() {
		TMP_USER.put("u1", new User(1L, "u1", "u1"));
		TMP_USER.put("u2", new User(2L, "u2", "u2"));
		TMP_USER.put("u3", new User(3L, "u3", "u3"));
	}

	public User getUser(String username) {
		return TMP_USER.get(username);
	}


}
