package org.jenkinsci.plugins.MaidsafeJenkins;


import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jenkinsci.plugins.MaidsafeJenkins.actions.AggregatedCheckoutSummaryAction;
import org.jenkinsci.plugins.MaidsafeJenkins.actions.GithubCheckoutAction;
import org.jenkinsci.plugins.MaidsafeJenkins.github.CommitStatus;
import org.jenkinsci.plugins.MaidsafeJenkins.github.CommitStatus.State;
import org.kohsuke.stapler.DataBoundConstructor;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.plugins.parameterizedtrigger.BuildInfoExporterAction;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;

public class GithubCheckoutResultAggregator extends Publisher  {
	
	private Result buildResult;
	private Map<String, Map<String, Object>> actualMatchingPR = null;
	private String gitHubOrgName;
	
	
	@DataBoundConstructor
	public GithubCheckoutResultAggregator() {	
	}
	
	public BuildStepMonitor getRequiredMonitorService() {		
		return BuildStepMonitor.NONE;
	}
		
	private HashMap<String, Object> aggregateBuildResults(List<AbstractBuild<?, ?>> triggeredBuilds) {
		buildResult = Result.SUCCESS;
		HashMap<String, Object> buildCheckoutAction;		
		final String BUILD_NAME = "%s #%d";		
		buildCheckoutAction = new HashMap<String, Object>();
		for (AbstractBuild<?, ?> build : triggeredBuilds) {
			if (build.getAction(GithubCheckoutAction.class) != null) {
				if (build.getResult() ==  Result.FAILURE) {
					buildResult = Result.FAILURE;
				}				
				buildCheckoutAction.put(String.format(BUILD_NAME, build.getProject().getDisplayName(), build.getNumber()), build.getAction(GithubCheckoutAction.class).getSummary());
				if (actualMatchingPR == null) {
					System.out.println("******** SETTING VALUES ******");
					gitHubOrgName = build.getAction(GithubCheckoutAction.class).getOrgName();
					actualMatchingPR = build.getAction(GithubCheckoutAction.class).getActualPRList();
					System.out.println("******** VALUES ****** " + gitHubOrgName);
				}
			}			 			
		}
		return buildCheckoutAction;
	}
		
	@Override
	public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
			throws InterruptedException, IOException {
		AggregatedCheckoutSummaryAction checkoutAction;
		CommitStatus commitStatus;
		List<AbstractBuild<?, ?>> triggeredBuilds = new ArrayList<AbstractBuild<?,?>>();
		for (Action action : build.getActions()) {					
			if (action instanceof BuildInfoExporterAction) {
				triggeredBuilds =  ((BuildInfoExporterAction) action).getTriggeredBuilds();
				break;
			}					
		}
		try {			
			listener.getLogger().println(build.getUrl());			
			checkoutAction = new AggregatedCheckoutSummaryAction();
			checkoutAction.setCheckoutSummary(aggregateBuildResults(triggeredBuilds));			
			build.addAction(checkoutAction);
			// build.setResult(buildResult); // this can be used when we enable parallel job execution from ProxyJob
			commitStatus = new CommitStatus(gitHubOrgName, listener.getLogger());
			listener.getLogger().println(actualMatchingPR);
			commitStatus.updateAll(actualMatchingPR, State.FAILURE, build.getBuildStatusUrl(), "Build Failed");
		}	catch(Exception e) {
			listener.getLogger().println(e);
		}
		return true;
	}
	
	@Extension	
	public static class CustomDesc extends BuildStepDescriptor<Publisher> {
		
		public CustomDesc() {
			load();
		}

		@Override
		public boolean isApplicable(Class<? extends AbstractProject> jobType) {			
			return true;
		}

		@Override
		public String getDisplayName() {
			return "Aggregate Github Checkout Results & Update PR Status";
		}
		
	}
	
	@Override
	public CustomDesc getDescriptor() {		
		return (CustomDesc) super.getDescriptor();
	}


}
