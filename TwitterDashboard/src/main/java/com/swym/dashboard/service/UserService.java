package com.swym.dashboard.service;

import java.util.List;
import java.util.Map;

import twitter4j.Status;
import twitter4j.User;

public interface UserService {
	public enum UserDimension {
		FOLLOWERS, TWEETS
	};

	public enum TweetDimension {
		HASHTAG, USER_MENTION, RETWEETS
	};

	public User getUserByTwitterId(String twitterId);

	public List<User> getTopTwitterFriends(User user, UserDimension userDimension);

	public List<Map.Entry<String, List<Status>>> getTopUserTweets(User user, TweetDimension tweetDimension);
}
