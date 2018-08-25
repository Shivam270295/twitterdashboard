package com.swym.dashboard.ui;

import static com.swym.dashboard.util.DashboardConstants.*;


import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.springframework.beans.factory.annotation.Autowired;

import com.swym.dashboard.service.UserService;
import com.swym.dashboard.service.UserService.TweetDimension;
import com.swym.dashboard.service.UserService.UserDimension;
import com.swym.dashboard.util.DashboardConfig;
import com.vaadin.annotations.PreserveOnRefresh;
import com.vaadin.annotations.Theme;
import com.vaadin.server.ExternalResource;
import com.vaadin.server.VaadinRequest;
import com.vaadin.spring.annotation.SpringUI;
import com.vaadin.spring.annotation.UIScope;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Grid;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Image;
import com.vaadin.ui.Label;
import com.vaadin.ui.Layout;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Panel;
import com.vaadin.ui.PopupView;
import com.vaadin.ui.TabSheet;
import com.vaadin.ui.TextField;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

import twitter4j.Status;
import twitter4j.User;

@Theme("valo")
@SpringUI
@UIScope
@PreserveOnRefresh
public class DashboardUI extends UI {

	private static final long serialVersionUID = 1L;
	Layout baseLayout;

	@Autowired
	UserService userService;

	@SuppressWarnings("deprecation")
	public Layout buildLayout() {
		if (baseLayout == null) {

			baseLayout = new VerticalLayout();
			((VerticalLayout) baseLayout).setMargin(false);

			TextField searchText = new TextField();
			Button searchButton = new Button(LBL_SEARCH_BUTTON);
			constructHeaderLayout(searchButton, searchText, true);

			HorizontalLayout tabSheetLayout = new HorizontalLayout();

			Grid<User> usersByFollowersGrid = new Grid<>();
			Grid<User> usersByTweetsGrid = new Grid<>();

			Grid<Map.Entry<String, List<Status>>> popularTweetsByHashTagGrid = new Grid<>();
			Grid<Map.Entry<String, List<Status>>> popularTweetsByMentionGrid = new Grid<>();
			Grid<Status> popularTweetsByRetweetsGrid = new Grid<>();

			TabSheet mainTabSheet = new TabSheet();
			mainTabSheet.setWidth("700px");

			mainTabSheet.setStyleName(ValoTheme.TABSHEET_CENTERED_TABS);

			TabSheet tabSheetPopularFriends = constructFriendsTabSheet(usersByFollowersGrid, usersByTweetsGrid);

			TabSheet tabSheetPopularTweets = constructTweetsTabSheet(popularTweetsByHashTagGrid,
					popularTweetsByMentionGrid, popularTweetsByRetweetsGrid);

			tabSheetPopularFriends.setStyleName(ValoTheme.TABSHEET_CENTERED_TABS);
			tabSheetPopularTweets.setStyleName(ValoTheme.TABSHEET_CENTERED_TABS);

			mainTabSheet.addTab(tabSheetPopularFriends, TAB_LBL_POPULAR_FRIEND);
			mainTabSheet.addTab(tabSheetPopularTweets, TAB_LBL_POPULAR_TWEETS);

			searchButton.addClickListener(event -> {
				User curUser = userService.getUserByTwitterId(searchText.getValue().trim());
				if (curUser != null) {
					List<User> usersbyFollowers = userService.getTopTwitterFriends(curUser, UserDimension.FOLLOWERS);
					usersByFollowersGrid.setCaption(GRID_LBL_TOP_FOLLOWERS + curUser.getScreenName());

					usersByFollowersGrid.setItems(usersbyFollowers);

					List<User> usersbyTweets = userService.getTopTwitterFriends(curUser, UserDimension.TWEETS);
					usersByTweetsGrid.setItems(usersbyTweets);

					List<Map.Entry<String, List<Status>>> popularTweetsByHashTag = userService.getTopUserTweets(curUser,
							TweetDimension.HASHTAG);
					popularTweetsByHashTagGrid.setItems(popularTweetsByHashTag);

					List<Map.Entry<String, List<Status>>> popularTweetsByMention = userService.getTopUserTweets(curUser,
							TweetDimension.USER_MENTION);
					popularTweetsByMentionGrid.setItems(popularTweetsByMention);

					List<Map.Entry<String, List<Status>>> popularTweetsByRetweet = userService.getTopUserTweets(curUser,
							TweetDimension.RETWEETS);
					if (popularTweetsByRetweet.size() > 0)
						popularTweetsByRetweetsGrid.setItems(popularTweetsByRetweet.get(0).getValue());

					Layout userInfoLayout = constructUserInfo(curUser);
					

					tabSheetLayout.removeAllComponents();
					baseLayout.removeAllComponents();
					constructHeaderLayout(searchButton, searchText, false);

					tabSheetLayout.setSpacing(true);
					tabSheetLayout.addComponent(userInfoLayout);
					tabSheetLayout.setComponentAlignment(userInfoLayout, Alignment.TOP_LEFT);

					tabSheetLayout.addComponent(mainTabSheet);
					baseLayout.addComponent(tabSheetLayout);

					VerticalLayout sideLayout = constructSideLayout(curUser);
					sideLayout.setSpacing(false);
					sideLayout.addComponentAsFirst(constructUserStats(curUser));
					tabSheetLayout.addComponent(sideLayout);

				} else {
					Notification.show(ERR_ID_DOESNT_EXIST, Notification.TYPE_WARNING_MESSAGE);
				}

			});

		}

		return baseLayout;

	}

