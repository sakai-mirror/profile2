package org.sakaiproject.profile2.tool.pages.panels;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.IndicatingAjaxButton;
import org.apache.wicket.extensions.ajax.markup.html.IndicatingAjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.image.ContextImage;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.sakaiproject.profile2.logic.ProfileLogic;
import org.sakaiproject.profile2.model.ExternalIntegrationInfo;
import org.sakaiproject.profile2.tool.Locator;
import org.sakaiproject.profile2.tool.models.SimpleText;
import org.sakaiproject.profile2.util.ProfileConstants;

import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.http.AccessToken;
import twitter4j.http.RequestToken;

public class TwitterPrefsPane extends Panel {

	private static final long serialVersionUID = 1L;
	private static final Logger log = Logger.getLogger(TwitterPrefsPane.class);
	private transient ExternalIntegrationInfo externalIntegrationInfo;
	private transient RequestToken requestToken;
	private transient ProfileLogic profileLogic; 
	
	private Fragment currentFragment;
	
	public TwitterPrefsPane(String id, String userUuid) {
		super(id);
		
		//get API's
		profileLogic = getProfileLogic();
		
		//get record
		externalIntegrationInfo = profileLogic.getExternalIntegrationInfo(userUuid);
		
		//setup Twitter request token
		setTwitterRequestToken();
		
		//setup relevant fragment
		if(externalIntegrationInfo.isTwitterAlreadyConfigured()) {
			currentFragment = linkedFragment();
		} else {
			currentFragment = unlinkedFragment();
		}

		add(currentFragment);
	}
	
	/**
	 * Fragment which returns the components for the unlinked view
	 * @return
	 */
	private Fragment unlinkedFragment() {
		
		Fragment frag = new Fragment("fragmentContainer", "unlinked", this);
		
		//twitter auth form
		SimpleText twitterModel = new SimpleText();
		final Form twitterForm = new Form("twitterForm", new Model(twitterModel));
		
		//auth code
		final TextField twitterAuthCode = new TextField("twitterAuthCode", new PropertyModel(twitterModel, "text"));        
		twitterAuthCode.setOutputMarkupId(true);
		twitterAuthCode.setEnabled(false);
		twitterForm.add(twitterAuthCode);

		//save button
		final IndicatingAjaxButton twitterSubmit = new IndicatingAjaxButton("twitterSubmit", twitterForm) {
			private static final long serialVersionUID = 1L;

			protected void onSubmit(AjaxRequestTarget target, Form form) {
				
				SimpleText stringModel = (SimpleText) form.getModelObject();
				String accessCode = stringModel.getText();
				
				if(StringUtils.isBlank(accessCode)) {
					//TODO change this
					target.appendJavascript("alert('AccessCode was null.');");
					return;
				}
				
				AccessToken accessToken = getOAuthAccessToken(accessCode);				
				if(accessToken == null) {
					//TODO change this
					target.appendJavascript("alert('AccessToken was null.');");
					return;
				}
				
				//set
				externalIntegrationInfo.setTwitterToken(accessToken.getToken());
				externalIntegrationInfo.setTwitterSecret(accessToken.getTokenSecret());

				//save, replace fragments
				if(profileLogic.updateExternalIntegrationInfo(externalIntegrationInfo)) {
					switchContentFragments(linkedFragment(), target);
				} else {
					target.appendJavascript("alert('Couldn't save info');");
					return;
				}
				
			}
		};
		twitterSubmit.setEnabled(false);
		twitterSubmit.setModel(new ResourceModel("button.link"));
		twitterForm.add(twitterSubmit);
		
		frag.add(twitterForm);
		
		
		//auth link/label
		final IndicatingAjaxLink twitterAuthLink = new IndicatingAjaxLink("twitterAuthLink") {
			private static final long serialVersionUID = 1L;

			public void onClick(AjaxRequestTarget target) {
				
				//get auth url
				String authorisationUrl = getTwitterAuthorisationUrl();
				
				if(StringUtils.isBlank(authorisationUrl)){
					//TODO change this
					target.appendJavascript("alert('Error getting the Twitter authorisation URL. Please try again later.');");
					return;
				}
				
				//open window
				target.appendJavascript("window.open('" + requestToken.getAuthorizationURL() + "','Link your Twitter account','width=800,height=400');");
				
				//enable code box and button
				twitterAuthCode.setEnabled(true);
				twitterSubmit.setEnabled(true);
				target.addComponent(twitterAuthCode);
				target.addComponent(twitterSubmit);
				
			}
		};
		Label twitterAuthLabel = new Label("twitterAuthLabel", new ResourceModel("twitter.auth.do"));
		twitterAuthLink.add(twitterAuthLabel);
		frag.add(twitterAuthLink);
		
		
		frag.setOutputMarkupId(true);
		
		return frag;
	}
	
	
	/**
	 * Fragment which returns the components for the linked view
	 * @return
	 */
	private Fragment linkedFragment() {
		
		Fragment frag = new Fragment("fragmentContainer", "linked", this);
		
		//label
		frag.add(new Label("twitterAuthLabel", new ResourceModel("twitter.auth.linked")));
		
		//screen name
		String twitterName = profileLogic.getTwitterName(externalIntegrationInfo);
		Label twitterAuthName = new Label("twitterAuthName", new Model(twitterName));
		
		if(StringUtils.isBlank(twitterName)){
			twitterAuthName.setModel(new ResourceModel("error.twitter.details.invalid"));
		}
		frag.add(twitterAuthName);

		//remove link
		IndicatingAjaxLink twitterAuthRemoveLink  = new IndicatingAjaxLink("twitterAuthRemoveLink") {
			private static final long serialVersionUID = 1L;

			public void onClick(AjaxRequestTarget target) {
				externalIntegrationInfo.setTwitterToken(null);
				externalIntegrationInfo.setTwitterSecret(null);
				
				//remove details
				if(profileLogic.updateExternalIntegrationInfo(externalIntegrationInfo)) {
					switchContentFragments(unlinkedFragment(), target);
				} else {
					target.appendJavascript("alert('Couldn't remove info');");
					return;
				}
			}
		};
		
		ContextImage twitterAuthRemoveIcon = new ContextImage("twitterAuthRemoveIcon",new Model(ProfileConstants.CROSS_IMG));
		twitterAuthRemoveLink.add(twitterAuthRemoveIcon);
		twitterAuthRemoveLink.add(new AttributeModifier("title", true,new ResourceModel("link.title.unlinktwitter")));
		frag.add(twitterAuthRemoveLink);
		
		frag.setOutputMarkupId(true);
		
		return frag;
	}
	
