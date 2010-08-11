package org.sakaiproject.profile2.logic;

import java.util.HashMap;
import java.util.List;

import org.sakaiproject.api.common.edu.person.SakaiPerson;
import org.sakaiproject.profile2.model.ResourceWrapper;
import org.sakaiproject.profile2.service.ProfileService;
import org.sakaiproject.user.api.User;
/**
 * This is a helper API used by the Profile2 tool and entities only.
 * 
 * DO NOT IMPLEMENT THIS YOURSELF, use the {@link ProfileService} instead
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
	 * Get firstname of a given userid (internal id)
	 * @return
	 */
	public String getUserFirstName(String userId);
	
	/**
	 * Get lastname of a given userid (internal id)
	 * @return
	 */
	public String getUserLastName(String userId);
	
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
	 * Is the current user a superUser?
	 * @return
	 */
	public boolean isSuperUser();
	
	/**
	 * Is the current user a superUser and are they performing an action on another user's profile?
	 * @param userId - userId of other user
	 * @return
	 */
	public boolean isSuperUserAndProxiedToUser(String userId);
	/**
	 * Get the type of this user's account
	 * @param userId
	 * @return
	 */
	public String getUserType(String userId);
	
	/**
	 * Get the User object for the given userId.
	 * <p>This will not log errors so that we can quietly use it to check if a User exists for a given profile,
	 *  ie in a search result, for example.</p>
	 * @param userId
	 * @return
	 */
	public User getUserQuietly(String userId);
	
	/**
	 * Get the title of the current tool
	 * @return
	 */
	public String getCurrentToolTitle();

	
	/**
	 * Get a SakaiPerson for a user
	 * @param userId
	 * @return
	 */
	public SakaiPerson getSakaiPerson(String userId);
	
	/**
	 * Get a SakaiPerson Jpeg photo for a user. Checks UserMutableType record first, then goes for SystemMutableType if none.
	 * <p>Returns null if nothing found.
	 * @param userId
	 * @return
	 */
	public byte[] getSakaiPersonJpegPhoto(String userId);
	
	/**
	 * Get a SakaiPerson image URL for a user. Checks UserMutableType record first, then goes for SystemMutableType if none.
	 * <p>Returns null if nothing found.
	 * @param userId
	 * @return
	 */
	public String getSakaiPersonImageUrl(String userId);
	
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
	 * Retrieve a resource from ContentHosting as a byte[]
	 *
	 * @param resourceId	the full resourceId of the file
	 */
	public byte[] getResource(String resourceId);
	
	/**
	 * Retrieve a resource from ContentHosting as a ResourceWrapper object
	 *
	 * @param resourceId	the full resourceId of the file
	 */
	public ResourceWrapper getResourceWrapped(String resourceId);
	
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
	 * @return
	 */
	public String getPortalUrl();
	
	/**
	 * Gets the portal path, generally /portal unless override in sakai.properties
	 * @return
	 */
	public String getPortalPath();
	
	/**
	 * Are we using the normal /portal? 
	 * Deep links are broken in xsl-portal, so we need a workaround to drop the toolstate param.
	 * See PRFL-264
	 * @return
	 */
	public boolean isUsingNormalPortal();
	
	/**
	 * Gets the serverUrl configuration parameter
	 * @return
	 */
	public String getServerUrl();
	
	/**
	 * Get the DNS name of this Sakai server (ie sakai.lancs.ac.uk)
	 * @return
	 */
	public String getServerName();
	
	/**
	 * Updates a user's email address
	 * If the user is a provided user (ie from LDAP) this will probably fail
	 * so only internal accounts can be updated.
	 * 
	 * @param userId	uuid of the user
	 * @param email	
	 */
	public void updateEmailForUser(String userId, String email);
	
	/**
	 * Updates a user's name
	 * If the user is a provided user (ie from LDAP) this will probably fail
	 * so only internal accounts can be updated.
	 * 
	 * @param userId	uuid of the user
	 * @param email	
	 */
	public void updateNameForUser(String userId, String firstName, String lastName);
	
	
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
	 * <p>Possible values for the sakai property are 'upload' and 'url'.
	 * If not set, defaults to 'upload'.</p>
	 * <p>This returns an int which matches ProfileConstants.PICTURE_SETTING_UPLOAD or ProfileConstants.PICTURE_SETTING_URL</p>
	 * 
	 * <p>Depending on this setting, Profile2 will decide how it retrieves a user's profile image, and the method by which
	 * users can cadd their own image. ie by uploading their own image or providing a URL.</p>
	 * 
	 * @return
	 */
	public int getProfilePictureType();
	
	
	
	/**
	 * Gets the profile2.profile.entity.set.academic list of properties that should be used in the academic profile view.
	 * Returns default list of ProfileConstants.ENTITY_SET_ACADEMIC if none.
	 * @return
	 */
	public List<String> getAcademicEntityConfigurationSet();
	
	/**
	 * Gets the profile2.profile.entity.set.minimal list of properties that should be used in the minimal profile view.
	 * Returns default list of ProfileConstants.ENTITY_SET_MINIMAL if none.
	 * @return
	 */
	public List<String> getMinimalEntityConfigurationSet();

	/**
	 * Convenience method to convert the given userId input (internal id or eid) to a uuid. 
	 * 
	 * There is a small risk that an eid could be created that matches the uuid of another user.
	 * 
	 * Since 99% of the time requests will be made with uuid as the param, to speed things up this checks for that first.
	 * If the above risk manifests itself, we will need to swap the order so usernames are checked first.
	 * 
	 * @param userId
	 * @return uuid or null
	 */
	public String getUuidForUserId(String userId);
	
	/**
	 * Is the profile2.privacy.change.enabled flag set in sakai.properties?
	 * If not set, defaults to true
	 * <p>
	 * 
	 * Allows an institution to lock down privacy changes as some things need to be never changed.
	 * Generally should coupled with the sakai.properties that override the default privacy settings
	 * 
	 * @return
	 */
	public boolean isPrivacyChangeAllowedGlobally();
	
	
	/**
	 * Gets the set of sakai.properties that can override the built in defaults. This is then called
	 * when a default privacy record is requested and the values used preferentially.
	 * 
	 * Note that mostly the value is an Integer but for some values its a Boolean, ie checkboxes
	 * 
	 * @return
	 */
	public HashMap<String, Object> getOverriddenPrivacySettings();
	
	/**
	 * Gets the profile2.invisible.users List for user's that should never show in searches or connection lists
	 * @return
	 */
	public List<String> getInvisibleUsers();
	
	/**
	 * Gets the list of usertypes from profile2.allowed.connection.usertypes.TYPE.
	 * <p>This defines what user types can connect to each other. The <b>requestingUserType</b> is appended to the property string
	 * and the list retrieved. If targetUserType is in the list, they can connect (true). If not, false.</p>
	 * <p>If the property does not exist for that userType, they can connect, to maintain backward compatibility and keep it open.</p>
	 *
	 * @param requestingUserType
	 * @param targetUserType
	 * @return
	 */
	public boolean isConnectionAllowedBetweenUserTypes(String requestingUserType, String targetUserType);
	
	/**
	 * Toggle a profile's locked status.
	 * @param userId
	 * @param locked
	 * @return
	 */
	public boolean toggleProfileLocked(String userId, boolean locked);
	
	/**
	 * Generic method to get a configuration parameter from sakai.properties
	 * @param key	key of property
	 * @param def	default value
	 * @return	value or default if none
	 */
	public String getServerConfigurationParameter(String key, String def);
	
	/**
	 * Check if specified site is a My Workspace site
	 * @param siteId id of site
	 * @return true if site is a My Workspace site, false otherwise.
	 */
	public boolean isUserMyWorkspace(String siteId);
	
	/**
	 * Generic method to check if user has permission in site
	 * @param userId userId of user to check permission
	 * @param permission the permission to check in site
	 * @param siteId site id
	 * @return
	 */
	public boolean isUserAllowedInSite(String userId, String permission, String siteId);
	
}
