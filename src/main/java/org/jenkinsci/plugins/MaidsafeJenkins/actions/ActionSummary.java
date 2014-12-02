package org.jenkinsci.plugins.MaidsafeJenkins.actions;


import java.util.HashMap;
import java.util.List;


public class ActionSummary {
	
	private HashMap<String, Object> summary;
	private final String BRANCH_TARGET_KEY = "targetBranch";
	private final String BASE_BRANCH_KEY = "baseBranch";
	private final String ISSUE_KEY = "issueKey";
	private final String MATCHIN_PR_LIST_KEY = "matchinPRList";
	private final String BUILD_PASSED_KEY = "buildPassed";
	private final String REASON_KEY = "failureReason";
	private final String MODULES_WITH_MATCHING_KEY = "modulesMatchingIssue";	
	
	public ActionSummary() {
		summary = new HashMap<String, Object>();
		summary.put(BUILD_PASSED_KEY, false);
	}

	public String getIssueKey() {
		return (String) summary.get(ISSUE_KEY);
	}

	public void setIssueKey(String issueKey) {
		summary.put(ISSUE_KEY, issueKey);
	}

	@SuppressWarnings("unchecked")
	public List<String> getMatchingPRList() {
		return (List<String>) summary.get(MATCHIN_PR_LIST_KEY);
	}

	public void setMatchingPRList(List<String> matchingPRList) {
		summary.put(MATCHIN_PR_LIST_KEY, matchingPRList);
	}

	public boolean isBuilPassed() {
		return (Boolean) summary.get(BUILD_PASSED_KEY);
	}

	public void setBuilPassed(boolean builPassed) {
		summary.put(BUILD_PASSED_KEY, builPassed);
	}

	public String getReasonForFailure() {
		return (String) summary.get(REASON_KEY);
	}

	public void setReasonForFailure(String reasonForFailure) {
		summary.put(ISSUE_KEY, reasonForFailure);
	}

	public String getBaseBranch() {
		return (String) summary.get(BASE_BRANCH_KEY);
    }

    public void setBaseBranch(String baseBranch) {
    	summary.put(BASE_BRANCH_KEY, baseBranch);
    }
        
    public String getBranchTarget() {
    	return (String) summary.get(BRANCH_TARGET_KEY);
    }

    public void setBranchTarget(String branchTarget) {
    	summary.put(BRANCH_TARGET_KEY, branchTarget);
    }
    
	@SuppressWarnings("unchecked")
	public List<String> getModulesWithMatchingPR() {
		return (List<String>) summary.get(MODULES_WITH_MATCHING_KEY);
	}

	public void setModulesWithMatchingPR(List<String> modulesWithMatchingPR) {
		summary.put(MODULES_WITH_MATCHING_KEY, modulesWithMatchingPR);
	}
	
	public HashMap<String, Object> getSummary() {
		return summary;
	}

}
