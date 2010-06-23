package org.sakaiproject.profile2.model;

import java.io.Serializable;

import org.sakaiproject.entitybroker.entityprovider.annotations.EntityId;


/**
 * Hibernate and EntityProvider model
 * 
 * @author Steve Swinsburg (s.swinsburg@lancaster.ac.uk)
 *
 */
public class ProfilePreferences implements Serializable {

	private static final long serialVersionUID = 1L;
	
	@EntityId
	private String userUuid;
	private boolean requestEmailEnabled;
	private boolean confirmEmailEnabled;
	
	/** 
	 * Empty constructor
	 */
	public ProfilePreferences(){
	}
	
	/**
	 * Basic constructor for creating default records
	 */
	public ProfilePreferences(String userUuid, boolean requestEmailEnabled, boolean confirmEmailEnabled){
		this.userUuid=userUuid;
		this.requestEmailEnabled=requestEmailEnabled;
		this.confirmEmailEnabled=confirmEmailEnabled;
	}
	
	
	public String getUserUuid() {
		return userUuid;
	}


	public void setUserUuid(String userUuid) {
		this.userUuid = userUuid;
	}

	public void setRequestEmailEnabled(boolean requestEmailEnabled) {
		this.requestEmailEnabled = requestEmailEnabled;
	}

	public boolean isRequestEmailEnabled() {
		return requestEmailEnabled;
	}

	public void setConfirmEmailEnabled(boolean confirmEmailEnabled) {
		this.confirmEmailEnabled = confirmEmailEnabled;
	}

	public boolean isConfirmEmailEnabled() {
		return confirmEmailEnabled;
	}

}
