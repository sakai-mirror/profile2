package uk.ac.lancs.e_science.profile2.impl;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.swing.ImageIcon;

import org.apache.log4j.Logger;
import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.jasypt.util.text.BasicTextEncryptor;
import org.sakaiproject.api.common.edu.person.SakaiPerson;
import org.sakaiproject.tinyurl.api.TinyUrlService;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;

import twitter4j.Twitter;
import uk.ac.lancs.e_science.profile2.api.Profile;
import uk.ac.lancs.e_science.profile2.api.ProfileFriendsManager;
import uk.ac.lancs.e_science.profile2.api.ProfileImageManager;
import uk.ac.lancs.e_science.profile2.api.ProfilePreferencesManager;
import uk.ac.lancs.e_science.profile2.api.ProfilePrivacyManager;
import uk.ac.lancs.e_science.profile2.api.ProfileUtilityManager;
import uk.ac.lancs.e_science.profile2.api.SakaiProxy;
import uk.ac.lancs.e_science.profile2.hbm.ProfileFriend;
import uk.ac.lancs.e_science.profile2.hbm.ProfileImage;
import uk.ac.lancs.e_science.profile2.hbm.ProfileImageExternal;
import uk.ac.lancs.e_science.profile2.hbm.ProfilePreferences;
import uk.ac.lancs.e_science.profile2.hbm.ProfilePrivacy;
import uk.ac.lancs.e_science.profile2.hbm.ProfileStatus;
import uk.ac.lancs.e_science.profile2.hbm.SearchResult;

import com.sun.image.codec.jpeg.JPEGCodec;
import com.sun.image.codec.jpeg.JPEGImageEncoder;

/**
 * This is the Profile2 API Implementation to be used by the Profile2 tool only. 
 * 
 * DO NOT USE THIS YOURSELF, use the ProfileService instead (todo)
 * 
 * @author Steve Swinsburg (s.swinsburg@lancaster.ac.uk)
 *
 */
public class ProfileImpl extends HibernateDaoSupport implements Profile {

	private transient final Logger log = Logger.getLogger(ProfileImpl.class);
	
	//surely this is in the Calendar API somewhere
	private static final String[] DAY_OF_WEEK_MAPPINGS = { "", "Sunday", "Monday",
		"Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"};
	
	/**
	 * the user's password needs to be decrypted and sent to Twitter for updates
	 * so we can't just one-way encrypt it. 
	 */
	private static final String BASIC_ENCRYPTION_KEY = "AbrA_ca-DabRa.123";
	
	
	private static final String QUERY_GET_FRIEND_REQUESTS_FOR_USER = "getFriendRequestsForUser";
	private static final String QUERY_GET_CONFIRMED_FRIEND_USERIDS_FOR_USER = "getConfirmedFriendUserIdsForUser";
	private static final String QUERY_GET_FRIEND_REQUEST = "getFriendRequest";
	private static final String QUERY_GET_FRIEND_RECORD = "getFriendRecord";
	private static final String QUERY_GET_USER_STATUS = "getUserStatus";
	private static final String QUERY_GET_PRIVACY_RECORD = "getPrivacyRecord";
	private static final String QUERY_GET_CURRENT_PROFILE_IMAGE_RECORD = "getCurrentProfileImageRecord";
	private static final String QUERY_OTHER_PROFILE_IMAGE_RECORDS = "getOtherProfileImageRecords";
	private static final String QUERY_FIND_SAKAI_PERSONS_BY_NAME_OR_EMAIL = "findSakaiPersonsByNameOrEmail";
	private static final String QUERY_FIND_SAKAI_PERSONS_BY_INTEREST = "findSakaiPersonsByInterest";
	private static final String QUERY_LIST_ALL_SAKAI_PERSONS = "listAllSakaiPersons";
	private static final String QUERY_GET_PREFERENCES_RECORD = "getPreferencesRecord";
	private static final String QUERY_GET_EXTERNAL_IMAGE_RECORD = "getProfileImageExternalRecord";

	
	
	
	//Hibernate object fields
	private static final String USER_UUID = "userUuid";
	private static final String FRIEND_UUID = "friendUuid";
	private static final String CONFIRMED = "confirmed";
	private static final String OLDEST_STATUS_DATE = "oldestStatusDate";
	private static final String SEARCH = "search";
	
	
	/**
 	 * {@inheritDoc}
 	 */
	public boolean checkContentTypeForProfileImage(String contentType) {
		
		ArrayList<String> allowedTypes = new ArrayList<String>();
		allowedTypes.add("image/jpeg");
		allowedTypes.add("image/gif");
		allowedTypes.add("image/png");

		if(allowedTypes.contains(contentType)) {
			return true;
		}
		
		return false;
	}
	
	
	/**
 	 * {@inheritDoc}
 	 */
	public byte[] scaleImage (byte[] imageData, int maxSize) {
	
	    log.debug("Scaling image...");

	    // Get the image from a file.
	    Image inImage = new ImageIcon(imageData).getImage();
	
	    // Determine the scale (we could change this to only determine scale from one dimension, ie the width only?)
	    double scale = (double) maxSize / (double) inImage.getHeight(null);
	    if (inImage.getWidth(null) > inImage.getHeight(null)) {
	        scale = (double) maxSize / (double) inImage.getWidth(null);
	    }
	    
	    /*
	    log.debug("===========Image scaling============");
	    log.debug("WIDTH: " + inImage.getWidth(null));
	    log.debug("HEIGHT: " + inImage.getHeight(null));
	    log.debug("SCALE: " + scale);
	    log.debug("========End of image scaling========");
	    */

	    //if image is smaller than desired image size (ie scale is larger) just return the original image bytes
	    if (scale >= 1.0d) {
	    	return imageData;
	    }
	    
	    
	
	    // Determine size of new image.
	    // One of the dimensions should equal maxSize.
	    int scaledW = (int) (scale * inImage.getWidth(null));
	    int scaledH = (int) (scale * inImage.getHeight(null));
	
	    // Create an image buffer in which to paint on.
	    BufferedImage outImage = new BufferedImage(scaledW, scaledH, BufferedImage.TYPE_INT_RGB);
	
	    // Set the scale.
	    AffineTransform tx = new AffineTransform();
	
	    //scale
	    tx.scale(scale, scale);
	
	    // Paint image.
	    Graphics2D g2d = outImage.createGraphics();
	    g2d.setRenderingHint(
	            RenderingHints.KEY_ANTIALIASING,
	            RenderingHints.VALUE_ANTIALIAS_ON
	        );
	    g2d.drawImage(inImage, tx, null);
	    g2d.dispose();
	
	    // JPEG-encode the image
	    // and write to file.
	    ByteArrayOutputStream os = new ByteArrayOutputStream();
	    try { 
	    	JPEGImageEncoder encoder = JPEGCodec.createJPEGEncoder(os);
	    	encoder.encode(outImage);
	    	os.close();
	    	log.debug("Scaling done.");
	    } catch (IOException e) {
	    	log.error("Scaling image failed.");
	    }
	    return os.toByteArray();
	}
	
	/**
 	 * {@inheritDoc}
 	 */
	public String convertDateToString(Date date, String format) {
		
		if(date == null || "".equals(format)) {
			throw new IllegalArgumentException("Null Argument in Profile.convertDateToString()");	
		}
		
		SimpleDateFormat dateFormat = new SimpleDateFormat(format);
        StringBuilder dateStr = new StringBuilder(dateFormat.format(date));
        
        log.debug("Profile.convertDateToString(): Input date: " + date.toString());
        log.debug("Profile.convertDateToString(): Converted date string: " + dateStr);

		return dateStr.toString();
		
	}
	
