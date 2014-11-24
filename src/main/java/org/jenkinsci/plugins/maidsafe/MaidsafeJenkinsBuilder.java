package org.jenkinsci.plugins.maidsafe;

import hudson.*;
import hudson.model.*;
import hudson.model.listeners.RunListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import java.io.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

/**
 * Sample {@link Builder}.
 *
 * <p>
 * When the user configures the project and enables this builder,
 * {@link DescriptorImpl#newInstance(StaplerRequest)} is invoked
 * and a new {@link HelloWorldBuilder} is created. The created
 * instance is persisted to the project configuration XML by using
 * XStream, so this allows you to use instance fields (like {@link #name})
 * to remember the configuration.
 *
 * <p>
 * When a build is performed, the {@link #perform(AbstractBuild, Launcher, BuildListener)}
 * method will be invoked. 
 *
 * 
 */
public class MaidsafeJenkinsBuilder extends Builder {

    private String orgName;
    private String repoSubFolder;
    private String superProjectName;
    private String defaultBaseBranch;

    public String getDefaultBaseBranch() {
        return defaultBaseBranch;
    }

    public void setDefaultBaseBranch(String defaultBaseBranch) {
        this.defaultBaseBranch = defaultBaseBranch;
    }

    public String getOrgName() {
        return orgName;
    }

    public void setOrgName(String orgName) {
        this.orgName = orgName;
    }

    public String getRepoSubFolder() {
        return repoSubFolder;
    }

    public void setRepoSubFolder(String repoSubFolder) {
        this.repoSubFolder = repoSubFolder;
    }

    public String getSuperProjectName() {
        return superProjectName;
    }

    public void setSuperProjectName(String superProjectName) {
        this.superProjectName = superProjectName;
    }
    
    // Fields in config.jelly must match the parameter names in the "DataBoundConstructor"
    @DataBoundConstructor
    public MaidsafeJenkinsBuilder(String orgName, String repoSubFolder, String superProjectName, String defaultBaseBranch) {        
        this.orgName = orgName;
        this.repoSubFolder = repoSubFolder;
        this.superProjectName = superProjectName;
        this.defaultBaseBranch = defaultBaseBranch;
    }
    
    
     @Override
    public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) {
          EnvVars envVars;
          GithubCheckoutAction checkoutAction;
          GitHubHelper submoduleHelper;
          GitHubPullRequestHelper ghprh;          
          final String ISSUE_KEY_PARAM = "issueKey";          
//          String ORG_NAME = "maidsafe"; // TODO  param from config
//          String ROOT_REPO = "Maidsafe"; // TODO  param from config
//          String DEF_BASE_BRANCH = "master"; // TODO  param from config
//          String superProjName = "MaidSafe"; //TODO params config or via cmd
          String issueKey;
          ShellScript script;
          FilePath rootDir;
          PrintStream logger;
          logger = listener.getLogger();
          rootDir = new FilePath(new File(build.getWorkspace() + "/" + repoSubFolder));
          logger.println("Git REPO :: " + rootDir.getRemote());
          try {
            envVars = build.getEnvironment(listener);
            script = new ShellScript(rootDir, launcher, envVars);
            /********PRAMETERS RECEIVED**********/            
            issueKey = envVars.get(ISSUE_KEY_PARAM, null);                         
            /***********************************/                                    
            script.run("git submodule update --init");
            if (issueKey == null || issueKey.isEmpty()) {
                logger.println("Build Stopped -- parameter not present");
                return false;
            }
            logger.println("Process initiated for token " + issueKey);
            submoduleHelper = new GitHubHelper(superProjectName, rootDir, logger, script, defaultBaseBranch);                                 
            ghprh = new GitHubPullRequestHelper(orgName, submoduleHelper.getModuleNames(), logger);
            Map<String, Map<String, Object>> pullRequest = ghprh
                    .getMatchingPR(issueKey, 
                    GitHubPullRequestHelper.Filter.OPEN, 
                    GitHubPullRequestHelper.PR_MATCH_STRATERGY
                    .BRANCH_NAME_STARTS_WITH_IGNORE_CASE);            
            if (pullRequest == null || pullRequest.isEmpty()) {
                logger.println("No Matching Pull Request found for " + issueKey);
                return false;
            }      
            checkoutAction = submoduleHelper.checkoutModules(pullRequest);
            if (checkoutAction != null) {
                checkoutAction.setScript(script);
                build.addAction(checkoutAction);
            }            
            return checkoutAction != null;                        
          } catch(Exception exception) {
            listener.getLogger().println(exception);  
          }
          return true;
     }
     
    @Extension
    public static class BuildRunlistener extends RunListener<Run> implements Serializable {

        @Override
        public void onCompleted(Run r, TaskListener tl) {
            super.onCompleted(r, tl);
            try {
                String DEL_BRANCH_CMD = "git submodule foreach 'git branch -D %s || : '";
                GithubCheckoutAction checkoutAction = r.getAction(GithubCheckoutAction.class);
                if (checkoutAction == null) {
                    return;
                }
                tl.getLogger().println("Cleaning up the temporary branch " + checkoutAction.getBranchTarget());
                checkoutAction
                        .getScript()
                        .run(String.format(DEL_BRANCH_CMD, checkoutAction.getBranchTarget()));
            } catch (Exception ex) {
                Logger.getLogger(MaidsafeJenkinsBuilder.class.getName()).log(Level.SEVERE, null, ex);
            }            
        }
         
     }
    @Extension
    public static class DescriptorImpl  extends BuildStepDescriptor<Builder> {

        /**
         * In order to load the persisted global configuration, you have to 
         * call load() in the constructor.
         */
        public DescriptorImpl() {
            load();
        }

        /**
         * Performs on-the-fly validation of the form field 'name'.
         *
         * @param value
         *      This parameter receives the value that the user has typed.
         * @return
         *      Indicates the outcome of the validation. This is sent to the browser.
         *      <p>
         *      Note that returning {@link FormValidation#error(String)} does not
         *      prevent the form from being saved. It just means that a message
         *      will be displayed to the user. 
         */
        public FormValidation doCheckOrgName(@QueryParameter String value)
                throws IOException, ServletException {
            if (value.length() == 0)
                return FormValidation.error("Please set a organisation name");            
            return FormValidation.ok();
        }
        

        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            // Indicates that this builder can be used with all kinds of project types 
            return true;
        }

        /**
         * This human readable name is used in the configuration screen.
         */
        public String getDisplayName() {
            return "Maidsafe CI Builder";
        }        
    }

    // Overridden for better type safety.
    // If your plugin doesn't really define any property on Descriptor,
    // you don't have to do this.
    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl)super.getDescriptor();
    }
   
}

