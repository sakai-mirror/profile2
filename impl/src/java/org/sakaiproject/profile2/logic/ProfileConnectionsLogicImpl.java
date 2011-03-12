package org.sakaiproject.profile2.logic;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.sakaiproject.memory.api.Cache;
import org.sakaiproject.profile2.cache.CacheManager;
import org.sakaiproject.profile2.dao.ProfileDao;
import org.sakaiproject.profile2.hbm.model.ProfileFriend;
import org.sakaiproject.profile2.model.BasicConnection;
import org.sakaiproject.profile2.model.Person;
import org.sakaiproject.profile2.util.ProfileConstants;
import org.sakaiproject.user.api.User;

/**
 * Implementation of ProfileConnectionsLogic for Profile2.
 * 
 * @author Steve Swinsburg (s.swinsburg@gmail.com)
 *
 */
public class ProfileConnectionsLogicImpl implements ProfileConnectionsLogic {

	private static final Logger log = Logger.getLogger(ProfileConnectionsLogicImpl.class);

	private Cache cache;
	private final String CACHE_NAME = "org.sakaiproject.profile2.cache.connections";
	
	
	/**
 	 * {@inheritDoc}
 	 */
	public List<BasicConnection> getBasicConnectionsForUser(final String userUuid) {
		List<User> users = getConnectedUsers(userUuid);
		return getBasicConnections(users);
	}
	
	/**
 	 * {@inheritDoc}
 	 */
	public List<Person> getConnectionsForUser(final String userUuid) {
		List<User> users = getConnectedUsers(userUuid);
		return profileLogic.getPersons(users);
	}
	
	
	/**
 	 * {@inheritDoc}
 	 */	
	public int getConnectionsForUserCount(final String userId) {
		return getConnectionsForUser(userId).size();
	}
	
	/**
 	 * {@inheritDoc}
 	 */	
	public List<Person> getConnectionRequestsForUser(final String userId) {
		
		List<User> users = sakaiProxy.getUsers(dao.getRequestedConnectionUserIdsForUser(userId));
		
		return profileLogic.getPersons(users);
	}
	
	/**
 	 * {@inheritDoc}
 	 */	
	public int getConnectionRequestsForUserCount(final String userId) {
		return dao.getConnectionRequestsForUserCount(userId);
	}

	/**
 	 * {@inheritDoc}
 	 */
	public boolean isUserXFriendOfUserY(String userX, String userY) {
		
		//if same then friends.
		//added this check so we don't need to do it everywhere else and can call isFriend for all user pairs.
		if(userY.equals(userX)) {
			return true;
		}
		
		//get friends of current user
		//TODO change this to be a single lookup rather than iterating over a list
		List<String> friendUuids = dao.getConfirmedConnectionUserIdsForUser(userY);
		
		//if list of confirmed friends contains this user, they are a friend
		if(friendUuids.contains(userX)) {
			return true;
		}
		
		return false;
	}
	

	
	/**
 	 * {@inheritDoc}
 	 */
	public List<Person> getConnectionsSubsetForSearch(List<Person> connections, String search) {
		
		List<Person> subList = new ArrayList<Person>();
		
		for(Person p : connections){
			if(StringUtils.startsWithIgnoreCase(p.getDisplayName(), search)) {
				if(subList.size() == ProfileConstants.MAX_CONNECTIONS_PER_SEARCH) {
					break;
				}
				subList.add(p);
			}
		}
		return subList;
	}
	
	
	/**
 	 * {@inheritDoc}
 	 */
	public int getConnectionStatus(String userA, String userB) {
		ProfileFriend record = dao.getConnectionRecord(userA, userB);
		
		//no connection
		if(record == null) {
			return ProfileConstants.CONNECTION_NONE;
		}
		
		//confirmed
		if(record.isConfirmed()) {
			return ProfileConstants.CONNECTION_CONFIRMED;
		}
		
		//requested
		if(StringUtils.equals(userA, record.getUserUuid()) && !record.isConfirmed()) {
			return ProfileConstants.CONNECTION_REQUESTED;
		}
		
		//incoming
		if(StringUtils.equals(userA, record.getFriendUuid()) && !record.isConfirmed()) {
			return ProfileConstants.CONNECTION_INCOMING;
		}
		
		return ProfileConstants.CONNECTION_NONE;
	}
	
	
	/**
 	 * {@inheritDoc}
 	 */
	public boolean requestFriend(String userId, String friendId) {
		
		if(userId == null || friendId == null){
	  		throw new IllegalArgumentException("Null argument in ProfileLogic.getFriendsForUser"); 
	  	}
		
		//TODO check values are valid, ie userId, friendId etc
		
		//make a ProfileFriend object with 'Friend Request' constructor
		ProfileFriend profileFriend = new ProfileFriend(userId, friendId, ProfileConstants.RELATIONSHIP_FRIEND);
		
		//make the request
		if(dao.addNewConnection(profileFriend)) {
			
			log.info("User: " + userId + " requested friend: " + friendId);  

			//send email notification
			sendConnectionEmailNotification(friendId, userId, ProfileConstants.EMAIL_NOTIFICATION_REQUEST);
			return true;
		}
		return false;
	}
	
