package guard.ics.backend.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class WebSocketConfigTest {

    @Captor private ArgumentCaptor<ChannelInterceptor> captor;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private ChannelInterceptor captureInterceptor() {
        WebSocketConfig config = new WebSocketConfig();
        ChannelRegistration registration = mock(ChannelRegistration.class);
        config.configureClientInboundChannel(registration);
        verify(registration).interceptors(captor.capture());
        return captor.getValue();
    }

    @Test
    void shouldAllowSubscribeToNonAlertTopic() {
        ChannelInterceptor interceptor = captureInterceptor();

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination("/topic/other");
        Message<byte[]> message = MessageBuilder.createMessage(new byte[]{}, accessor.getMessageHeaders());
        MessageChannel channel = mock(MessageChannel.class);

        Message<?> result = interceptor.preSend(message, channel);
        assertThat(result).isSameAs(message);
    }

    @Test
    void shouldAllowSubscribeToAlertTopicWithPermission() {
        ChannelInterceptor interceptor = captureInterceptor();

        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("admin", "pass",
                        List.of(new SimpleGrantedAuthority("alerts:subscribe"))));

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination("/topic/alerts/device1");
        Message<byte[]> message = MessageBuilder.createMessage(new byte[]{}, accessor.getMessageHeaders());
        MessageChannel channel = mock(MessageChannel.class);

        Message<?> result = interceptor.preSend(message, channel);
        assertThat(result).isSameAs(message);
    }

    @Test
    void shouldDenySubscribeToAlertTopicWithoutPermission() {
        ChannelInterceptor interceptor = captureInterceptor();

        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("user", "pass", List.of()));

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination("/topic/alerts/device1");
        Message<byte[]> message = MessageBuilder.createMessage(new byte[]{}, accessor.getMessageHeaders());
        MessageChannel channel = mock(MessageChannel.class);

        assertThatThrownBy(() -> interceptor.preSend(message, channel))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("alerts:subscribe");
    }

    @Test
    void shouldDenySubscribeToAlertTopicWhenUnauthenticated() {
        ChannelInterceptor interceptor = captureInterceptor();

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination("/topic/alerts/device1");
        Message<byte[]> message = MessageBuilder.createMessage(new byte[]{}, accessor.getMessageHeaders());
        MessageChannel channel = mock(MessageChannel.class);

        assertThatThrownBy(() -> interceptor.preSend(message, channel))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("alerts:subscribe");
    }

    @Test
    void shouldAllowConnectCommand() {
        ChannelInterceptor interceptor = captureInterceptor();

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        Message<byte[]> message = MessageBuilder.createMessage(new byte[]{}, accessor.getMessageHeaders());
        MessageChannel channel = mock(MessageChannel.class);

        Message<?> result = interceptor.preSend(message, channel);
        assertThat(result).isSameAs(message);
    }
}
