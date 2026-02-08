package kg.freelance.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import kg.freelance.dto.request.PortfolioRequest;
import kg.freelance.dto.response.PortfolioResponse;
import kg.freelance.entity.enums.UserRole;
import kg.freelance.exception.ForbiddenException;
import kg.freelance.exception.GlobalExceptionHandler;
import kg.freelance.exception.ResourceNotFoundException;
import kg.freelance.security.UserPrincipal;
import kg.freelance.service.PortfolioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PortfolioController Tests")
class PortfolioControllerTest {

    @Mock
    private PortfolioService portfolioService;

    @InjectMocks
    private PortfolioController portfolioController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private UserPrincipal userPrincipal;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(portfolioController)
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();

        userPrincipal = UserPrincipal.builder()
                .id(1L).email("test@example.com").fullName("Test User")
                .role(UserRole.USER).active(true).build();

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userPrincipal, null, userPrincipal.getAuthorities()));
    }

    private PortfolioResponse buildPortfolioResponse() {
        return PortfolioResponse.builder()
                .id(1L).title("My Project").description("Description")
                .images(List.of("/img/1.jpg")).sortOrder(0)
                .createdAt(LocalDateTime.now()).build();
    }

    @Nested
    @DisplayName("GET /api/v1/portfolio/me")
    class GetMyPortfolioTests {

        @Test
        @DisplayName("Should return my portfolio")
        void shouldReturnMyPortfolio() throws Exception {
            when(portfolioService.getMyPortfolio(1L)).thenReturn(List.of(buildPortfolioResponse()));

            mockMvc.perform(get("/api/v1/portfolio/me"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].title").value("My Project"));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/portfolio/me")
    class AddPortfolioItemTests {

        @Test
        @DisplayName("Should add portfolio item")
        void shouldAddPortfolioItem() throws Exception {
            PortfolioRequest request = new PortfolioRequest();
            request.setTitle("New Project");
            request.setDescription("Desc");

            when(portfolioService.addPortfolioItem(eq(1L), any())).thenReturn(buildPortfolioResponse());

            mockMvc.perform(post("/api/v1/portfolio/me")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.title").value("My Project"));
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/portfolio/me/{id}")
    class UpdatePortfolioItemTests {

        @Test
        @DisplayName("Should update portfolio item")
        void shouldUpdateItem() throws Exception {
            PortfolioRequest request = new PortfolioRequest();
            request.setTitle("Updated");

            when(portfolioService.updatePortfolioItem(eq(1L), eq(1L), any()))
                    .thenReturn(buildPortfolioResponse());

            mockMvc.perform(put("/api/v1/portfolio/me/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Should return 403 when not owner")
        void shouldReturn403WhenNotOwner() throws Exception {
            PortfolioRequest request = new PortfolioRequest();
            request.setTitle("Hack");

            when(portfolioService.updatePortfolioItem(eq(1L), eq(1L), any()))
                    .thenThrow(new ForbiddenException("You can only update your own portfolio items"));

            mockMvc.perform(put("/api/v1/portfolio/me/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/portfolio/me/{id}")
    class DeletePortfolioItemTests {

        @Test
        @DisplayName("Should delete portfolio item")
        void shouldDeleteItem() throws Exception {
            doNothing().when(portfolioService).deletePortfolioItem(1L, 1L);

            mockMvc.perform(delete("/api/v1/portfolio/me/1"))
                    .andExpect(status().isNoContent());
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/portfolio/me/reorder")
    class ReorderPortfolioTests {

        @Test
        @DisplayName("Should reorder portfolio")
        void shouldReorderPortfolio() throws Exception {
            doNothing().when(portfolioService).reorderPortfolio(eq(1L), anyList());

            mockMvc.perform(put("/api/v1/portfolio/me/reorder")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("[3, 1, 2]"))
                    .andExpect(status().isOk());

            verify(portfolioService).reorderPortfolio(eq(1L), eq(List.of(3L, 1L, 2L)));
        }
    }
}
