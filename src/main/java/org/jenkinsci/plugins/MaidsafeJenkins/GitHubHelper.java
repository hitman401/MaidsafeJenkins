/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jenkinsci.plugins.MaidsafeJenkins;

import hudson.EnvVars;
import hudson.FilePath;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintStream;
import java.util.*;

/**
 *
 * @author krishnakumarp
 */
public class GitHubHelper {
    
    private PrintStream consoleLogger;    
    private FilePath superProject;    
    private ShellScript script;
    private HashMap<String, String> modulePathMapping;       
    private String defaultBaseBranch = "master";
    private final String SUB_MODULE_UPDATE_CMD = 
            "git submodule foreach 'git checkout %s && git pull'\n";
    private final String TEMP_TXT_FILE = 
            "submod_temp.txt";
    private final String SUBMOD_GREP_CMD = 
            "git config --list | sed -rn 's/submodule\\.([^.]*).*\\/(.*)/\\1,\\2/p' > %s\n";    
    private final String SUPER_PROJ_UPDATE_CMD = 
            "git checkout %s && git pull\n";
    public GitHubHelper(String superProjectName, FilePath superProject, 
            PrintStream consoleLogger, ShellScript script, String defaultBaseBranch) {
        this.superProject = superProject;        
        this.consoleLogger = consoleLogger;
        this.script = script;
        if (defaultBaseBranch != null && !defaultBaseBranch.isEmpty()) {
            this.defaultBaseBranch = defaultBaseBranch;
        }        
        updateSubModuleConfig();
        modulePathMapping.put(superProjectName, ".");
    }
    
    private void updateSubModuleConfig() {      
        String readLine;
        String[] splittedArray;
        try {
            // TODO handle with stream instead of file
            if (script.run(String.format(SUBMOD_GREP_CMD, TEMP_TXT_FILE)) == 0) {                               
               File tempFile = new File(superProject.getRemote() + File.separator + TEMP_TXT_FILE);
               FileReader reader = new FileReader(tempFile);
               BufferedReader buff = new BufferedReader(reader);               
               modulePathMapping = new HashMap<String, String>();               
               while ((readLine = buff.readLine()) != null) {
                  splittedArray = readLine.split(",");
                  if (splittedArray.length != 2) {
                     consoleLogger.println("Invalid character while reading submodules -- grep cmd");
                     continue;
                  }
                  modulePathMapping.put(splittedArray[1].toLowerCase(), splittedArray[0]); 
               }              
               tempFile.delete();
            }
        } catch(Exception ex) {
            consoleLogger.println(ex);
        }        
    }
    
    private String backToRootDirFromSubmodule(String submodulePath) {
        String[] dirs = submodulePath.split("/");
        StringBuilder command = new StringBuilder("cd ");
        for (int i=0; i< dirs.length; i++) {
            command.append("../");
        }
        command.append("\n");
        return command.toString();
    }
        
    public GithubCheckoutAction checkoutModules(Map<String, Map<String, Object>> prList) throws Exception {        
        String temp = null;                     
        GithubCheckoutAction checkoutAction = null;
        StringBuilder command = new StringBuilder();        
        if (prList == null || prList.isEmpty()) {
            return checkoutAction;
        }                     
        command.append(String.format(SUPER_PROJ_UPDATE_CMD, defaultBaseBranch));
        command.append(String.format(SUB_MODULE_UPDATE_CMD, defaultBaseBranch));
        Iterator<String> prModules = prList.keySet().iterator();
        while (prModules.hasNext()) {            
            temp = prModules.next();
            if (!modulePathMapping.containsKey(temp)) {                
                consoleLogger.println("ERROR :: " + temp + " could not be found. ");
            }
            command.append("cd ").append(modulePathMapping.get(temp)).append("\n");
            command.append(buildPRMergeCommands(prList.get(temp)));
            command.append(backToRootDirFromSubmodule(modulePathMapping.get(temp)));                            
        }
        
        if (script.run(command.toString()) == 0) {
            checkoutAction = new GithubCheckoutAction();
            checkoutAction.setBranchTarget(((Map<String, Object>) prList.get(temp).get("head")).get("ref").toString());
        }
        return checkoutAction;
    }
            
    private String buildPRMergeCommands(Map<String, Object> pullRequest) {        
        StringBuilder mergeCommand = new StringBuilder();
        Map<String, Object> prHead = (Map<String, Object>) pullRequest.get("head");
        String localBranch = prHead.get("ref").toString();
        String baseBranch = ((Map) pullRequest.get("base")).get("ref").toString();
        String pullRemoteSSHUrl = ((Map) prHead.get("repo")).get("ssh_url").toString();                    
        mergeCommand.append("git checkout -b ").append(localBranch).append(" ").append(baseBranch).append("\n");
        mergeCommand.append("git pull ").append(pullRemoteSSHUrl).append(" ").append(localBranch).append("\n");
        return mergeCommand.toString();    
    }
    
    public List<String> getModuleNames() {
        List<String> moduleNames = new ArrayList<String>();
        Iterator<String> keysIterator = modulePathMapping.keySet().iterator();
        while(keysIterator.hasNext()) {
            moduleNames.add(keysIterator.next()); 
        }
        return moduleNames; 
    }
    
    public String getSubModulePath(String submoduleName) {
        if (submoduleName == null || submoduleName.isEmpty()) {
            return null;
        }
        return modulePathMapping.get(submoduleName.toLowerCase());
    }
                    
}