	/**
 	 * {@inheritDoc}
 	 */
	public boolean isFriendRequestPending(String fromUser, String toUser) {
		
		ProfileFriend profileFriend = dao.getPendingConnection(fromUser, toUser);

		if(profileFriend == null) {
			log.debug("ProfileLogic.isFriendRequestPending: No pending friend request from userId: " + fromUser + " to friendId: " + toUser + " found.");   
			return false;
		}
		
		return true;
	}
	
	/**
 	 * {@inheritDoc}
 	 */
	public boolean confirmFriendRequest(final String fromUser, final String toUser) {
		
		if(fromUser == null || toUser == null){
	  		throw new IllegalArgumentException("Null argument in ProfileLogic.confirmFriendRequest"); 
	  	}
		
		//get pending ProfileFriend object request for the given details
		ProfileFriend profileFriend = dao.getPendingConnection(fromUser, toUser);

		if(profileFriend == null) {
			log.error("ProfileLogic.confirmFriendRequest() failed. No pending friend request from userId: " + fromUser + " to friendId: " + toUser + " found.");   
			return false;
		}
		
	  	//make necessary changes to the ProfileFriend object.
	  	profileFriend.setConfirmed(true);
	  	profileFriend.setConfirmedDate(new Date());
		
		if(dao.updateConnection(profileFriend)) {
			
			log.info("User: " + fromUser + " confirmed friend request from: " + toUser); 
			//send email notification
			sendConnectionEmailNotification(fromUser, toUser, ProfileConstants.EMAIL_NOTIFICATION_CONFIRM);
			
			return true;
		} 
		
		return false;
	}
	
	/**
 	 * {@inheritDoc}
 	 */
	public boolean ignoreFriendRequest(final String fromUser, final String toUser) {
		
		if(fromUser == null || toUser == null){
	  		throw new IllegalArgumentException("Null argument in ProfileLogic.ignoreFriendRequest"); 
	  	}
		
		//get pending ProfileFriend object request for the given details
		ProfileFriend profileFriend = dao.getPendingConnection(fromUser, toUser);

		if(profileFriend == null) {
			log.error("ProfileLogic.ignoreFriendRequest() failed. No pending friend request from userId: " + fromUser + " to friendId: " + toUser + " found.");   
			return false;
		}
	  	
		//delete
		if(dao.removeConnection(profileFriend)) {
			log.info("User: " + toUser + " ignored friend request from: " + fromUser);  
			return true;
		}
		
		return false;
	}
	
	/**
 	 * {@inheritDoc}
 	 */
	public boolean removeFriend(String userId, String friendId) {
		
		if(userId == null || friendId == null){
	  		throw new IllegalArgumentException("Null argument in ProfileLogic.removeFriend"); 
	  	}
		
		//get the friend object for this connection pair (could be any way around)
		ProfileFriend profileFriend = dao.getConnectionRecord(userId, friendId);
		
		if(profileFriend == null){
			log.error("ProfileFriend record does not exist for userId: " + userId + ", friendId: " + friendId);  
			return false;
		}
				
		//delete
		if(dao.removeConnection(profileFriend)) {
			log.info("User: " + userId + " remove friend: " + friendId);  
			return true;
		}
		return false;
	}
	
	
	/**
 	 * {@inheritDoc}
 	 */
	public BasicConnection getBasicConnection(String userUuid) {
		return getBasicConnection(sakaiProxy.getUserById(userUuid));
	}

