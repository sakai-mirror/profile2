package org.sakaiproject.profile2.logic;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.sakaiproject.id.cover.IdManager;
import org.sakaiproject.profile2.dao.ProfileDao;
import org.sakaiproject.profile2.hbm.model.ProfileImageExternal;
import org.sakaiproject.profile2.hbm.model.ProfileImageOfficial;
import org.sakaiproject.profile2.hbm.model.ProfileImageUploaded;
import org.sakaiproject.profile2.model.GalleryImage;
import org.sakaiproject.profile2.model.Person;
import org.sakaiproject.profile2.model.ProfileImage;
import org.sakaiproject.profile2.model.ProfilePreferences;
import org.sakaiproject.profile2.model.ProfilePrivacy;
import org.sakaiproject.profile2.util.ProfileConstants;
import org.sakaiproject.profile2.util.ProfileUtils;
import org.sakaiproject.user.api.User;

/**
 * Implementation of ProfileImageLogic API
 * 
 * @author Steve Swinsburg (steve.swinsburg@gmail.com)
 *
 */
public class ProfileImageLogicImpl implements ProfileImageLogic {

	private static final Logger log = Logger.getLogger(ProfileImageLogicImpl.class);

	
	/**
 	 * {@inheritDoc}
 	 */
	public ProfileImage getProfileImage(String userUuid, ProfilePreferences prefs, ProfilePrivacy privacy, int size) {
		
		ProfileImage image = new ProfileImage();
		boolean allowed = false;
		String officialImageSource;
		
		String defaultImageUrl = getUnavailableImageURL();
		
		//check prefs supplied was valid, if given
		if(prefs != null && !StringUtils.equals(userUuid, prefs.getUserUuid())) {
			log.error("ProfilePreferences data supplied was not for user: " + userUuid);
			image.setExternalImageUrl(defaultImageUrl);
			return image;
		}
		
		//check privacy supplied was valid, if given
		if(privacy != null && !StringUtils.equals(userUuid, privacy.getUserUuid())) {
			log.error("ProfilePrivacy data supplied was not for user: " + userUuid);
			image.setExternalImageUrl(defaultImageUrl);
			return image;
		}
		
		//check if same user
		String currentUserUuid = sakaiProxy.getCurrentUserId();
		if(StringUtils.equals(userUuid, currentUserUuid)){
			allowed = true;
		}
		
		//if not allowed yet, check we have a privacy record, if not, get one
		if(!allowed && privacy == null) {
			privacy = profileLogic.getPrivacyRecordForUser(userUuid);
			//if still null, default image
			if(privacy == null) {
				log.error("Couldn't retrieve ProfilePrivacy data for user: " + userUuid + ". Using default image.");
				image.setExternalImageUrl(defaultImageUrl);
				return image;
			} 
		}
		
		//if not allowed, check privacy record
		if(!allowed) {
			boolean friend = profileLogic.isUserXFriendOfUserY(userUuid, currentUserUuid);
			allowed = profileLogic.isUserXProfileImageVisibleByUserY(userUuid, privacy, currentUserUuid, friend);
		}
		
		//default if still not allowed
		if(!allowed){
			image.setExternalImageUrl(defaultImageUrl);
			return image;
		}
		
		//lookup global image setting, this will be used if no preferences were supplied.
		int imageType = sakaiProxy.getProfilePictureType();
		
		//if we have no prefs, try to get one, it won't be considered if it is still null.
		if(prefs == null){
			prefs = profileLogic.getPreferencesRecordForUser(userUuid);
		}
		
		//if we have prefs and the conditions are set for a user to be able to make a choice, get the pref.
		if(prefs != null && sakaiProxy.isOfficialImageSelectionEnabled()) {
			if(prefs.isUseOfficialImage()){
				imageType = ProfileConstants.PICTURE_SETTING_OFFICIAL;
			}
		}
		
		//get the image based on the global type/preference
		switch (imageType) {
			case ProfileConstants.PICTURE_SETTING_UPLOAD:
				byte[] bytes = getUploadedProfileImage(userUuid, size);
				//if no uploaded image, set the default image url
				if(bytes == null) {
					image.setExternalImageUrl(defaultImageUrl);
				} else {
					image.setUploadedImage(getUploadedProfileImage(userUuid, size));
				}
			break;
			
			case ProfileConstants.PICTURE_SETTING_URL: 
				image.setExternalImageUrl(getExternalProfileImageUrl(userUuid, size));
			break;
			
			case ProfileConstants.PICTURE_SETTING_OFFICIAL: 
				officialImageSource = sakaiProxy.getOfficialImageSource();
				
				//check source and get appropriate value
				if(StringUtils.equals(officialImageSource, ProfileConstants.OFFICIAL_IMAGE_SETTING_URL)){
					image.setOfficialImageUrl(getOfficialImageUrl(userUuid));
				} else if(StringUtils.equals(officialImageSource, ProfileConstants.OFFICIAL_IMAGE_SETTING_PROVIDER)){
					String data = getOfficialImageEncoded(userUuid);
					if(StringUtils.isBlank(data)) {
						image.setExternalImageUrl(defaultImageUrl);
					}
				}
			break;
			
			default:
				image.setExternalImageUrl(defaultImageUrl);
			break;
				
		}
		
		return image;
	}
	
