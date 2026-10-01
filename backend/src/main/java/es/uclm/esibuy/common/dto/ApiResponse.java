package es.uclm.esibuy.common.dto;

public class ApiResponse<T> {

    private final boolean success;
    private final String message;
    private final T data;
    private final String correlationId;

    public ApiResponse(boolean success, String message, T data, String correlationId) {
        this.success = success;
        this.message = message;
        this.data = data;
        this.correlationId = correlationId;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public T getData() {
        return data;
    }

    public String getCorrelationId() {
        return correlationId;
    }
}