	/**
 	 * {@inheritDoc}
 	 */
	public BasicConnection getBasicConnection(User user) {
		BasicConnection p = new BasicConnection();
		p.setUuid(user.getId());
		p.setDisplayName(user.getDisplayName());
		p.setType(user.getType());
		p.setOnlineStatus(getOnlineStatus(user.getId()));
		return p;
	}

	/**
 	 * {@inheritDoc}
 	 */
	public List<BasicConnection> getBasicConnections(List<User> users) {
		
		List<BasicConnection> list = new ArrayList<BasicConnection>();
		
		//get online status
		Map<String,Integer> onlineStatus = getOnlineStatus(sakaiProxy.getUuids(users));
		
		//this is created manually so that we can use the bulk retrieval of the online status method.
		for(User u:users){
			BasicConnection p = new BasicConnection();
			p.setUuid(u.getId());
			p.setDisplayName(u.getDisplayName());
			p.setType(u.getType());
			p.setOnlineStatus(onlineStatus.get(u.getId()));
			
			list.add(p);
		}
		return list;
	}
	
	
	/**
 	 * {@inheritDoc}
 	 */
	public int getOnlineStatus(String userUuid) {
		//check if user has an active session
		boolean active = sakaiProxy.isUserActive(userUuid);
		if(!active) {
			return ProfileConstants.ONLINE_STATUS_OFFLINE;
		}
		
		//if active, when was their last event
		Long lastEventTime = sakaiProxy.getLastEventTimeForUser(userUuid);
		if(lastEventTime == null) {
			return ProfileConstants.ONLINE_STATUS_OFFLINE;
		}
		
		//if time between now and last event is less than the interval, they are online.
		long timeNow = new Date().getTime();
		
		if((timeNow - lastEventTime.longValue()) < ProfileConstants.ONLINE_INACTIVITY_INTERVAL) {
			return ProfileConstants.ONLINE_STATUS_ONLINE;
		}
		
		//user is online but inactive
		return ProfileConstants.ONLINE_STATUS_AWAY;
	}
	
	/**
 	 * {@inheritDoc}
 	 */
	public Map<String, Integer> getOnlineStatus(List<String> userUuids) {
		
		//get the list of users that have active sessions
		List<String> activeUuids = sakaiProxy.getActiveUsers(userUuids);
		
		//get last event times for the new list
		Map<String, Long> lastEventTimes = sakaiProxy.getLastEventTimeForUsers(activeUuids);

		long timeNow = new Date().getTime();
		
		//iterate over original list, create the map
		Map<String, Integer> map = new HashMap<String, Integer>();
		for(String uuid: userUuids) {
			if(lastEventTimes.containsKey(uuid)){
				//calc time, iff less than interval, online, otherwise away
				if((timeNow - lastEventTimes.get(uuid).longValue()) < ProfileConstants.ONLINE_INACTIVITY_INTERVAL) {
					map.put(uuid, ProfileConstants.ONLINE_STATUS_ONLINE);
				} else {
					map.put(uuid, ProfileConstants.ONLINE_STATUS_AWAY);
				}
			} else {
				//no session/no last event time
				map.put(uuid, ProfileConstants.ONLINE_STATUS_OFFLINE);
			}
		}
		return map;
	}
	
	
	
	
	
	/**
	 * Check auth, privacy and get the list of users that are connected to this user.
	 * @param userUuid
	 * @return List<User>, will be empty if none or not allowed.
	 */
	private List<User> getConnectedUsers(final String userUuid) {
		//check auth and get currentUserUuid
		String currentUserUuid = sakaiProxy.getCurrentUserId();
		if(currentUserUuid == null) {
			throw new SecurityException("You must be logged in to get a connection list.");
		}
		
		List<User> users = new ArrayList<User>();
		
		//check privacy
		boolean friend = isUserXFriendOfUserY(userUuid, currentUserUuid);
		if(!privacyLogic.isUserXFriendsListVisibleByUserY(userUuid, currentUserUuid, friend)) {
			return users;
		}
		
		users = sakaiProxy.getUsers(dao.getConfirmedConnectionUserIdsForUser(userUuid));
		return users;
	}
	

	
	
	
	
	
	
