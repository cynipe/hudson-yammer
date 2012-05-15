package hudson.plugins.yammer;

import hudson.ProxyConfiguration;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;

public class YammerUtils {
	private static final String TINYURL_URL = "http://tinyurl.com/api-create.php?url=";

	/**
	 * Restful Yammer URL for groups api.
	 */
	private static final String YAMMER_API_V1_GROUPS = "https://www.yammer.com/api/v1/groups";

	private static final String MESSAGE_GROUP_ID_PARAM_NAME = "group_id";

	private static final String MESSAGE_BODY_PARAM_NAME = "body";

	/**
	 * Restful Yammer URL for messages api.
	 */
	private static final String YAMMER_API_V1_MESSAGES = "https://www.yammer.com/api/v1/messages";

	private static final String AUTHORIZATION_HEADER = "Authorization";

	/**
	 * Yammer URL for getting access token.
	 */
	private static final String OAUTH_ACCESS_TOKEN_URL = "https://www.yammer.com/oauth/access_token?callback_token=";

	/**
	 * Yammer URL for getting request token.
	 */
	private static final String OAUTH_REQUEST_TOKEN_URL = "https://www.yammer.com/oauth/request_token";

	/**
	 * Constant for oauth_token query string parameter name.
	 */
	public static final String OAUTH_TOKEN = "oauth_token";

	/**
	 * Constant for oauth_token_secret query string parameter name.
	 */
	public static final String OAUTH_SECRET = "oauth_token_secret";

	/**
	 * Creates a string of the oauth_headers for this plugin.
	 * 
	 * @param token
	 * @param token_secret
	 * @return
	 */
	private static String oauth_headers(String token, String token_secret,
			String applicationKey, String applicationSecret) {
		StringBuffer buff = new StringBuffer();
		buff.append("OAuth realm=\"");
		buff.append("\", oauth_consumer_key=\"");
		buff.append(applicationKey);
		buff.append("\", ");

		if (token != null) {
			buff.append("oauth_token=\"");
			buff.append(token);
			buff.append("\", ");
		}

		buff.append("oauth_signature_method=\"");
		buff.append("PLAINTEXT");
		buff.append("\", oauth_signature=\"");
		buff.append(applicationSecret);
		buff.append("%26");
		if (token_secret != null) {
			buff.append(token_secret);
		}
		buff.append("\", oauth_timestamp=\"");
		buff.append(new Date().getTime());
		buff.append("\", oauth_nonce=\"");
		buff.append(new Date().getTime());
		buff.append("\", oauth_version=\"1.0\"");

		System.out.println(buff.toString());
		return buff.toString();
	}

	/**
	 * Gets a new set of request token parameters for this plugin. These
	 * parameters are used in getting an access token for the plugin. Please not
	 * that request tokens expire.
	 * 
	 * @return
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	public static Map<String, String> getRequestTokenParameters(
			String applicationKey, String applicationSecret)
			throws ClientProtocolException, IOException {

		HttpClient httpclient = getHttpClient();
		try {
			HttpPost httpost = new HttpPost(OAUTH_REQUEST_TOKEN_URL);
			httpost.addHeader(
					AUTHORIZATION_HEADER,
					oauth_headers(null, null, applicationKey, applicationSecret));

			BufferedReader reader = getResponseReader(httpclient
					.execute(httpost));

			// Request token parameters are on the first line of the response
			String requestTokenResponseString = reader.readLine();
			return parseQueryStringParameters(requestTokenResponseString);
		} finally {
			httpclient.getConnectionManager().shutdown();
		}
	}

	/**
	 * Gets the access token parameters for this plugin using the supplied
	 * request token parameters and user authenticated access token.
	 * 
	 * @param requestAuthToken
	 * @param requestAuthSecret
	 * @param accessToken
	 * @return
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	public static Map<String, String> getAccessTokenParameters(
			String requestAuthToken, String requestAuthSecret,
			String accessToken, String applicationKey, String applicationSecret)
			throws ClientProtocolException, IOException {

		HttpClient httpclient = getHttpClient();
		try {
			HttpPost httpost = new HttpPost(OAUTH_ACCESS_TOKEN_URL
					+ accessToken);
			httpost.addHeader(
					AUTHORIZATION_HEADER,
					oauth_headers(requestAuthToken, requestAuthSecret,
							applicationKey, applicationSecret));

			BufferedReader reader = getResponseReader(httpclient
					.execute(httpost));

			// Access token parameters are on the first line of the response
			String accessTokenResponseString = reader.readLine();
			return parseQueryStringParameters(accessTokenResponseString);
		} finally {
			httpclient.getConnectionManager().shutdown();
		}
	}

	private static DefaultHttpClient getHttpClient() throws IOException {
		DefaultHttpClient httpClient = new DefaultHttpClient();

		ProxyConfiguration proxyConfiguration = ProxyConfiguration.load();
		if (proxyConfiguration != null) {
			httpClient.getCredentialsProvider().setCredentials(
					new AuthScope(proxyConfiguration.name,
							proxyConfiguration.port),
					new UsernamePasswordCredentials(proxyConfiguration
							.getUserName(), proxyConfiguration.getPassword()));

			HttpHost proxy = new HttpHost(proxyConfiguration.name,
					proxyConfiguration.port);

			httpClient.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY,
					proxy);
		}

		return httpClient;
	}

	/**
	 * Post a message to Yammer within the supplied group.
	 * 
	 * @param accessAuthToken
	 * @param accessAuthSecret
	 * @param message
	 * @param group
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	public static void sendMessage(String accessAuthToken,
			String accessAuthSecret, String message, String group,
			String applicationKey, String applicationSecret)
			throws ClientProtocolException, IOException {

		HttpClient httpclient = getHttpClient();
		try {
			HttpPost httpPost = new HttpPost(YAMMER_API_V1_MESSAGES);
			httpPost.addHeader(
					AUTHORIZATION_HEADER,
					oauth_headers(accessAuthToken, accessAuthSecret,
							applicationKey, applicationSecret));

			List<NameValuePair> nvps = new ArrayList<NameValuePair>();
			nvps.add(new BasicNameValuePair(MESSAGE_BODY_PARAM_NAME, message));

			// If a group name is supplied find the group id and post the
			// message to it
			if (group != null && !group.equals("")) {
				nvps.add(new BasicNameValuePair(MESSAGE_GROUP_ID_PARAM_NAME,
						group));
			}

			httpPost.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));
			httpclient.execute(httpPost);
		} finally {
			httpclient.getConnectionManager().shutdown();
		}
	}

	/**
	 * Post a message to Yammer.
	 * 
	 * @param accessAuthToken
	 * @param accessAuthSecret
	 * @param message
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	public static void sendMessage(String accessAuthToken,
			String accessAuthSecret, String message, String applicationKey,
			String applicationSecret) throws ClientProtocolException,
			IOException {

		sendMessage(accessAuthToken, accessAuthSecret, message, "",
				applicationKey, applicationSecret);
	}

	/**
	 * Get a reader for the supplied HttpResponse.
	 * 
	 * @param response
	 * @return
	 * @throws IllegalStateException
	 * @throws IOException
	 */
	private static BufferedReader getResponseReader(HttpResponse response)
			throws IllegalStateException, IOException {
		HttpEntity entity = response.getEntity();

		if (entity != null) {
			InputStream instream = entity.getContent();
			return new BufferedReader(new InputStreamReader(instream));
		}
		return null;
	}

