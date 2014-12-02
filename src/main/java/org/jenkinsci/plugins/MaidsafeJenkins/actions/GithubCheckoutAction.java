package org.jenkinsci.plugins.MaidsafeJenkins.actions;

import java.util.HashMap;

import org.jenkinsci.plugins.MaidsafeJenkins.util.ShellScript;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

import hudson.Functions;
import hudson.model.Action;
import hudson.model.Api;

@ExportedBean(defaultVisibility=999)
public class GithubCheckoutAction extends ActionSummary implements Action {
	  private final String DISPLAY_NAME = "Github Checkout Summary"; 
	    private final String URL = "checkoutSummary";
	    private final String ICON = Functions.getResourcePath() + "/plugin/MaidsafeJenkins/icons/git-32x32.png";    
	    private ShellScript script;
	    
	    public Api getApi() {
	        return new Api(this);
	    }
	    
	    public String getDisplayName() {		
			return DISPLAY_NAME;
		}

		public String getIconFileName() { 
			return ICON;
		}

		public String getUrlName() {		
			return URL;
		}
	    
	    public ShellScript getScript() {
	        return script;
	    }

	    public void setScript(ShellScript script) {
	        this.script = script;
	    }	    	  
	    
	    @Exported
	    public HashMap<String, Object> getGithubCheckoutAction() {
	    	return getSummary();
	    }
	   	    	  
}
