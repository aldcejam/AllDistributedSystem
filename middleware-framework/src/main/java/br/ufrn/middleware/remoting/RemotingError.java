package br.ufrn.middleware.remoting;

public record RemotingError(String message, int statusCode) {
}
