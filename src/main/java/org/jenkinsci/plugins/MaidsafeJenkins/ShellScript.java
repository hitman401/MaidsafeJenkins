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
    	StringBuilder builder = new StringBuilder();
    	for (String cmd : cmds) {
    		builder.append("echo + " + cmd).append("\n");
    		builder.append(cmd).append("\n");    		
    	}
    	builder.append("exit");
    	return builder.toString();
    }
    
    public void execute(List<String> cmds) throws Exception {
    	execute(cmds, logger);
    }
    
    private void runAsShellCommand(List<String> cmds, OutputStream outputStream) throws Exception {
    	launcher.launch().cmds(cmds).envs(env).join();
    }
    
    private void runWinBatch(List<String> cmds, OutputStream outputStream) throws Exception {
    	FilePath tempFile = tempPath.createTempFile("sricpt_"+ new Date().getTime(), launcher.isUnix() ? ".sh" : ".bat");
        OutputStream outStr = tempFile.write();
        outStr.write(convertToString(cmds).getBytes());
        outStr.flush();
        outStr.close();
        ArgumentListBuilder command = new ArgumentListBuilder();
        command.addTokenized("sh --login " + tempFile.getRemote());        
        Launcher.ProcStarter ps = launcher.new ProcStarter();
        ps = ps.cmds(command).stdout(outputStream);
        ps = ps.pwd(tempPath).envs(env);
        Proc proc = launcher.launch(ps);                        
        proc.join();
        tempFile.delete();
    }
    
    public void execute(List<String> cmds, OutputStream outputStream) throws Exception{
    	if (outputStream == null) {
            outputStream = logger;
        }
    	if (launcher.isUnix()) {
    		runAsShellCommand(cmds, outputStream);
    	} else {
    		runWinBatch(cmds, outputStream);
    	}               
    }
    
}
