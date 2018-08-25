package com.swym.dashboard.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import static com.swym.dashboard.util.DashboardConfig.*;
import com.swym.dashboard.service.UserService;
import com.swym.dashboard.service.UserService.TweetDimension;
import com.swym.dashboard.service.UserService.UserDimension;

import twitter4j.HashtagEntity;
import twitter4j.Status;
import twitter4j.User;
import twitter4j.UserMentionEntity;

public class InMemoryAggregator {
	public static HashMap<String, List<User>> followerCountMap = new HashMap<>();
	public static HashMap<String, List<User>> tweetCountMap = new HashMap<>();
	public static HashMap<String, List<Map.Entry<String, List<Status>>>> popularTweetsByHashTagMap = new HashMap<>();
	public static HashMap<String, List<Map.Entry<String, List<Status>>>> popularTweetsByMentionMap = new HashMap<>();
	public static HashMap<String, List<Map.Entry<String, List<Status>>>> popularTweetsByRetweetMap = new HashMap<>();

	public static List<User> calculateTopUsers(List<User> users, User searchUser, UserDimension userDimension) {
		TreeMap<Integer, List<User>> mapFollowerCount = new TreeMap<>(Collections.reverseOrder());
		TreeMap<Integer, List<User>> mapTweetCount = new TreeMap<>(Collections.reverseOrder());

		for (User user : users) {
			if (mapFollowerCount.containsKey(user.getFollowersCount())) {
				mapFollowerCount.get(user.getFollowersCount()).add(user);
			} else {
				ArrayList<User> mapUsers = new ArrayList<>();
				mapUsers.add(user);
				mapFollowerCount.put(user.getFollowersCount(), mapUsers);
			}

			if (mapTweetCount.containsKey(user.getStatusesCount())) {
				mapTweetCount.get(user.getStatusesCount()).add(user);
			} else {
				ArrayList<User> mapUsers = new ArrayList<>();
				mapUsers.add(user);
				mapTweetCount.put(user.getStatusesCount(), mapUsers);
			}
		}

		LinkedList<User> resultUserFollowerCount = new LinkedList<>();
		LinkedList<User> resultUserTweetCount = new LinkedList<>();

		int userCount = 1;
		for (Map.Entry<Integer, List<User>> entry : mapFollowerCount.entrySet()) {
			resultUserFollowerCount.addAll(entry.getValue());
			userCount += entry.getValue().size();
			if (userCount > MAX_AGGR_RESULT)
				break;
		}

		userCount = 1;
		for (Map.Entry<Integer, List<User>> entry : mapTweetCount.entrySet()) {
			resultUserTweetCount.addAll(entry.getValue());
			userCount += entry.getValue().size();
			if (userCount > MAX_AGGR_RESULT)
				break;
		}

		followerCountMap.put(searchUser.getScreenName(), resultUserFollowerCount);
		tweetCountMap.put(searchUser.getScreenName(), resultUserTweetCount);
		if (userDimension.equals(UserDimension.FOLLOWERS))
			return resultUserFollowerCount;
		else
			return resultUserTweetCount;
	}

	@SuppressWarnings("unchecked")
	public static List<Map.Entry<String, List<Status>>> calculateTopTweetTopics(List<Status> result, User user,
			UserService.TweetDimension tweetDimension) {

		Map<String, List<Status>> hashtagMap = new HashMap<>();
		Map<String, List<Status>> mentionMap = new HashMap<>();
		Map<String, List<Status>> retweetMap = new HashMap<>();

		List<Map.Entry<String, List<Status>>> hashTagEntryList = new LinkedList<>();
		List<Map.Entry<String, List<Status>>> mentionEntryList = new LinkedList<>();

		for (Status status : result) {
			for (HashtagEntity hashTag : status.getHashtagEntities()) {
				if (!hashtagMap.containsKey(hashTag.getText())) {
					LinkedList<Status> tweetList = new LinkedList<>();
					tweetList.add(status);
					hashtagMap.put(hashTag.getText(), tweetList);
				} else {
					hashtagMap.get(hashTag.getText()).add(status);
				}

			}

			for (UserMentionEntity userMention : status.getUserMentionEntities()) {
				if (!mentionMap.containsKey(userMention.getScreenName())) {
					LinkedList<Status> tweetList = new LinkedList<>();
					tweetList.add(status);
					mentionMap.put(userMention.getScreenName(), tweetList);
				} else {
					mentionMap.get(userMention.getScreenName()).add(status);
				}

			}

		}

		hashTagEntryList.addAll(hashtagMap.entrySet());
		Collections.sort(hashTagEntryList, (entry1, entry2) -> {
			return -1 * Integer.compare(((List<Status>) entry1.getValue()).size(),
					((List<Status>) entry2.getValue()).size());
		});

		mentionEntryList.addAll(mentionMap.entrySet());
		Collections.sort(mentionEntryList, (entry1, entry2) -> {
			return -1 * Integer.compare(((List<Status>) entry1.getValue()).size(),
					((List<Status>) entry2.getValue()).size());
		});

		Collections.sort(result, (status1, status2) -> {
			return -1 * Integer.compare(status1.getRetweetCount(), status2.getRetweetCount());
		});
		retweetMap.put(user.getScreenName(),
				result.size() < MAX_AGGR_RESULT ? result : result.subList(0, MAX_AGGR_RESULT));
		LinkedList<Map.Entry<String, List<Status>>> retweetList = new LinkedList<Map.Entry<String, List<Status>>>(
				retweetMap.entrySet());

		popularTweetsByHashTagMap.put(user.getScreenName(), hashTagEntryList.size() < MAX_AGGR_RESULT ? hashTagEntryList
				: hashTagEntryList.subList(0, MAX_AGGR_RESULT));
		popularTweetsByMentionMap.put(user.getScreenName(), mentionEntryList.size() < MAX_AGGR_RESULT ? mentionEntryList
				: mentionEntryList.subList(0, MAX_AGGR_RESULT));
		popularTweetsByRetweetMap.put(user.getScreenName(), retweetList);

		if (tweetDimension.equals(TweetDimension.HASHTAG))
			return hashTagEntryList.size() < MAX_AGGR_RESULT ? hashTagEntryList
					: hashTagEntryList.subList(0, MAX_AGGR_RESULT);
		else if (tweetDimension.equals(TweetDimension.RETWEETS))
			return mentionEntryList.size() < MAX_AGGR_RESULT ? mentionEntryList
					: mentionEntryList.subList(0, MAX_AGGR_RESULT);
		else
			return retweetList;
	}
}
