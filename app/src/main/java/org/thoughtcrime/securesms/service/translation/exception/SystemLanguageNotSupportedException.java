package org.thoughtcrime.securesms.service.translation.exception;

public class SystemLanguageNotSupportedException  extends Exception {
    public SystemLanguageNotSupportedException(String errorMessage) {
        super(errorMessage);
    }
}
