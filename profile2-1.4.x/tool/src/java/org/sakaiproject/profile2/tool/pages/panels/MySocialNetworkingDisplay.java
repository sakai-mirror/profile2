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

package org.sakaiproject.profile2.tool.pages.panels;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxFallbackLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.sakaiproject.profile2.logic.SakaiProxy;
import org.sakaiproject.profile2.model.UserProfile;
import org.sakaiproject.profile2.util.ProfileUtils;

/**
 * Panel for displaying social networking profile data.
 */
public class MySocialNetworkingDisplay extends Panel {

	private static final long serialVersionUID = 1L;
	private static final Logger log = Logger.getLogger(MySocialNetworkingDisplay.class);
	
	@SpringBean(name="org.sakaiproject.profile2.logic.SakaiProxy")
	private SakaiProxy sakaiProxy;
	
	public MySocialNetworkingDisplay(final String id, final UserProfile userProfile) {
		super(id);

		log.debug("MySocialNetworkingDisplay()");

		add(new Label("heading", new ResourceModel("heading.social")));
			
		// social networking
		String facebookUsername = userProfile.getSocialInfo().getFacebookUsername();
		String linkedinUsername = userProfile.getSocialInfo().getLinkedinUsername();
		String myspaceUsername = userProfile.getSocialInfo().getMyspaceUsername();
		String skypeUsername = userProfile.getSocialInfo().getSkypeUsername();
		String twitterUsername = userProfile.getSocialInfo().getTwitterUsername();
		
		int visibleFieldCount = 0;
		
		//facebook
		WebMarkupContainer facebookContainer = new WebMarkupContainer("facebookContainer");
		facebookContainer.add(new Label("facebookLabel", new ResourceModel("profile.socialnetworking.facebook")));
		facebookContainer.add(new ExternalLink("facebookLink", ProfileUtils.getFacebookURL(facebookUsername), ProfileUtils.getFacebookURL(facebookUsername)));
		add(facebookContainer);
		if(StringUtils.isBlank(facebookUsername)) {
			facebookContainer.setVisible(false);
		} else {
			visibleFieldCount++;
		}
		
		//linkedin
		WebMarkupContainer linkedinContainer = new WebMarkupContainer("linkedinContainer");
		linkedinContainer.add(new Label("linkedinLabel", new ResourceModel("profile.socialnetworking.linkedin")));
		linkedinContainer.add(new ExternalLink("linkedinLink", ProfileUtils.getLinkedinURL(linkedinUsername), ProfileUtils.getLinkedinURL(linkedinUsername)));
		add(linkedinContainer);
		if(StringUtils.isBlank(linkedinUsername)) {
			linkedinContainer.setVisible(false);
		} else {
			visibleFieldCount++;
		}
		
		//myspace
		WebMarkupContainer myspaceContainer = new WebMarkupContainer("myspaceContainer");
		myspaceContainer.add(new Label("myspaceLabel", new ResourceModel("profile.socialnetworking.myspace")));
		myspaceContainer.add(new ExternalLink("myspaceLink", ProfileUtils.getMyspaceURL(myspaceUsername), ProfileUtils.getMyspaceURL(myspaceUsername)));
		add(myspaceContainer);
		if(StringUtils.isBlank(myspaceUsername)) {
			myspaceContainer.setVisible(false);
		} else {
			visibleFieldCount++;
		}
		
		//twitter
		WebMarkupContainer twitterContainer = new WebMarkupContainer("twitterContainer");
		twitterContainer.add(new Label("twitterLabel", new ResourceModel("profile.socialnetworking.twitter")));
		twitterContainer.add(new ExternalLink("twitterLink", ProfileUtils.getTwitterURL(twitterUsername), ProfileUtils.getTwitterURL(twitterUsername)));
		add(twitterContainer);
		if(StringUtils.isBlank(twitterUsername)) {
			twitterContainer.setVisible(false);
		} else {
			visibleFieldCount++;
		}
		
		//skypeme (no URL, as we don't want user skyping themselves)
		WebMarkupContainer skypeContainer = new WebMarkupContainer("skypeContainer");
		skypeContainer.add(new Label("skypeLabel", new ResourceModel("profile.socialnetworking.skype")));
		skypeContainer.add(new Label("skypeLink", skypeUsername));
		add(skypeContainer);
		if (StringUtils.isBlank(skypeUsername)) {
			skypeContainer.setVisible(false);
		} else {
			visibleFieldCount++;
		}
		
		//edit button
		AjaxFallbackLink editButton = new AjaxFallbackLink("editButton",new ResourceModel("button.edit")) {

			private static final long serialVersionUID = 1L;

			public void onClick(AjaxRequestTarget target) {
				Component newPanel = new MySocialNetworkingEdit(id, userProfile);
				newPanel.setOutputMarkupId(true);
				MySocialNetworkingDisplay.this.replaceWith(newPanel);
				if (target != null) {
					target.addComponent(newPanel);
					// resize iframe
					target.appendJavascript("setMainFrameHeight(window.name);");
				}

			}

		};
		editButton.add(new Label("editButtonLabel", new ResourceModel("button.edit")));
		editButton.setOutputMarkupId(true);

		if (userProfile.isLocked() && !sakaiProxy.isSuperUser()) {
			editButton.setVisible(false);
		}

		add(editButton);
		
		// no fields message
		Label noFieldsMessage = new Label("noFieldsMessage", new ResourceModel("text.no.fields"));
		add(noFieldsMessage);
		if (visibleFieldCount > 0) {
			noFieldsMessage.setVisible(false);
		}
	}
	
	
}