	/**
 	 * {@inheritDoc}
 	 */
	public Date convertStringToDate(String dateStr, String format) {
		
		if("".equals(dateStr) || "".equals(format)) {
			throw new IllegalArgumentException("Null Argument in Profile.convertStringToDate()");	
		}
		
		SimpleDateFormat dateFormat = new SimpleDateFormat(format);
		 
		try {
			Date date = dateFormat.parse(dateStr);
			
	        log.debug("Profile.convertStringToDate(): Input date string: " + dateStr);
	        log.debug("Profile.convertStringToDate(): Converted date: " + date.toString());
			return date;
		} catch (Exception e) {
			log.error("Profile.convertStringToDate() failed. " + e.getClass() + ": " + e.getMessage());
			return null;
		}            
        
	}


	
	/**
 	 * {@inheritDoc}
 	 */
	public List<String> getFriendRequestsForUser(final String userId) {
		
		if(userId == null){
	  		throw new IllegalArgumentException("Null Argument in Profile.getFriendRequestsForUser()");
	  	}
		
		List<String> requests = new ArrayList<String>();
		
		//get friends of this user [and map it automatically to the Friend object]
		//updated: now just returns a List of Strings
		HibernateCallback hcb = new HibernateCallback() {
	  		public Object doInHibernate(Session session) throws HibernateException, SQLException {
	  			
	  			Query q = session.getNamedQuery(QUERY_GET_FRIEND_REQUESTS_FOR_USER);
	  			q.setParameter(USER_UUID, userId, Hibernate.STRING);
	  			q.setBoolean("false", Boolean.FALSE);
	  			//q.setResultTransformer(Transformers.aliasToBean(Friend.class));
	  			
	  			return q.list();
	  		}
	  	};
	  	
	  	requests = (List<String>) getHibernateTemplate().executeFind(hcb);
	  	
	  	return requests;
	}
	
	/**
 	 * {@inheritDoc}
 	 */
	public List<String> getConfirmedFriendUserIdsForUser(final String userId) {
		
		List<String> userUuids = new ArrayList<String>();
		
		//get 
		HibernateCallback hcb = new HibernateCallback() {
	  		public Object doInHibernate(Session session) throws HibernateException, SQLException {
	  			
	  		
	  			Query q = session.getNamedQuery(QUERY_GET_CONFIRMED_FRIEND_USERIDS_FOR_USER);
	  			q.setParameter(USER_UUID, userId, Hibernate.STRING);
	  			q.setBoolean("true", Boolean.TRUE);
	  			return q.list();
	  		}
	  	};
	  	
	  	userUuids = (List<String>) getHibernateTemplate().executeFind(hcb);
	
	  	return userUuids;
	}
	
	
	/**
 	 * {@inheritDoc}
 	 */	
	public int countConfirmedFriendUserIdsForUser(final String userId) {
		
		//this should operhaps be a count(*) query but since we need to use unions, hmm.
		List<String> userUuids = new ArrayList<String>(getConfirmedFriendUserIdsForUser(userId));
		int count = userUuids.size();
	
	  	return count;
	}
	
	
	/**
 	 * {@inheritDoc}
 	 */
	public boolean requestFriend(String userId, String friendId) {
		if(userId == null || friendId == null){
	  		throw new IllegalArgumentException("Null Argument in Profile.getFriendsForUser");
	  	}
		
		//check values are valid, ie userId, friendId etc
		
		try {
			//make a ProfileFriend object with 'Friend Request' constructor
			ProfileFriend profileFriend = new ProfileFriend(userId, friendId, ProfileFriendsManager.RELATIONSHIP_FRIEND);
			getHibernateTemplate().save(profileFriend);
			log.info("User: " + userId + " requested friend: " + friendId);
			return true;
		} catch (Exception e) {
			log.error("Profile.requestFriend() failed. " + e.getClass() + ": " + e.getMessage());
			return false;
		}
	
	}
	
	/**
 	 * {@inheritDoc}
 	 */
	public boolean isFriendRequestPending(String fromUser, String toUser) {
		
		ProfileFriend profileFriend = getPendingFriendRequest(fromUser, toUser);

		if(profileFriend == null) {
			log.debug("Profile.isFriendRequestPending: No pending friend request from userId: " + fromUser + " to friendId: " + toUser + " found.");
			return false;
		}
		return true;
	}
	
	/**
 	 * {@inheritDoc}
 	 */
	public boolean confirmFriendRequest(final String fromUser, final String toUser) {
		
		if(fromUser == null || toUser == null){
	  		throw new IllegalArgumentException("Null Argument in Profile.confirmFriendRequest");
	  	}
		
		//get pending ProfileFriend object request for the given details
		ProfileFriend profileFriend = getPendingFriendRequest(fromUser, toUser);

		if(profileFriend == null) {
			log.error("Profile.confirmFriendRequest() failed. No pending friend request from userId: " + fromUser + " to friendId: " + toUser + " found.");
			return false;
		}
		
	  	//make necessary changes to the ProfileFriend object.
	  	profileFriend.setConfirmed(true);
	  	profileFriend.setConfirmedDate(new Date());
		
		//save
		try {
			getHibernateTemplate().update(profileFriend);
			log.info("User: " + fromUser + " confirmed friend request from: " + toUser);
			return true;
		} catch (Exception e) {
			log.error("Profile.confirmFriendRequest() failed. " + e.getClass() + ": " + e.getMessage());
			return false;
		}
	
	}
	
	/**
 	 * {@inheritDoc}
 	 */
	public boolean ignoreFriendRequest(final String fromUser, final String toUser) {
		
		if(fromUser == null || toUser == null){
	  		throw new IllegalArgumentException("Null Argument in Profile.ignoreFriendRequest");
	  	}
		
		//get pending ProfileFriend object request for the given details
		ProfileFriend profileFriend = getPendingFriendRequest(fromUser, toUser);

		if(profileFriend == null) {
			log.error("Profile.ignoreFriendRequest() failed. No pending friend request from userId: " + fromUser + " to friendId: " + toUser + " found.");
			return false;
		}
		
	  	
		//delete
		try {
			getHibernateTemplate().delete(profileFriend);
			log.info("User: " + toUser + " ignored friend request from: " + fromUser);
			return true;
		} catch (Exception e) {
			log.error("Profile.ignoreFriendRequest() failed. " + e.getClass() + ": " + e.getMessage());
			return false;
		}
	
	}
	
	/**
 	 * {@inheritDoc}
 	 */
	public boolean removeFriend(String userId, String friendId) {
		
		if(userId == null || friendId == null){
	  		throw new IllegalArgumentException("Null Argument in Profile.removeFriend");
	  	}
		
		//get the friend object for this connection pair (could be any way around)
		ProfileFriend profileFriend = getFriendRecord(userId, friendId);
		
		if(profileFriend == null){
			log.error("ProfileFriend record does not exist for userId: " + userId + ", friendId: " + friendId);
			return false;
		}
				
		//if ok, delete it
		try {
			getHibernateTemplate().delete(profileFriend);
			log.info("User: " + userId + " removed friend: " + friendId);
			return true;
		} catch (Exception e) {
			log.error("Profile.removeFriend() failed. " + e.getClass() + ": " + e.getMessage());
			return false;
		}
		
		
	}
	
	//only gets a pending request
	private ProfileFriend getPendingFriendRequest(final String userId, final String friendId) {
		
		if(userId == null || friendId == null){
	  		throw new IllegalArgumentException("Null Argument in Profile.getPendingFriendRequest");
	  	}
		
		HibernateCallback hcb = new HibernateCallback() {
	  		public Object doInHibernate(Session session) throws HibernateException, SQLException {
	  			Query q = session.getNamedQuery(QUERY_GET_FRIEND_REQUEST);
	  			q.setParameter(USER_UUID, userId, Hibernate.STRING);
	  			q.setParameter(FRIEND_UUID, friendId, Hibernate.STRING);
	  			q.setParameter(CONFIRMED, false, Hibernate.BOOLEAN);
	  			q.setMaxResults(1);
	  			return q.uniqueResult();
			}
		};
	
		return (ProfileFriend) getHibernateTemplate().execute(hcb);
	
	}
	
	
	
