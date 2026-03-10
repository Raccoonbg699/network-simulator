package com.networksim.logic;

import com.networksim.model.Connection;
import com.networksim.model.Device;
import com.networksim.model.DeviceType;
import com.networksim.model.RouteEntry;
import javafx.collections.ObservableList;

import java.util.*;

public class RoutingManager {

    public static class RoutingStep {
        public final Device currentNode;
        public final Set<Device> visitedNodes;
        public final Map<Device, Integer> distances;
        public final Device updatedNeighbor; // The neighbor currently being relaxed

        public RoutingStep(Device currentNode, Set<Device> visitedNodes, Map<Device, Integer> distances, Device updatedNeighbor) {
            this.currentNode = currentNode;
            this.visitedNodes = new HashSet<>(visitedNodes);
            this.distances = new HashMap<>(distances);
            this.updatedNeighbor = updatedNeighbor;
        }
    }

    public static List<RoutingStep> getDijkstraSteps(Device source, List<Device> devices, List<Connection> connections) {
        List<RoutingStep> steps = new ArrayList<>();
        Map<Device, Integer> dist = new HashMap<>();
        Set<Device> visited = new HashSet<>();
        
        for (Device d : devices) dist.put(d, Integer.MAX_VALUE);
        dist.put(source, 0);

        PriorityQueue<Device> pq = new PriorityQueue<>(Comparator.comparingInt(dist::get));
        pq.add(source);

        while (!pq.isEmpty()) {
            Device u = pq.poll();
            if (visited.contains(u)) continue;
            
            // Step 1: Visiting node u
            steps.add(new RoutingStep(u, visited, dist, null));
            visited.add(u);

            for (Connection conn : connections) {
                if (!conn.isOperational()) continue;
                Device v = (conn.getFrom() == u) ? conn.getTo() : (conn.getTo() == u ? conn.getFrom() : null);

                if (v != null && !visited.contains(v)) {
                    int weight = conn.getWeight();
                    int newDist = dist.get(u) + weight;
                    if (newDist < dist.get(v)) {
                        dist.put(v, newDist);
                        pq.add(v);
                        // Step 2: Relaxing edge (u, v)
                        steps.add(new RoutingStep(u, visited, dist, v));
                    }
                }
            }
        }
        return steps;
    }

    public static void recalculateRoutes(List<Device> devices, List<Connection> connections) {
        // Clear all routing tables
        for (Device d : devices) {
            d.getRoutingTable().clear();
        }

        // Only run routing for Routers (and L3 Switches if we had them)
        for (Device source : devices) {
            if (isRouter(source)) {
                runDijkstra(source, devices, connections);
            }
        }
    }

    private static boolean isRouter(Device d) {
        return d.getType().name().startsWith("ROUTER") || d.getType().name().startsWith("SWITCH_3560");
    }

    private static void runDijkstra(Device source, List<Device> allDevices, List<Connection> allConnections) {
        Map<Device, Integer> dist = new HashMap<>();
        Map<Device, Device> prev = new HashMap<>();
        Map<Device, Connection> edgeUsed = new HashMap<>(); // To know which interface/link
        Set<Device> visited = new HashSet<>();
        
        for (Device d : allDevices) {
            dist.put(d, Integer.MAX_VALUE);
        }
        dist.put(source, 0);

        PriorityQueue<Device> pq = new PriorityQueue<>(Comparator.comparingInt(dist::get));
        pq.add(source);

        while (!pq.isEmpty()) {
            Device u = pq.poll();
            if (visited.contains(u)) continue;
            visited.add(u);

            // Find neighbors
            for (Connection conn : allConnections) {
                if (!conn.isOperational()) continue; // Skip broken links
                
                Device v = null;
                if (conn.getFrom() == u) v = conn.getTo();
                else if (conn.getTo() == u) v = conn.getFrom();

                if (v != null && !visited.contains(v)) {
                    int weight = conn.getWeight();
                    int newDist = dist.get(u) + weight;
                    if (newDist < dist.get(v)) {
                        dist.put(v, newDist);
                        prev.put(v, u);
                        edgeUsed.put(v, conn); // The edge that leads TO v from u
                        pq.add(v);
                    }
                }
            }
        }

        // Build Routing Table from 'prev' map
        for (Device dest : allDevices) {
            if (dest == source) continue;
            if (dist.get(dest) == Integer.MAX_VALUE) continue; // Unreachable

            // We need to find the "Next Hop" from source to dest.
            // Backtrack from dest until we find the node directly connected to source.
            Device curr = dest;
            Device nextHop = null;
            Connection egressConn = null;

            // If direct connection
            if (prev.get(dest) == source) {
                nextHop = dest; // Directly connected
                egressConn = edgeUsed.get(dest);
            } else {
                // Backtrack
                while (prev.get(curr) != null && prev.get(curr) != source) {
                    curr = prev.get(curr);
                }
                nextHop = curr;
                egressConn = edgeUsed.get(curr);
            }

            if (nextHop != null) {
                String nextHopStr = nextHop.getName(); // Or IP if we had valid IPs
                String type = "Remote";
                if (nextHop == dest) type = "Direct";
                
                // For simplicity, Interface Name is just "Link-" + ID or similar
                String iface = "Link"; // In real sim, ports have names

                source.getRoutingTable().add(new RouteEntry(
                    dest.getName(),
                    nextHopStr,
                    dist.get(dest),
                    type
                ));
            }
        }
    }
    
    // Helper to find shortest path for visualization
    public static List<Connection> getShortestPath(Device start, Device end, List<Connection> connections) {
        List<Connection> path = new ArrayList<>();
        // Re-run Dijkstra specifically or cache results. 
        // For simplicity/robustness, let's just run a quick search here or reuse logic.
        // Copy-paste Dijkstra logic but optimized for single target return path.
        
        Map<Device, Integer> dist = new HashMap<>();
        Map<Device, Device> prev = new HashMap<>();
        Map<Device, Connection> edgeTo = new HashMap<>();
        Set<Device> visited = new HashSet<>();
        
        List<Device> allDevices = new ArrayList<>();
        // Collect all devices from connections
        Set<Device> devSet = new HashSet<>();
        for(Connection c : connections) { devSet.add(c.getFrom()); devSet.add(c.getTo()); }
        allDevices.addAll(devSet);

        for (Device d : allDevices) dist.put(d, Integer.MAX_VALUE);
        dist.put(start, 0);
        
        PriorityQueue<Device> pq = new PriorityQueue<>(Comparator.comparingInt(dist::get));
        pq.add(start);
        
        while(!pq.isEmpty()){
            Device u = pq.poll();
            if(u == end) break; // Found
            if(visited.contains(u)) continue;
            visited.add(u);
            
            for(Connection c : connections) {
                if (!c.isOperational()) continue; // Skip broken links
                
                Device v = null;
                if(c.getFrom()==u) v = c.getTo();
                else if(c.getTo()==u) v = c.getFrom();
                
                if(v!=null && !visited.contains(v)) {
                    int alt = dist.get(u) + c.getWeight();
                    if(alt < dist.get(v)) {
                        dist.put(v, alt);
                        prev.put(v, u);
                        edgeTo.put(v, c);
                        pq.add(v);
                    }
                }
            }
        }
        
        if (dist.get(end) == Integer.MAX_VALUE) return path; // No path
        
        Device curr = end;
        while(curr != start && curr != null) {
            Connection c = edgeTo.get(curr);
            if(c != null) {
                path.add(c);
                if(c.getFrom() == curr) curr = c.getTo();
                else curr = c.getFrom();
            } else {
                break;
            }
        }
        Collections.reverse(path);
        return path;
    }
}
