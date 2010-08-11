package org.sakaiproject.profile2.tool.pages;

import java.util.Locale;

import org.apache.log4j.Logger;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.IHeaderContributor;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.model.IModel;
import org.sakaiproject.component.cover.ServerConfigurationService;
import org.sakaiproject.profile2.logic.ProfileLogic;
import org.sakaiproject.profile2.logic.SakaiProxy;
import org.sakaiproject.profile2.tool.Locator;
import org.sakaiproject.profile2.util.ProfileUtils;
import org.sakaiproject.site.cover.SiteService;
import org.sakaiproject.tool.cover.SessionManager;

import wicket.contrib.tinymce.settings.TinyMCESettings;


public class BasePage extends WebPage implements IHeaderContributor {

	private static final Logger log = Logger.getLogger(BasePage.class); 
	protected transient SakaiProxy sakaiProxy;
	protected transient ProfileLogic profileLogic;
	
	public BasePage() {
		//super();
		
		log.debug("BasePage()");
		
		//get SakaiProxy API
		sakaiProxy = getSakaiProxy();
		
		//get ProfileLogic API
		profileLogic = getProfileLogic();
		
		//set Locale - all pages will inherit this.
		setUserPreferredLocale();
		
    	//profile link
    	Link myProfileLink = new Link("myProfileLink") {
			public void onClick() {
				setResponsePage(new MyProfile());
			}
		};
		myProfileLink.add(new Label("myProfileLabel",new ResourceModel("link.my.profile")));
		myProfileLink.add(new AttributeModifier("title", true, new ResourceModel("link.my.profile.tooltip")));
		add(myProfileLink);
		

		
		//my friends link
    	Link myFriendsLink = new Link("myFriendsLink") {
			public void onClick() {
				setResponsePage(new MyFriends());
			}
		};
		myFriendsLink.add(new Label("myFriendsLabel",new ResourceModel("link.my.friends")));
		myFriendsLink.add(new AttributeModifier("title", true, new ResourceModel("link.my.friends.tooltip")));
		add(myFriendsLink);
		
		
		/*
		//messages link
    	Link myMessagesLink = new Link("myMessagesLink") {
			public void onClick() {
				//setResponsePage(new MyFriends());
			}
		};
		myMessagesLink.add(new Label("myMessagesLabel",new ResourceModel("link.my.messages")));
		myMessagesLink.add(new AttributeModifier("title", true, new ResourceModel("link.my.messages.tooltip")));
		
		//calculate new messages
		int unreadMessages = profile.getUnreadMessagesCount(sakaiProxy.getCurrentUserId());
		Label unreadMessagesLabel = new Label("unreadMessagesLabel", new Model(unreadMessages));
		myMessagesLink.add(unreadMessagesLabel);

		if(unreadMessages == 0) {
			unreadMessagesLabel.setVisible(false);
		}
		
		add(myMessagesLink);
		myMessagesLink.setVisible(false);

		
		//photos link
    	Link myPhotosLink = new Link("myPhotosLink") {
			public void onClick() {
				setResponsePage(new MyProfile());
			}
		};
		myPhotosLink.add(new Label("myPhotosLabel",new ResourceModel("link.my.photos")));
		add(myPhotosLink);
		myPhotosLink.setVisible(false);
	*/
	
		//privacy link
    	Link myPrivacyLink = new Link("myPrivacyLink") {
			public void onClick() {
				setResponsePage(new MyPrivacy());
			}
		};
		myPrivacyLink.add(new Label("myPrivacyLabel",new ResourceModel("link.my.privacy")));
		myPrivacyLink.add(new AttributeModifier("title", true, new ResourceModel("link.my.privacy.tooltip")));
		add(myPrivacyLink);
		
		//search link
    	Link searchLink = new Link("searchLink") {
			public void onClick() {
				setResponsePage(new MySearch());
			}
		};
		searchLink.add(new Label("searchLabel",new ResourceModel("link.my.search")));
		searchLink.add(new AttributeModifier("title", true, new ResourceModel("link.my.search.tooltip")));
		add(searchLink);
		
		//preferences link
    	Link preferencesLink = new Link("preferencesLink") {
			public void onClick() {
				setResponsePage(new MyPreferences());
			}
		};
		preferencesLink.add(new Label("preferencesLabel",new ResourceModel("link.my.preferences")));
		preferencesLink.add(new AttributeModifier("title", true, new ResourceModel("link.my.preferences.tooltip")));
		add(preferencesLink);
				
		//rss link
		/*
		ContextImage icon = new ContextImage("icon",new Model(ProfileImageManager.RSS_IMG));
		Link rssLink = new Link("rssLink") {
			public void onClick() {
			}
		};
		rssLink.add(icon);
		rssLink.add(new AttributeModifier("title", true,new ResourceModel("link.title.rss")));
		icon.setVisible(true);
		add(rssLink);
		*/
		
    }
	
	
	
	
	
	
	

	
	
	
	
	
	
	
	
	
	
	
	
	
	//Style it like a Sakai tool
	protected static final String HEADSCRIPTS = "/library/js/headscripts.js";
	protected static final String BODY_ONLOAD_ADDTL="setMainFrameHeight( window.name )";
	
