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

    private final String name;
    private final String branch;
    private static File file;
    private static final String REPO_NAME = "MaidSafe";
    
    // Fields in config.jelly must match the parameter names in the "DataBoundConstructor"
    @DataBoundConstructor
    public MaidsafeJenkinsBuilder(String name, String branch) {        
        this.name = name;
        this.branch = branch;
    }

    /**
     * We'll use this from the <tt>config.jelly</tt>.
     */
    public String getName() {
        return name;
    }
        
    public String getBranch() {
        return branch;
    }           
    
     @Override
    public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) {
          EnvVars envVars;
          GithubCheckoutAction checkoutAction;
          GitHubHelper submoduleHelper;
          GitHubPullRequestHelper ghprh;          
          final String ISSUE_KEY_PARAM = "issueKey";          
          String ORG_NAME = "maidsafe"; // TODO  param from config
          String ROOT_REPO = "Maidsafe"; // TODO  param from config
          String DEF_BASE_BRANCH = "master"; // TODO  param from config
          String superProjName = "MaidSafe"; //TODO params config or via cmd
          String issueKey;
          ShellScript script;
          FilePath rootDir;
          PrintStream logger;
          logger = listener.getLogger();
          rootDir = new FilePath(new File(build.getWorkspace() + "/" + ROOT_REPO));
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
            submoduleHelper = new GitHubHelper(superProjName, rootDir, logger, script, DEF_BASE_BRANCH);                                 
            ghprh = new GitHubPullRequestHelper(ORG_NAME, submoduleHelper.getModuleNames(), logger);
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
         * To persist global configuration information,
         * simply store it in a field and call save().
         *
         * <p>
         * If you don't want fields to be persisted, use <tt>transient</tt>.
         */
        private boolean useFrench;

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
        public FormValidation doCheckName(@QueryParameter String value)
                throws IOException, ServletException {
            if (value.length() == 0)
                return FormValidation.error("Please set a name");
            if (value.length() < 4)
                return FormValidation.warning("Isn't the name too short?");
            return FormValidation.ok();
        }
        
        public FormValidation doCheckBranch(@QueryParameter String branch)
                throws IOException, ServletException {
            if (branch.length() == 0)
                return FormValidation.error("Please set a branch");
            if (branch.length() < 4)
                return FormValidation.warning("Isn't the name too short?");
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
            return "Say hello world";
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws Descriptor.FormException {
            // To persist global configuration information,
            // set that to properties and call save().
            useFrench = formData.getBoolean("useFrench");
            // ^Can also use req.bindJSON(this, formData);
            //  (easier when there are many fields; need set* methods for this, like setUseFrench)
            save();
            return super.configure(req,formData);
        }

        /**
         * This method returns true if the global configuration says we should speak French.
         *
         * The method name is bit awkward because global.jelly calls this method to determine
         * the initial state of the checkbox by the naming convention.
         */
        public boolean getUseFrench() {
            return useFrench;
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

