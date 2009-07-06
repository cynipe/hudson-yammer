package rsh.hudson.plugin;

import hudson.Plugin;
import hudson.tasks.BuildStep;

/**
 * Entry point of a plugin.
 *
 * <p>
 * There must be one {@link Plugin} class in each plugin.
 * See javadoc of {@link Plugin} for more about what can be done on this class.
 */
public class PluginImpl extends Plugin {
    @SuppressWarnings("deprecation")
	public void start() throws Exception {
        // Register the Yammer publisher
        BuildStep.PUBLISHERS.add(YammerPublisher.DESCRIPTOR);
    }
    
    public static String DISPLAY_NAME = "Yammer Plugin";
}