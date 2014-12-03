package org.jenkinsci.plugins.MaidsafeJenkins;


import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.jenkinsci.plugins.MaidsafeJenkins.actions.AggregatedCheckoutSummaryAction;
import org.jenkinsci.plugins.MaidsafeJenkins.actions.GithubCheckoutAction;
import org.kohsuke.stapler.DataBoundConstructor;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.BuildListener;
import hudson.plugins.parameterizedtrigger.BuildInfoExporterAction;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;

public class GithubCheckoutResultAggregator extends Publisher  {
	
	@DataBoundConstructor
	public GithubCheckoutResultAggregator() {	
	}
	
	public BuildStepMonitor getRequiredMonitorService() {		
		return BuildStepMonitor.NONE;
	}
		
	private HashMap<String, Object> aggregateBuildResults(List<AbstractBuild<?, ?>> triggeredBuilds) {
		HashMap<String, Object> buildCheckoutAction;		
		final String BUILD_NAME = "%s #%d";
		buildCheckoutAction = new HashMap<String, Object>();
		for (AbstractBuild<?, ?> build : triggeredBuilds) {
			if (build.getAction(GithubCheckoutAction.class) != null) {								
				buildCheckoutAction.put(String.format(BUILD_NAME, build.getProject().getDisplayName(), build.getNumber()), build.getAction(GithubCheckoutAction.class).getSummary());
			}			 			
		}		
		return buildCheckoutAction;
	}
		
	@Override
	public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
			throws InterruptedException, IOException {
		AggregatedCheckoutSummaryAction checkoutAction;
		List<AbstractBuild<?, ?>> triggeredBuilds = new ArrayList<AbstractBuild<?,?>>();
		for (Action action : build.getActions()) {					
			if (action instanceof BuildInfoExporterAction) {
				triggeredBuilds =  ((BuildInfoExporterAction) action).getTriggeredBuilds();
				break;
			}					
		}
		try {
			checkoutAction = new AggregatedCheckoutSummaryAction();
			checkoutAction.setCheckoutSummary(aggregateBuildResults(triggeredBuilds));
			build.addAction(checkoutAction);
			listener.getLogger().println();
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
			return "Aggregate Github Checkout Results";
		}
		
	}
	
	@Override
	public CustomDesc getDescriptor() {		
		return (CustomDesc) super.getDescriptor();
	}


}
