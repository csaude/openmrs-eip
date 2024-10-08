package org.openmrs.eip.app.receiver;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.Base64;

import org.openmrs.eip.component.SyncProfiles;
import org.openmrs.eip.component.exception.EIPException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

/**
 * Submits http calls to OpenMRS.
 */
@Component
@Profile(SyncProfiles.RECEIVER)
public class CustomHttpClient {
	
	protected static final String PATH = "/ws/rest/v1/";
	
	protected static final BodyHandler<String> BODY_HANDLER = BodyHandlers.ofString(UTF_8);
	
	@Value("${openmrs.baseUrl}")
	private String baseUrl;
	
	@Value("${openmrs.username}")
	private String username;
	
	@Value("${openmrs.password}")
	private char[] password;
	
	private byte[] auth;
	
	private HttpClient client;
	
	public void sendRequest(String resource, String body) throws EIPException {
		if (client == null) {
			client = HttpClient.newHttpClient();
		}
		
		if (auth == null) {
			final String userAndPass = username + ":" + new String(password);
			auth = Base64.getEncoder().encode(userAndPass.getBytes(UTF_8));
		}
		
		HttpRequest.Builder reqBuilder = HttpRequest.newBuilder();
		reqBuilder.uri(URI.create(baseUrl + PATH + resource)).setHeader(HttpHeaders.AUTHORIZATION,
		    "Basic " + new String(auth, UTF_8));
		HttpRequest.BodyPublisher bodyPublisher;
		if (body != null) {
			reqBuilder.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
			bodyPublisher = BodyPublishers.ofString(body, UTF_8);
		} else {
			bodyPublisher = BodyPublishers.noBody();
		}
		
		reqBuilder.POST(bodyPublisher);
		
		HttpResponse response;
		try {
			response = client.send(reqBuilder.build(), BODY_HANDLER);
		}
		catch (Exception e) {
			throw new EIPException("An error occurred while making http call to OpenMRS resource: " + resource, e);
		}
		
		if (response.statusCode() != HttpStatus.NO_CONTENT.value()) {
			throw new EIPException("Http call to OpenMRS resource failed with status code " + response.statusCode());
		}
	}
	
}
