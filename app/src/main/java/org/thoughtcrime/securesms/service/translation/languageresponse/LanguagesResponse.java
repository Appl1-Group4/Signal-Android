package org.thoughtcrime.securesms.service.translation.languageresponse;

import java.util.HashMap;
import java.util.Map;

/**
 * Response from GET /language of Azure Translation
 *
 * WARNING : Only work with scope=translation only
 *
 * @author Fauzan Lubis
 */
public class LanguagesResponse {
    public Map<String, Language> translation = new HashMap<String, Language>();
}

