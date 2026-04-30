package guard.ics.backend.common;

import guard.ics.backend.common.exception.BadRequestException;
import guard.ics.backend.common.exception.BusinessException;
import guard.ics.backend.common.exception.ErrorCode;
import guard.ics.backend.common.exception.GlobalExceptionHandler;
import guard.ics.backend.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void shouldHandleBusinessException() {
        var ex = new ResourceNotFoundException("Alert", 42L);
        var response = handler.handleBusiness(ex, new MockHttpServletRequest());

        assertThat(response.getStatusCode().value()).isEqualTo(404);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo(404);
        assertThat(response.getBody().msg()).contains("Alert");
    }

    @Test
    void shouldHandleBadRequestException() {
        var ex = new BadRequestException("Invalid input");
        var response = handler.handleBusiness(ex, new MockHttpServletRequest());

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody().msg()).isEqualTo("Invalid input");
    }

    @Test
    void shouldReturnErrorCodeHttpStatus() {
        var ex = new BusinessException(ErrorCode.SDN_CONTROLLER_UNAVAILABLE);
        var response = handler.handleBusiness(ex, new MockHttpServletRequest());

        assertThat(response.getStatusCode().value()).isEqualTo(503);
    }
}
