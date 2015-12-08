package org.visallo.it;

import org.visallo.core.model.properties.VisalloProperties;
import org.visallo.web.clientapi.VisalloApi;
import org.visallo.web.clientapi.codegen.ApiException;
import org.visallo.web.clientapi.model.*;
import org.visallo.web.clientapi.util.ObjectMapperFactory;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;

import static org.junit.Assert.*;

public class WorkspaceSandboxingIntegrationTest extends TestBase {
    private String susanFengVertexId;
    private String artifactVertexId;

    @Test
    public void testWorkspaceSandboxing() throws ApiException, IOException {
        setupTest();
        addPropertyWithPublicChangeSandboxStatus();
        addPropertyWithPrivateSandboxStatus();
        assertAllPropertiesArePublic();
        addEdge();
    }

    private void setupTest() throws ApiException, IOException {
        VisalloApi visalloApi = login(USERNAME_TEST_USER_1);
        addUserAuths(visalloApi, USERNAME_TEST_USER_1, "A");
        addUserAuths(visalloApi, USERNAME_TEST_USER_1, "B");
        addUserAuths(visalloApi, USERNAME_TEST_USER_1, "C");

        ClientApiArtifactImportResponse artifact = visalloApi.getVertexApi().importFile("", "test.txt", new ByteArrayInputStream("Susan Feng knows Joe Ferner.".getBytes()));
        assertEquals(1, artifact.getVertexIds().size());
        assertNotNull(artifact.getVertexIds().get(0));
        artifactVertexId = artifact.getVertexIds().get(0);

        visalloTestCluster.processGraphPropertyQueue();

        ClientApiElement susanFengVertex = visalloApi.getVertexApi().create(TestOntology.CONCEPT_PERSON, "", "justification");
        susanFengVertexId = susanFengVertex.getId();
        visalloApi.getVertexApi().setProperty(susanFengVertexId, TEST_MULTI_VALUE_KEY, VisalloProperties.TITLE.getPropertyName(), "Susan Feng", "", "test");

        visalloTestCluster.processGraphPropertyQueue();
        visalloApi.getVertexApi().setProperty(susanFengVertexId, "key1", TestOntology.PROPERTY_NAME, "Joe", "A", "test", null, null);
        visalloApi.getVertexApi().setProperty(susanFengVertexId, "key2", TestOntology.PROPERTY_NAME, "Bob", "A", "test", null, null);
        visalloApi.getVertexApi().setProperty(susanFengVertexId, "key2", TestOntology.PROPERTY_NAME, "Sam", "B", "test", null, null);

        assertPublishAll(visalloApi, 17);

        susanFengVertex = visalloApi.getVertexApi().getByVertexId(susanFengVertexId);
        assertEquals(SandboxStatus.PUBLIC, susanFengVertex.getSandboxStatus());
        List<ClientApiProperty> properties = susanFengVertex.getProperties();

        assertHasProperty(properties, "key1", TestOntology.PROPERTY_NAME, "Joe");
        List<ClientApiProperty> key2Properties = getProperties(properties, "key2", TestOntology.PROPERTY_NAME);
        assertEquals(2, key2Properties.size());
        assertEquals("Bob", key2Properties.get(0).getValue());
        assertEquals("Sam", key2Properties.get(1).getValue());
        assertEquals(SandboxStatus.PUBLIC, key2Properties.get(0).getSandboxStatus());
        assertEquals(SandboxStatus.PUBLIC, key2Properties.get(1).getSandboxStatus());

        for (ClientApiProperty property : properties) {
            assertEquals(SandboxStatus.PUBLIC, property.getSandboxStatus());
        }

        visalloApi.logout();
    }

    private void addPropertyWithPublicChangeSandboxStatus() throws ApiException, IOException {
        VisalloApi visalloApi = login(USERNAME_TEST_USER_1);
        ClientApiElement susanFengVertex = visalloApi.getVertexApi().setProperty(susanFengVertexId, "key1", TestOntology.PROPERTY_NAME, "Tom", "A", "test", null, null);
        List<ClientApiProperty> key1Properties = getProperties(susanFengVertex.getProperties(), "key1", TestOntology.PROPERTY_NAME);
        assertEquals(2, key1Properties.size());
        assertEquals("Tom", key1Properties.get(0).getValue());
        assertEquals(SandboxStatus.PUBLIC_CHANGED, key1Properties.get(0).getSandboxStatus());
        assertEquals("Joe", key1Properties.get(1).getValue());
        assertEquals(SandboxStatus.PUBLIC, key1Properties.get(1).getSandboxStatus());

        List<ClientApiProperty> key2Properties = getProperties(susanFengVertex.getProperties(), "key2", TestOntology.PROPERTY_NAME);
        assertEquals("Bob", key2Properties.get(0).getValue());
        assertEquals(SandboxStatus.PUBLIC, key2Properties.get(0).getSandboxStatus());
        assertEquals("Sam", key2Properties.get(1).getValue());
        assertEquals(SandboxStatus.PUBLIC, key2Properties.get(1).getSandboxStatus());
        visalloApi.logout();
    }

