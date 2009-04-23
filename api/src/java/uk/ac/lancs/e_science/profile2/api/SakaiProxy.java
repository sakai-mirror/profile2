package uk.ac.lancs.e_science.profile2.api;

import java.util.List;
import java.util.Locale;

import org.sakaiproject.api.common.edu.person.SakaiPerson;
/**
 * This is a helper API used by the Profile2 tool only. 
 * 
 * DO NOT IMPLEMENT THIS YOURSELF
 * 
 * @author Steve Swinsburg (s.swinsburg@lancaster.ac.uk)
 *
 */
public interface SakaiProxy {
	
	/**
	 * Get current siteid
	 * @return
	 */
	public String getCurrentSiteId();
	
	/**
	 * Get current user
	 * @return
	 */
	public String getCurrentUserId();
	
	/**
	 * Convert internal userid to eid (jsmith26)
	 * @return
	 */
	public String getUserEid(String userId);
	
	/**
	 * Convert eid to internal userid
	 * @return
	 */
	public String getUserIdForEid(String eid);

	/**
	 * Get displayname of a given userid (internal id)
	 * @return
	 */
	public String getUserDisplayName(String userId);
	
	/**
	 * Get email address for a given userid (internal id)
	 * @return
	 */
	public String getUserEmail(String userId);
	
	/**
	 * Check if a user with the given internal id (ie 6ec73d2a-b4d9-41d2-b049-24ea5da03fca) exists
	 * @param userId
	 * @return
	 */
	public boolean checkForUser(String userId);
	
	/**
	 * Check if a user with the given eid (ie jsmith26) exists
	 * @param userId
	 * @return
	 */
	public boolean checkForUserByEid(String eid);
	
	/**
	 * Get a SakaiPerson for a user
	 * @param userId
	 * @return
	 */
	public SakaiPerson getSakaiPerson(String userId);
	
	/**
	 * Get a SakaiPerson prototype if they don't have a profile.
	 * <p>This is not persistable so should only be used for temporary views.
	 * Use createSakaiPerson if need persistable object for saving a profile.
	 * @param userId
	 * @return
	 */
	public SakaiPerson getSakaiPersonPrototype();
	
	/**
	 * Create a new persistable SakaiPerson object for a user
	 * @param userId
	 * @return
	 */
	public SakaiPerson createSakaiPerson(String userId);

	/**
	 * Update a SakaiPerson object in the db
	 * @param sakaiPerson
	 * @return
	 */
	public boolean updateSakaiPerson(SakaiPerson sakaiPerson);
	
	/**
	 * Get the maximum filesize that can be uploaded (profile2.picture.max=2)
	 * @return
	 */
	public int getMaxProfilePictureSize();
	
	/**
	 * Get the location for a profileImage given the user and type
	 * 
	 * @param userId
	 * @param type
	 * @param fileName
	 * @return
	 */
	public String getProfileImageResourcePath(String userId, int type);
	
	/**
	 * Save a file to CHS
	 * 
	 * @param fullResourceId
	 * @param userId
	 * @param fileName
	 * @param mimeType
	 * @param fileData
	 * @return
	 */
	public boolean saveFile(String fullResourceId, String userId, String fileName, String mimeType, byte[] fileData);
		
	/**
	 * Retrieve a resource from ContentHosting
	 *
	 * @param resourceId	the full resourceId of the file
	 */
	public byte[] getResource(String resourceId);
	
	/**
	 * Search UserDirectoryService for a user that matches in name or email
	 *
	 * @param search	search string. Return's List of Sakai userId's 
	 */
	public List<String> searchUsers(String search);
	
	/**
	 * Post an event to Sakai
	 * 
	 * @param event			name of event
	 * @param reference		reference
	 * @param modify		true if something changed, false if just access
	 * 
	 */
	public void postEvent(String event,String reference,boolean modify);

