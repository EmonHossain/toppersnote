package com.sharenote.ai;

public interface AiModelClient {

    boolean supports(AiProvider provider);

    String complete(AiModelInvocation invocation);
}
