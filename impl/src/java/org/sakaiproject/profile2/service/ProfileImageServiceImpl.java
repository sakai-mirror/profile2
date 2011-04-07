package org.sakaiproject.profile2.service;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.sakaiproject.profile2.logic.ProfileLogic;
import org.sakaiproject.profile2.logic.SakaiProxy;
import org.sakaiproject.profile2.model.ResourceWrapper;
import org.sakaiproject.profile2.util.ProfileConstants;
import org.sakaiproject.profile2.util.ProfileUtils;

public class ProfileImageServiceImpl implements ProfileImageService {

	private static final Logger log = Logger.getLogger(ProfileImageServiceImpl.class);
	
	/**
	 * {@inheritDoc}
	 */
	public ResourceWrapper getProfileImage(String userId, int imageType) {
		return getProfileImage(userId, imageType, null);
	}
	
	/**
	 * {@inheritDoc}
	 */
	public ResourceWrapper getProfileImage(String userId, int imageType, String siteId) {
		
		ResourceWrapper resource = new ResourceWrapper();
		boolean allowed = false;
		
		//check auth and get currentUserUuid
		String currentUserUuid = sakaiProxy.getCurrentUserId();
		if(currentUserUuid == null) {
			throw new SecurityException("You must be logged in to make a request for a user's profile image.");
		}
		
		//convert userId into uuid
		String userUuid = sakaiProxy.getUuidForUserId(userId);
		if(userUuid == null) {
			log.error("Invalid userId: " + userId);
			return null;
		}
		
		//if we have a siteId and it's not a my workspace site, check if the current user has permissions to view the image
		if(StringUtils.isNotBlank(siteId)){
			if(!sakaiProxy.isUserMyWorkspace(siteId)) {
				log.debug("checking if user: " + currentUserUuid + " has permissions in site: " + siteId);
				allowed = sakaiProxy.isUserAllowedInSite(currentUserUuid, ProfileConstants.ROSTER_VIEW_PHOTO, siteId);
			}
		}
		
		//check friend status
		if(!allowed){
			boolean friend = profileLogic.isUserXFriendOfUserY(userUuid, currentUserUuid);
		
			//check if photo is allowed
			allowed = profileLogic.isUserXProfileImageVisibleByUserY(userUuid, currentUserUuid, friend);
		}
		
		//if not allowed, return default
		if(!allowed){
			return getDefaultImage();
		}
		
		
		//check environment configuration (will be url or upload) and get image accordingly
		//fall back by default. there is no real use case for not doing it.
		if(sakaiProxy.getProfilePictureType() == ProfileConstants.PICTURE_SETTING_URL) {
			String url = profileLogic.getExternalImageUrl(userUuid, imageType);
			if(url == null) {
				return getDefaultImage();
			} else {
				resource.setExternal(true);
				resource.setResourceID(url);
				return resource;
			}
		} else {
			resource = profileLogic.getCurrentProfileImageForUserWrapped(userUuid, imageType);
			if(resource == null || resource.getBytes() == null) {
				return getDefaultImage();
			} else {
				return resource;
			}
		} 
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean setProfileImage(String userId, byte[] imageBytes, String mimeType, String fileName) {
		
		//check auth and get currentUserUuid
		String currentUserUuid = sakaiProxy.getCurrentUserId();
		if(currentUserUuid == null) {
			throw new SecurityException("You must be logged in to update a user's profile image.");
		}
		
		//convert userId into uuid
		String userUuid = sakaiProxy.getUuidForUserId(userId);
		if(userUuid == null) {
			log.error("Invalid userId: " + userId);
			return false;
		}
		
		//check admin, or the currentUser and object uuid match
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
		if(!sakaiProxy.saveFile(mainResourceId, userId, fileName, mimeType, imageBytes)) {
			log.error("Couldn't add main image to CHS. Aborting.");
			return false;
		}

		/*
		 * THUMBNAIL PROFILE IMAGE
		 */
		//scale image
		imageBytes = ProfileUtils.scaleImage(imageBytes, ProfileConstants.MAX_THUMBNAIL_IMAGE_XY);
		 
		//create resource ID
		String thumbnailResourceId = sakaiProxy.getProfileImageResourcePath(userId, ProfileConstants.PROFILE_IMAGE_THUMBNAIL);
		log.debug("Profile.ChangeProfilePicture.onSubmit thumbnailResourceId: " + thumbnailResourceId);
		
		//save, if error, warn, erase thumbnail reference, and continue (we really only need the main image)
		if(!sakaiProxy.saveFile(thumbnailResourceId, userId, fileName, mimeType, imageBytes)) {
			log.warn("Couldn't add thumbnail image to CHS. Main image will be used instead.");
			thumbnailResourceId = null;
		}
		
		/*
		 * SAVE IMAGE RESOURCE IDS
		 */
		//save
		if(profileLogic.addNewProfileImage(userId, mainResourceId, thumbnailResourceId)) {
			return true;
		}
		return false;		
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean setProfileImage(String userId, String url, String thumbnail) {
		
		//check auth and get currentUserUuid
		String currentUserUuid = sakaiProxy.getCurrentUserId();
		if(currentUserUuid == null) {
			throw new SecurityException("You must be logged in to update a user's profile image.");
		}
		
		//convert userId into uuid
		String userUuid = sakaiProxy.getUuidForUserId(userId);
		if(userUuid == null) {
			log.error("Invalid userId: " + userId);
			return false;
		}
		
		//check admin, or the currentUser and object uuid match
		if(!sakaiProxy.isSuperUser() && !StringUtils.equals(currentUserUuid, userUuid)) {
			throw new SecurityException("Not allowed to save.");
		}
		
		//check image is actually allowed to be changed
		if(!sakaiProxy.isProfilePictureChangeEnabled()) {
			log.warn("Profile image changes are not permitted as per sakai.properties setting 'profile2.picture.change.enabled'.");
			return false;
		}
		
		//save it
		return profileLogic.saveExternalImage(userId, url, thumbnail);
	}
	
	/**
	 * This is a helper method to take care of getting the default unavailable image and returning it, along with some metadata about it
	 * @return
	 */
	private ResourceWrapper getDefaultImage() {
		ResourceWrapper res = new ResourceWrapper();
		res.setExternal(true);
		res.setResourceID(profileLogic.getUnavailableImageURL());
		return res;
	}
	
	private SakaiProxy sakaiProxy;
	public void setSakaiProxy(SakaiProxy sakaiProxy) {
		this.sakaiProxy = sakaiProxy;
	}
	
	private ProfileLogic profileLogic;
	public void setProfileLogic(ProfileLogic profileLogic) {
		this.profileLogic = profileLogic;
	}

}
