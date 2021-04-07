package org.thoughtcrime.securesms.service.translation.translateresponse;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Response from POSY /translate of Azure Translation
 * <p>
 * WARNING : Will only parse translations part of the response
 *
 * @author Fauzan Lubis
 */

@JsonIgnoreProperties(ignoreUnknown = true)
public class TranslationResponse {
    public List<Translation> translations;
}