	/**
 	 * {@inheritDoc}
 	 */
	public ProfileImage getProfileImage(Person person, int size) {
		return getProfileImage(person.getUuid(), person.getPreferences(), person.getPrivacy(), size);
	}
	
	
	/**
	 * {@inheritDoc}
	 */
	public boolean setUploadedProfileImage(String userUuid, byte[] imageBytes, String mimeType, String fileName) {
		
		//check auth and get currentUserUuid
		String currentUserUuid = sakaiProxy.getCurrentUserId();
		if(currentUserUuid == null) {
			throw new SecurityException("You must be logged in to update a user's profile image.");
		}
		
		//check admin, or the currentUser and given uuid match
		if(!sakaiProxy.isSuperUser() && !StringUtils.equals(currentUserUuid, userUuid)) {
			throw new SecurityException("Not allowed to save.");
		}
		
		//check image is actually allowed to be changed
		if(!sakaiProxy.isProfilePictureChangeEnabled()) {
			log.warn("Profile image changes are not permitted as per sakai.properties setting 'profile2.picture.change.enabled'.");
			return false;
		}
		
		/*
		 * MAIN PROFILE IMAGE
		 */
		//scale image
		imageBytes = ProfileUtils.scaleImage(imageBytes, ProfileConstants.MAX_IMAGE_XY);
		 
		//create resource ID
		String mainResourceId = sakaiProxy.getProfileImageResourcePath(userUuid, ProfileConstants.PROFILE_IMAGE_MAIN);
		
		//save, if error, log and return.
		if(!sakaiProxy.saveFile(mainResourceId, userUuid, fileName, mimeType, imageBytes)) {
			log.error("Couldn't add main image to CHS. Aborting.");
			return false;
		}

		/*
		 * THUMBNAIL PROFILE IMAGE
		 */
		//scale image
		imageBytes = ProfileUtils.scaleImage(imageBytes, ProfileConstants.MAX_THUMBNAIL_IMAGE_XY);
		 
		//create resource ID
		String thumbnailResourceId = sakaiProxy.getProfileImageResourcePath(userUuid, ProfileConstants.PROFILE_IMAGE_THUMBNAIL);
		log.debug("Profile.ChangeProfilePicture.onSubmit thumbnailResourceId: " + thumbnailResourceId);
		
		//save, if error, warn, erase thumbnail reference, and continue (we really only need the main image)
		if(!sakaiProxy.saveFile(thumbnailResourceId, userUuid, fileName, mimeType, imageBytes)) {
			log.warn("Couldn't add thumbnail image to CHS. Main image will be used instead.");
			thumbnailResourceId = null;
		}
		
		/*
		 * SAVE IMAGE RESOURCE IDS
		 */
		//save
		ProfileImageUploaded profileImage = new ProfileImageUploaded(userUuid, mainResourceId, thumbnailResourceId, true);
		if(dao.addNewProfileImage(profileImage)){
			log.info("Added a new profile image for user: " + userUuid);
			return true;
		}
		
		return false;
				
	}
	
