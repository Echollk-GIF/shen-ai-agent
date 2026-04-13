package com.chuanlong.shenaiagent.advisor;

import java.util.HashMap;
import java.util.Map;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;

/**
 * 自定义 Re2 Advisor
 * 可提高大型语言模型的推理能力
 */
public class ReReadingAdvisor implements CallAdvisor, StreamAdvisor {

    private static final String RE2_INPUT_QUERY = "re2_input_query";

    private ChatClientRequest before(ChatClientRequest request) {
        Prompt prompt = request.prompt();
        String original = userText(prompt);
        String re2UserText = original + "\nRead the question again: " + original;

        Map<String, Object> context = new HashMap<>(request.context());
        context.put(RE2_INPUT_QUERY, original);

        Prompt newPrompt = prompt.augmentUserMessage(re2UserText);

        return request.mutate()
                .prompt(newPrompt)
                .context(context)
                .build();
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        return chain.nextCall(this.before(request));
    }

    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest request, StreamAdvisorChain chain) {
        return chain.nextStream(this.before(request));
    }

    @Override
    public int getOrder() {
        return 0;
    }

    @Override
    public String getName() {
        return this.getClass().getSimpleName();
    }

    private static String userText(Prompt prompt) {
        UserMessage user = prompt.getUserMessage();
        if (user != null) {
            return user.getText();
        }
        var users = prompt.getUserMessages();
        if (!users.isEmpty()) {
            return users.get(users.size() - 1).getText();
        }
        return prompt.getContents();
    }
}
