package org.jenkinsci.plugins.MaidsafeJenkins.actions;

import java.io.IOException;
import java.util.List;

import javax.servlet.ServletException;

import jenkins.model.Configuration;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;

import org.jenkinsci.plugins.MaidsafeJenkins.MaidsafeJenkinsBuilder;
import org.jenkinsci.plugins.MaidsafeJenkins.MaidsafeJenkinsBuilder.DescriptorImpl;
import org.jenkinsci.plugins.MaidsafeJenkins.util.TargetBuildParameterUtil;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.export.Exported;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.mapper.Mapper;
import com.thoughtworks.xstream.persistence.XmlMap;

import hudson.Functions;
import hudson.model.Action;
import hudson.model.AbstractProject;
import hudson.model.Cause;
import hudson.model.CauseAction;
import hudson.model.Descriptor;
import hudson.model.Hudson;
import hudson.util.ListBoxModel;

public class GitHubTargetParameterAction implements Action {	
	private AbstractProject<?, ?> project;	
	
	public GitHubTargetParameterAction(AbstractProject<?, ?> project) {
		this.project = project;			
	}	
	
	@Exported
	public String getProjectName() {
		return project.getName();
	}	
		
	@Exported
	public String getDefaultBaseBranch() {
		String baseBranch = "";
		try{
			String PACKAGE = "org.jenkinsci.plugins.MaidsafeJenkins.MaidsafeJenkinsBuilder";
			String field = "defaultBaseBranch";			
			String xml = project.getConfigFile().asString();
			xml = xml.substring(xml.indexOf(PACKAGE), xml.lastIndexOf(PACKAGE));			
			xml = xml.substring(xml.indexOf(field), xml.lastIndexOf(field));			
			baseBranch = xml.substring(xml.indexOf(">")+1, xml.indexOf("<"));
		}catch(Exception ex) {
			ex.printStackTrace();
		}		
		return baseBranch;
	}
	
	@Exported
	public String getAccessToken() {
		MaidsafeJenkinsBuilder.DescriptorImpl descriptor;
		descriptor = (MaidsafeJenkinsBuilder.DescriptorImpl) Jenkins.getInstance().getDescriptor(MaidsafeJenkinsBuilder.class);		
		return descriptor.getGithubToken();
	}
	
	public void doParamsSubmit(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException {			
		List<BuildTargetParameter> buildTargetParams;
		TargetParameterBuildAction paramAction;
		JSONObject jsonObject;
		TargetBuildParameterUtil buildParamUtil = new TargetBuildParameterUtil();
		jsonObject = req.getSubmittedForm();
		buildTargetParams = buildParamUtil.parse(jsonObject);
		if (buildTargetParams == null) {
			rsp.sendRedirect(400, "Invalid Parameters - All Fields must be filed");
			return;
		} else {
			paramAction = new TargetParameterBuildAction();
			paramAction.setBaseBranch(jsonObject.getString("baseBranch"));			
			paramAction.setParameters(buildTargetParams);
			Hudson.getInstance().getQueue().schedule2(project, 0, paramAction, new CauseAction(new Cause.UserIdCause()));
		}
		rsp.sendRedirect("../");
	}

	public String getIconFileName() {		
		return Functions.getResourcePath() + "/plugin/MaidsafeJenkins/icons/octocat.jpg";
	}

	public String getDisplayName() {
		return "Build Now";
	}

	public String getUrlName() {
		return "maidsafeBuild";
	}

}
