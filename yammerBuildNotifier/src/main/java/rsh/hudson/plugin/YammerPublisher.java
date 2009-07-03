package rsh.hudson.plugin;

import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.Build;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.tasks.Publisher;

import java.io.IOException;
import java.util.Map;

import net.sf.json.JSONObject;

import org.apache.http.client.ClientProtocolException;
import org.apache.log4j.Logger;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

/** 
 * <p>
 * When the user configures the project and enables this publisher,
 * {@link DescriptorImpl#newInstance(StaplerRequest)} is invoked and a new
 * {@link YammerPublisher} is created. The created instance is persisted to the
 * project configuration XML by using XStream, so this allows you to use
 * instance fields (like {@link #name}) to remember the configuration.
 * 
 * <p>
 * When a build is performed, the
 * {@link #perform(Build, Launcher, BuildListener)} method will be invoked.
 * 
 * @author Russell Hart
 */
public class YammerPublisher extends Publisher {
	protected static final Logger LOGGER = Logger.getLogger(YammerPublisher.class.getName());
	
	public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();
	
	private String yammerGroup;
		
    @SuppressWarnings("deprecation")
	@DataBoundConstructor
    public YammerPublisher(String yammerGroup) {
        this.yammerGroup = yammerGroup;
    }

	public String getYammerGroup() {
		return this.yammerGroup;
	}
    
    public Descriptor<Publisher> getDescriptor() {
		return DESCRIPTOR;
	}
	
	public boolean perform(AbstractBuild<?,?> build, Launcher launcher, BuildListener listener) throws IOException{
		StringBuilder messageBuilder = new StringBuilder();
		messageBuilder.append("Hudson Build Results - ");
		messageBuilder.append(build.getFullDisplayName());
		messageBuilder.append(" ");
		messageBuilder.append(build.getResult().toString());
		messageBuilder.append(" ");
		messageBuilder.append(DESCRIPTOR.hudsonUrl + build.getUrl());
		
		YammerUtils.sendMessage(DESCRIPTOR.accessAuthToken, DESCRIPTOR.accessAuthSecret, messageBuilder.toString(), this.yammerGroup);
		
		return true;
	}
	
	public static final class DescriptorImpl extends Descriptor<Publisher> {
		/**
		 * This is the Yammer request auth token used in getting the access token.
		 * See http://www.yammer.com/api_oauth.html 
		 */
		private String accessToken;
		
		private String requestAuthToken;
		
		private String requestAuthSecret;
		
		private String accessAuthToken = "";
		
		private String accessAuthSecret = "";
		
		private String hudsonUrl;

        public DescriptorImpl() {
        	super(YammerPublisher.class);
        	// Load the saved configuration
        	load();
        	initialseRequestAuthParameters();
        }
        
        private void initialseRequestAuthParameters(){
        	// Get request auth parameters
    		Map<String, String> parametersMap;
			try {
				parametersMap = YammerUtils.getRequestTokenParameters();
				this.requestAuthToken = parametersMap.get(YammerUtils.OAUTH_TOKEN); 
				this.requestAuthSecret = parametersMap.get(YammerUtils.OAUTH_SECRET);
			} catch (ClientProtocolException e) {
				throw new RuntimeException(e);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
        }
		
		public String getDisplayName() {
            return "Publish results in Yammer";
        }
        
        public String accessAuthToken() {
        	return accessAuthToken;
        }
        
        public String accessAuthSecret() {
        	return accessAuthSecret;
        }
        
        public String requestAuthToken() {
        	return requestAuthToken;
        }
        
        public String requestAuthSecret() {
        	return requestAuthSecret;
        }
        
        public String accessToken() {
        	return accessToken;
        }
        
        public String hudsonUrl() {
        	return hudsonUrl;
        }
        
        @SuppressWarnings("deprecation")
		@Override
        public boolean configure(StaplerRequest req, JSONObject o) throws FormException {
            // to persist global configuration information, set that to properties and call save().
            String newAccessToken = o.getString("accessToken");
            this.hudsonUrl = o.getString("hudsonUrl");
            
            // If accessToken isnt blank and different to current saved one then get the authAccess params
            if (!newAccessToken.equals("") && !newAccessToken.equals(this.accessToken)) {

            	this.accessToken = newAccessToken;
            	
            	Map<String, String> parametersMap;
            	try {
					parametersMap = YammerUtils.getAccessTokenParameters(this.requestAuthToken, this.requestAuthSecret, this.accessToken);
					this.accessAuthToken = parametersMap.get(YammerUtils.OAUTH_TOKEN); 
					this.accessAuthSecret = parametersMap.get(YammerUtils.OAUTH_SECRET);
				} catch (ClientProtocolException e) {
					throw new RuntimeException(e);
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
            }
            
            save();
            return super.configure(req);
        }
	}

}