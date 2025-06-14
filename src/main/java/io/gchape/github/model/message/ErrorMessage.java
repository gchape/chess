package io.gchape.github.model.message;

public class ErrorMessage extends Message {
    private String type = "ERROR";
    private String errorCode;
    private String errorMessage;

    public ErrorMessage() {}

    public ErrorMessage(String errorCode, String errorMessage) {
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }

    @Override
    public String getType() { return "ERROR"; }

    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
}