	public void renderHead(IHeaderResponse response) {
		//get Sakai skin
		String skinRepo = ServerConfigurationService.getString("skin.repo");
		String toolCSS = getToolSkinCSS(skinRepo);
		String toolBaseCSS = skinRepo + "/tool_base.css";
		
		//Sakai additions
		response.renderJavascriptReference(HEADSCRIPTS);
		response.renderCSSReference(toolBaseCSS);
		response.renderCSSReference(toolCSS);
		response.renderOnLoadJavascript(BODY_ONLOAD_ADDTL);
		
		//for jQuery
		response.renderJavascriptReference("javascript/jquery-1.2.5.min.js");
			
		//for datepicker
		response.renderCSSReference("css/flora.datepicker.css");
		response.renderJavascriptReference("javascript/jquery.ui.core-1.5.2.min.js");
		response.renderJavascriptReference("javascript/jquery.datepicker-1.5.2.min.js");

		//for cluetip
		response.renderCSSReference("css/jquery.cluetip.css");
		response.renderJavascriptReference("javascript/jquery.dimensions.js");
		response.renderJavascriptReference("javascript/jquery.hoverIntent.js");
		response.renderJavascriptReference("javascript/jquery.cluetip.js");
		
		//for color plugin
		//response.renderJavascriptReference("javascript/jquery.color.js");
		
		//wicketstuff TinyMCE
		response.renderJavascriptReference(TinyMCESettings.javaScriptReference());
		
		//Tool additions (at end so we can override if required)
		response.renderString("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\" />");
		response.renderCSSReference("css/profile2.css");
		response.renderJavascriptReference("javascript/profile2.js");
		
	}
	
	protected String getToolSkinCSS(String skinRepo) {
		String skin = null;
		try {
			skin = SiteService.findTool(SessionManager.getCurrentToolSession().getPlacementId()).getSkin();			
		}
		catch(Exception e) {
			skin = ServerConfigurationService.getString("skin.default");
		}
		
		if(skin == null) {
			skin = ServerConfigurationService.getString("skin.default");
		}
		
		return skinRepo + "/" + skin + "/tool.css";
	}
	
	/*
	protected Label newResourceLabel(String id, Component component) {
		return new Label(id, new StringResourceModel(id, component, null));
	}
	
	public String getResourceModel(String resourceKey, IModel model) {
		return new StringResourceModel(resourceKey, this, model).getString();
	}
	*/
	
	public BasePage getBasePage() {
		return this;
	}

	
	/* helper methods for our child pages to get at the API's */
	protected SakaiProxy getSakaiProxy() {
		return Locator.getSakaiProxy();
	}

	protected ProfileLogic getProfileLogic() {
		return Locator.getProfileLogic();
	}

	
	/* disable caching
	protected void setHeaders(WebResponse response) { 
		response.setHeader("Pragma", "no-cache"); 
		response.setHeader("Cache-Control", "no-cache, max-age=0, must-revalidate, no-store"); 
    } 
	*/
	
	public void setUserPreferredLocale() {
		Locale locale = ProfileUtils.getUserPreferredLocale();
		log.debug("User preferred locale: " + locale);
		getSession().setLocale(locale);
	}
	
	/**
	 * Allow Pages to set the title
	 * @param model
	 */
	/*
	protected void setPageTitle(IModel model) {  
		get("pageTitle").setModel(model);  
	} 
	*/ 
	
	
	
}