    private void addPropertyWithPrivateSandboxStatus() throws ApiException, IOException {
        VisalloApi visalloApi = login(USERNAME_TEST_USER_1);
        visalloApi.getVertexApi().setProperty(susanFengVertexId, "key2", TestOntology.PROPERTY_NAME, "Dave", "C", "test", null, null);
        ClientApiElement susanFengVertex = visalloApi.getVertexApi().setProperty(susanFengVertexId, "key3", TestOntology.PROPERTY_NAME, "Susan", "", "test", null, null);

        List<ClientApiProperty> key1Properties = getProperties(susanFengVertex.getProperties(), "key1", TestOntology.PROPERTY_NAME);
        assertEquals(2, key1Properties.size());
        assertEquals("Tom", key1Properties.get(0).getValue());
        assertEquals(SandboxStatus.PUBLIC_CHANGED, key1Properties.get(0).getSandboxStatus());
        assertEquals(SandboxStatus.PUBLIC, key1Properties.get(1).getSandboxStatus());

        List<ClientApiProperty> key2Properties = getProperties(susanFengVertex.getProperties(), "key2", TestOntology.PROPERTY_NAME);
        assertEquals(3, key2Properties.size());
        assertEquals("Bob", key2Properties.get(0).getValue());
        assertEquals(SandboxStatus.PUBLIC, key2Properties.get(0).getSandboxStatus());
        assertEquals("Sam", key2Properties.get(1).getValue());
        assertEquals(SandboxStatus.PUBLIC, key2Properties.get(1).getSandboxStatus());
        assertEquals("Dave", key2Properties.get(2).getValue());
        assertEquals(SandboxStatus.PRIVATE, key2Properties.get(2).getSandboxStatus());

        List<ClientApiProperty> key3Properties = getProperties(susanFengVertex.getProperties(), "key3", TestOntology.PROPERTY_NAME);
        assertEquals(1, key3Properties.size());
        assertEquals("Susan", key3Properties.get(0).getValue());
        assertEquals(SandboxStatus.PRIVATE, key3Properties.get(0).getSandboxStatus());
        visalloApi.logout();
    }

    private void assertAllPropertiesArePublic() throws ApiException, IOException {
        VisalloApi visalloApi = login(USERNAME_TEST_USER_1);
        assertPublishAll(visalloApi, 3);
        ClientApiElement susanFengVertex = visalloApi.getVertexApi().getByVertexId(susanFengVertexId);
        List<ClientApiProperty> properties = susanFengVertex.getProperties();
        assertEquals(8, properties.size());

        VisibilityJson visibilityJsonA = new VisibilityJson();
        visibilityJsonA.setSource("A");

        VisibilityJson visibilityJsonB = new VisibilityJson();
        visibilityJsonB.setSource("B");

        VisibilityJson visibilityJsonC = new VisibilityJson();
        visibilityJsonC.setSource("C");

        VisibilityJson visibilityJsonNoSource = new VisibilityJson();

        List<ClientApiProperty> key1Properties = getProperties(susanFengVertex.getProperties(), "key1", TestOntology.PROPERTY_NAME);
        assertEquals(1, key1Properties.size());
        assertHasProperty(key1Properties, "key1", TestOntology.PROPERTY_NAME, "Tom");
        ClientApiProperty key1Property = key1Properties.get(0);
        checkVisibility(key1Property, visibilityJsonA);

        List<ClientApiProperty> key2Properties = getProperties(susanFengVertex.getProperties(), "key2", TestOntology.PROPERTY_NAME);
        assertEquals(3, key2Properties.size());
        assertEquals("Bob", key2Properties.get(0).getValue());
        assertEquals("Sam", key2Properties.get(1).getValue());
        assertEquals("Dave", key2Properties.get(2).getValue());
        checkVisibility(key2Properties.get(0), visibilityJsonA);
        checkVisibility(key2Properties.get(1), visibilityJsonB);
        checkVisibility(key2Properties.get(2), visibilityJsonC);

        List<ClientApiProperty> key3Properties = getProperties(susanFengVertex.getProperties(), "key3", TestOntology.PROPERTY_NAME);
        assertEquals(1, key3Properties.size());
        assertHasProperty(key3Properties, "key3", TestOntology.PROPERTY_NAME, "Susan");
        ClientApiProperty key3Property = key3Properties.get(0);
        checkVisibility(key3Property, visibilityJsonNoSource);
        visalloApi.logout();
    }

    private void addEdge() throws ApiException {
        VisalloApi visalloApi = login(USERNAME_TEST_USER_1);

        ClientApiEdgeWithVertexData edge = visalloApi.getEdgeApi().create(artifactVertexId, susanFengVertexId, "http://visallo.org/test#artifactHasEntity", "", null);
        visalloApi.getEdgeApi().setProperty(edge.getId(), "key1", "http://visallo.org/test#firstName", "edge property value", "", "");

        assertPublishAll(visalloApi, 2);

        visalloApi.logout();

        visalloApi = login(USERNAME_TEST_USER_2);

        edge = visalloApi.getEdgeApi().getByEdgeId(edge.getId());
        boolean foundFirstNameEdgeProperty = false;
        for (ClientApiProperty edgeProperty : edge.getProperties()) {
            if (edgeProperty.getKey().equals("key1") && edgeProperty.getName().equals("http://visallo.org/test#firstName")) {
                assertEquals("edge property value", edgeProperty.getValue().toString());
                foundFirstNameEdgeProperty = true;
            }
        }
        assertTrue(foundFirstNameEdgeProperty);

        visalloApi.logout();
    }

    private void checkVisibility(ClientApiProperty property, VisibilityJson expectedVisibilityJson) {
        try {
            String visibilityJson = ObjectMapperFactory.getInstance().writeValueAsString(property.getMetadata().get(VisalloProperties.VISIBILITY_JSON.getPropertyName()));
            assertEquals(expectedVisibilityJson.toString(), visibilityJson);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}