	/**
	 * Helper to switch content fragments for us
	 * 
	 * @param replacement	replacement Fragment
	 * @param target		AjaxRequestTarget
	 */
	private void switchContentFragments(Fragment replacement, AjaxRequestTarget target) {
		
		replacement.setOutputMarkupId(true);
		currentFragment.replaceWith(replacement);
		if(target != null) {
			target.addComponent(replacement);
			//resize iframe
			target.appendJavascript("setMainFrameHeight(window.name);");
		}
		
		//must keep reference up to date
		currentFragment=replacement;
	}

	/**
	 * Helper to get and set the Twitter request token we need for linking accounts
	 */
	private void setTwitterRequestToken() {
		
		Map<String,String> config = profileLogic.getTwitterOAuthConsumerDetails();
		
		Twitter twitter = new TwitterFactory().getInstance();
	    twitter.setOAuthConsumer(config.get("key"), config.get("secret"));
	    
	    try {
			requestToken = twitter.getOAuthRequestToken();
		} catch (TwitterException e) {
			e.printStackTrace();
		}
	   
	}
	
	/**
	 * Helper to get the user access token from the request token and supplied access code
	 * @param accessCode
	 * @return
	 */
	private AccessToken getOAuthAccessToken(String accessCode) {
		
		Map<String,String> config = profileLogic.getTwitterOAuthConsumerDetails();

		Twitter twitter = new TwitterFactory().getInstance();
	    twitter.setOAuthConsumer(config.get("key"), config.get("secret"));
	    
	    try {
			return twitter.getOAuthAccessToken(requestToken, accessCode);
		} catch (TwitterException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	

	/**
	 * Helper to get the Twitter auth url
	 * @return
	 */
	private String getTwitterAuthorisationUrl() {
		
		if(requestToken == null) {
			return null;
		}
		return requestToken.getAuthenticationURL();
	}
	
	/* reinit for deserialisation (ie back button) */
	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
		in.defaultReadObject();
		log.debug("TwitterPrefsPane has been deserialized");
		//re-init our transient objects
		profileLogic = getProfileLogic();
	}
	
	private ProfileLogic getProfileLogic() {
		return Locator.getProfileLogic();
	}
	

}
