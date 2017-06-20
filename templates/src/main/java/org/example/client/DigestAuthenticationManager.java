package org.example.client;

import org.example.util.CredentialsUtil;
import org.example.util.URIUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.concurrent.ExecutionException;

/**
 * Implements Spring Security's AuthenticationManager interface so that it can authenticate users by making a simple
 * request to MarkLogic and checking for a 401. Also implements AuthenticationProvider so that it can be used with
 * Spring Security's ProviderManager.
 *
 *
 */
@Component
public class DigestAuthenticationManager implements AuthenticationProvider, AuthenticationManager {

	@Autowired
	private RestTemplateCache restTemplateCache;

	@Autowired
	private URIUtil uriUtil;

    private String pathToAuthenticateAgainst = "/ext/login-check.xqy";

    /**
     * A RestConfig instance is needed so a request can be made to MarkLogic to see if the user can successfully
     * authenticate.
     *
     */
    public DigestAuthenticationManager() {
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {

		RestTemplateCacheKey key = CredentialsUtil.authTokenToCacheKey(authentication);
		try {
			RestTemplate client = restTemplateCache.fetchOrCreate(key);
			URI uri = uriUtil.buildUri(pathToAuthenticateAgainst, "");
            client.getForEntity(uri, String.class);
        } catch (HttpClientErrorException ex) {
            if (HttpStatus.NOT_FOUND.equals(ex.getStatusCode())) {
                // Authenticated, but the path wasn't found - that's okay, we just needed to verify authentication
            } else if (HttpStatus.UNAUTHORIZED.equals(ex.getStatusCode())) {
                throw new BadCredentialsException("Invalid credentials");
            } else {
                throw new AuthenticationServiceException(ex.getMessage(), ex);
            }
        } catch (ExecutionException e) {
			throw new AuthenticationServiceException(e.getMessage(), e);
		}
		return CredentialsUtil.clone(authentication);
    }

    public void setPathToAuthenticateAgainst(String pathToAuthenticateAgainst) {
        this.pathToAuthenticateAgainst = pathToAuthenticateAgainst;
    }
}

