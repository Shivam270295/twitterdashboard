package com.swym.dashboard.service;

import java.util.List;
import java.util.concurrent.CountDownLatch;

import twitter4j.Paging;
import twitter4j.Status;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.User;

public class TweetFetchThread implements Runnable{

	private List<Status> result;
	private Paging paging;
	private User user;
	private CountDownLatch countDownLatch;
	
	public TweetFetchThread(List<Status> result, Paging paging, User user,CountDownLatch countDownLatch) {
		this.result = result;
		this.paging = paging;
		this.user= user;
		this.countDownLatch=countDownLatch;
	}


	@Override
	public void run() {
		try {
			result.addAll(TwitterFactory.getSingleton().getUserTimeline(user.getId(),paging));
			countDownLatch.countDown();
		} catch (TwitterException e) {
			e.printStackTrace();
		}
	}

}
