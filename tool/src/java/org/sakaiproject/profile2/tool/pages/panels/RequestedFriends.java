package org.sakaiproject.profile2.tool.pages.panels;

import java.io.IOException;
import java.io.ObjectInputStream;

import org.apache.log4j.Logger;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.image.ContextImage;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.sakaiproject.profile2.logic.ProfileLogic;
import org.sakaiproject.profile2.logic.SakaiProxy;
import org.sakaiproject.profile2.model.ProfilePrivacy;
import org.sakaiproject.profile2.tool.ProfileApplication;
import org.sakaiproject.profile2.tool.components.ProfileImageRenderer;
import org.sakaiproject.profile2.tool.components.ProfileStatusRenderer;
import org.sakaiproject.profile2.tool.dataproviders.RequestedFriendsDataProvider;
import org.sakaiproject.profile2.tool.models.FriendAction;
import org.sakaiproject.profile2.tool.pages.MyFriends;
import org.sakaiproject.profile2.tool.pages.ViewProfile;
import org.sakaiproject.profile2.tool.pages.windows.ConfirmFriend;
import org.sakaiproject.profile2.tool.pages.windows.IgnoreFriend;
import org.sakaiproject.profile2.util.ProfileConstants;

public class RequestedFriends extends Panel {
	
	private static final long serialVersionUID = 1L;
	private static final Logger log = Logger.getLogger(RequestedFriends.class);
	private transient SakaiProxy sakaiProxy;
	private transient ProfileLogic profileLogic; 
	private int numRequestedFriends = 0;
	
