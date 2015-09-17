package org.visallo.vertexium.model.search;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.vertexium.*;
import org.vertexium.inmemory.InMemoryGraph;
import org.visallo.core.model.properties.VisalloProperties;
import org.visallo.core.model.search.SearchProperties;
import org.visallo.core.model.user.AuthorizationRepository;
import org.visallo.core.model.user.UserRepository;
import org.visallo.core.user.User;
import org.visallo.web.clientapi.model.ClientApiSearch;
import org.visallo.web.clientapi.model.ClientApiSearchListResponse;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import static org.vertexium.util.IterableUtils.toList;

@RunWith(MockitoJUnitRunner.class)
public class VertexiumSearchRepositoryTest {
    private VertexiumSearchRepository searchRepository;
    private InMemoryGraph graph;
    private Authorizations authorizations;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuthorizationRepository authorizationRepository;

    @Mock
    private User user;
    private String userId;

    @Before
    public void setUp() {
        graph = InMemoryGraph.create();
        authorizations = graph.createAuthorizations(VertexiumSearchRepository.VISIBILITY_STRING, UserRepository.VISIBILITY_STRING);
        searchRepository = new VertexiumSearchRepository(graph, userRepository, authorizationRepository);

        userId = "USER123";
        when(user.getUserId()).thenReturn(userId);
        graph.addVertex(userId, new Visibility(""), authorizations);
        graph.flush();

        when(userRepository.getAuthorizations(eq(user), eq(VertexiumSearchRepository.VISIBILITY_STRING), eq(UserRepository.VISIBILITY_STRING))).thenReturn(authorizations);
    }

    @Test
    public void testSaveSearch() {
        String id = "123";
        String name = "search1";
        String url = "/vertex/search";
        JSONObject searchParameters = new JSONObject();
        searchParameters.put("key1", "value1");

        String foundId = searchRepository.saveSearch(user, id, name, url, searchParameters);
        assertEquals(id, foundId);

        Vertex userVertex = graph.getVertex(userId, authorizations);
        List<Edge> hasSavedSearchEdges = toList(userVertex.getEdges(Direction.OUT, SearchProperties.HAS_SAVED_SEARCH, authorizations));
        assertEquals(1, hasSavedSearchEdges.size());
        Vertex savedSearchVertex = hasSavedSearchEdges.get(0).getOtherVertex(userId, authorizations);
        assertEquals(SearchProperties.CONCEPT_TYPE_SAVED_SEARCH, VisalloProperties.CONCEPT_TYPE.getPropertyValue(savedSearchVertex, null));
        assertEquals(name, SearchProperties.NAME.getPropertyValue(savedSearchVertex, null));
        assertEquals(url, SearchProperties.URL.getPropertyValue(savedSearchVertex, null));
        assertEquals(searchParameters.toString(), SearchProperties.PARAMETERS.getPropertyValue(savedSearchVertex).toString());
    }

    @Test
    public void testGetSavedSearched() {
        String id = "SS123";
        String name = "saved search 123";
        String url = "/vertex/search";
        JSONObject parameters = new JSONObject();
        parameters.put("key1", "value1");
        VertexBuilder vb = graph.prepareVertex(id, new Visibility(""));
        VisalloProperties.CONCEPT_TYPE.setProperty(vb, SearchProperties.CONCEPT_TYPE_SAVED_SEARCH, new Visibility(""));
        SearchProperties.NAME.setProperty(vb, name, new Visibility(""));
        SearchProperties.URL.setProperty(vb, url, new Visibility(""));
        SearchProperties.PARAMETERS.setProperty(vb, parameters, new Visibility(""));
        vb.save(authorizations);

        graph.addEdge(userId, id, SearchProperties.HAS_SAVED_SEARCH, new Visibility(""), authorizations);

        graph.flush();

        ClientApiSearchListResponse response = searchRepository.getSavedSearches(user);
        assertEquals(1, response.searches.size());
        ClientApiSearch search = response.searches.get(0);
        assertEquals(id, search.id);
        assertEquals(name, search.name);
        assertEquals(url, search.url);
        assertEquals(parameters.keySet().size(), search.parameters.size());
        assertEquals(parameters.getString("key1"), search.parameters.get("key1"));
    }

    @Test
    public void testGetSavedSearch() {
        String id = "SS123";
        String name = "saved search 123";
        String url = "/vertex/search";
        JSONObject parameters = new JSONObject();
        parameters.put("key1", "value1");
        VertexBuilder vb = graph.prepareVertex(id, new Visibility(""));
        VisalloProperties.CONCEPT_TYPE.setProperty(vb, SearchProperties.CONCEPT_TYPE_SAVED_SEARCH, new Visibility(""));
        SearchProperties.NAME.setProperty(vb, name, new Visibility(""));
        SearchProperties.URL.setProperty(vb, url, new Visibility(""));
        SearchProperties.PARAMETERS.setProperty(vb, parameters, new Visibility(""));
        vb.save(authorizations);

        graph.addEdge(userId, id, SearchProperties.HAS_SAVED_SEARCH, new Visibility(""), authorizations);

        graph.flush();

        ClientApiSearch search = searchRepository.getSavedSearch(id, user);
        assertEquals(id, search.id);
        assertEquals(name, search.name);
        assertEquals(url, search.url);
        assertEquals(parameters.keySet().size(), search.parameters.size());
        assertEquals(parameters.getString("key1"), search.parameters.get("key1"));
    }

    @Test
    public void testDeleteSearch() {
        String id = "SS123";
        String name = "saved search 123";
        String url = "/vertex/search";
        JSONObject parameters = new JSONObject();
        parameters.put("key1", "value1");
        VertexBuilder vb = graph.prepareVertex(id, new Visibility(""));
        VisalloProperties.CONCEPT_TYPE.setProperty(vb, SearchProperties.CONCEPT_TYPE_SAVED_SEARCH, new Visibility(""));
        SearchProperties.NAME.setProperty(vb, name, new Visibility(""));
        SearchProperties.URL.setProperty(vb, url, new Visibility(""));
        SearchProperties.PARAMETERS.setProperty(vb, parameters, new Visibility(""));
        vb.save(authorizations);

        graph.addEdge(userId, id, SearchProperties.HAS_SAVED_SEARCH, new Visibility(""), authorizations);

        graph.flush();

        assertNotEquals(null, graph.getVertex(id, authorizations));
        searchRepository.deleteSearch(id, user);
        assertEquals(null, graph.getVertex(id, authorizations));
    }
}