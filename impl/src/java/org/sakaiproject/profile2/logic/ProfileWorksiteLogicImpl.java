/**
 * 
 */
package org.sakaiproject.profile2.logic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.apache.log4j.Logger;
import org.sakaiproject.component.api.ServerConfigurationService;
import org.sakaiproject.exception.IdInvalidException;
import org.sakaiproject.exception.IdUnusedException;
import org.sakaiproject.exception.IdUsedException;
import org.sakaiproject.exception.PermissionException;
import org.sakaiproject.id.api.IdManager;
import org.sakaiproject.profile2.model.Person;
import org.sakaiproject.profile2.util.Messages;
import org.sakaiproject.profile2.util.ProfileConstants;
import org.sakaiproject.site.api.Site;
import org.sakaiproject.site.api.SitePage;
import org.sakaiproject.site.api.SiteService;
import org.sakaiproject.site.api.ToolConfiguration;
import org.sakaiproject.tool.api.Tool;
import org.sakaiproject.tool.api.ToolManager;
import org.sakaiproject.user.api.User;
import org.sakaiproject.user.api.UserDirectoryService;
import org.sakaiproject.user.api.UserNotDefinedException;

/**
 * Implementation of ProfileWorksiteLogic API
 * 
 * @author d.b.robinson@lancaster.ac.uk
 */
public class ProfileWorksiteLogicImpl implements ProfileWorksiteLogic {

	private static final Logger log = Logger.getLogger(ProfileWorksiteLogicImpl.class);
	
	/**
	 * Profile2 creates <code>project</code> type worksites.
	 */
	private static final String SITE_TYPE_PROJECT = "project";
	
	/**
	 * Connections invited to worksites are initially given the
	 * <code>access</code> role.
	 */
	private static final String ROLE_ACCESS = "access";
	
	/**
	 * Users who create worksites are initially given the <code>maintain</code>
	 * role.
	 */
	private static final String ROLE_MAINTAIN = "maintain";
	
	/**
	 * The id of the worksite home page.
	 */
	private static final String TOOL_ID_HOME = "home";

	/**
	 * The id of the iframe tool.
	 */
	private static final String TOOL_ID_IFRAME = "sakai.iframe";
		
	/**
	 * The id of the synoptic calendar tool.
	 */
	private static final String TOOL_ID_SUMMARY_CALENDAR = "sakai.summary.calendar";
	
	/**
	 * The id of the synoptic announcements tool.
	 */
	private static final String TOOL_ID_SYNOPTIC_ANNOUNCEMENT = "sakai.synoptic.announcement";

	/**
	 * The id of the synoptic chat tool.
	 */
	private static final String TOOL_ID_SYNOPTIC_CHAT = "sakai.synoptic.chat";

	/**
	 * The id of the synoptic discussions tool.
	 */
	private static final String TOOL_ID_SYNOPTIC_DISCUSSION = "sakai.synoptic.discussion";
	
	/**
	 * The id of the synoptic message center tool.
	 */
	private static final String TOOL_ID_SYNOPTIC_MESSAGECENTER = "sakai.synoptic.messagecenter";
	
	/**
	 *  Map of synoptic tool and the related tool ids.
	 */
	private final static Map<String, List<String>> SYNOPTIC_TOOL_ID_MAP;
	static
	{
		SYNOPTIC_TOOL_ID_MAP = new HashMap<String, List<String>>();
		SYNOPTIC_TOOL_ID_MAP.put(TOOL_ID_SUMMARY_CALENDAR, new ArrayList<String>(Arrays.asList("sakai.schedule")));
		SYNOPTIC_TOOL_ID_MAP.put(TOOL_ID_SYNOPTIC_ANNOUNCEMENT, new ArrayList<String>(Arrays.asList("sakai.announcements")));
		SYNOPTIC_TOOL_ID_MAP.put(TOOL_ID_SYNOPTIC_CHAT, new ArrayList<String>(Arrays.asList("sakai.chat")));
		SYNOPTIC_TOOL_ID_MAP.put(TOOL_ID_SYNOPTIC_DISCUSSION, new ArrayList<String>(Arrays.asList("sakai.discussion")));
		SYNOPTIC_TOOL_ID_MAP.put(TOOL_ID_SYNOPTIC_MESSAGECENTER, new ArrayList<String>(Arrays.asList("sakai.messages", "sakai.forums", "sakai.messagecenter")));
	}
	