	private VerticalLayout constructSideLayout(User curUser) {
		VerticalLayout sideLayout = new VerticalLayout();
		Panel panel = new Panel();
		panel.setCaption(PANEL_LBL_LATEST_TWEET);
		panel.setWidth("300px");
		panel.setHeight("260px");
		Label label = new Label(curUser.getStatus().getText());
		label.setHeightUndefined();
		label.setWidth("250px");

		panel.setContent(label);
		panel.setStyleName(ValoTheme.PANEL_BORDERLESS);

		sideLayout.addComponent(panel);
		return sideLayout;
	}

	private VerticalLayout constructHeaderLayout(Button searchButton, TextField searchText, boolean initial) {
		searchText.setStyleName(ValoTheme.TEXTFIELD_LARGE);
		searchText.setPlaceholder(SEARCH_PLACEHOLDER);

		searchButton.setStyleName(ValoTheme.BUTTON_LARGE);
		searchButton.setWidth("200px");
		searchButton.setHeight("44px");
		searchButton.setStyleName(ValoTheme.BUTTON_PRIMARY);
		((VerticalLayout) baseLayout).setMargin(false);
		VerticalLayout headerLayout = new VerticalLayout();
		headerLayout.setWidth("100%");

		if (initial) {
			Label dashboradHeading = new Label(DASHBOARD_HEADER);
			dashboradHeading.setStyleName(ValoTheme.LABEL_H1);

			Image img = new Image("", new ExternalResource(DashboardConfig.DASHBOARD_IMG_URL));
			img.setWidth(200, Unit.PIXELS);

			headerLayout.setHeight("100%");
			;
			headerLayout.setSpacing(false);

			Label dashboardHeading = new Label(DASHBOARD_HEADER);
			dashboardHeading.setStyleName(ValoTheme.LABEL_LARGE);
			Label label2 = new Label(DASHBOARD_FOOTER);
			label2.setStyleName(ValoTheme.LABEL_TINY);
			headerLayout.addComponent(dashboardHeading);
			headerLayout.addComponent(label2);
			headerLayout.addComponent(img);

			headerLayout.setComponentAlignment(img, Alignment.MIDDLE_CENTER);
			headerLayout.setComponentAlignment(dashboardHeading, Alignment.MIDDLE_CENTER);
			headerLayout.setComponentAlignment(label2, Alignment.MIDDLE_CENTER);

		}

		HorizontalLayout searchLayout = new HorizontalLayout();

		searchText.setWidth("700px");
		searchLayout.addComponent(searchText);
		searchLayout.addComponent(searchButton);

		headerLayout.addComponent(searchLayout);

		headerLayout.setStyleName(ValoTheme.LAYOUT_WELL);

		headerLayout.setComponentAlignment(searchLayout, Alignment.MIDDLE_CENTER);

		baseLayout.addComponent(headerLayout);

		return headerLayout;
	}

