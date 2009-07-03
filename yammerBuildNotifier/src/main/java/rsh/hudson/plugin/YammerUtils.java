package rsh.hudson.plugin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;

public class YammerUtils {
	/**
	 * The Yammer generated Key for this app.
	 */
	private static final String KEY = "31kJmOgiAH0btAGONINog";

	/**
	 * The Yammer generated Secret for this app.
	 */
	private static final String SECRET = "tUIqe5BUDFAYOC2TGWT8uhCLD7SMBljHo3sOOHeAkT8";

	public static final String OAUTH_TOKEN = "oauth_token";

	public static final String OAUTH_SECRET = "oauth_token_secret";

	private static String oauth_headers(String token, String token_secret) {
		StringBuffer buff = new StringBuffer();
		buff.append("OAuth realm=\"");
		buff.append("\", oauth_consumer_key=\"");
		buff.append(KEY);
		buff.append("\", ");

		if (token != null) {
			buff.append("oauth_token=\"");
			buff.append(token);
			buff.append("\", ");
		}

		buff.append("oauth_signature_method=\"");
		buff.append("PLAINTEXT");
		buff.append("\", oauth_signature=\"");
		buff.append(SECRET);
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

	public static Map<String, String> getRequestTokenParameters()
			throws ClientProtocolException, IOException {
		HttpClient httpclient = new DefaultHttpClient();
		try {
			HttpPost httpost = new HttpPost(
					"https://www.yammer.com/oauth/request_token");
			httpost.addHeader("Authorization", oauth_headers(null, null));

			BufferedReader reader = getResponseReader(httpclient
					.execute(httpost));
			String requestTokenResponseString = reader.readLine();
			return parseQueryStringParameters(requestTokenResponseString);
		} finally {
			httpclient.getConnectionManager().shutdown();
		}
	}

	public static Map<String, String> getAccessTokenParameters(
			String requestAuthToken, String requestAuthSecret,
			String accessToken) throws ClientProtocolException, IOException {
		HttpClient httpclient = new DefaultHttpClient();
		try {
			HttpPost httpost = new HttpPost(
					"https://www.yammer.com/oauth/access_token?callback_token="
							+ accessToken);
			httpost.addHeader("Authorization", oauth_headers(requestAuthToken,
					requestAuthSecret));

			BufferedReader reader = getResponseReader(httpclient
					.execute(httpost));
			String accessTokenResponseString = reader.readLine();
			return parseQueryStringParameters(accessTokenResponseString);
		} finally {
			httpclient.getConnectionManager().shutdown();
		}
	}

	public static void sendMessage(String accessAuthToken,
			String accessAuthSecret, String message, String group) {
		HttpClient httpclient = new DefaultHttpClient();

		try {
			HttpPost httpPost = new HttpPost(
					"https://www.yammer.com/api/v1/messages");
			httpPost.addHeader("Authorization", oauth_headers(accessAuthToken,
					accessAuthSecret));

			List<NameValuePair> nvps = new ArrayList<NameValuePair>();
			nvps.add(new BasicNameValuePair("body", message));

			if (!group.equals("")) {
				nvps.add(new BasicNameValuePair("group_id", group));
			}

			httpPost.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));
			httpclient.execute(httpPost);
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		} catch (ClientProtocolException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		} finally {
			httpclient.getConnectionManager().shutdown();
		}
	}

	private static BufferedReader getResponseReader(HttpResponse response)
			throws IllegalStateException, IOException {
		HttpEntity entity = response.getEntity();

		if (entity != null) {
			InputStream instream = entity.getContent();
			return new BufferedReader(new InputStreamReader(instream));
		}
		return null;
	}

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

}
