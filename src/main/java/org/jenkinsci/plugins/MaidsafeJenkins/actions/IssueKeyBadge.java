package org.jenkinsci.plugins.MaidsafeJenkins.actions;

import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;
import hudson.model.*;

@ExportedBean(defaultVisibility=999)
public class IssueKeyBadge implements BuildBadgeAction {
	
	private String issueKey;	

	@Exported
	public String getIssueKey() {
		return issueKey;
	}

	public void setIssueKey(String issueKey) {
		this.issueKey = issueKey;
	}

	public String getIconFileName() {		
		return null;
	}

	public String getDisplayName() {
		return "IssueKey";
	}

	public String getUrlName() {
		return null;
	}

}