	/**
	 * {@inheritDoc}
	 */
	public boolean setExternalProfileImage(String userUuid, String url, String thumbnail) {
		
		//check auth and get currentUserUuid
		String currentUserUuid = sakaiProxy.getCurrentUserId();
		if(currentUserUuid == null) {
			throw new SecurityException("You must be logged in to update a user's profile image.");
		}
		
		//check admin, or the currentUser and given uuid match
		if(!sakaiProxy.isSuperUser() && !StringUtils.equals(currentUserUuid, userUuid)) {
			throw new SecurityException("Not allowed to save.");
		}
		
		//check image is actually allowed to be changed
		if(!sakaiProxy.isProfilePictureChangeEnabled()) {
			log.warn("Profile image changes are not permitted as per sakai.properties setting 'profile2.picture.change.enabled'.");
			return false;
		}
		
		//save
		ProfileImageExternal externalImage = new ProfileImageExternal(userUuid, url, thumbnail);
		if(dao.saveExternalImage(externalImage)) {
			log.info("Updated external image record for user: " + userUuid); 
			return true;
		} 
		
		return false;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public boolean addGalleryImage(String userUuid, byte[] imageBytes, String mimeType, String fileName) {

		// check auth and get currentUserUuid
		String currentUserUuid = sakaiProxy.getCurrentUserId();
		if (currentUserUuid == null) {
			throw new SecurityException("You must be logged in to add a gallery image.");
		}

		// check admin, or the currentUser and given uuid match
		if (!sakaiProxy.isSuperUser() && !StringUtils.equals(currentUserUuid, userUuid)) {
			throw new SecurityException("You are not allowed to add a gallery image.");
		}

		String imageId = IdManager.createUuid();

		// create resource ID
		String mainResourcePath = sakaiProxy.getProfileGalleryImagePath(userUuid, imageId);

		byte[] scaledImageBytes = ProfileUtils.scaleImage(imageBytes, ProfileConstants.MAX_GALLERY_IMAGE_XY);
		
		// save image
		if (!sakaiProxy.saveFile(mainResourcePath, userUuid, fileName, mimeType,scaledImageBytes)) {
			log.error("Couldn't add gallery image to CHS. Aborting.");
			return false;
		}

		// create thumbnail
		byte[] thumbnailBytes = ProfileUtils.scaleImage(imageBytes, ProfileConstants.MAX_GALLERY_THUMBNAIL_IMAGE_XY);
		String thumbnailResourcePath = sakaiProxy.getProfileGalleryThumbnailPath(userUuid, imageId);
		sakaiProxy.saveFile(thumbnailResourcePath, userUuid, fileName, mimeType,thumbnailBytes);
		
		//save
		GalleryImage galleryImage = new GalleryImage(userUuid,mainResourcePath, thumbnailResourcePath, fileName);
		if(dao.addNewGalleryImage(galleryImage)){
			log.info("Added new gallery image for user: " + galleryImage.getUserUuid()); 
			return true;
		} 
			
		return false;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public List<GalleryImage> getGalleryImages(String userUuid) {

		// check auth and get currentUserUuid
		String currentUserUuid = sakaiProxy.getCurrentUserId();
		if (currentUserUuid == null) {
			throw new SecurityException("You must be logged in to make a request for a user's gallery images.");
		}

		return dao.getGalleryImages(userUuid);
	}
	
	
	/**
	 * {@inheritDoc}
	 */
	public boolean removeGalleryImage(String userUuid, GalleryImage image) {

		// check auth and get currentUserUuid
		String currentUserUuid = sakaiProxy.getCurrentUserId();
		if (currentUserUuid == null) {
			throw new SecurityException("You must be logged in to remove a gallery image.");
		}

		// check admin, or the currentUser and given uuid match
		if (!sakaiProxy.isSuperUser() && !StringUtils.equals(currentUserUuid, userUuid)) {
			throw new SecurityException("You are not allowed to remove this gallery image.");
		}

		//delete main image
		if (!sakaiProxy.removeResource(image.getMainResource())) {
			log.error("Gallery image not removed: " + image.getMainResource());
		}
		
		//delete thumbnail
		if (!sakaiProxy.removeResource(image.getThumbnailResource())) {
			log.error("Gallery thumbnail not removed: " + image.getThumbnailResource());
		}
		
		//remove record
		if(dao.removeGalleryImage(image)){
			log.info("User: " + userUuid + " removed gallery image: " + image.getId());
			return true;
		} 
		
		return false;
	}
	
	
	/**
	 * {@inheritDoc}
	 */
	public boolean removeGalleryImage(String userId, long imageId) {
		if(userId == null || new Long(imageId) == null){
	  		throw new IllegalArgumentException("Null argument in ProfileLogicImpl.removeGalleryImage()"); 
	  	}
		
		GalleryImage galleryImage = dao.getGalleryImageRecord(userId, imageId);
		
		if(galleryImage == null){
			log.error("GalleryImage record does not exist for userId: " + userId + ", imageId: " + imageId);
			return false;
		}
		
		if(dao.removeGalleryImage(galleryImage)){
			log.info("User: " + userId + " removed gallery image: " + imageId);
			return true;
		} 
		
		return false;
	}
	
	/**
	 * Get the full URL to the default unavailable image defined in ProfileConstants
	 * @return
	 */
	private String getUnavailableImageURL() {
		StringBuilder path = new StringBuilder();
		path.append(sakaiProxy.getServerUrl());
		path.append(ProfileConstants.UNAVAILABLE_IMAGE_FULL);
		return path.toString();
	}
	
	/**
	 * Get the profile image for the given user, allowing fallback if no thumbnail exists.
	 * 
	 * @param userUuid 		the uuid of the user we are querying
	 * @param size			comes from ProfileConstants, main or thumbnail, also maps to a directory in ContentHosting
	 * @return byte[] or null
	 * 
	 * <p>Note: if thumbnail is requested and none exists, the main image will be returned instead. It can be scaled in the markup.</p>
	 *
	 */
	private byte[] getUploadedProfileImage(String userUuid, int size) {
		
		byte[] image = null;
		
		//get record from db
		ProfileImageUploaded profileImage = dao.getCurrentProfileImageRecord(userUuid);
		
		if(profileImage == null) {
			log.debug("ProfileLogic.getUploadedProfileImage() null for userUuid: " + userUuid);
			return null;
		}
		
		//get main image
		if(size == ProfileConstants.PROFILE_IMAGE_MAIN) {
			image = sakaiProxy.getResource(profileImage.getMainResource());
		}
		
		//or get thumbnail
		if(size == ProfileConstants.PROFILE_IMAGE_THUMBNAIL) {
			image = sakaiProxy.getResource(profileImage.getThumbnailResource());
			if(image == null) {
				image = sakaiProxy.getResource(profileImage.getMainResource());
			}
		}
		
		return image;
	}
	
	/**
	 * Get the URL to an image that a user has specified as their profile image
	 * @param userId		uuid of user
	 * @param size			comes from ProfileConstants. main or thumbnail.
	 *
	 * <p>Note: if thumbnail is requested and none exists, the main image will be returned instead. It can be scaled in the markup.</p>
	 * 
	 * @return url to the image, or a default image if none.
	 */
	private String getExternalProfileImageUrl(final String userUuid, final int size) {
	
		//get external image record for this user
		ProfileImageExternal externalImage = dao.getExternalImageRecordForUser(userUuid);
		
		//setup default
		String defaultImageUrl = getUnavailableImageURL();
		
		//if none, return null
    	if(externalImage == null) {
    		return getUnavailableImageURL();
    	}
    	
    	//else return the url for the type they requested
    	if(size == ProfileConstants.PROFILE_IMAGE_MAIN) {
    		String url = externalImage.getMainUrl();
    		if(StringUtils.isBlank(url)) {
    			return defaultImageUrl;
    		}
    		return url;
    	}
    	
    	if(size == ProfileConstants.PROFILE_IMAGE_THUMBNAIL) {
    		String url = externalImage.getThumbnailUrl();
    		if(StringUtils.isBlank(url)) {
    			url = externalImage.getMainUrl();
    			if(StringUtils.isBlank(url)) {
    				return defaultImageUrl;
    			}
    		}
    		return url;
    	}
    	
    	//no url	
    	log.info("ProfileLogic.getExternalProfileImageUrl. No URL for userId: " + userUuid + ", imageType: " + size + ". Returning default.");  
    	return defaultImageUrl;
	}

	
	
	/**
	 * Get the URL to a user's official profile image
	 * @param userUuid		uuid of user
	 * 
	 * @return url or a default image if none
	 */
	private String getOfficialImageUrl(final String userUuid) {
		
		//get external image record for this user
		ProfileImageOfficial official = dao.getOfficialImageRecordForUser(userUuid);
		
		//setup default
		String defaultImageUrl = getUnavailableImageURL();
		
		//if none, return null
    	if(official == null) {
    		return defaultImageUrl;
    	}
    	
    	if(StringUtils.isBlank(official.getUrl())) {
        	log.info("ProfileLogic.getOfficialImageUrl. No URL for userUuid: " + userUuid + ". Returning default.");  
			return defaultImageUrl;
		}
    	
    	return official.getUrl();
	}
	
	/**
	 * Get the official image data from the user properties, encoded in BASE64
	 * @param userUuid	uuid of user
	 * @return base64 encoded data, or null.
	 */
	private String getOfficialImageEncoded(final String userUuid) {
		User u = sakaiProxy.getUserById(userUuid);
		return u.getProperties().getProperty(sakaiProxy.getOfficialImageAttribute());
	}
	
	/**
	 * Get a list of all SakaiPerson's userIds (ie list of all people with profile records)
	 *
	 * @return	List of Sakai userId's 
	 */
	private List<String> getAllSakaiPersonIds() {
		return dao.getAllSakaiPersonIds();
	}
	
	//service init
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
		List<String> allUsers = new ArrayList<String>(getAllSakaiPersonIds());
		
		if(allUsers.isEmpty()){
			log.info("Profile2 conversion util: No SakaiPersons to process.");
			return;
		}
		//for each, do they have a profile image record. if so, skip (perhaps null the SakaiPerson JPEG_PHOTO bytes?)
		for(Iterator<String> i = allUsers.iterator(); i.hasNext();) {
			String userUuid = (String)i.next();
			
			//get image record from dao directly, we don't need privacy/prefs here
			ProfileImageUploaded uploadedProfileImage = dao.getCurrentProfileImageRecord(userUuid);
			
			if(uploadedProfileImage != null) {
				log.info("Profile2 conversion util: ProfileImage record exists for " + userUuid + ". Nothing to do here, skipping to next section...");
			} else {
				log.info("Profile2 conversion util: No existing ProfileImage record for " + userUuid + ". Processing...");
				
				//get photo from SakaiPerson
				byte[] image = sakaiProxy.getSakaiPersonJpegPhoto(userUuid);
				
				//if none, nothing to do
				if(image == null || image.length == 0) {
					log.info("Profile2 conversion util: No image binary to convert for " + userUuid + ". Skipping to next section...");
				} else {
					
					//set some defaults for the image we are adding to ContentHosting
					String fileName = "Profile Image";
					String mimeType = "image/jpeg";
					
					//scale the main image
					byte[] imageMain = ProfileUtils.scaleImage(image, ProfileConstants.MAX_IMAGE_XY);
					
					//create resource ID
					String mainResourceId = sakaiProxy.getProfileImageResourcePath(userUuid, ProfileConstants.PROFILE_IMAGE_MAIN);
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
					byte[] imageThumbnail = ProfileUtils.scaleImage(image, ProfileConstants.MAX_THUMBNAIL_IMAGE_XY);
					 
					//create resource ID
					String thumbnailResourceId = sakaiProxy.getProfileImageResourcePath(userUuid, ProfileConstants.PROFILE_IMAGE_THUMBNAIL);
	
					log.info("Profile2 conversion util: thumbnailResourceId:" + thumbnailResourceId);
					
					//save, if error, log and return.
					if(!sakaiProxy.saveFile(thumbnailResourceId, userUuid, fileName, mimeType, imageThumbnail)) {
						log.warn("Profile2 conversion util: Saving thumbnail profile image failed. Main image will be used instead.");
						thumbnailResourceId = null;
					}
	
					/*
					 * SAVE IMAGE RESOURCE IDS
					 */
					uploadedProfileImage = new ProfileImageUploaded(userUuid, mainResourceId, thumbnailResourceId, true);
					if(dao.addNewProfileImage(uploadedProfileImage)){
						log.info("Profile2 conversion util: Binary image converted and saved for " + userUuid);
					} else {
						log.warn("Profile2 conversion util: Binary image conversion failed for " + userUuid);
					}					
					
				}
			} 
			
			//process any image URLs, if they don't already have a valid record.
			ProfileImageExternal externalProfileImage = dao.getExternalImageRecordForUser(userUuid);
			if(externalProfileImage != null) {
				log.info("Profile2 conversion util: ProfileImageExternal record exists for " + userUuid + ". Nothing to do here, skipping...");
			} else {
				log.info("Profile2 conversion util: No existing ProfileImageExternal record for " + userUuid + ". Processing...");
				
				String url = sakaiProxy.getSakaiPersonImageUrl(userUuid);
				
				//if none, nothing to do
				if(StringUtils.isBlank(url)) {
					log.info("Profile2 conversion util: No url image to convert for " + userUuid + ". Skipping...");
				} else {
					externalProfileImage = new ProfileImageExternal(userUuid, url, null);
					if(dao.saveExternalImage(externalProfileImage)) {
						log.info("Profile2 conversion util: Url image converted and saved for " + userUuid);
					} else {
						log.warn("Profile2 conversion util: Url image conversion failed for " + userUuid);
					}
				}
				
			}
			
			log.info("Profile2 conversion util: Finished converting user profile for: " + userUuid);
			//go to next user
		}
		
		return;
	}
	
	
	
	private SakaiProxy sakaiProxy;
	public void setSakaiProxy(SakaiProxy sakaiProxy) {
		this.sakaiProxy = sakaiProxy;
	}
	
	private ProfileLogic profileLogic;
	public void setProfileLogic(ProfileLogic profileLogic) {
		this.profileLogic = profileLogic;
	}
	
	private ProfileDao dao;
	public void setDao(ProfileDao dao) {
		this.dao = dao;
	}

	
	
}
