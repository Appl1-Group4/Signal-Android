package org.thoughtcrime.securesms.service.translation.languageresponse;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * Response from GET /language of Azure Translation
 * <p>
 * WARNING : Will only parse translation part of the response
 *
 * @author Fauzan Lubis
 */

@JsonIgnoreProperties(ignoreUnknown = true)
public class LanguagesResponse {
    public Map<String, Language> translation = new HashMap<String, Language>();
}

