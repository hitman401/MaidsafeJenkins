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
    
    //TODO add a random number 
    public int run(String cmd) throws Exception {
        return run(cmd, logger);
    }
    //TODO convert to list or override with list
    public int run(String cmd, OutputStream outputStream) throws Exception {        
        if (outputStream == null) {
            outputStream = logger;
        }
        FilePath tempFile = tempPath.createTempFile("sricpt_"+ new Date().getTime(), launcher.isUnix() ? ".sh" : ".bat");
        OutputStream outStr = tempFile.write();
        outStr.write(cmd.getBytes());
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
        return 0;
    }
    
}
