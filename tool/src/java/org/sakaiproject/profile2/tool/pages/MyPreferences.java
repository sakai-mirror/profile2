package org.sakaiproject.profile2.tool.pages;


import java.io.IOException;
import java.io.ObjectInputStream;

import org.apache.log4j.Logger;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormChoiceComponentUpdatingBehavior;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.extensions.ajax.markup.html.AjaxLazyLoadPanel;
import org.apache.wicket.extensions.ajax.markup.html.IndicatingAjaxButton;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.markup.html.form.Radio;
import org.apache.wicket.markup.html.form.RadioGroup;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;
import org.sakaiproject.profile2.exception.ProfilePreferencesNotDefinedException;
import org.sakaiproject.profile2.model.ProfilePreferences;
import org.sakaiproject.profile2.tool.components.EnablingCheckBox;
import org.sakaiproject.profile2.tool.pages.panels.TwitterPrefsPane;
import org.sakaiproject.profile2.util.ProfileConstants;


public class MyPreferences extends BasePage{

	private static final Logger log = Logger.getLogger(MyPreferences.class);
	private transient ProfilePreferences profilePreferences;

	public MyPreferences() {
		
		log.debug("MyPreferences()");
		
		//get current user
		final String userUuid = sakaiProxy.getCurrentUserId();

		//get the preferences object for this user from the database
		profilePreferences = profileLogic.getPreferencesRecordForUser(userUuid);
		
		//if null, create one
		if(profilePreferences == null) {
			profilePreferences = profileLogic.createDefaultPreferencesRecord(userUuid);
			//if its still null, throw exception
			
			if(profilePreferences == null) {
				throw new ProfilePreferencesNotDefinedException("Couldn't create default preferences record for " + userUuid);
			}
			
			//post create event
			sakaiProxy.postEvent(ProfileConstants.EVENT_PREFERENCES_NEW, "/profile/"+userUuid, true);
		}
		
		//get email address for this user
		String emailAddress = sakaiProxy.getUserEmail(userUuid);
		//if no email, set a message into it fo display
		if(emailAddress == null || emailAddress.length() == 0) {
			emailAddress = new ResourceModel("preferences.email.none").getObject().toString();
		}
				
		Label heading = new Label("heading", new ResourceModel("heading.preferences"));
		add(heading);
		
		//feedback for form submit action
		final Label formFeedback = new Label("formFeedback");
		formFeedback.setOutputMarkupPlaceholderTag(true);
		final String formFeedbackId = formFeedback.getMarkupId();
		add(formFeedback);
		
				
		//create model
		CompoundPropertyModel preferencesModel = new CompoundPropertyModel(profilePreferences);
		
		//setup form		
		Form form = new Form("form", preferencesModel);
		form.setOutputMarkupId(true);
		
	
		//EMAIL SECTION
		
		//email settings
		form.add(new Label("emailSectionHeading", new ResourceModel("heading.section.email")));
		form.add(new Label("emailSectionText", new StringResourceModel("preferences.email.message", null, new Object[] { emailAddress })).setEscapeModelStrings(false));
	
		//on/off labels
		form.add(new Label("prefOn", new ResourceModel("preference.option.on")));
		form.add(new Label("prefOff", new ResourceModel("preference.option.off")));

		//request emails
		final RadioGroup emailRequests = new RadioGroup("requestEmailEnabled", new PropertyModel(preferencesModel, "requestEmailEnabled"));
		emailRequests.add(new Radio("requestsOn", new Model(new Boolean(true))));
		emailRequests.add(new Radio("requestsOff", new Model(new Boolean(false))));
		emailRequests.add(new Label("requestsLabel", new ResourceModel("preferences.email.requests")));
		form.add(emailRequests);
		
		//updater
		emailRequests.add(new AjaxFormChoiceComponentUpdatingBehavior() {
            protected void onUpdate(AjaxRequestTarget target) {
            	target.appendJavascript("$('#" + formFeedbackId + "').fadeOut();");
            }
        });
		
		//confirm emails
		final RadioGroup emailConfirms = new RadioGroup("confirmEmailEnabled", new PropertyModel(preferencesModel, "confirmEmailEnabled"));
		emailConfirms.add(new Radio("confirmsOn", new Model(new Boolean(true))));
		emailConfirms.add(new Radio("confirmsOff", new Model(new Boolean(false))));
		emailConfirms.add(new Label("confirmsLabel", new ResourceModel("preferences.email.confirms")));
		form.add(emailConfirms);
		
		//updater
		emailConfirms.add(new AjaxFormChoiceComponentUpdatingBehavior() {
            protected void onUpdate(AjaxRequestTarget target) {
            	target.appendJavascript("$('#" + formFeedbackId + "').fadeOut();");
            }
        });
        
		// TWITTER SECTION

		//headings
		WebMarkupContainer twitterSectionHeadingContainer = new WebMarkupContainer("twitterSectionHeadingContainer");
		twitterSectionHeadingContainer.add(new Label("twitterSectionHeading", new ResourceModel("heading.section.twitter")));
		twitterSectionHeadingContainer.add(new Label("twitterSectionText", new ResourceModel("preferences.twitter.message")));
		form.add(twitterSectionHeadingContainer);
		
		//panel
		if(sakaiProxy.isTwitterIntegrationEnabledGlobally()) {
			form.add(new AjaxLazyLoadPanel("twitterPanel"){
				private static final long serialVersionUID = 1L;
	
				@Override
				public Component getLazyLoadComponent(String markupId) {
					return new TwitterPrefsPane(markupId, userUuid);
				}
			});
		} else {
			form.add(new EmptyPanel("twitterPanel"));
			twitterSectionHeadingContainer.setVisible(false);
		}
		
		//submit button
		IndicatingAjaxButton submitButton = new IndicatingAjaxButton("submit", form) {
			protected void onSubmit(AjaxRequestTarget target, Form form) {
				
				//get the backing model
				ProfilePreferences profilePreferences = (ProfilePreferences) form.getModelObject();
				
				formFeedback.setModel(new ResourceModel("success.preferences.save.ok"));
				formFeedback.add(new AttributeModifier("class", true, new Model("success")));

				//save
				if(profileLogic.savePreferencesRecord(profilePreferences)) {
					log.info("Saved ProfilePreferences for: " + profilePreferences.getUserUuid());
					formFeedback.setModel(new ResourceModel("success.preferences.save.ok"));
					formFeedback.add(new AttributeModifier("class", true, new Model("success")));
					
					//post update event
					sakaiProxy.postEvent(ProfileConstants.EVENT_PREFERENCES_UPDATE, "/profile/"+userUuid, true);
					
					
				} else {
					log.info("Couldn't save ProfilePreferences for: " + profilePreferences.getUserUuid());
					formFeedback.setModel(new ResourceModel("error.preferences.save.failed"));
					formFeedback.add(new AttributeModifier("class", true, new Model("alertMessage")));	
				}
				
				
				target.addComponent(formFeedback);
            }
			
			
		};
		submitButton.setModel(new ResourceModel("button.save.settings"));
		submitButton.setDefaultFormProcessing(false);
		form.add(submitButton);
		
        add(form);
		
	}
	
	/* reinit for deserialisation (ie back button) */
	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
		in.defaultReadObject();
		log.debug("MyPreferences has been deserialized.");
		profileLogic = getProfileLogic();
		sakaiProxy = getSakaiProxy();
	}

}