	/**
 	 * {@inheritDoc}
 	 */
	public int getUnreadMessagesCount(String userId) {
		int unreadMessages = 0;
		return unreadMessages;
		
	}
	
	
	
	/**
 	 * {@inheritDoc}
 	 */
	public ProfileStatus getUserStatus(final String userId) {
		
		if(userId == null){
	  		throw new IllegalArgumentException("Null Argument in Profile.getUserStatus");
	  	}
		
		// compute oldest date for status 
		Calendar cal = Calendar.getInstance(); 
		cal.add(Calendar.DAY_OF_YEAR, -7); 
		final Date oldestStatusDate = cal.getTime(); 
				
		HibernateCallback hcb = new HibernateCallback() {
	  		public Object doInHibernate(Session session) throws HibernateException, SQLException {
	  			Query q = session.getNamedQuery(QUERY_GET_USER_STATUS);
	  			q.setParameter(USER_UUID, userId, Hibernate.STRING);
	  			q.setParameter(OLDEST_STATUS_DATE, oldestStatusDate, Hibernate.DATE);
	  			q.setMaxResults(1);
	  			return q.uniqueResult();
			}
		};
	
		return (ProfileStatus) getHibernateTemplate().execute(hcb);
	}
	
	
	/**
 	 * {@inheritDoc}
 	 */
	public String getUserStatusMessage(String userId) {
		
		if(userId == null){
	  		throw new IllegalArgumentException("Null Argument in Profile.getUserStatusMessage");
	  	}
		
		ProfileStatus profileStatus = getUserStatus(userId);
		if(profileStatus == null) {
			return null;
		}
		return profileStatus.getMessage();
	}
	
	/**
 	 * {@inheritDoc}
 	 */
	public Date getUserStatusDate(String userId) {
		if(userId == null){
	  		throw new IllegalArgumentException("Null Argument in Profile.getUserStatusDate");
	  	}
		
		ProfileStatus profileStatus = getUserStatus(userId);
		if(profileStatus == null) {
			return null;
		}
		return profileStatus.getDateAdded();
	}
	
	
	/**
 	 * {@inheritDoc}
 	 */
	public boolean setUserStatus(String userId, String status) {
		
		//validate userId here - TODO
		
		ProfileStatus profileStatus = new ProfileStatus(userId,status,new Date());
		
		//this now uses saveOrUpdate as we are only allowing single status records
		//so that we can get the friends/statuses more easily via single SQL statements
		try {
			getHibernateTemplate().saveOrUpdate(profileStatus);
			log.info("Updated status for user: " + userId);
			return true;
		} catch (Exception e) {
			log.error("Profile.setUserStatus() failed. " + e.getClass() + ": " + e.getMessage());
			return false;
		}
		
	}
	
	
	/**
 	 * {@inheritDoc}
 	 */
	public boolean clearUserStatus(String userId) {
		
		//validate userId here - TODO
		
		ProfileStatus profileStatus = getUserStatus(userId);
		
		if(profileStatus == null){
			log.error("ProfileStatus null for userId: " + userId);
			return false;
		}
				
		//if ok, delete it
		try {
			getHibernateTemplate().delete(profileStatus);
			log.info("User: " + userId + " cleared status");
			return true;
		} catch (Exception e) {
			log.error("Profile.clearUserStatus() failed. " + e.getClass() + ": " + e.getMessage());
			return false;
		}
		
	}

	
	

	/**
 	 * {@inheritDoc}
 	 */
	public String convertDateForStatus(Date date) {
		
		//current time (can also specify timezome and local here, see API)
		Calendar currentCal = Calendar.getInstance();
		long currentTimeMillis = currentCal.getTimeInMillis();
		
		//posting time (set calendar time to be this)
		//Calendar postingDate = Calendar.getInstance();
		//postingDate.setTimeInMillis(date.getTime());
		long postingTimeMillis = date.getTime();
		
		//difference
		int diff = (int)(currentTimeMillis - postingTimeMillis);
		
		
		//System.out.println("currentDate:" + currentTimeMillis);
		//System.out.println("postingDate:" + postingTimeMillis);
		//System.out.println("diff:" + diff);
		
		
		int MILLIS_IN_SECOND = 1000;
		int MILLIS_IN_MINUTE = 1000 * 60;
		int MILLIS_IN_HOUR = 1000 * 60 * 60;
		int MILLIS_IN_DAY = 1000 * 60 * 60 * 24;
		int MILLIS_IN_WEEK = 1000 * 60 * 60 * 24 * 7;

		String message="";
		
		if(diff < MILLIS_IN_SECOND) {
			//less than a second
			message = "just then";
		} else if (diff < MILLIS_IN_MINUTE) {
			//less than a minute, calc seconds
			int numSeconds = diff/MILLIS_IN_SECOND;
			message = numSeconds + " seconds ago";
		} else if (diff < MILLIS_IN_HOUR) {
			//less than an hour, calc minutes
			int numMinutes = diff/MILLIS_IN_MINUTE;
			message = numMinutes + " minutes ago";
		} else if (diff < MILLIS_IN_DAY) {
			//less than a day, calc hours
			int numHours = diff/MILLIS_IN_HOUR;
			message = numHours + " hours ago";
		} else if (diff < MILLIS_IN_WEEK) {
			//less than a week, calculate days
			int numDays = diff/MILLIS_IN_DAY;
			
			//System.out.println("day diff = " + numDays);
			
			//now calculate which day it was
			if(numDays == 1) {
				message = "yesterday";
			} else {
				//copy calendar, then subtract number of days to find posting day
				Calendar postingCal = currentCal;
				postingCal.add(Calendar.DATE, -numDays);
				int postingDay = postingCal.get(Calendar.DAY_OF_WEEK);
				//System.out.println("day of week of post = " + postingDay);
				//System.out.println("day of week of post = " + DAY_OF_WEEK_MAPPINGS[postingDay]);

				//use calendar API to get name of day here, for now using array at top
				message = "on " + DAY_OF_WEEK_MAPPINGS[postingDay];
			}
			
		} else {
			//nothing, we want it blank.
		}

		return message;
	}
	
	
	
	
	/**
 	 * {@inheritDoc}
 	 */
	public String truncateAndPadStringToSize(String string, int size) {
		
		String returnStr = string.substring(0, size);
		return (returnStr.concat("..."));
		
	}
	
	
	/**
 	 * {@inheritDoc}
 	 */
	public ProfilePrivacy createDefaultPrivacyRecord(String userId) {
		
		//see ProfilePrivacy for this constructor and what it all means
		ProfilePrivacy profilePrivacy = new ProfilePrivacy(
				userId,
				ProfilePrivacyManager.DEFAULT_PRIVACY_OPTION_PROFILEIMAGE,
				ProfilePrivacyManager.DEFAULT_PRIVACY_OPTION_BASICINFO,
				ProfilePrivacyManager.DEFAULT_PRIVACY_OPTION_CONTACTINFO,
				ProfilePrivacyManager.DEFAULT_PRIVACY_OPTION_PERSONALINFO,
				ProfilePrivacyManager.DEFAULT_BIRTHYEAR_VISIBILITY,
				ProfilePrivacyManager.DEFAULT_PRIVACY_OPTION_SEARCH,
				ProfilePrivacyManager.DEFAULT_PRIVACY_OPTION_MYFRIENDS,
				ProfilePrivacyManager.DEFAULT_PRIVACY_OPTION_MYSTATUS);
		
		//save
		try {
			getHibernateTemplate().save(profilePrivacy);
			log.info("Created default privacy record for user: " + userId);
			return profilePrivacy;
		} catch (Exception e) {
			log.error("Profile.createDefaultPrivacyRecord() failed. " + e.getClass() + ": " + e.getMessage());
			return null;
		}
	}
	
	
	/**
 	 * {@inheritDoc}
 	 */
	public ProfilePrivacy getPrivacyRecordForUser(final String userId) {
		
		if(userId == null){
	  		throw new IllegalArgumentException("Null Argument in Profile.getPrivacyRecordForUser");
	  	}
		
		HibernateCallback hcb = new HibernateCallback() {
	  		public Object doInHibernate(Session session) throws HibernateException, SQLException {
	  			Query q = session.getNamedQuery(QUERY_GET_PRIVACY_RECORD);
	  			q.setParameter(USER_UUID, userId, Hibernate.STRING);
	  			q.setMaxResults(1);
	  			return q.uniqueResult();
			}
		};
	
		return (ProfilePrivacy) getHibernateTemplate().execute(hcb);

	}
	
