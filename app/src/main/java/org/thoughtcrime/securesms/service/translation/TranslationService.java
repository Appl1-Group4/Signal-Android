package org.thoughtcrime.securesms.service.translation;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.signal.core.util.logging.Log;
import org.thoughtcrime.securesms.BuildConfig;
import org.thoughtcrime.securesms.service.translation.exception.ResourcesNotAvailableException;
import org.thoughtcrime.securesms.service.translation.exception.ResourcesOverLimitException;
import org.thoughtcrime.securesms.service.translation.exception.SystemLanguageNotSupportedException;
import org.thoughtcrime.securesms.service.translation.languageresponse.LanguagesResponse;
import org.thoughtcrime.securesms.service.translation.translateresponse.TranslationResponse;

import java.io.IOException;
import java.util.Locale;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Services that initialize on start
 * and provided translation services on runtime
 * <p>
 * Uses Azure Translation API services and
 * depends on BuildConfig configuration of
 * <p>
 * <p>
 * - TRANSLATION_API_ENDPOINT   : URL of the API to be used
 * <p>
 * - TRANSLATION_API_KEYS       : API keys of the API
 * <p>
 * - TRANSLATION_API_VERSION    : Version of the API to be used
 * <p>
 * - TRANSLATION_API_REGION     : Region used by the API
 *
 * @author Fauzan Lubis, Ali Nurdin
 */
public class TranslationService {

    private final String TAG = TranslationService.class.getSimpleName();

    private final String API_ENDPOINT = BuildConfig.TRANSLATION_API_ENDPOINT;
    private final String API_KEY = BuildConfig.TRANSLATION_API_KEYS;
    private final String API_VERSION = BuildConfig.TRANSLATION_API_VERSION;
    private final String API_REGION = BuildConfig.TRANSLATION_API_REGION;

    private final Context context;
    private final OkHttpClient client = new OkHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private BroadcastReceiver languageChangeReceiver = null;

    private String currentSystemLanguage;

    // Set to false first as to make service not available before initialization completes
    private Boolean isCurrentLanguageSupported = false;

    public TranslationService(Context context) {
        this.context = context;

        this.currentSystemLanguage = Locale.getDefault().getLanguage();
        this.isCurrentLanguageSupported = this.checkLanguageSupport(this.currentSystemLanguage);

        Log.i(TAG, "Current system language : " + currentSystemLanguage + ", Supported : " + isCurrentLanguageSupported);
        this.languageChangeReceiver = setupLanguageReceiver();
    }

    /**
     * Setups a receiver of locales changes
     * <p>
     * On any device language changes onReceive will be called
     *
     * @return BroadcastReceiver receiving on locales change action
     */
    private BroadcastReceiver setupLanguageReceiver() {
        if (this.languageChangeReceiver == null) {
            this.languageChangeReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    currentSystemLanguage = Locale.getDefault().getLanguage();
                    Log.i(TAG, "Locales changes, new language : " + currentSystemLanguage);

                    isCurrentLanguageSupported = checkLanguageSupport(currentSystemLanguage);
                }
            };

            context.registerReceiver(
                    this.languageChangeReceiver,
                    new IntentFilter(Intent.ACTION_LOCALE_CHANGED));

            Log.i(TAG, "Successfully registered language receiver");
        }

        return this.languageChangeReceiver;
    }

    private LanguagesResponse getSupportedLanguage() throws IOException {
        final String LANGUAGES_ENDPOINT = API_ENDPOINT + "/languages?"
                + "scope=translation"
                + "&api-version=" + API_VERSION;

        Request languageRequest = new Request.Builder()
                .url(LANGUAGES_ENDPOINT)
                .addHeader("Content-type", "application/json")
                .build();
        Response languageResponse = client.newCall(languageRequest).execute();

        return objectMapper.readValue(languageResponse.body().string(), LanguagesResponse.class);
    }

    private Boolean checkLanguageSupport(String language) {
        try {
            LanguagesResponse supportedLanguage = this.getSupportedLanguage();

            return supportedLanguage.translation.containsKey(language);
        } catch (IOException e) {
            Log.e(TAG, "Error Getting Supported Language, " + e);
        }

        return false;
    }

    private TranslationResponse[] getTranslation(String sourceText, String toLanguage) throws IOException, ResourcesOverLimitException, ResourcesNotAvailableException {
        final String TRANSLATION_ENDPOINT = API_ENDPOINT + "/translate?"
                + "to=" + toLanguage
                + "&api-version=" + API_VERSION;

        MediaType jsonMediaType = MediaType.parse("application/json");
        RequestBody translationBody = RequestBody.create(jsonMediaType,
                "[{\n\t\"Text\": \"" + sourceText + "\"\n}]");
        Request translationRequest = new Request.Builder()
                .url(TRANSLATION_ENDPOINT)
                .post(translationBody)
                .addHeader("Ocp-Apim-Subscription-Key", API_KEY)
                .addHeader("Ocp-Apim-Subscription-Region", API_REGION)
                .addHeader("Content-type", "application/json")
                .build();
        Response translationResponse = client.newCall(translationRequest).execute();

        int responseCode = translationResponse.code();
        if (BuildConfig.DEBUG && (responseCode == 401)) {
            throw new AssertionError("API Key is not valid! Check configuration");
        }

        if (responseCode == 429 || responseCode == 403) {
            throw new ResourcesOverLimitException("Translation Service Over Limit, Try Again Later");
        } else if (responseCode == 400 || responseCode == 500 || responseCode == 503) {
            throw new ResourcesNotAvailableException("Translation Service Is Currently Not Available");
        }

        return objectMapper.readValue(translationResponse.body().string(),
                TranslationResponse[].class);
    }

    public String translate(String sourceText) throws ResourcesOverLimitException, ResourcesNotAvailableException, SystemLanguageNotSupportedException {
        if (!this.isCurrentLanguageSupported)
            throw new SystemLanguageNotSupportedException("System Language Not Supported");

        try {
            TranslationResponse[] translationResponses = this.getTranslation(sourceText, this.currentSystemLanguage);

            return translationResponses[0].translations.get(0).text;
        } catch (IndexOutOfBoundsException e) {
            throw new ResourcesNotAvailableException("Translation Service Error Response");
        } catch (IOException e) {
            Log.e(TAG, "Cannot connect to the services " + e);
            throw new ResourcesNotAvailableException("Cannot Connect To Translation Service");
        }
    }

    public Boolean isAvailable() {
        return this.isCurrentLanguageSupported;
    }
}
