package org.jenkinsci.plugins.MaidsafeJenkins.actions;

import java.io.IOException;
import java.util.List;

import javax.servlet.ServletException;

import org.jenkinsci.plugins.MaidsafeJenkins.util.TargetBuildParameterUtil;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.export.Exported;

import hudson.model.Action;
import hudson.model.AbstractProject;
import hudson.model.Cause;
import hudson.model.CauseAction;
import hudson.model.Hudson;

public class GitHubTargetParameterAction implements Action {	
	AbstractProject<?, ?> project;
	
	public GitHubTargetParameterAction(AbstractProject<?, ?> project) {
		this.project = project;
	}
	
	@Exported
	public String getProjectName() {
		return project.getName();
	}
	
	public void doParamsSubmit(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException {			
		List<BuildTargetParameter> buildTargetParams;
		TargetParameterBuildAction paramAction;
		TargetBuildParameterUtil buildParamUtil = new TargetBuildParameterUtil();
		buildTargetParams = buildParamUtil.parse(req.getSubmittedForm());
		if (buildTargetParams == null) {
			rsp.sendRedirect(400, "Invalid Parameters - All Fields must be filed");
			return;
		} else {
			paramAction = new TargetParameterBuildAction();
			paramAction.setParameters(buildTargetParams);
			Hudson.getInstance().getQueue().schedule2(project, 0, paramAction, new CauseAction(new Cause.UserIdCause()));
		}
		rsp.sendRedirect("../");
	}

	public String getIconFileName() {		
		return "";
	}

	public String getDisplayName() {
		return "Build Now";
	}

	public String getUrlName() {
		return "maidsafeBuild";
	}

}
