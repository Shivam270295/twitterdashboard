package com.swym.dashboard.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.swym.dashboard.util.DashboardConfig.*;
import com.swym.dashboard.util.InMemoryAggregator;

import twitter4j.IDs;
import twitter4j.Paging;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.User;

public class InMemoryUserService implements UserService {

	private static ExecutorService userFetchExecutor = Executors.newFixedThreadPool(USER_FETCH_THREAD_POOL);
	private static ExecutorService pageFetchExecutor = Executors.newFixedThreadPool(PAGE_FETCH_THREAD_POOL);

	private Twitter twitter = TwitterFactory.getSingleton();

	@Override
	public List<User> getTopTwitterFriends(User user, UserDimension popularityMetric) {
		IDs ids = null;
		long[] friendIds;

		if (InMemoryAggregator.followerCountMap.containsKey(user.getScreenName())
				&& popularityMetric.equals(UserDimension.FOLLOWERS))
			return InMemoryAggregator.followerCountMap.get(user.getScreenName());
		else if (InMemoryAggregator.tweetCountMap.containsKey(user.getScreenName()))
			return InMemoryAggregator.tweetCountMap.get(user.getScreenName());

		List<User> resultUsers = Collections.synchronizedList(new ArrayList<User>());

		int noOfFriends = user.getFriendsCount();

		int latchCount = noOfFriends % USER_PER_PAGE == 0 ? noOfFriends / USER_PER_PAGE
				: (noOfFriends / USER_PER_PAGE) + 1;
		CountDownLatch countDownLatch = new CountDownLatch(latchCount);
		do {
			try {
				ids = twitter.getFriendsIDs(user.getId(), -1);
				friendIds = ids.getIDs();
				for (int j = 0; j < latchCount; j++) {
					int end = USER_PER_PAGE * (j + 1) < friendIds.length ? USER_PER_PAGE * (j + 1) : friendIds.length;
					userFetchExecutor.execute(
							new UserFetchThread(resultUsers, friendIds, (USER_PER_PAGE * j) + 1, end, countDownLatch));
				}

			} catch (TwitterException e) {
				e.printStackTrace();
			}

		} while (ids.hasNext());

		try {
			countDownLatch.await();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		return InMemoryAggregator.calculateTopUsers(resultUsers, user, popularityMetric);
	}

	@Override
	public User getUserByTwitterId(String twitterId) {
		List<User> resultUsers = Collections.emptyList();
		try {
			resultUsers = twitter.searchUsers(twitterId, 1);
			for (User searchedUser : resultUsers) {
				if (searchedUser.getScreenName().equals(twitterId)) {
					return searchedUser;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public List<Map.Entry<String, List<Status>>> getTopUserTweets(User user, TweetDimension tweetDimension) {
		List<Status> result = Collections.synchronizedList(new LinkedList<>());
		if (tweetDimension.equals(TweetDimension.HASHTAG)
				&& InMemoryAggregator.popularTweetsByHashTagMap.containsKey(user.getScreenName()))
			return InMemoryAggregator.popularTweetsByHashTagMap.get(user.getScreenName());
		else if (tweetDimension.equals(TweetDimension.USER_MENTION)
				&& InMemoryAggregator.popularTweetsByMentionMap.containsKey(user.getScreenName()))
			return InMemoryAggregator.popularTweetsByMentionMap.get(user.getScreenName());
		else if (tweetDimension.equals(TweetDimension.RETWEETS)
				&& InMemoryAggregator.popularTweetsByRetweetMap.containsKey(user.getScreenName()))
			return InMemoryAggregator.popularTweetsByRetweetMap.get(user.getScreenName());

		CountDownLatch countDownLatch = new CountDownLatch(STATUS_PAGE_LIMIT);
		for (int i = 1; i <= STATUS_PAGE_LIMIT; i++) {
			Paging paging = new Paging(i, STATUS_PER_PAGE);
			pageFetchExecutor.execute(new TweetFetchThread(result, paging, user, countDownLatch));
		}

		try {
			countDownLatch.await();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		List<Map.Entry<String, List<Status>>> tweets = InMemoryAggregator.calculateTopTweetTopics(result, user,
				tweetDimension);

		return tweets;
	}

}