	/**
	 * Map of tools and the related synoptic tool ids.
	 */
	private final static Map<String, String> TOOLS_WITH_SYNOPTIC_ID_MAP;
	static
	{
		TOOLS_WITH_SYNOPTIC_ID_MAP = new HashMap<String, String>();
		TOOLS_WITH_SYNOPTIC_ID_MAP.put("sakai.schedule", TOOL_ID_SUMMARY_CALENDAR);
		TOOLS_WITH_SYNOPTIC_ID_MAP.put("sakai.announcements", TOOL_ID_SYNOPTIC_ANNOUNCEMENT);
		TOOLS_WITH_SYNOPTIC_ID_MAP.put("sakai.chat", TOOL_ID_SYNOPTIC_CHAT);
		TOOLS_WITH_SYNOPTIC_ID_MAP.put("sakai.discussion", TOOL_ID_SYNOPTIC_DISCUSSION);
		TOOLS_WITH_SYNOPTIC_ID_MAP.put("sakai.messages", TOOL_ID_SYNOPTIC_MESSAGECENTER);
		TOOLS_WITH_SYNOPTIC_ID_MAP.put("sakai.forums", TOOL_ID_SYNOPTIC_MESSAGECENTER);
		TOOLS_WITH_SYNOPTIC_ID_MAP.put("sakai.messagecenter", TOOL_ID_SYNOPTIC_MESSAGECENTER);
	}
	
	/**
	 * The tool to place on the home page.
	 */
	private static final String HOME_TOOL = "sakai.iframe.site";
	
	/**
	 * The tool used to modify the worksite after creation.
	 */
	private static final String SITEINFO_TOOL = "sakai.siteinfo";

	/**
	 * The tool used to unjoin worksites.
	 */
	private static final String MEMBERSHIP_TOOL = "sakai.membership";
	
	/**
	 * Worksite setup tools.
	 */
	private static final String WORKSITE_SETUP_TOOLS = "wsetup.home.toolids";
	
	/**
	 * {@inheritDoc}
	 */
	public boolean createWorksite(final String siteTitle, final String ownerId,
			final Collection<Person> members, boolean notifyByEmail) {

		// double-check
		if (false == sakaiProxy.isUserAllowedAddSite(ownerId)) {
			log .warn("user " + ownerId + " tried to create worksite without site.add");
			return false;
		}

		// ensure site id is unique
		String siteId = idManager.createUuid();
		while (true == siteService.siteExists(siteId)) {
			siteId = idManager.createUuid();
		}

		try {
			final Site site = siteService.addSite(siteId, SITE_TYPE_PROJECT);
			
			try {
				User user = userDirectoryService.getUser(ownerId);
				if (null != user) {
					// false == provided
					site.addMember(ownerId, ROLE_MAINTAIN, true, false);					
				}
			} catch (UserNotDefinedException e) {
				log .warn("unknown user " + ownerId + " tried to create worksite");
				e.printStackTrace();
				return false;
			}

			// user could create worksite without any connections
			if (null != members) {
				for (Person member : members) {
					try {
						User user = userDirectoryService.getUser(member.getUuid());
						if (null != user) {
							
							// TODO privacy/preference check if/when added?
							
							// false == provided
							site.addMember(member.getUuid(), ROLE_ACCESS, true, false);
						}
					} catch (UserNotDefinedException e) {
						log .warn("attempt to add unknown user " + member.getUuid() + " to worksite");
						e.printStackTrace();
					}
				}
			}

			// finishing setting up site
			site.setTitle(siteTitle);
			// add description for editing the worksite
			site.setDescription(Messages.getString("worksite.help",
					new Object[] { toolManager.getTool(SITEINFO_TOOL).getTitle(),
					toolManager.getTool(MEMBERSHIP_TOOL).getTitle(),
					sakaiProxy.getUserDisplayName(ownerId),
					sakaiProxy.getUserEmail(ownerId),
					siteTitle}));
						
			// we will always have a home page
			SitePage homePage = site.addPage();
			homePage.getPropertiesEdit().addProperty(
					SitePage.IS_HOME_PAGE, Boolean.TRUE.toString());
			
			Tool homeTool = toolManager.getTool(HOME_TOOL);
			
			ToolConfiguration homeToolConfig = homePage.addTool();
			homeToolConfig.setTool(TOOL_ID_HOME, homeTool);
			homeToolConfig.setTitle(homeTool.getTitle());
						
			// normally brings in sakai.siteinfo
			List<String> toolIds = serverConfigurationService.getToolsRequired(SITE_TYPE_PROJECT);

			// home tools specified in sakai.properties or default set of home tools
			List<String> homeToolIds;

			if (null != serverConfigurationService.getStrings(WORKSITE_SETUP_TOOLS + "." + SITE_TYPE_PROJECT)) {
				homeToolIds = new ArrayList<String>(Arrays.asList(
					serverConfigurationService.getStrings(WORKSITE_SETUP_TOOLS + "." + SITE_TYPE_PROJECT)));
			} else if (null != serverConfigurationService.getStrings(WORKSITE_SETUP_TOOLS)) {
				homeToolIds = new ArrayList<String>(Arrays.asList(
						serverConfigurationService.getStrings(WORKSITE_SETUP_TOOLS)));
			} else {
				homeToolIds = new ArrayList<String>();
			}
						
			int synopticToolIndex = 0;
			for (String toolId : toolIds) {
				
				if (isToolToIgnore(toolId)) {
					continue;
				} else if (isToolWithSynopticTool(toolId)) {
					
					// add tool
					SitePage toolPage = site.addPage();
					toolPage.addTool(toolId);
					
					// add corresponding synoptic tool
					ToolConfiguration toolConfig = homePage.addTool(TOOLS_WITH_SYNOPTIC_ID_MAP.get(toolId));
					if (null != toolConfig) {
						toolConfig.setLayoutHints(synopticToolIndex + ",1");
	
						for (int i = 0; i < synopticToolIndex; i++) {
							toolConfig.moveUp();
						}
						
						synopticToolIndex++;
					}
				} else if (null != toolManager.getTool(toolId)) {
												
						SitePage toolPage = site.addPage();
						toolPage.addTool(toolId);
				}
			}
			
			// for synoptic tools
			if (synopticToolIndex > 0) {
				homePage.setLayout(SitePage.LAYOUT_DOUBLE_COL);
			}
			
			for (String homeToolId : homeToolIds) {
				
				if (isToolToIgnore(homeToolId)) {
					continue;
				} else {
					
					// check for corresponding tool
					if (SYNOPTIC_TOOL_ID_MAP.get(homeToolId) != null && CollectionUtils.containsAny(SYNOPTIC_TOOL_ID_MAP.get(homeToolId), toolIds)) {
						
						ToolConfiguration toolConfig = homePage.addTool(homeToolId);
						if (null != toolConfig) {
							toolConfig.setLayoutHints(synopticToolIndex + ",1");
			
							for (int i = 0; i < synopticToolIndex; i++) {
								toolConfig.moveUp();
							}
							
							synopticToolIndex++;
						}						
					}

				}
			}
			
			site.setPublished(true);
			siteService.save(site);
			
			if (true == notifyByEmail) {
				
				Thread thread = new Thread() {
					public void run() {
						emailSiteMembers(siteTitle, site.getUrl(), ownerId, members);		
					}
				};
				thread.start();
				
			}
			
			return true;
			
		} catch (IdInvalidException e) {
			e.printStackTrace();
		} catch (IdUsedException e) {
			e.printStackTrace();
		} catch (PermissionException e) {
			e.printStackTrace();
		} catch (IdUnusedException e) {
			e.printStackTrace();
		}
		
		// if we get here then site creation failed.
		return false;
	}