	/**
	 * Sends an email notification to the users. Used for connections. This formats the data and calls {@link SakaiProxy.sendEmail(String userId, String emailTemplateKey, Map<String,String> replacementValues)}
	 * @param toUuid		user to send the message to - this will be formatted depending on their email preferences for this message type so it is safe to pass any users you need
	 * @param fromUuid		uuid from
	 * @param messageType	the message type to send from ProfileConstants. Retrieves the emailTemplateKey based on this value
	 */
	private void sendConnectionEmailNotification(String toUuid, final String fromUuid, final int messageType) {
		//check if email preference enabled
		if(!preferencesLogic.isEmailEnabledForThisMessageType(toUuid, messageType)) {
			return;
		}
		
		//request
		if(messageType == ProfileConstants.EMAIL_NOTIFICATION_REQUEST) {
			
			String emailTemplateKey = ProfileConstants.EMAIL_TEMPLATE_KEY_CONNECTION_REQUEST;
			
			//create the map of replacement values for this email template
			Map<String,String> replacementValues = new HashMap<String,String>();
			replacementValues.put("senderDisplayName", sakaiProxy.getUserDisplayName(fromUuid));
			replacementValues.put("localSakaiName", sakaiProxy.getServiceName());
			replacementValues.put("connectionLink", linkLogic.getEntityLinkToProfileConnections());
			replacementValues.put("localSakaiUrl", sakaiProxy.getPortalUrl());
			replacementValues.put("toolName", sakaiProxy.getCurrentToolTitle());

			sakaiProxy.sendEmail(toUuid, emailTemplateKey, replacementValues);
			return;
		}
		
		//confirm
		if(messageType == ProfileConstants.EMAIL_NOTIFICATION_CONFIRM) {
			
			String emailTemplateKey = ProfileConstants.EMAIL_TEMPLATE_KEY_CONNECTION_CONFIRM;
			
			//create the map of replacement values for this email template
			Map<String,String> replacementValues = new HashMap<String,String>();
			replacementValues.put("senderDisplayName", sakaiProxy.getUserDisplayName(fromUuid));
			replacementValues.put("localSakaiName", sakaiProxy.getServiceName());
			replacementValues.put("connectionLink", linkLogic.getEntityLinkToProfileHome(fromUuid));
			replacementValues.put("localSakaiUrl", sakaiProxy.getPortalUrl());
			replacementValues.put("toolName", sakaiProxy.getCurrentToolTitle());

			sakaiProxy.sendEmail(toUuid, emailTemplateKey, replacementValues);
			return;
		}
		
	}
	
	public void init() {
		cache = cacheManager.createCache(CACHE_NAME);
	}
	
	
	private SakaiProxy sakaiProxy;
	public void setSakaiProxy(SakaiProxy sakaiProxy) {
		this.sakaiProxy = sakaiProxy;
	}
	
	private ProfileDao dao;
	public void setDao(ProfileDao dao) {
		this.dao = dao;
	}
	
	private ProfileLogic profileLogic;
	public void setProfileLogic(ProfileLogic profileLogic) {
		this.profileLogic = profileLogic;
	}
	
	private ProfileLinkLogic linkLogic;
	public void setLinkLogic(ProfileLinkLogic linkLogic) {
		this.linkLogic = linkLogic;
	}
	
	private ProfilePrivacyLogic privacyLogic;
	public void setPrivacyLogic(ProfilePrivacyLogic privacyLogic) {
		this.privacyLogic = privacyLogic;
	}
	
	private ProfilePreferencesLogic preferencesLogic;
	public void setPreferencesLogic(ProfilePreferencesLogic preferencesLogic) {
		this.preferencesLogic = preferencesLogic;
	}
	
	private CacheManager cacheManager;
	public void setCacheManager(CacheManager cacheManager) {
		this.cacheManager = cacheManager;
	}

}