	/**
 	 * {@inheritDoc}
 	 */
	public boolean savePrivacyRecord(ProfilePrivacy profilePrivacy) {

		try {
			getHibernateTemplate().update(profilePrivacy);
			log.info("Updated privacy record for user: " + profilePrivacy.getUserUuid());
			return true;
		} catch (Exception e) {
			log.error("Profile.savePrivacyRecordForUser() failed. " + e.getClass() + ": " + e.getMessage());
			return false;
		}
		
	}
	

	/**
 	 * {@inheritDoc}
 	 */
	public boolean addNewProfileImage(String userId, String mainResource, String thumbnailResource) {
		
		//first get the current ProfileImage records
		List<ProfileImage> currentImages = new ArrayList<ProfileImage>(getCurrentProfileImageRecords(userId));
		for(Iterator<ProfileImage> i = currentImages.iterator(); i.hasNext();){
			ProfileImage currentImage = (ProfileImage)i.next();
			
			//invalidate each
			currentImage.setCurrent(false);
			
			//save
			try {
				getHibernateTemplate().update(currentImage);
				log.info("Profile.saveProfileImageRecord(): Invalidated profileImage: " + currentImage.getId() + " for user: " + currentImage.getUserUuid());
			} catch (Exception e) {
				log.error("Profile.saveProfileImageRecord(): Couldn't invalidate profileImage: " + e.getClass() + ": " + e.getMessage());
			}
		}
				
		//now create a new ProfileImage object with the new data - this is our new current ProfileImage
		ProfileImage newProfileImage = new ProfileImage(userId, mainResource, thumbnailResource, true);
		
		//save the new ProfileImage to the db
		try {
			getHibernateTemplate().save(newProfileImage);
			log.info("Profile.saveProfileImageRecord(): Saved new profileImage for user: " + newProfileImage.getUserUuid());
			return true;
		} catch (Exception e) {
			log.error("Profile.saveProfileImageRecord() failed. " + e.getClass() + ": " + e.getMessage());
			return false;
		}
	}


	/**
 	 * {@inheritDoc}
 	 */
	public List<SearchResult> findUsersByNameOrEmail(String search, String userId) {
		
		//perform search (uses private method to wrap the two searches into one)
		List<String> userUuids = new ArrayList<String>(findUsersByNameOrEmail(search));

		//restrict to only return the max number. UI will print message
		int maxResults = ProfileUtilityManager.MAX_SEARCH_RESULTS;
		if(userUuids.size() >= maxResults) {
			userUuids = userUuids.subList(0, maxResults);
		}
		
		//format into SearchResult records (based on friend status, privacy status etc)
		List<SearchResult> results = new ArrayList<SearchResult>(createSearchResultRecordsFromSearch(userUuids, userId));
		
		return results;
		
	}
	
	
	
	/**
 	 * {@inheritDoc}
 	 */
	public List<SearchResult> findUsersByInterest(String search, String userId) {
		
		//perform search (uses private method to wrap the search)
		List<String> userUuids = new ArrayList<String>(findSakaiPersonsByInterest(search));
		
		//restrict to only return the max number. UI will print message
		int maxResults = ProfileUtilityManager.MAX_SEARCH_RESULTS;
		if(userUuids.size() >= maxResults) {
			userUuids = userUuids.subList(0, maxResults);
		}
		
		//format into SearchResult records (based on friend status, privacy status etc)
		List<SearchResult> results = new ArrayList<SearchResult>(createSearchResultRecordsFromSearch(userUuids, userId));
		
		return results;
		
	}
	
	
	/**
 	 * {@inheritDoc}
 	 */
	public List<String> listAllSakaiPersons() {
		
		List<String> userUuids = new ArrayList<String>();
		
		//get 
		HibernateCallback hcb = new HibernateCallback() {
	  		public Object doInHibernate(Session session) throws HibernateException, SQLException {
	  			
	  			Query q = session.getNamedQuery(QUERY_LIST_ALL_SAKAI_PERSONS);
	  			return q.list();
	  		}
	  	};
	  	
	  	userUuids = (List<String>) getHibernateTemplate().executeFind(hcb);
	
	  	return userUuids;
	}
	
	
	
	
	
	
	/**
 	 * {@inheritDoc}
 	 */
	public boolean isUserXFriendOfUserY(String userX, String userY) {
		
		//get friends of current user
		//TODO change this to be a single lookup rather than iterating over a list
		List<String> friendUuids = new ArrayList<String>(getConfirmedFriendUserIdsForUser(userY));
		
		//if list of confirmed friends contains this user, they are a friend
		if(friendUuids.contains(userX)) {
			return true;
		}
		
		return false;
	}
	
		
	
	
	/**
 	 * {@inheritDoc}
 	 */
	public boolean isUserXVisibleInSearchesByUserY(String userX, String userY, boolean friend) {
				
		//get ProfilePrivacy record for user
    	ProfilePrivacy profilePrivacy = getPrivacyRecordForUser(userX);
    	//if none, return whatever the flag is set as by default
    	if(profilePrivacy == null) {
    		log.debug("SEARCH VISIBILITY for " + userX + ": no record, returning default visibility");
    		return ProfilePrivacyManager.DEFAULT_SEARCH_VISIBILITY;
    	}
    	
    	//if userX is userY (ie they found themself in a search)
    	if(userY.equals(userX)) {
    		log.debug("SEARCH VISIBILITY for " + userX + ": user is current user");
    		return ProfilePrivacyManager.SELF_SEARCH_VISIBILITY;
    	}
    	
    	//if restricted to only self, not allowed
    	/* DEPRECATED via PRFL-24 when the privacy settings were relaxed
    	if(profilePrivacy.getSearch() == ProfilePrivacyManager.PRIVACY_OPTION_ONLYME) {
    		log.debug("SEARCH VISIBILITY for " + userX + ": only me");
    		return false;
    	}
    	*/
    	
    	//if friend and set to friends only
    	if(friend && profilePrivacy.getSearch() == ProfilePrivacyManager.PRIVACY_OPTION_ONLYFRIENDS) {
    		log.debug("SEARCH VISIBILITY for " + userX + ": only friends and  " + userY + " is friend");
    		return true;
    	}
    	
    	//if not friend and set to friends only
    	if(!friend && profilePrivacy.getSearch() == ProfilePrivacyManager.PRIVACY_OPTION_ONLYFRIENDS) {
    		log.debug("SEARCH VISIBILITY for " + userX + ": only friends and  " + userY + " not friend");
    		return false;
    	}
    	
    	//if everyone is allowed
    	if(profilePrivacy.getSearch() == ProfilePrivacyManager.PRIVACY_OPTION_EVERYONE) {
    		log.debug("SEARCH VISIBILITY: everyone");
    		return true;
    	}
    	
    	//uncaught rule, return false
    	log.error("Profile.isUserXVisibleInSearchesByUserY. Uncaught rule. userX: " + userX + ", userY: " + userY + ", friend: " + friend);
    	return false;
		
	}
	
	
	
	
	/**
 	 * {@inheritDoc}
 	 */
	public boolean isUserXProfileImageVisibleByUserY(String userX, String userY, boolean friend) {
		
		
		//get privacy record for this user
    	ProfilePrivacy profilePrivacy = getPrivacyRecordForUser(userX);
    	
    	//if none, return whatever the flag is set as by default
    	if(profilePrivacy == null) {
    		return ProfilePrivacyManager.DEFAULT_PROFILEIMAGE_VISIBILITY;
    	}
    	
    	//if userX is userY, they ARE allowed to view their own picture!
    	if(userY.equals(userX)) {
    		return true;
    	}
    	
    	//if restricted to only self, not allowed
    	/* DEPRECATED via PRFL-24 when the privacy settings were relaxed
    	if(profilePrivacy.getProfile() == ProfilePrivacyManager.PRIVACY_OPTION_ONLYME) {
    		return false;
    	}
    	*/
    	
    	//if user is friend and friends are allowed
    	if(friend && profilePrivacy.getProfileImage() == ProfilePrivacyManager.PRIVACY_OPTION_ONLYFRIENDS) {
    		return true;
    	}
    	
    	//if not friend and set to friends only
    	if(!friend && profilePrivacy.getProfileImage() == ProfilePrivacyManager.PRIVACY_OPTION_ONLYFRIENDS) {
    		return false;
    	}
    	
    	//if everyone is allowed
    	if(profilePrivacy.getProfileImage() == ProfilePrivacyManager.PRIVACY_OPTION_EVERYONE) {
    		return true;
    	}
    	
    	//uncaught rule, return false
    	log.error("Profile.isUserProfileImageVisibleByCurrentUser. Uncaught rule. userX: " + userX + ", userY: " + userY + ", friend: " + friend);

    	return false;
	}
	
