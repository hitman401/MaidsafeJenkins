/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jenkinsci.plugins.MaidsafeJenkins;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Proc;
import hudson.util.ArgumentListBuilder;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 *
 * @author krishnakumarp
 */
public class ShellScript {
    private FilePath tempPath;
    private Launcher launcher;
    private PrintStream logger;
    private EnvVars env;
    
    public ShellScript(FilePath tempPath, Launcher launcher, EnvVars envVars) {
        this.tempPath = tempPath;
        this.launcher = launcher;
        this.logger = launcher.getListener().getLogger();
        this.env = envVars;
    }
    
    private String convertToString(List<String> cmds) {
    	String echoCmd = "echo \"+ %s\"\n";
    	StringBuilder builder = new StringBuilder();
    	for (String cmd : cmds) {
    		builder.append(String.format(echoCmd, cmd));
    		builder.append(cmd).append("\n");    		
    	}
    	return builder.toString();
    }
    
    public int execute(List<String> cmds) throws Exception {
    	return execute(cmds, logger);
    }
    

    
    private int runCommands(List<String> cmds, OutputStream outputStream) throws Exception {
    	int status;
    	FilePath tempFile = tempPath.createTempFile("sricpt_"+ new Date().getTime(), launcher.isUnix() ? ".sh" : ".bat");
        OutputStream outStr = tempFile.write();        
        outStr.write(convertToString(cmds).getBytes());
        outStr.flush();
        outStr.close();
        ArgumentListBuilder command = new ArgumentListBuilder();
        command.addTokenized("sh " + (launcher.isUnix() ? "" : "--login ") + tempFile.getRemote());        
        Launcher.ProcStarter ps = launcher.new ProcStarter();
        ps = ps.cmds(command).stdout(outputStream);
        ps = ps.pwd(tempPath).envs(env);
        Proc proc = launcher.launch(ps);                        
        status = proc.join();
        tempFile.delete();
        return status;
    }
    
    public int execute(List<String> cmds, OutputStream outputStream) throws Exception {  
    	if (outputStream == null) {
            outputStream = logger;
        }
    	return  runCommands(cmds, outputStream);
    }
    
}
