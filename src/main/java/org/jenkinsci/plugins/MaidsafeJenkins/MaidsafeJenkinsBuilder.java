package org.jenkinsci.plugins.MaidsafeJenkins;

import hudson.*;
import hudson.model.*;
import hudson.model.AbstractProject;
import hudson.model.listeners.RunListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import java.io.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import net.sf.json.JSONObject;
import org.jenkinsci.plugins.MaidsafeJenkins.actions.GithubCheckoutAction;
import org.jenkinsci.plugins.MaidsafeJenkins.actions.GithubInitializerAction;
import org.jenkinsci.plugins.MaidsafeJenkins.github.CommitStatus;
import org.jenkinsci.plugins.MaidsafeJenkins.github.CommitStatus.State;
import org.jenkinsci.plugins.MaidsafeJenkins.github.GitHubHelper;
import org.jenkinsci.plugins.MaidsafeJenkins.github.GitHubPullRequestHelper;
import org.jenkinsci.plugins.MaidsafeJenkins.util.ShellScript;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

/**
 * MaidsafeJenkinsBuilder 
 * Builder provides a easy integration for managing Super project and its corresponding Submodule projects 
 * 
 */
public class MaidsafeJenkinsBuilder extends Builder {
	private final static String BUILDER_NAME = "MAIDSafe CI Builder";
	private final String orgName;
	private final String repoSubFolder;
	private final String superProjectName;
	private final String defaultBaseBranch;
	private final String initializer;

	public String getDefaultBaseBranch() {
		return defaultBaseBranch;
	}

	public String getOrgName() {
		return orgName;
	}

	public String getRepoSubFolder() {
		return repoSubFolder;
	}

	public String getSuperProjectName() {
		return superProjectName;
	}

	// Fields in config.jelly must match the parameter names in the
	// "DataBoundConstructor"
	@DataBoundConstructor
	public MaidsafeJenkinsBuilder(String orgName, String repoSubFolder, String superProjectName,
			String defaultBaseBranch, String initializer) {
		this.orgName = orgName;
		this.repoSubFolder = repoSubFolder;		
		this.superProjectName = superProjectName;
		this.defaultBaseBranch = defaultBaseBranch;	
		this.initializer = initializer;
	}
	
	private void updateCheckoutActionForPR(GithubCheckoutAction action, Map<String, Map<String, Object>> prList) {
		String module;
		List<String> urls = new ArrayList<String>();
		List<String> modules = new ArrayList<String>();
		Iterator<String> iterator = prList.keySet().iterator();
		while(iterator.hasNext()) {
			module = iterator.next();
			urls.add((String)prList.get(module).get("html_url"));
			modules.add(module);
		}
		action.setMatchingPRList(urls);
		action.setModulesWithMatchingPR(modules);
		action.setActualPRList(prList);
	}
	
		
	
	private GithubInitializerAction getInitalizer(FilePath projectPath, PrintStream logger, ShellScript script, GithubCheckoutAction checkoutAction) {
		GithubInitializerAction initializerAction;
		GitHubHelper githubHelper;	
		initializerAction = new GithubInitializerAction();
		githubHelper = new GitHubHelper(superProjectName, projectPath, logger, script, defaultBaseBranch, checkoutAction);
		githubHelper.setAccessToken(getDescriptor().getGithubToken());		
		initializerAction.setOauthAccessToken(getDescriptor().getGithubToken());
		initializerAction.setModules(githubHelper.getModuleNames());		
		return initializerAction;
	}
	
	private Map<String, Map<String, Object>> getPullRequest(String issueKey, List<String> modules, PrintStream logger) throws Exception {
		GitHubPullRequestHelper ghprh;
		ghprh = new GitHubPullRequestHelper(orgName, modules, logger);
		ghprh.setAccessToken(getDescriptor().getGithubToken());
		return ghprh.getMatchingPR(issueKey,
				GitHubPullRequestHelper.Filter.OPEN,
				GitHubPullRequestHelper.PR_MATCH_STRATERGY.BRANCH_NAME_STARTS_WITH_IGNORE_CASE);
	}
	
	private GithubInitializerAction getGithubInitializerAction(AbstractBuild<?, ?> build) {
		Cause.UpstreamCause upstreamCause;
		GithubInitializerAction action = null;
		upstreamCause = build.getCause(Cause.UpstreamCause.class);
		if (upstreamCause != null) {
			action = build.getCause(Cause.UpstreamCause.class).getUpstreamRun().getAction(GithubInitializerAction.class);
		}		
		return action;
	}
		