	/**
 	 * {@inheritDoc}
 	 */
	public boolean isUserXBasicInfoVisibleByUserY(String userX, String userY, boolean friend) {
		
		//get privacy record for this user
    	ProfilePrivacy profilePrivacy = getPrivacyRecordForUser(userX);
    	
    	//if none, return whatever the flag is set as by default
    	if(profilePrivacy == null) {
    		return ProfilePrivacyManager.DEFAULT_BASICINFO_VISIBILITY;
    	}
    	
    	//if user is the current user, they ARE allowed to view their own picture!
    	//but this will never be called as the current user cannot access ViewProfile.
    	//so this has been removed from this function and all other privacy checks
    	/*
    	if(currentUserId.equals(userId)) {
    		return true;
    	}
    	*/
    	
    	//if restricted to only self, not allowed
    	if(profilePrivacy.getBasicInfo() == ProfilePrivacyManager.PRIVACY_OPTION_ONLYME) {
    		return false;
    	}
    	
    	//if user is friend and friends are allowed
    	if(friend && profilePrivacy.getBasicInfo() == ProfilePrivacyManager.PRIVACY_OPTION_ONLYFRIENDS) {
    		return true;
    	}
    	
    	//if not friend and set to friends only
    	if(!friend && profilePrivacy.getBasicInfo() == ProfilePrivacyManager.PRIVACY_OPTION_ONLYFRIENDS) {
    		return false;
    	}
    	
    	//if everyone is allowed
    	if(profilePrivacy.getBasicInfo() == ProfilePrivacyManager.PRIVACY_OPTION_EVERYONE) {
    		return true;
    	}
    	
    	//uncaught rule, return false
    	log.error("Profile.isBasicInfoVisibleByCurrentUser. Uncaught rule. userX: " + userX + ", userY: " + userY + ", friend: " + friend);

    	return false;
	}
	
	
	/**
 	 * {@inheritDoc}
 	 */
	public boolean isUserXContactInfoVisibleByUserY(String userX, String userY, boolean friend) {
		
		//get privacy record for this user
    	ProfilePrivacy profilePrivacy = getPrivacyRecordForUser(userX);
    	
    	//if none, return whatever the flag is set as by default
    	if(profilePrivacy == null) {
    		return ProfilePrivacyManager.DEFAULT_CONTACTINFO_VISIBILITY;
    	}
    	
    	//if restricted to only self, not allowed
    	if(profilePrivacy.getContactInfo() == ProfilePrivacyManager.PRIVACY_OPTION_ONLYME) {
    		return false;
    	}
    	
    	//if user is friend and friends are allowed
    	if(friend && profilePrivacy.getContactInfo() == ProfilePrivacyManager.PRIVACY_OPTION_ONLYFRIENDS) {
    		return true;
    	}
    	
    	//if not friend and set to friends only
    	if(!friend && profilePrivacy.getContactInfo() == ProfilePrivacyManager.PRIVACY_OPTION_ONLYFRIENDS) {
    		return false;
    	}
    	
    	//if everyone is allowed
    	if(profilePrivacy.getContactInfo() == ProfilePrivacyManager.PRIVACY_OPTION_EVERYONE) {
    		return true;
    	}
    	
    	//uncaught rule, return false
    	log.error("Profile.isContactInfoVisibleByCurrentUser. Uncaught rule. userX: " + userX + ", userY: " + userY + ", friend: " + friend);

    	return false;
	}
	
	
	/**
 	 * {@inheritDoc}
 	 */
	public boolean isUserXPersonalInfoVisibleByUserY(String userX, String userY, boolean friend) {
		
		//get privacy record for this user
    	ProfilePrivacy profilePrivacy = getPrivacyRecordForUser(userX);
    	
    	//if none, return whatever the flag is set as by default
    	if(profilePrivacy == null) {
    		return ProfilePrivacyManager.DEFAULT_PERSONALINFO_VISIBILITY;
    	}
    	
    	//if restricted to only self, not allowed
    	if(profilePrivacy.getPersonalInfo() == ProfilePrivacyManager.PRIVACY_OPTION_ONLYME) {
    		return false;
    	}
    	
    	//if user is friend and friends are allowed
    	if(friend && profilePrivacy.getPersonalInfo() == ProfilePrivacyManager.PRIVACY_OPTION_ONLYFRIENDS) {
    		return true;
    	}
    	
    	//if not friend and set to friends only
    	if(!friend && profilePrivacy.getPersonalInfo() == ProfilePrivacyManager.PRIVACY_OPTION_ONLYFRIENDS) {
    		return false;
    	}
    	
    	//if everyone is allowed
    	if(profilePrivacy.getPersonalInfo() == ProfilePrivacyManager.PRIVACY_OPTION_EVERYONE) {
    		return true;
    	}
    	    	
    	//uncaught rule, return false
    	log.error("Profile.isPersonalInfoVisibleByCurrentUser. Uncaught rule. userX: " + userX + ", userY: " + userY + ", friend: " + friend);

    	return false;
	}
	
	
	/**
 	 * {@inheritDoc}
 	 */
	public boolean isUserXFriendsListVisibleByUserY(String userX, String userY, boolean friend) {
		
		//get privacy record for this user
    	ProfilePrivacy profilePrivacy = getPrivacyRecordForUser(userX);
    	
    	//if none, return whatever the flag is set as by default
    	if(profilePrivacy == null) {
    		return ProfilePrivacyManager.DEFAULT_MYFRIENDS_VISIBILITY;
    	}
    	
    	//if restricted to only self, not allowed
    	if(profilePrivacy.getMyFriends() == ProfilePrivacyManager.PRIVACY_OPTION_ONLYME) {
    		return false;
    	}
    	
    	//if user is friend and friends are allowed
    	if(friend && profilePrivacy.getMyFriends() == ProfilePrivacyManager.PRIVACY_OPTION_ONLYFRIENDS) {
    		return true;
    	}
    	
    	//if not friend and set to friends only
    	if(!friend && profilePrivacy.getMyFriends() == ProfilePrivacyManager.PRIVACY_OPTION_ONLYFRIENDS) {
    		return false;
    	}
    	
    	//if everyone is allowed
    	if(profilePrivacy.getMyFriends() == ProfilePrivacyManager.PRIVACY_OPTION_EVERYONE) {
    		return true;
    	}
    	    	
    	//uncaught rule, return false
    	log.error("Profile.isFriendsListVisibleByCurrentUser. Uncaught rule. userX: " + userX + ", userY: " + userY + ", friend: " + friend);

    	return false;
	}
	
	
	/**
 	 * {@inheritDoc}
 	 */
	public boolean isUserXStatusVisibleByUserY(String userX, String userY, boolean friend) {
		
		//get privacy record for this user
    	ProfilePrivacy profilePrivacy = getPrivacyRecordForUser(userX);
    	
    	//if none, return whatever the flag is set as by default
    	if(profilePrivacy == null) {
    		return ProfilePrivacyManager.DEFAULT_MYSTATUS_VISIBILITY;
    	}
    	
    	//if restricted to only self, not allowed
    	/* DEPRECATED via PRFL-24 when the privacy settings were relaxed
    	if(profilePrivacy.getMyStatus() == ProfilePrivacyManager.PRIVACY_OPTION_ONLYME) {
    		return false;
    	}
    	*/
    	
    	//if user is friend and friends are allowed
    	if(friend && profilePrivacy.getMyStatus() == ProfilePrivacyManager.PRIVACY_OPTION_ONLYFRIENDS) {
    		return true;
    	}
    	
    	//if not friend and set to friends only
    	if(!friend && profilePrivacy.getMyStatus() == ProfilePrivacyManager.PRIVACY_OPTION_ONLYFRIENDS) {
    		return false;
    	}
    	
    	//if everyone is allowed
    	if(profilePrivacy.getMyStatus() == ProfilePrivacyManager.PRIVACY_OPTION_EVERYONE) {
    		return true;
    	}
    	    	
    	//uncaught rule, return false
    	log.error("Profile.isUserXStatusVisibleByUserY. Uncaught rule. userX: " + userX + ", userY: " + userY + ", friend: " + friend);

    	return false;
	}
	
	
	