	private TabSheet constructTweetsTabSheet(Grid<Map.Entry<String, List<Status>>> popularTweetsByHashTagGrid,
			Grid<Entry<String, List<Status>>> popularTweetsByMentionGrid, Grid<Status> popularTweetsByRetweetsGrid) {
		TabSheet tabSheet = new TabSheet();
		tabSheet.addTab(popularTweetsByHashTagGrid, TAB_TOP_HASHTAG);
		tabSheet.addTab(popularTweetsByMentionGrid, TAB_TOP_MENTION);
		tabSheet.addTab(popularTweetsByRetweetsGrid, TAB_TOP_RETWEETS);

		popularTweetsByHashTagGrid.addColumn(Map.Entry::getKey).setCaption(COL_LBL_HASHTAG);
		popularTweetsByHashTagGrid.addColumn((entry) -> (entry.getValue()).size()).setCaption(COL_LBL_TWEETNO);

		popularTweetsByHashTagGrid.addComponentColumn(entry -> {
			return getPopupViewForTweets(entry.getValue());
		}).setCaption(COL_LBL_LINK_TWEET);

		popularTweetsByMentionGrid.addColumn(Map.Entry::getKey).setCaption(COL_LBL_MENTION);
		popularTweetsByMentionGrid.addColumn((entry) -> (entry.getValue()).size()).setCaption(COL_LBL_TWEETNO);
		popularTweetsByMentionGrid.addComponentColumn(entry -> {
			return getPopupViewForTweets(entry.getValue());
		}).setCaption(COL_LBL_LINK_TWEET);

		popularTweetsByRetweetsGrid.setRowHeight(50.0);
		popularTweetsByRetweetsGrid.addComponentColumn(status -> {
			HorizontalLayout hl = new HorizontalLayout();
			Label tweetSummary = new Label();
			tweetSummary.setValue(status.getText());
			tweetSummary.setWidth(300, Unit.PIXELS);
			tweetSummary.setStyleName(ValoTheme.LAYOUT_HORIZONTAL_WRAPPING);

			Label tweet = new Label();
			tweet.setValue(status.getText());
			tweet.setWidth(300, Unit.PIXELS);
			VerticalLayout popupLayout = new VerticalLayout();
			popupLayout.addComponent(tweet);
			PopupView popUpView = new PopupView(LBL_READMORE_BUTTON, popupLayout);

			hl.addComponent(tweetSummary);
			hl.addComponent(popUpView);
			return hl;
		}).setCaption(COL_LBL_TWEET).setWidth(400);

		popularTweetsByRetweetsGrid.addColumn(Status::getRetweetCount).setCaption(COL_LBL_RETWEETCOUNT);

		popularTweetsByHashTagGrid.setSizeFull();
		popularTweetsByMentionGrid.setSizeFull();
		popularTweetsByRetweetsGrid.setSizeFull();

		return tabSheet;
	}

	private PopupView getPopupViewForTweets(List<Status> tweets) {
		VerticalLayout popupLayout = new VerticalLayout();
		Panel panel = new Panel(LBL_TWEETS);
		for (Status status : tweets) {
			Label label = new Label();
			label.setValue(status.getText());
			label.setStyleName(ValoTheme.LABEL_SMALL);
			popupLayout.addComponent(label);
		}
		panel.setHeight(300,Unit.PIXELS);
		panel.setWidth(300,Unit.PIXELS);
		panel.setContent(popupLayout);
		popupLayout.setSizeUndefined();
		PopupView popUpView = new PopupView(POPUP_LBL_VIEW, panel);
		return popUpView;
	}

