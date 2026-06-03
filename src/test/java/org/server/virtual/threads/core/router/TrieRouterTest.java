package org.server.virtual.threads.core.router;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.server.virtual.threads.core.handler.RouteHandler;
import org.server.virtual.threads.core.model.HttpMethod;
import org.server.virtual.threads.core.model.HttpRequest;
import org.server.virtual.threads.core.model.HttpResponse;
import org.server.virtual.threads.core.model.HttpStatus;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.server.virtual.threads.core.constants.HttpConstants.EMPTY;

@DisplayName("TrieRouter")
class TrieRouterTest {

    private TrieRouter router;

    @BeforeEach
    void setUp() {
        router = new TrieRouter();
    }

    @Test
    @DisplayName("Should register and find GET handler")
    void shouldRegisterAndFindGetHandler() {
        RouteHandler handler = (_, res) -> res.getText(HttpStatus.OK.getReason());
        router.get("/test", handler);

        var pathParams = new HashMap<String, String>();
        var found = router.findHandler(HttpMethod.GET, "/test", pathParams);

        assertThat(found).isSameAs(handler);
        assertThat(pathParams).isEmpty();
    }

    @Test
    @DisplayName("Should return null for non-existent route")
    void shouldReturnNullForNonExistentRoute() {
        var pathParams = new HashMap<String, String>();
        var found = router.findHandler(HttpMethod.GET, "/unknown", pathParams);
        assertThat(found).isNull();
    }

    @Test
    @DisplayName("Should handle path parameters :id")
    void shouldExtractPathParameter() {
        RouteHandler handler = (req, res) -> res.getText(req.getParam("id"));
        router.get("/users/:id", handler);

        var pathParams = new HashMap<String, String>();
        var found = router.findHandler(HttpMethod.GET, "/users/123", pathParams);

        assertThat(found).isSameAs(handler);
        assertThat(pathParams).containsEntry("id", "123");
    }

    @Test
    @DisplayName("Should handle multiple path parameters")
    void shouldExtractMultiplePathParameters() {
        RouteHandler handler = (_, res) -> res.getText(HttpStatus.OK.getReason());
        router.get("/posts/:postId/comments/:commentId", handler);

        var pathParams = new HashMap<String, String>();
        var found = router.findHandler(HttpMethod.GET, "/posts/42/comments/7", pathParams);

        assertThat(found).isSameAs(handler);
        assertThat(pathParams).containsEntry("postId", "42").containsEntry("commentId", "7");
    }

    @Test
    @DisplayName("Should ignore static segments before parameter")
    void shouldMatchStaticThenParameter() {
        RouteHandler handler = (_, res) -> res.getText(HttpStatus.OK.getReason());
        router.get("/api/users/:id", handler);

        var pathParams = new HashMap<String, String>();
        var found = router.findHandler(HttpMethod.GET, "/api/users/99", pathParams);

        assertThat(found).isSameAs(handler);
        assertThat(pathParams).containsEntry("id", "99");
    }

    @Test
    @DisplayName("Should distinguish different HTTP methods")
    void shouldDistinguishMethods() {
        RouteHandler getHandler = (_, res) -> res.getText(HttpMethod.GET.name());
        RouteHandler postHandler = (_, res) -> res.getText(HttpMethod.POST.name());
        router.get("/resource", getHandler);
        router.post("/resource", postHandler);

        var pathParams = new HashMap<String, String>();
        var foundGet = router.findHandler(HttpMethod.GET, "/resource", pathParams);
        var foundPost = router.findHandler(HttpMethod.POST, "/resource", pathParams);

        assertThat(foundGet).isSameAs(getHandler);
        assertThat(foundPost).isSameAs(postHandler);
    }

    @Test
    @DisplayName("Should return 404 when no handler found via handle method")
    void shouldReturn404WhenNoHandler() throws Exception {
        var request = new HttpRequest(HttpMethod.GET, "/missing", Map.of(), Map.of(), EMPTY);
        var response = new HttpResponse();

        router.handle(request, response);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.bodyAsString()).contains("404: Not Found");
    }

    @Test
    @DisplayName("Should execute handler via handle method with path parameters")
    void shouldExecuteHandlerWithPathParams() throws Exception {
        router.get("/users/:id", (req, res) -> res.getText("User " + req.getParam("id")));

        var request = new HttpRequest(HttpMethod.GET, "/users/42", Map.of(), Map.of(),EMPTY);
        var response = new HttpResponse();

        router.handle(request, response);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK);
        assertThat(response.bodyAsString()).isEqualTo("User 42");
    }

    @ParameterizedTest
    @DisplayName("Should match routes with different methods and paths")
    @CsvSource({
            "GET, /hello, true",
            "POST, /hello, false",
            "GET, /unknown, false"
    })
    void shouldMatchRoutesCorrectly(String method, String path, boolean shouldMatch) {
        router.get("/hello", (_, res) -> res.getText("Hello"));

        var httpMethod = HttpMethod.fromString(method);
        var pathParams = new HashMap<String, String>();
        var found = router.findHandler(httpMethod, path, pathParams);

        if (shouldMatch) {
            assertThat(found).isNotNull();
        } else {
            assertThat(found).isNull();
        }
    }

    @Test
    @DisplayName("Should register and find PUT handler")
    void shouldRegisterAndFindPutHandler() {
        RouteHandler handler = (_, res) -> res.getText("PUT OK");
        router.put("/resource", handler);

        var pathParams = new HashMap<String, String>();
        var found = router.findHandler(HttpMethod.PUT, "/resource", pathParams);

        assertThat(found).isSameAs(handler);
    }

    @Test
    @DisplayName("Should register and find DELETE handler")
    void shouldRegisterAndFindDeleteHandler() {
        RouteHandler handler = (_, res) -> res.getText("DELETE OK");
        router.delete("/resource", handler);

        var pathParams = new HashMap<String, String>();
        var found = router.findHandler(HttpMethod.DELETE, "/resource", pathParams);

        assertThat(found).isSameAs(handler);
    }

    @Test
    @DisplayName("Should execute PUT handler via handle method")
    void shouldExecutePutHandler() throws Exception {
        router.put("/resource", (_, res) -> res.getText("PUT called"));

        var request = new HttpRequest(HttpMethod.PUT, "/resource", Map.of(), Map.of(), "");
        var response = new HttpResponse();

        router.handle(request, response);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK);
        assertThat(response.bodyAsString()).isEqualTo("PUT called");
    }

    @Test
    @DisplayName("Should execute DELETE handler via handle method")
    void shouldExecuteDeleteHandler() throws Exception {
        router.delete("/resource", (_, res) -> res.getText("DELETE called"));

        var request = new HttpRequest(HttpMethod.DELETE, "/resource", Map.of(), Map.of(), "");
        var response = new HttpResponse();

        router.handle(request, response);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK);
        assertThat(response.bodyAsString()).isEqualTo("DELETE called");
    }
}