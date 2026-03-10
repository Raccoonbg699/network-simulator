package com.networksim.logic;

import com.networksim.model.Connection;
import com.networksim.model.Device;
import com.networksim.model.DeviceType;
import com.networksim.model.RouteEntry;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RoutingManagerTest {

    @Test
    void testRecalculateRoutes_DijkstraShortestPath() {
        // Arrange
        Device r1 = new Device("1", DeviceType.ROUTER_1941, "R1", 0, 0);
        Device r2 = new Device("2", DeviceType.ROUTER_1941, "R2", 0, 0);
        Device r3 = new Device("3", DeviceType.ROUTER_1941, "R3", 0, 0);

        List<Device> devices = new ArrayList<>(List.of(r1, r2, r3));

        // R1 - R2 (Cost 10)
        // R2 - R3 (Cost 10)
        // R1 - R3 (Cost 50)
        Connection c1 = new Connection("c1", r1, r2, "Copper"); c1.setWeight(10);
        Connection c2 = new Connection("c2", r2, r3, "Copper"); c2.setWeight(10);
        Connection c3 = new Connection("c3", r1, r3, "Copper"); c3.setWeight(50);

        List<Connection> connections = new ArrayList<>(List.of(c1, c2, c3));

        // Act
        RoutingManager.recalculateRoutes(devices, connections);

        // Assert
        // R1 should route to R3 via R2 (cost 20) instead of direct (cost 50)
        boolean foundOptimalRoute = false;
        for (RouteEntry entry : r1.getRoutingTable()) {
            if (entry.getDestination().equals("R3")) {
                assertEquals("R2", entry.getNextHop(), "Next hop should be R2 for shortest path.");
                assertEquals(20, entry.getMetric(), "Metric should be sum of R1-R2 and R2-R3 (10+10=20).");
                foundOptimalRoute = true;
            }
        }
        assertTrue(foundOptimalRoute, "Route to R3 was not found in R1's routing table.");
    }

    @Test
    void testGetShortestPath() {
        // Arrange
        Device a = new Device("A", DeviceType.PC, "PC-A", 0, 0);
        Device b = new Device("B", DeviceType.SWITCH_2960, "SW-B", 0, 0);
        Device c = new Device("C", DeviceType.ROUTER_1941, "R-C", 0, 0);
        Device d = new Device("D", DeviceType.PC, "PC-D", 0, 0);

        Connection ab = new Connection("ab", a, b, "Cable"); ab.setWeight(1);
        Connection bc = new Connection("bc", b, c, "Cable"); bc.setWeight(1);
        Connection cd = new Connection("cd", c, d, "Cable"); cd.setWeight(1);

        List<Connection> connections = new ArrayList<>(List.of(ab, bc, cd));

        // Act
        List<Connection> path = RoutingManager.getShortestPath(a, d, connections);

        // Assert
        assertEquals(3, path.size(), "Path from A to D should have 3 hops.");
        assertEquals(ab, path.get(0));
        assertEquals(bc, path.get(1));
        assertEquals(cd, path.get(2));
    }
}