	private TabSheet constructFriendsTabSheet(Grid<User> usersByFollowersGrid, Grid<User> usersByTweetsGrid) {
		TabSheet tabSheet = new TabSheet();

		tabSheet.addTab(usersByFollowersGrid, TAB_LBL_TOP_FRIEND_FOLLOWER);
		tabSheet.addTab(usersByTweetsGrid, TAB_LBL_TOP_FRIEND_TWEET);
		tabSheet.setSelectedTab(usersByFollowersGrid);
		usersByTweetsGrid.setSizeFull();
		usersByFollowersGrid.setSizeFull();

		usersByFollowersGrid.addColumn(User::getName).setCaption(COL_LBL_NAME);
		usersByFollowersGrid.addColumn(User::getScreenName).setCaption(COL_LBL_TWITTERID);
		usersByFollowersGrid.addColumn(User::getFollowersCount).setCaption(COL_LBL_FOLLOWER_COUNT);
		usersByFollowersGrid.addComponentColumn(user->{
			PopupView popupView = new PopupView(POPUP_LBL_VIEW, constructUserStats(user));
			return popupView;
		}).setCaption(COL_LBL_VIEW_STATS);

		usersByTweetsGrid.addColumn(User::getName).setCaption(COL_LBL_NAME);
		usersByTweetsGrid.addColumn(User::getScreenName).setCaption(COL_LBL_TWITTERID);
		usersByTweetsGrid.addColumn(User::getStatusesCount).setCaption(COL_LBL_TWEET_COUNT);
		usersByTweetsGrid.addComponentColumn(user->{
			PopupView popupView = new PopupView(POPUP_LBL_VIEW, constructUserStats(user));
			return popupView;
		}).setCaption(COL_LBL_VIEW_STATS);
		return tabSheet;
	}

	private Layout constructUserInfo(User curUser) {
		VerticalLayout mainLayout = new VerticalLayout();
		Image profileImage = new Image("", new ExternalResource(curUser.get400x400ProfileImageURL()));
		profileImage.setWidth(150, Unit.PIXELS);
		Label name = new Label(curUser.getName());
		name.setStyleName(ValoTheme.LABEL_H2);
		Label screenName = new Label("@" + curUser.getScreenName());
		screenName.setStyleName(ValoTheme.LABEL_BOLD);
		Label description = new Label(curUser.getDescription());
		description.setStyleName(ValoTheme.LABEL_SMALL);
		description.setWidth("200px");

		mainLayout.addComponent(profileImage);
		mainLayout.addComponent(screenName);
		mainLayout.addComponent(name);
		mainLayout.addComponent(description);
		mainLayout.setSpacing(false);

		return mainLayout;
	}
	
	private Panel constructUserStats(User curUser) {
		VerticalLayout mainLayout = new VerticalLayout();
		
		Panel panel = new Panel();
		panel.setCaption("User Stats");
		panel.setWidth("300px");
		panel.setHeight("250px");
		panel.setStyleName(ValoTheme.PANEL_BORDERLESS);
		
		
		Label tweetsHeading = new Label(LBL_TWEETS);
		tweetsHeading.setStyleName(ValoTheme.LABEL_TINY);
		Label tweets = new Label(Integer.toString(curUser.getStatusesCount()));
		tweets.setStyleName(ValoTheme.LABEL_BOLD);
		mainLayout.addComponent(tweetsHeading);
		mainLayout.addComponent(tweets);
		

		Label followerHeading = new Label(LBL_FOLLOWERS);
		followerHeading.setStyleName(ValoTheme.LABEL_TINY);
		Label followers = new Label(Integer.toString(curUser.getFollowersCount()));
		followers.setStyleName(ValoTheme.LABEL_BOLD);
		mainLayout.addComponent(followerHeading);
		mainLayout.addComponent(followers);

		
		Label friendsHeading = new Label(LBL_FRIENDS);
		friendsHeading.setStyleName(ValoTheme.LABEL_TINY);
		Label friends = new Label(Integer.toString(curUser.getFriendsCount()));
		friends.setStyleName(ValoTheme.LABEL_BOLD);
		mainLayout.addComponent(friendsHeading);
		mainLayout.addComponent(friends);
		
		Label listheading = new Label(LBL_LIST);
		listheading.setStyleName(ValoTheme.LABEL_TINY);
		Label lists = new Label(Integer.toString(curUser.getListedCount()));
		lists.setStyleName(ValoTheme.LABEL_BOLD);
		mainLayout.addComponent(listheading);
		mainLayout.addComponent(lists);
		
		mainLayout.setSpacing(false);
		panel.setContent(mainLayout);
		return panel;
	}

	@Override
	protected void init(VaadinRequest request) {
		setContent(buildLayout());

	}

}
