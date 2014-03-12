package com.wart.magister.util;

public class ROException extends Exception {

    private static final long serialVersionUID = 4771589802826686886L;

    public ROException() {
        super("ROException");
    }

    public ROException(String paramString) {
        super(paramString);
    }

    public ROException(String paramString, Throwable paramThrowable) {
        super(paramString, paramThrowable);
    }

    public ROException(Throwable paramThrowable) {
        super(paramThrowable);
    }
}