	/**
 	 * {@inheritDoc}
 	 */
	public boolean isBirthYearVisible(String userId) {
		
		//get privacy record for this user
		ProfilePrivacy profilePrivacy = this.getPrivacyRecordForUser(userId);
		
		//return value or whatever the flag is set as by default
    	if(profilePrivacy == null) {
    		return ProfilePrivacyManager.DEFAULT_BIRTHYEAR_VISIBILITY;
    	} else {
    		return profilePrivacy.isShowBirthYear();
    	}
		
	}

	
	
	/**
 	 * {@inheritDoc}
 	 */
	public byte[] getCurrentProfileImageForUser(String userId, int imageType) {
		
		byte[] image = null;
		
		//get record from db
		ProfileImage profileImage = getCurrentProfileImageRecord(userId);
		
		if(profileImage == null) {
			log.debug("Profile.getCurrentProfileImageForUser() null for userId: " + userId);
			return null;
		}
		
		//get main image
		if(imageType == ProfileImageManager.PROFILE_IMAGE_MAIN) {
			image = sakaiProxy.getResource(profileImage.getMainResource());
		}
		
		//or get thumbnail
		if(imageType == ProfileImageManager.PROFILE_IMAGE_THUMBNAIL) {
			image = sakaiProxy.getResource(profileImage.getThumbnailResource());
		}
		
		return image;
	}

	
	/**
 	 * {@inheritDoc}
 	 */
	public boolean hasProfileImage(String userId) {
		
		//get record from db
		ProfileImage profileImage = getCurrentProfileImageRecord(userId);
		
		if(profileImage == null) {
			return false;
		}
		return true;
		
	}
	
	
	/**
 	 * {@inheritDoc}
 	 */
	public ProfilePreferences createDefaultPreferencesRecord(final String userId) {
		
		//see ProfilePreferences for this constructor and what it all means
		ProfilePreferences profilePreferences = new ProfilePreferences(
				userId,
				ProfilePreferencesManager.DEFAULT_EMAIL_REQUEST_SETTING,
				ProfilePreferencesManager.DEFAULT_EMAIL_CONFIRM_SETTING,
				ProfilePreferencesManager.DEFAULT_TWITTER_SETTING);
		
		//save
		try {
			getHibernateTemplate().save(profilePreferences);
			log.info("Created default preferences record for user: " + userId);
			return profilePreferences;
		} catch (Exception e) {
			log.error("Profile.createDefaultPreferencesRecord() failed. " + e.getClass() + ": " + e.getMessage());
			return null;
		}
	}
	
	/**
 	 * {@inheritDoc}
 	 */
	public ProfilePreferences getPreferencesRecordForUser(final String userId) {
		
		if(userId == null){
	  		throw new IllegalArgumentException("Null Argument in Profile.getPreferencesRecordForUser");
	  	}
		
		ProfilePreferences prefs = null;
		
		HibernateCallback hcb = new HibernateCallback() {
	  		public Object doInHibernate(Session session) throws HibernateException, SQLException {
	  			Query q = session.getNamedQuery(QUERY_GET_PREFERENCES_RECORD);
	  			q.setParameter(USER_UUID, userId, Hibernate.STRING);
	  			q.setMaxResults(1);
	  			return q.uniqueResult();
			}
		};
	
		prefs = (ProfilePreferences) getHibernateTemplate().execute(hcb);
		
		if(prefs == null) {
			return null;
		}
		
		//decrypt password and set into field
		prefs.setTwitterPasswordDecrypted(decrypt(prefs.getTwitterPasswordEncrypted()));
		
		return prefs;
		
	}
	
	/**
 	 * {@inheritDoc}
 	 */
	public boolean savePreferencesRecord(ProfilePreferences prefs) {
		
		//encrypt and set
		prefs.setTwitterPasswordEncrypted(encrypt(prefs.getTwitterPasswordDecrypted()));
		
		try {
			getHibernateTemplate().update(prefs);
			log.info("Updated preferences record for user: " + prefs.getUserUuid());
			return true;
		} catch (Exception e) {
			log.error("Profile.savePreferencesRecord() failed. " + e.getClass() + ": " + e.getMessage());
			return false;
		}
	}
	
	
	
	/**
 	* {@inheritDoc}
 	*/
	public boolean isTwitterIntegrationEnabledForUser(final String userId) {
		
		//check global settings
		if(!sakaiProxy.isTwitterIntegrationEnabledGlobally()) {
			return false;
		}
		
		//check own preferences
		ProfilePreferences profilePreferences = getPreferencesRecordForUser(userId);
		if(profilePreferences == null) {
			return false;
		}
		
		if(profilePreferences.isTwitterEnabled()) {
			return true;
		}
		
		return false;
	}
	
	/**
 	 * {@inheritDoc}
 	 */
	public void sendMessageToTwitter(final String userId, final String message){
		//setup class thread to call later
		class TwitterUpdater implements Runnable{
			private Thread runner;
			private String username;
			private String password;
			private String message;

			public TwitterUpdater(String username, String password, String message) {
				this.username=username;
				this.password=password;
				this.message=message;
				
				runner = new Thread(this,"Profile2 TwitterUpdater thread");
				runner.start();
			}
			

			//do it!
			public synchronized void run() {
				
				Twitter twitter = new Twitter(username, password);
				
				try {
					twitter.setSource(sakaiProxy.getTwitterSource());
					twitter.update(message);
					log.info("Twitter status updated for: " + userId);
				}
				catch (Exception e) {
					log.error("Profile.sendMessageToTwitter() failed. " + e.getClass() + ": " + e.getMessage());
				}
			}
		}
		
		//get preferences for this user
		ProfilePreferences profilePreferences = getPreferencesRecordForUser(userId);
		
		if(profilePreferences == null) {
			return;
		}
		//get details
		String username = profilePreferences.getTwitterUsername();
		String password = profilePreferences.getTwitterPasswordDecrypted();
		
		//instantiate class to send the data
		new TwitterUpdater(username, password, message);
		
	}
	
	/**
 	 * {@inheritDoc}
 	 */
	public boolean validateTwitterCredentials(final String twitterUsername, final String twitterPassword) {
		
		Twitter twitter = new Twitter(twitterUsername, twitterPassword);
		
		if(twitter.verifyCredentials()) {
			return true;
		}
		
		return false;
	}

	
		
