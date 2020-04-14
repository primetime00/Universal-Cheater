package message;

import response.Response;

import java.util.concurrent.CompletableFuture;

public class Message {
    private MessageData data;
    private CompletableFuture<Response> response;

    public Message(MessageData data) {
        this(data, null);
    }
    public Message(MessageData data, CompletableFuture<Response> response) {
        this.data = data;
        this.response = response;
    }

    public MessageData getData() {
        return data;
    }

    public CompletableFuture<Response> getResponse() {
        return response;
    }

    public void setResponse(CompletableFuture<Response> response) {
        this.response = response;
    }
}
