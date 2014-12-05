package org.jenkinsci.plugins.MaidsafeJenkins.github;

import java.io.PrintStream;
import java.util.Iterator;
import java.util.Map;

import jenkins.model.JenkinsLocationConfiguration;


public class CommitStatus {
	private String orgName;
	private String accessToken;
	private final String context = "jenkins-ci";
	private PrintStream logger;	
	private final String END_POINT = "https://api.github.com/repos/%s/%s/statuses/%s";
	private JenkinsLocationConfiguration locationConfig = new JenkinsLocationConfiguration();
	
	public enum State {
		PENDING, SUCCESS, FAILURE
	}
	
	public CommitStatus(String orgName, PrintStream logger) {
		this.orgName = orgName;
		this.logger = logger;
	}
	
	public void setAccessToken(String token) {
		accessToken = token;
	}
	
	private String getStateText(State state) {
		String stateText = null;
		switch (state) {
			case SUCCESS:
				stateText = "success";
				break;
				
			case FAILURE:
				stateText = "failure";
				break;	
				
			default:			
				stateText = "pending";
				break;
		}
		return stateText;
	}
	
	private CommitStatusPayload getPayload( State status, String buildRefUrl, String description , String context) {		
		CommitStatusPayload payload = new CommitStatusPayload();
		payload.setContext(context);
		payload.setDescription(description);
		payload.setTarget_url(locationConfig.getUrl() + buildRefUrl);
		payload.setState(getStateText(status));
		return payload;
	}
	
	public void updateAll(Map<String, Map<String, Object>> pullRequests, State state, String buildRefUrl, String description) {
		if (pullRequests == null) {
			return;
		}
		Map<String, Object> pr;
		String module;
		String sha;
		Iterator<String> modules = pullRequests.keySet().iterator();
		while (modules.hasNext()) {
			module = modules.next();
			pr = pullRequests.get(module);
			sha = (String) ((Map<String, Object>) pr.get("head")).get("sha");
			update(module, sha, state, buildRefUrl, description);
		}
		
	}

	public void update(String repo, String sha, State status, String buildRefUrl, String description) {
		update(repo, sha, status, buildRefUrl, description, context);
	}
	
	public void update(String repo, String sha, State status, String buildRefUrl, String description , String context) {
		CommitStatusPayload payload = getPayload(status, buildRefUrl, description, context);
		logger.println("URL :: " +  String.format(END_POINT, orgName, repo, sha));
		logger.println("Post Data :: " +  payload);
	}
	
}