	/**
 	 * {@inheritDoc}
 	 */
	public String generateTinyUrl(final String url) {
		return tinyUrlService.generateTinyUrl(url);
	}
	
	
	/**
 	 * {@inheritDoc}
 	 */
	public boolean isEmailEnabledForThisMessageType(final String userId, final int messageType) {
		
		//get preferences record for this user
    	ProfilePreferences profilePreferences = getPreferencesRecordForUser(userId);
    	
    	//if none, return whatever the flag is set as by default
    	if(profilePreferences == null) {
    		return ProfilePreferencesManager.DEFAULT_EMAIL_NOTIFICATION_SETTING;
    	}
    	
    	
    	//if its a request and requests enabled, true
    	if(messageType == ProfilePreferencesManager.EMAIL_NOTIFICATION_REQUEST && profilePreferences.isRequestEmailEnabled()) {
    		return true;
    	}
    	
    	//if its a confirm and confirms enabled, true
    	if(messageType == ProfilePreferencesManager.EMAIL_NOTIFICATION_CONFIRM && profilePreferences.isConfirmEmailEnabled()) {
    		return true;
    	}
    	
    	//add more cases here as need progresses
    	
    	//no notification for this message type, return false 	
    	log.debug("Profile.isEmailEnabledForThisMessageType. False for userId: " + userId + ", messageType: " + messageType);

    	return false;
		
	}

	
	/**
 	 * {@inheritDoc}
 	 */
	public ProfileImageExternal getExternalImageRecordForUser(final String userId) {
		
		if(userId == null){
	  		throw new IllegalArgumentException("Null Argument in Profile.getExternalImageRecordForUser");
	  	}
		
		HibernateCallback hcb = new HibernateCallback() {
	  		public Object doInHibernate(Session session) throws HibernateException, SQLException {
	  			Query q = session.getNamedQuery(QUERY_GET_EXTERNAL_IMAGE_RECORD);
	  			q.setParameter(USER_UUID, userId, Hibernate.STRING);
	  			q.setMaxResults(1);
	  			return q.uniqueResult();
			}
		};
	
		return (ProfileImageExternal) getHibernateTemplate().execute(hcb);
	}
	
	/**
 	 * {@inheritDoc}
 	 */
	public String getExternalImageUrl(final String userId, final int imageType) {
		
		//get external image record for this user
		ProfileImageExternal externalImage = getExternalImageRecordForUser(userId);
		
		//if none, return null
    	if(externalImage == null) {
    		return null;
    	}
    	
    	//else return the url for the type they requested
    	if(imageType == ProfileImageManager.PROFILE_IMAGE_MAIN) {
    		String url = externalImage.getMainUrl();
    		if(url == null || url.length() == 0) {
    			return null;
    		}
    		return url;
    	}
    	
    	if(imageType == ProfileImageManager.PROFILE_IMAGE_THUMBNAIL) {
    		String url = externalImage.getThumbnailUrl();
    		if(url == null || url.length() == 0) {
    			return null;
    		}
    		return url;
    	}
    	
    	//no notification for this message type, return false 	
    	log.error("Profile.getExternalImageUrl. No URL for userId: " + userId + ", imageType: " + imageType);

    	return null;
		
	}

	
	
	
	
	
	//these encrypt/decrypt methods are bound always to the method before its saved or returned
	private String decrypt(final String encryptedText) {
		
		BasicTextEncryptor textEncryptor = new BasicTextEncryptor();
		textEncryptor.setPassword(BASIC_ENCRYPTION_KEY);
		return(textEncryptor.decrypt(encryptedText));
		
	}
	
	private String encrypt(final String plainText) {
		BasicTextEncryptor textEncryptor = new BasicTextEncryptor();
		textEncryptor.setPassword(BASIC_ENCRYPTION_KEY);
		return(textEncryptor.encrypt(plainText));
		
	}
	
	
	
	//private method to query SakaiPerson for matches
	//this should go in the profile ProfilePersistence API
	private List<String> findSakaiPersonsByNameOrEmail(final String search) {
		
		List<String> userUuids = new ArrayList<String>();
		
		//get 
		HibernateCallback hcb = new HibernateCallback() {
	  		public Object doInHibernate(Session session) throws HibernateException, SQLException {
	  			
	  			Query q = session.getNamedQuery(QUERY_FIND_SAKAI_PERSONS_BY_NAME_OR_EMAIL);
	  			q.setParameter(SEARCH, '%' + search + '%', Hibernate.STRING);
	  			return q.list();
	  		}
	  	};
	  	
	  	userUuids = (List<String>) getHibernateTemplate().executeFind(hcb);
	
	  	return userUuids;
	}
	
	
	//private method to query SakaiPerson for matches
	//this should go in the profile ProfilePersistence API
	private List<String> findSakaiPersonsByInterest(final String search) {
		
		List<String> userUuids = new ArrayList<String>();
		
		//get 
		HibernateCallback hcb = new HibernateCallback() {
	  		public Object doInHibernate(Session session) throws HibernateException, SQLException {
	  			
	  			Query q = session.getNamedQuery(QUERY_FIND_SAKAI_PERSONS_BY_INTEREST);
	  			q.setParameter(SEARCH, '%' + search + '%', Hibernate.STRING);
	  			return q.list();
	  		}
	  	};
	  	
	  	userUuids = (List<String>) getHibernateTemplate().executeFind(hcb);
	
	  	return userUuids;
	}


	
	
	
	/**
	 * Get the current ProfileImage records from the database.
	 * There should only ever be one, but in case things get out of sync this returns all.
	 * This method is only used when we are adding a new image as we need to invalidate all of the others
	 * If you are just wanting to retrieve the latest image, see getCurrentProfileImageRecord()
	 *
	 * @param userId		userId of the user
	 */
	private List<ProfileImage> getCurrentProfileImageRecords(final String userId) {
		
		List<ProfileImage> images = new ArrayList<ProfileImage>();
		
		//get 
		HibernateCallback hcb = new HibernateCallback() {
	  		public Object doInHibernate(Session session) throws HibernateException, SQLException {
	  			
	  			Query q = session.getNamedQuery(QUERY_GET_CURRENT_PROFILE_IMAGE_RECORD);
	  			q.setParameter(USER_UUID, userId, Hibernate.STRING);
	  			return q.list();
	  		}
	  	};
	  	
	  	images = (List<ProfileImage>) getHibernateTemplate().executeFind(hcb);
	  	
	  	return images;
	}


	/**
	 * Get the current ProfileImage record from the database.
	 * There should only ever be one, but if there are more this will return the latest. 
	 * This is called when retrieving a profile image for a user. When adding a new image, there is a call
	 * to a private method called getCurrentProfileImageRecords() which should invalidate any multiple current images
	 *
	 * @param userId		userId of the user
	 */
	private ProfileImage getCurrentProfileImageRecord(final String userId) {
		
		HibernateCallback hcb = new HibernateCallback() {
	  		public Object doInHibernate(Session session) throws HibernateException, SQLException {
	  			Query q = session.getNamedQuery(QUERY_GET_CURRENT_PROFILE_IMAGE_RECORD);
	  			q.setParameter(USER_UUID, userId, Hibernate.STRING);
	  			q.setMaxResults(1);
	  			return q.uniqueResult();
			}
		};
	
		return (ProfileImage) getHibernateTemplate().execute(hcb);
	}
	
	
	/**
	 * Get old ProfileImage records from the database. 
	 * TODO: Used for displaying old the profile pictures album
	 *
	 * @param userId		userId of the user
	 */
	private List<ProfileImage> getOtherProfileImageRecords(final String userId) {
		
		List<ProfileImage> images = new ArrayList<ProfileImage>();
		
		//get 
		HibernateCallback hcb = new HibernateCallback() {
	  		public Object doInHibernate(Session session) throws HibernateException, SQLException {
	  			
	  			Query q = session.getNamedQuery(QUERY_OTHER_PROFILE_IMAGE_RECORDS);
	  			q.setParameter(USER_UUID, userId, Hibernate.STRING);
	  			return q.list();
	  		}
	  	};
	  	
	  	images = (List<ProfileImage>) getHibernateTemplate().executeFind(hcb);
	  	
	  	return images;
	}


