/**
 * Copyright (c) 2008-2010 The Sakai Foundation
 *
 * Licensed under the Educational Community License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *             http://www.osedu.org/licenses/ECL-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.sakaiproject.profile2.tool.pages;

import java.io.IOException;
import java.io.ObjectInputStream;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.navigation.paging.AjaxPagingNavigator;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.ajax.markup.html.IndicatingAjaxButton;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.sakaiproject.profile2.model.Message;
import org.sakaiproject.profile2.model.MessageParticipant;
import org.sakaiproject.profile2.model.MessageThread;
import org.sakaiproject.profile2.model.ProfilePreferences;
import org.sakaiproject.profile2.model.ProfilePrivacy;
import org.sakaiproject.profile2.tool.components.ProfileImageRenderer;
import org.sakaiproject.profile2.tool.dataproviders.MessageThreadsDataProvider;
import org.sakaiproject.profile2.tool.pages.panels.ComposeNewMessage;
import org.sakaiproject.profile2.tool.pages.panels.ConfirmedFriends;
import org.sakaiproject.profile2.util.ProfileConstants;
import org.sakaiproject.profile2.util.ProfileUtils;

public class MyMessageThreads extends BasePage {
	
	private static final long serialVersionUID = 1L;
	private static final Logger log = Logger.getLogger(ConfirmedFriends.class);
    
	public MyMessageThreads() {
		
		log.debug("MessageThreads()");
		
		//get current user
		final String currentUserUuid = sakaiProxy.getCurrentUserId();
		
		//new message panel
		final ComposeNewMessage newMessagePanel = new ComposeNewMessage("newMessagePanel");
		newMessagePanel.setOutputMarkupPlaceholderTag(true);
		newMessagePanel.setVisible(false);
		add(newMessagePanel);
		
		//no messages label
		Label noMessagesLabel = new Label("noMessagesLabel");
		noMessagesLabel.setOutputMarkupPlaceholderTag(true);
		add(noMessagesLabel);
		
		//new message button
		Form<Void> form = new Form<Void>("form");
		IndicatingAjaxButton newMessageButton = new IndicatingAjaxButton("newMessage", form) {
			private static final long serialVersionUID = 1L;

			public void onSubmit(AjaxRequestTarget target, Form<?> form) {
				//show panel
				newMessagePanel.setVisible(true);
				target.addComponent(newMessagePanel);
				
				//resize iframe
				target.prependJavascript("setMainFrameHeight(window.name);");
			}
		};
		newMessageButton.setModel(new ResourceModel("button.message.new"));
		newMessageButton.setDefaultFormProcessing(false);
		form.add(newMessageButton);
		add(form);
		
		//container which wraps list
		final WebMarkupContainer messageThreadListContainer = new WebMarkupContainer("messageThreadListContainer");
		messageThreadListContainer.setOutputMarkupId(true);
		
		//get our list of messages
		final MessageThreadsDataProvider provider = new MessageThreadsDataProvider(currentUserUuid);
		int numMessages = provider.size();
		
		//message list
		DataView<MessageThread> messageThreadList = new DataView<MessageThread>("messageThreadList", provider) {
			private static final long serialVersionUID = 1L;

			protected void populateItem(final Item<MessageThread> item) {
		        
				MessageThread thread = (MessageThread)item.getDefaultModelObject();
				Message message = thread.getMostRecentMessage();
				String messageFromUuid = message.getFrom();
				
				//we need to know if this message has been read or not so we can style it accordingly
				//we only need this if we didn't send the message
				MessageParticipant participant = null;
				
				boolean messageOwner = false;
				if(StringUtils.equals(messageFromUuid, currentUserUuid)) {
					messageOwner = true;
				}
				if(!messageOwner) {
					participant = profileLogic.getMessageParticipant(message.getId(), currentUserUuid);
				}
				
				//prefs and privacy
				ProfilePreferences prefs = profileLogic.getPreferencesRecordForUser(messageFromUuid);
				ProfilePrivacy privacy = profileLogic.getPrivacyRecordForUser(messageFromUuid);
				
				//photo link
				AjaxLink<String> photoLink = new AjaxLink<String>("photoLink", new Model<String>(messageFromUuid)) {
					private static final long serialVersionUID = 1L;
					public void onClick(AjaxRequestTarget target) {
						setResponsePage(new ViewProfile(getModelObject()));
					}
					
				};
				
				//photo
				photoLink.add(new ProfileImageRenderer("messagePhoto", messageFromUuid, prefs, privacy, ProfileConstants.PROFILE_IMAGE_THUMBNAIL, false));
				item.add(photoLink);
				
				
				//name link
				AjaxLink<String> messageFromLink = new AjaxLink<String>("messageFromLink", new Model<String>(messageFromUuid)) {
					private static final long serialVersionUID = 1L;
					public void onClick(AjaxRequestTarget target) {
						setResponsePage(new ViewProfile(getModelObject()));
					}
					
				};
				messageFromLink.add(new Label("messageFromName", new Model<String>(sakaiProxy.getUserDisplayName(messageFromUuid))));
				item.add(messageFromLink);
			
				//date
				item.add(new Label("messageDate", ProfileUtils.convertDateToString(message.getDatePosted(), ProfileConstants.MESSAGE_DISPLAY_DATE_FORMAT)));
				
				//subject link
				AjaxLink<MessageThread> messageSubjectLink = new AjaxLink<MessageThread>("messageSubjectLink", new Model<MessageThread>(thread)) {
					private static final long serialVersionUID = 1L;
					public void onClick(AjaxRequestTarget target) {
						//load messageview panel
						setResponsePage(new MyMessageView(currentUserUuid, getModelObject().getId(), getModelObject().getSubject()));
					}
					
				};
				messageSubjectLink.add(new Label("messageSubject", new Model<String>(thread.getSubject())));
				item.add(messageSubjectLink);
				
				//message body
				item.add(new Label("messageBody", new Model<String>(StringUtils.abbreviate(message.getMessage(), ProfileConstants.MESSAGE_PREVIEW_MAX_LENGTH))));
				
				//highlight if new
				if(!messageOwner && !participant.isRead()) {
					item.add(new AttributeAppender("class", true, new Model<String>("unread-message"), " "));
				}
				
				
				
				
				
				item.setOutputMarkupId(true);
		    }
			
		};
		messageThreadList.setOutputMarkupId(true);
		messageThreadList.setItemsPerPage(ProfileConstants.MAX_MESSAGES_PER_PAGE);
		messageThreadListContainer.add(messageThreadList);
		add(messageThreadListContainer);
		
		//pager
		AjaxPagingNavigator pager = new AjaxPagingNavigator("navigator", messageThreadList);
		add(pager);
	
		//initially, if no message threads to show, hide container and pager, set and show label
		if(numMessages == 0) {
			messageThreadListContainer.setVisible(false);
			pager.setVisible(false);
			
			noMessagesLabel.setDefaultModel(new ResourceModel("text.messages.none"));
			noMessagesLabel.setVisible(true);
		}
		
		//also, if num less than num required for pager, hide it
		if(numMessages <= ProfileConstants.MAX_MESSAGES_PER_PAGE) {
			pager.setVisible(false);
		}
		
		
	}
	
	/* reinit for deserialisation (ie back button) */
	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
		in.defaultReadObject();
		log.debug("MessageThreads has been deserialized.");
		//re-init our transient objects
		
	}
	
}