	private boolean isToolWithSynopticTool(String toolId) {
		return TOOLS_WITH_SYNOPTIC_ID_MAP.containsKey(toolId);
	}
	
	private boolean isToolToIgnore(String toolId) {
		return toolId.equals(TOOL_ID_IFRAME) || toolId.equals(HOME_TOOL);
	}

	private void emailSiteMembers(String siteTitle, String siteUrl, String ownerId,
			Collection<Person> members) {
		
		for (Person member : members) {
			if (true == member.getPreferences().isWorksiteNewEmailEnabled()) {
				emailSiteMember(siteTitle, siteUrl, ownerId, member);
			}
		}
	}

	private void emailSiteMember(String siteTitle, String siteUrl, String ownerId, Person member) {
		
		// create the map of replacement values for this email template
		Map<String, String> replacementValues = new HashMap<String, String>();
		replacementValues.put("senderDisplayName", sakaiProxy.getUserDisplayName(ownerId));
		replacementValues.put("worksiteTitle", siteTitle);
		replacementValues.put("worksiteLink", siteUrl);
		replacementValues.put("localSakaiName", sakaiProxy.getServiceName());
		replacementValues.put("localSakaiUrl", sakaiProxy.getPortalUrl());
		replacementValues.put("toolName", sakaiProxy.getCurrentToolTitle());
		replacementValues.put("displayName", member.getDisplayName());
		
		sakaiProxy.sendEmail(member.getUuid(),
				ProfileConstants.EMAIL_TEMPLATE_KEY_WORKSITE_NEW, replacementValues);
	}

	// API injections
	private SakaiProxy sakaiProxy;
	public void setSakaiProxy(SakaiProxy sakaiProxy) {
		this.sakaiProxy = sakaiProxy;
	}
	
	private IdManager idManager;
	public void setIdManager(IdManager idManager) {
		this.idManager = idManager;
	}
	
	private SiteService siteService;
	public void setSiteService(SiteService siteService) {
		this.siteService = siteService;
	}
	
	private ToolManager toolManager;
	public void setToolManager(ToolManager toolManager) {
		this.toolManager = toolManager;
	}
	
	private UserDirectoryService userDirectoryService;
	public void setUserDirectoryService(UserDirectoryService userDirectoryService) {
		this.userDirectoryService = userDirectoryService;
	}
	
	private ServerConfigurationService serverConfigurationService;
	public void setServerConfigurationService(ServerConfigurationService serverConfigurationService) {
		this.serverConfigurationService = serverConfigurationService;
	}
}