	//gets a friend record and tries both column arrangements
	private ProfileFriend getFriendRecord(final String userId, final String friendId) {
		
		if(userId == null || friendId == null){
	  		throw new IllegalArgumentException("Null Argument in Profile.getFriendRecord");
	  	}
		
		ProfileFriend profileFriend = null;
		
		//this particular query checks for records when userId/friendId is in either column
		HibernateCallback hcb = new HibernateCallback() {
	  		public Object doInHibernate(Session session) throws HibernateException, SQLException {
	  			Query q = session.getNamedQuery(QUERY_GET_FRIEND_RECORD);
	  			q.setParameter(USER_UUID, userId, Hibernate.STRING);
	  			q.setParameter(FRIEND_UUID, friendId, Hibernate.STRING);
	  			q.setMaxResults(1);
	  			return q.uniqueResult();
			}
		};
	
		profileFriend = (ProfileFriend) getHibernateTemplate().execute(hcb);
	
		return profileFriend;
	}
	
	
	//private method to back the search methods. returns only uuids which then need to be checked for privacy settings etc.
	private List<String> findUsersByNameOrEmail(String search) {
		
		//get users from SakaiPerson
		List<String> userUuids = new ArrayList<String>(findSakaiPersonsByNameOrEmail(search));

		//get users from UserDirectoryService
		List<String> usersUuidsFromUserDirectoryService = new ArrayList<String>(sakaiProxy.searchUsers(search));
		
		//combine with no duplicates
		userUuids.removeAll(usersUuidsFromUserDirectoryService);
		userUuids.addAll(usersUuidsFromUserDirectoryService);
		
		return userUuids;
	
	}
	
	
	//private utility method used by findUsersByNameOrEmail() and findUsersByInterest() to format results from
	//the supplied userUuids to SearchResult records based on friend or friendRequest status and the privacy settings for each user
	//that was in the initial search results
	private List<SearchResult> createSearchResultRecordsFromSearch(List<String> userUuids, String userId) {

		List<SearchResult> results = new ArrayList<SearchResult>();
				
		//for each userUuid, is userId a friend?
		//also, get privacy record for the userUuid. if searches not allowed for this user pair, skip to next
		//otherwise create SearchResult record and add to list
		for(Iterator<String> i = userUuids.iterator(); i.hasNext();){
			String userUuid = (String)i.next();
			
			//friend?
			boolean friend = isUserXFriendOfUserY(userUuid, userId);
			
			//init request flags
			boolean friendRequestToThisPerson = false;
			boolean friendRequestFromThisPerson = false;
			
			//if not friend, has a friend request already been made to this person?
			if(!friend) {
				friendRequestToThisPerson = isFriendRequestPending(userId, userUuid);
			}
			
			//if not friend and no friend request to this person, has a friend request been made from this person to the current user?
			if(!friend && !friendRequestToThisPerson) {
				friendRequestFromThisPerson = isFriendRequestPending(userUuid, userId);
			}
			
			//is this user visible in searches by this user? if not, skip
			if(!isUserXVisibleInSearchesByUserY(userUuid, userId, friend)) {
				continue; 
			}
			
			//is profile photo visible to this user
			boolean profileImageAllowed = isUserXProfileImageVisibleByUserY(userUuid, userId, friend);
			
			//is profile/ photo visible to this user
			boolean friendsListVisible = isUserXFriendsListVisibleByUserY(userUuid, userId, friend);
			
			//make object
			SearchResult searchResult = new SearchResult(
					userUuid,
					friend,
					profileImageAllowed,
					friendsListVisible,
					friendRequestToThisPerson,
					friendRequestFromThisPerson
					);
			
			results.add(searchResult);
		}
		
		return results;
	}
	
	
	//init method called when Tomcat starts up
	public void init() {
		
		log.info("Profile2: init()");
		
		//do we need to run the conversion utility?
		if(sakaiProxy.isProfileConversionEnabled()) {
			convertProfile();
		}
	}
	
	
	
	//method to convert profileImages
	private void convertProfile() {
		log.info("Profile2: ===============================");
		log.info("Profile2: Conversion utility starting up.");
		log.info("Profile2: ===============================");

		//get list of users
		List<String> allUsers = new ArrayList<String>(listAllSakaiPersons());
		
		if(allUsers.isEmpty()){
			log.info("Profile2 conversion util: No SakaiPersons to process.");
			return;
		}
		//for each, do they have a profile image record. if so, skip (perhaps null the SakaiPerson JPEG_PHOTO bytes?)
		for(Iterator<String> i = allUsers.iterator(); i.hasNext();) {
			String userUuid = (String)i.next();
			
			//if already have a current ProfileImage record, skip to next user
			if(hasProfileImage(userUuid)) {
				log.info("Profile2 conversion util: valid ProfileImage record already exists for " + userUuid + ". Skipping...");
				continue;
			}

			//get SakaiPerson
			SakaiPerson sakaiPerson = sakaiProxy.getSakaiPerson(userUuid);
			
			if(sakaiPerson == null) {
				log.error("Profile2 conversion util: No valid SakaiPerson record for " + userUuid + ". Skipping...");
				continue;
			}
			
			//get photo from SakaiPerson
			byte[] image = null;
			image = sakaiPerson.getJpegPhoto();
			
			//if none, nothing to do
			if(image == null) {
				log.info("Profile2 conversion util: Nothing to convert for " + userUuid + ". Skipping...");
				continue;
			}
			
			//set some defaults for the image we are adding to ContentHosting
			String fileName = "Profile Image";
			String mimeType = "image/jpeg";
			
			//scale the main image
			byte[] imageMain = scaleImage(image, ProfileImageManager.MAX_IMAGE_XY);
			
			//create resource ID
			String mainResourceId = sakaiProxy.getProfileImageResourcePath(userUuid, ProfileImageManager.PROFILE_IMAGE_MAIN);
			log.info("Profile2 conversion util: mainResourceId: " + mainResourceId);
			
			//save, if error, log and return.
			if(!sakaiProxy.saveFile(mainResourceId, userUuid, fileName, mimeType, imageMain)) {
				log.error("Profile2 conversion util: Saving main profile image failed.");
				continue;
			}

			/*
			 * THUMBNAIL PROFILE IMAGE
			 */
			//scale image
			byte[] imageThumbnail = scaleImage(image, ProfileImageManager.MAX_THUMBNAIL_IMAGE_XY);
			 
			//create resource ID
			String thumbnailResourceId = sakaiProxy.getProfileImageResourcePath(userUuid, ProfileImageManager.PROFILE_IMAGE_THUMBNAIL);

			log.info("Profile2 conversion util: thumbnailResourceId:" + thumbnailResourceId);
			
			//save, if error, log and return.
			if(!sakaiProxy.saveFile(thumbnailResourceId, userUuid, fileName, mimeType, imageThumbnail)) {
				log.error("Profile2 conversion util: Saving thumbnail profile image failed.");
				continue;
			}

			/*
			 * SAVE IMAGE RESOURCE IDS
			 */
			if(addNewProfileImage(userUuid, mainResourceId, thumbnailResourceId)) {
				log.info("Profile2 conversion util: images converted and saved for " + userUuid);
			} else {
				log.error("Profile2 conversion util: image conversion failed for " + userUuid);
				continue;
			}
			
			//go to next user
		}
		
		return;
	}
	
	
	//setup SakaiProxy API
	private SakaiProxy sakaiProxy;
	public void setSakaiProxy(SakaiProxy sakaiProxy) {
		this.sakaiProxy = sakaiProxy;
	}

	
	//setup TinyUrlService API
	private TinyUrlService tinyUrlService;
	public void setTinyUrlService(TinyUrlService tinyUrlService) {
		this.tinyUrlService = tinyUrlService;
	}
	
	
	
}