	public RequestedFriends(final String id, final MyFriends parent, final String userUuid) {
		super(id);
		
		log.debug("RequestedFriends()");
		
		//get SakaiProxy
		sakaiProxy = ProfileApplication.get().getSakaiProxy();
		
		//get ProfileLogic
		profileLogic = ProfileApplication.get().getProfileLogic();
		
		//setup model to store the actions in the modal windows
		final FriendAction friendActionModel = new FriendAction();
	
		//get our list of friend requests as an IDataProvider
		RequestedFriendsDataProvider provider = new RequestedFriendsDataProvider(userUuid);
		
		//init number of requests
		numRequestedFriends = provider.size();
		
		//model so we can update the number of requests
		IModel numRequestedFriendsModel = new Model() {
			private static final long serialVersionUID = 1L;

			public Object getObject() {
				return numRequestedFriends;
			} 
		};
		
		//heading
		final WebMarkupContainer requestedFriendsHeading = new WebMarkupContainer("requestedFriendsHeading");
		requestedFriendsHeading.add(new Label("requestedFriendsLabel", new ResourceModel("heading.friend.requests")));
		requestedFriendsHeading.add(new Label("requestedFriendsNumber", numRequestedFriendsModel));
		requestedFriendsHeading.setOutputMarkupId(true);
		add(requestedFriendsHeading);
		
		//container which wraps list
		final WebMarkupContainer requestedFriendsContainer = new WebMarkupContainer("requestedFriendsContainer");
		requestedFriendsContainer.setOutputMarkupId(true);
		
		//search results
		DataView requestedFriendsDataView = new DataView("results-list", provider) {
			private static final long serialVersionUID = 1L;

			protected void populateItem(final Item item) {
		        
		    	//get friendUuid
		    	final String friendUuid = (String)item.getModelObject();
		    			    	
		    	//get name
		    	String displayName = sakaiProxy.getUserDisplayName(friendUuid);
		    	
		    	//get privacy record for the friend
		    	ProfilePrivacy privacy = profileLogic.getPrivacyRecordForUser(friendUuid);
		    	
		    	//is profile image allowed to be viewed by this user/friend?
				final boolean isProfileImageAllowed = profileLogic.isUserXProfileImageVisibleByUserY(friendUuid, privacy, userUuid, false);
				
				//image
				item.add(new ProfileImageRenderer("result-photo", friendUuid, isProfileImageAllowed, ProfileConstants.PROFILE_IMAGE_THUMBNAIL, true));
		    			    	
		    	//name and link to profile
		    	Link profileLink = new Link("result-profileLink") {
					private static final long serialVersionUID = 1L;

					public void onClick() {
						setResponsePage(new ViewProfile(friendUuid));
					}
					
				};
				profileLink.add(new Label("result-name", displayName));
		    	item.add(profileLink);
		    	
		    	
		    	//status component
				ProfileStatusRenderer status = new ProfileStatusRenderer("result-status", friendUuid, privacy, userUuid, false, "friendsListInfoStatusMessage", "friendsListInfoStatusDate");
				status.setOutputMarkupId(true);
				item.add(status);
		    	
		    	
		    	
		    	/* ACTIONS */
		    	
		    	//CONFIRM FRIEND MODAL WINDOW
				final ModalWindow confirmFriendWindow = new ModalWindow("confirmFriendWindow");
				confirmFriendWindow.setContent(new ConfirmFriend(confirmFriendWindow.getContentId(), confirmFriendWindow, friendActionModel, userUuid, friendUuid)); 
				
				//IGNORE FRIEND MODAL WINDOW
				final ModalWindow ignoreFriendWindow = new ModalWindow("ignoreFriendWindow");
				ignoreFriendWindow.setContent(new IgnoreFriend(ignoreFriendWindow.getContentId(), ignoreFriendWindow, friendActionModel, userUuid, friendUuid)); 
				
				
				
				//IGNORE FRIEND LINK
		    	final AjaxLink ignoreFriendLink = new AjaxLink("ignoreFriendLink") {
					private static final long serialVersionUID = 1L;
					public void onClick(AjaxRequestTarget target) {
						
						ignoreFriendWindow.show(target);
						target.appendJavascript("fixWindowVertical();"); 

					}
				};
				ContextImage ignoreFriendIcon = new ContextImage("ignoreFriendIcon",new Model(ProfileConstants.CANCEL_IMG));
				ignoreFriendLink.add(ignoreFriendIcon);
				ignoreFriendLink.add(new AttributeModifier("title", true,new ResourceModel("link.title.ignorefriend")));
				item.add(ignoreFriendLink);
				
				//CONFIRM FRIEND LINK
		    	final AjaxLink confirmFriendLink = new AjaxLink("confirmFriendLink") {
					private static final long serialVersionUID = 1L;
					public void onClick(AjaxRequestTarget target) {
						
						confirmFriendWindow.show(target);
						target.appendJavascript("fixWindowVertical();"); 

					}
				};
				ContextImage confirmFriendIcon = new ContextImage("confirmFriendIcon",new Model(ProfileConstants.ACCEPT_IMG));
				confirmFriendLink.add(confirmFriendIcon);
				confirmFriendLink.add(new AttributeModifier("title", true,new ResourceModel("link.title.confirmfriend")));
				item.add(confirmFriendLink);

				
				
				
				
				// CONFIRM FRIEND MODAL WINDOW HANDLER 
				confirmFriendWindow.setWindowClosedCallback(new ModalWindow.WindowClosedCallback() {
					private static final long serialVersionUID = 1L;
					public void onClose(AjaxRequestTarget target){
		            	if(friendActionModel.isConfirmed()) { 
		            		
		            		//decrement number of requests
		            		numRequestedFriends--;
		            		
		            		//remove friend item from display
		            		target.appendJavascript("$('#" + item.getMarkupId() + "').slideUp();");
		            		
		            		//update label
		            		target.addComponent(requestedFriendsHeading);
		            		
		            		//TODO repaint confirmed friends panel by calling method in MyFriends to repaint it for us
		            		parent.updateConfirmedFriends(target, userUuid);
		            		
		            		//if none left, hide everything
		            		if(numRequestedFriends==0) {
		            			target.appendJavascript("$('#" + requestedFriendsHeading.getMarkupId() + "').fadeOut();");
		            			target.appendJavascript("$('#" + requestedFriendsContainer.getMarkupId() + "').fadeOut();");
		            		}
		            		
		            	}
		            }
		        });
				item.add(confirmFriendWindow);
				
				// IGNORE FRIEND MODAL WINDOW HANDLER 
				ignoreFriendWindow.setWindowClosedCallback(new ModalWindow.WindowClosedCallback() {
					private static final long serialVersionUID = 1L;
					public void onClose(AjaxRequestTarget target){
		            	if(friendActionModel.isIgnored()) { 
		            		
		            		//decrement number of requests
		            		numRequestedFriends--;
		            		
		            		//remove friend item from display
		            		target.appendJavascript("$('#" + item.getMarkupId() + "').slideUp();");
		            		
		            		//update label
		            		target.addComponent(requestedFriendsHeading);
		            				            		
		            		//if none left, hide everything
		            		if(numRequestedFriends==0) {
		            			target.appendJavascript("$('#" + requestedFriendsHeading.getMarkupId() + "').fadeOut();");
		            			target.appendJavascript("$('#" + requestedFriendsContainer.getMarkupId() + "').fadeOut();");
		            		}
		            		
		            	}
		            }
		        });
				item.add(ignoreFriendWindow);
				
				
				item.setOutputMarkupId(true);
				
		    }
			
		};
		requestedFriendsDataView.setOutputMarkupId(true);
		requestedFriendsContainer.add(requestedFriendsDataView);

		//add results container
		add(requestedFriendsContainer);
		
		//initially, if no requests, hide everything
		if(numRequestedFriends == 0) {
			this.setVisible(false);
		}
		
	}
	
	/* reinit for deserialisation (ie back button) */
	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
		in.defaultReadObject();
		log.debug("RequestedFriends has been deserialized");
		//re-init our transient objects
		profileLogic = ProfileApplication.get().getProfileLogic();
		sakaiProxy = ProfileApplication.get().getSakaiProxy();
	}
	
}
