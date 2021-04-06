package org.thoughtcrime.securesms.service.translation.exception;

public class ResourcesOverLimitException extends Exception {
    public ResourcesOverLimitException(String errorMessage) {
        super(errorMessage);
    }
}
