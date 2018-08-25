package com.swym.dashboard.service;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.User;

public class UserFetchThread implements Runnable {
	private List<User> userList;
	private long[] friendIds;
	private int start;
	private int end;
	private CountDownLatch countDownLatch;

	public UserFetchThread(List<User> userList, long[] friendIds, int start, int end,
			CountDownLatch countDownLatch) {
		this.userList = userList;
		this.friendIds = friendIds;
		this.start = start;
		this.end = end;
		this.countDownLatch = countDownLatch;
	}

	@Override
	public void run() {
		Twitter twitter = TwitterFactory.getSingleton();
		try {
			List<User> friends = twitter.lookupUsers(Arrays.copyOfRange(friendIds, start, end));
			userList.addAll(friends);
			countDownLatch.countDown();
		} catch (TwitterException e) {
			e.printStackTrace();
		}
	}

}