	/**
	 * Send an email message. The message should be ready to go as is. The message will be formatted with
	 * mime boundaries, html escaped then sent.
	 * 
	 * @param userId	userId to send the message to
	 * @param subject	subject of message
	 * @param message	complete with newlines and any links.
	 */
	public void sendEmail(String userId, String subject,String message);
	
	
	/**
	 * Get the name of this Sakai installation (ie Sakai@Lancs)
	 * @return
	 */
	public String getServiceName();
	
	/**
	 * Gets the portalUrl configuration parameter (ie http://sakai.lancs.ac.uk/portal)
	 * 
	 * @param userId
	 * @return
	 */
	public String getPortalUrl();
	
	/**
	 * Get the DNS name of this Sakai server (ie sakai.lancs.ac.uk)
	 * @return
	 */
	public String getServerName();
	
	/**
	 * Updates a user's email address
	 * If the user is a provided user (ie from LDAP) this will probably fail
	 * so only internal accounts can be updated. That's ok since LDAP accounts will always 
	 * have an email address associated with tehm (most likely ;)
	 * 
	 * @param userId	uuid of the user
	 * @param email	
	 */
	public void updateEmailForUser(String userId, String email);
	
	
	/**
	 * Creates a direct URL to a user's profile page on their My Workspace
	 * Any other parameters supplied in string are appended and encoded.
	 * @param toolString
	 * @return
	 */
	public String getDirectUrlToUserProfile(String userId, String extraParams);
	
	/**
	 * Check if a user is allowed to update their account. The User could come from LDAP
	 * so updates not allowed. This will check if any updates are allowed.
	 * 
	 * Note userDirectoryService.allowUpdateUserEmail etc are NOT the right methods to use
	 * as they don't check if account updates are allowed, just if the user doing the update is allowed.
	 * 
	 * @param userId
	 * @return
	 */
	public boolean isAccountUpdateAllowed(String userId);
	
	/**
	 * Is the profile2.convert flag set in sakai.properties?
	 * If not set, defaults to false
	 * 
	 * <p>This will convert profiles from the original Profile tool in Sakai, to Profile2 format. 
	 * Any images that were uploaded via various methods will be placed into Sakai's ContentHostingSystem
	 * and thumbnails generated for use in various parts of Profile2.</p>
	 * 
	 * @return
	 */
	public boolean isProfileConversionEnabled();
	
	
	/**
	 * Is the profile2.integration.twitter.enabled flag set in sakai.properties?
	 * If not set, defaults to true
	 * 
	 * <p>Depending on this setting, the UI will allow a user to input their Twitter settings
	 * and their status updates will be sent to Twitter.</p>
	 * 
	 * @return
	 */
	public boolean isTwitterIntegrationEnabledGlobally();
	
	/**
	 * 
	 * Get the profile2.integration.twitter.source parameter
	 *
	 * See here:
	 * http://bugs.sakaiproject.org/confluence/display/PROFILE/Profile2
	 */
	public String getTwitterSource();
	
	/**
	 * Is the profile2.picture.change.enabled flag set in sakai.properties?
	 * If not set, defaults to true
	 * 
	 * <p>Depending on this setting, usesr will be able to upload thier own image. 
	 * This can be useful to disable for institutions which may provide official photos for students.</p>
	 * 
	 * @return
	 */
	public boolean isProfilePictureChangeEnabled();
	
	/**
	 * Get the profile2.picture.type setting in sakai.properties
	 * Possible values are 'upload' and 'url'. If not set, defaults to 'upload'
	 * 
	 * <p>Depending on this setting, Profile2 will deicde how it retrieves the image.
	 * Users will also be able to upload thier own image or provide a URL, depending on this setting.</p>
	 * 
	 * @return
	 */
	public int getProfilePictureType();
	
	
	/**
	 * Gets the users preferred locale, either from the user's session or Sakai preferences and returns it
	 * @return
	 */
	public Locale getUserPreferredLocale();
	
	

}
