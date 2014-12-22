package org.jenkinsci.plugins.MaidsafeJenkins.util;

import hudson.FilePath;
import java.io.File;
import java.io.FileWriter;
import java.util.List;

/**
 * Utility class for exposing the Environment Variable as a prop file which can loaded in to the job using EnvInject Jenkins plugin
 * 
 */
public class ExposeEnvVariable {
	
	/**
	 * Creates modules.prop file which will expose the affected modules as a space separated list
	 * @param workspace - {@link FilePath} to create the prop file
	 * @param modules
	 * @param buildNo
	 * @return affectedSubModule - File. Prop file with the env variables exposed. <code>null</code> will be returned on error
	 */
	public static File createAffectedSubmoduleFile(FilePath workspace, List<String> modules, int buildNo) {
		StringBuilder builder = null;
		File affetcedSubmoduleFile = null;
		try {
			FileWriter writer;
			affetcedSubmoduleFile = new File(workspace.toURI().getPath() + File.separator + "modules.prop" );
			for (String module : modules) {
				if (builder == null) {
					builder = new StringBuilder("BUILD_NO#=" + buildNo);
					builder.append("\n");
					builder.append("MODULES=");
				} else {
					builder.append(",");
				}
				builder.append(module);
			}
			writer = new FileWriter(affetcedSubmoduleFile);			
			writer.write(builder.toString());
			writer.flush();
			writer.close();			
		} catch(Exception e) {
			e.printStackTrace();
		}
		return affetcedSubmoduleFile;
	}
}