	/**
	 * Parse a string of query string parameters into a Map.
	 * 
	 * @param queryString
	 * @return
	 */
	private static Map<String, String> parseQueryStringParameters(
			String queryString) {

		Map<String, String> parametersMap = new HashMap<String, String>();
		StringTokenizer tokenizerNameValuePair = new StringTokenizer(
				queryString, "&");

		while (tokenizerNameValuePair.hasMoreTokens()) {
			try {
				String strNameValuePair = tokenizerNameValuePair.nextToken();
				StringTokenizer tokenizerValue = new StringTokenizer(
						strNameValuePair, "=");

				String strName = tokenizerValue.nextToken();
				String strValue = tokenizerValue.nextToken();

				parametersMap.put(strName, strValue);
			} catch (Throwable t) {
				// If we cannot parse a parameter, ignore it
			}
		}

		return parametersMap;
	}

	/**
	 * Gets the group id of the supplied group name.
	 * 
	 * @param accessAuthToken
	 * @param accessAuthSecret
	 * @param groupName
	 * @return
	 * @throws IllegalStateException
	 * @throws ClientProtocolException
	 * @throws IOException
	 * @throws DocumentException
	 * @throws JDOMException
	 */
	@SuppressWarnings("unchecked")
	public static String getGroupId(String accessAuthToken,
			String accessAuthSecret, String groupName, String applicationKey,
			String applicationSecret) throws IllegalStateException,
			ClientProtocolException, IOException, DocumentException {

		HttpClient httpclient = getHttpClient();
		try {
			Integer currentPage = 1;
			Integer responses = null;
			String groupId = null;

			// Yammer returns 20 results per page so need to go through all the
			// pages trying to find the group
			while ((responses == null || responses != 0) && groupId == null) {
				HttpGet httpGet = new HttpGet(YAMMER_API_V1_GROUPS + "?page="
						+ currentPage + "&letter=" + groupName.substring(0, 1));
				httpGet.addHeader(
						AUTHORIZATION_HEADER,
						oauth_headers(accessAuthToken, accessAuthSecret,
								applicationKey, applicationSecret));

				SAXReader reader = new SAXReader();
				Document doc = reader.read(getResponseReader(httpclient
						.execute(httpGet)));

				List nodes = doc.selectNodes("/response/response");
				responses = nodes.size();

				if (responses > 0) {
					Node node = doc
							.selectSingleNode("/response/response[full-name='"
									+ groupName + "']/id");
					groupId = node != null ? node.getStringValue() : null;
				}

				currentPage++;
			}

			return groupId;
		} finally {
			httpclient.getConnectionManager().shutdown();
		}
	}

	public static String createTinyUrl(String url)
			throws IllegalStateException, ClientProtocolException, IOException {

		HttpClient httpClient = getHttpClient();
		try {
			HttpGet httpGet = new HttpGet(TINYURL_URL + url.replace(" ", "%20"));

			BufferedReader reader = getResponseReader(httpClient
					.execute(httpGet));

			return reader.readLine();
		} finally {
			httpClient.getConnectionManager().shutdown();
		}
	}
}