	@Override
	public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) {		
		EnvVars envVars;
		GithubCheckoutAction checkoutAction;
		GithubInitializerAction initializerAction = null;
		GitHubHelper githubHelper;
		Map<String, Map<String, Object>> pullRequest;
		final String ISSUE_KEY_PARAM = "issueKey";
		String issueKey;
		ShellScript script;
		FilePath rootDir;
		PrintStream logger;
		logger = listener.getLogger();
		checkoutAction = new GithubCheckoutAction();
		checkoutAction.setBaseBranch(defaultBaseBranch);	
		rootDir = new FilePath(new File(build.getWorkspace() + "/" + repoSubFolder));
		logger.println("Git REPO :: " + rootDir.getRemote());
		try {			
			envVars = build.getEnvironment(listener);
			script = new ShellScript(rootDir, launcher, envVars);
			/******** PRAMETERS RECEIVED **********/
			issueKey = envVars.get(ISSUE_KEY_PARAM, null);		
			/**************************************/			
			checkoutAction.setIssueKey(issueKey);
			checkoutAction.setOrgName(orgName);			
			initializerAction = getGithubInitializerAction(build);		
			if (initializerAction == null) {
				logger.println("Initializer Running for Project");
				initializerAction = getInitalizer(rootDir, logger, script, checkoutAction);
				if (issueKey != null && !issueKey.isEmpty()) {
					initializerAction.setPullRequests(getPullRequest(issueKey, initializerAction.getModules(), logger));
				}
				build.addAction(initializerAction);
				if (initializer != null) {					
					CommitStatus commitStatus = new CommitStatus(orgName, logger);
					commitStatus.updateAll(initializerAction.getPullRequests(), State.PENDING, build.getUrl(), "Build triggered");
					return true;
				}				
			}
			build.addAction(checkoutAction);
			List<String> shellCommands = new ArrayList<String>();
			shellCommands.add("git submodule update --init");
			script.execute(shellCommands);
			logger.println("Process initiated for token " + issueKey);
			pullRequest = initializerAction.getPullRequests();			
			if ((issueKey != null || !issueKey.isEmpty()) && (pullRequest == null || pullRequest.isEmpty())) {				
				checkoutAction.setBuildPassed(false);
				checkoutAction.setReasonForFailure("No Matching Pull Request found for " + issueKey);
				logger.println("No Matching Pull Request found for " + issueKey);
				build.addAction(checkoutAction);				
				return false;
			}			
			updateCheckoutActionForPR(checkoutAction, pullRequest);
			githubHelper = new GitHubHelper(superProjectName, rootDir, logger, script, defaultBaseBranch, checkoutAction);
			checkoutAction = githubHelper.checkoutModules(pullRequest);						
			checkoutAction.setScript(script);
			checkoutAction.setBaseBranch(defaultBaseBranch);											
			return checkoutAction.isBuilPassed();
		} catch (Exception exception) {
			listener.getLogger().println(exception);
		}
		return false;
	}
		

	/*
	 * BuildRunListner provides Callbacks at the build action events.
	 *  
	 */
	@Extension
	public static class BuildRunlistener extends RunListener<Run> implements Serializable {

		/**
		 * When the build run is completed, the temporary branches created are to be deleted.		 
		 */
		@Override
		public void onCompleted(Run r, TaskListener tl) {		
			super.onCompleted(r, tl);
			try {
				String DEL_BRANCH_CMD = "git checkout %S && git branch -D %s";
				String DEL_BRANCH_SUBMOD_CMD = "git submodule foreach 'git checkout %s && git branch -D %s || : '";
				GithubCheckoutAction checkoutAction = r.getAction(GithubCheckoutAction.class);
				if (checkoutAction == null) {
					return;
				}
				if (!checkoutAction.isBuilPassed()) {
					return;
				}
				tl.getLogger().println("Cleaning up the temporary branch " + checkoutAction.getBranchTarget());
				List<String> cmds = new ArrayList<String>();
				cmds.add(String.format(DEL_BRANCH_CMD, checkoutAction.getBaseBranch(), checkoutAction.getBranchTarget()));
				cmds.add(String.format(DEL_BRANCH_SUBMOD_CMD, checkoutAction.getBaseBranch(),
						checkoutAction.getBranchTarget()));
				checkoutAction.getScript().execute(cmds);
			} catch (Exception ex) {
				Logger.getLogger(MaidsafeJenkinsBuilder.class.getName()).log(Level.SEVERE, null, ex);
			}
		}

	}

	@Extension
	public static class DescriptorImpl extends  BuildStepDescriptor<Builder> {
		private String githubToken;
		/**
		 * In order to load the persisted global configuration, you have to call
		 * load() in the constructor.
		 */
		public DescriptorImpl() {
			load();
		}
			

		/**
		 * Performs on-the-fly validation of the form field 'name'.
		 *
		 * @param value
		 *            This parameter receives the value that the user has typed.
		 * @return Indicates the outcome of the validation. This is sent to the
		 *         browser.
		 *         <p>
		 *         Note that returning {@link FormValidation#error(String)} does
		 *         not prevent the form from being saved. It just means that a
		 *         message will be displayed to the user.
		 */
		public FormValidation doCheckOrgName(@QueryParameter String value) throws IOException, ServletException {
			if (value.length() == 0)
				return FormValidation.error("Please set organisation name");
			return FormValidation.ok();
		}

		public FormValidation doCheckSuperProjectName(@QueryParameter String value) throws IOException,
				ServletException {
			if (value.length() == 0)
				return FormValidation.error("Please set Super project name");
			return FormValidation.ok();
		}

		public FormValidation doCheckDefaultBaseBranch(@QueryParameter String value) throws IOException,
				ServletException {
			if (value.length() == 0)
				return FormValidation.error("Please set base branch to default while merging target builds");
			return FormValidation.ok();
		}

		public boolean isApplicable(Class<? extends AbstractProject> aClass) {
			return true;
		}

		@Override
		public boolean configure(StaplerRequest req, JSONObject formData) throws Descriptor.FormException {					
			githubToken = formData.getString("githubToken");				
			save();
			return super.configure(req, formData);
		}

		/**
		 * This human readable name is used in the configuration screen.
		 */
		public String getDisplayName() {
			return BUILDER_NAME;
		}
		
		public String getGithubToken() {
			return githubToken;
		}
	}

	// Overridden for better type safety.
	// If your plugin doesn't really define any property on Descriptor,
	// you don't have to do this.
	@Override
	public DescriptorImpl getDescriptor() {
		return (DescriptorImpl) super.getDescriptor();
	}

}
