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
		TargetBuildParameterUtil buildParamUtil = new TargetBuildParameterUtil();
		buildTargetParams = buildParamUtil.parse(req.getSubmittedForm());
		if (buildTargetParams == null) {
			rsp.sendRedirect(400, "Invalid Parameters - All Fields must be filed");
		} else {
			System.out.println(buildTargetParams.toString());
		}
		rsp.sendRedirect("../");
	}

	public String getIconFileName() {		
		return "";
	}

	public String getDisplayName() {
		return "Build with Target Repos.";
	}

	public String getUrlName() {
		return "maidsafeBuild";
	}

}
