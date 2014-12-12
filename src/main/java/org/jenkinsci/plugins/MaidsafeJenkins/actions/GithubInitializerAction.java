package org.jenkinsci.plugins.MaidsafeJenkins.actions;

import java.util.List;
import java.util.Map;
import hudson.model.InvisibleAction;

public class GithubInitializerAction extends InvisibleAction {
	
	private String orgName;
	private String oauthAccessToken;
	private List<String> modules;
	private Map<String, Map<String, Object>> pullRequests;	
	
	public String getOrgName() {
		return orgName;
	}
	public void setOrgName(String orgName) {
		this.orgName = orgName;
	}
	public String getOauthAccessToken() {
		return oauthAccessToken;
	}
	public void setOauthAccessToken(String oauthAccessToken) {
		this.oauthAccessToken = oauthAccessToken;
	}
	public List<String> getModules() {
		return modules;
	}
	public void setModules(List<String> subModules) {
		this.modules = subModules;
	}
	public Map<String, Map<String, Object>> getPullRequests() {
		return pullRequests;
	}
	public void setPullRequests(Map<String, Map<String, Object>> pullRequests) {
		this.pullRequests = pullRequests;
	}	
	
}
