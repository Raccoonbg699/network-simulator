package com.networksim.logic;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.networksim.model.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class PersistenceManager {
    private static final String SAVE_FILE = ".network_cache.json";
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public static class ProjectData {
        public List<DeviceData> devices = new ArrayList<>();
        public List<ConnectionData> connections = new ArrayList<>();
    }

    public static class DeviceData {
        public String id, name, type, ip, gateway, installedModule;
        public double x, y;
        public boolean poweredOn;
    }

    public static class ConnectionData {
        public String fromId, toId, type;
        public int weight;
    }

    public static void save(List<Device> devices, List<Connection> connections) {
        ProjectData data = new ProjectData();
        for (Device d : devices) {
            DeviceData dd = new DeviceData();
            dd.id = d.getId();
            dd.name = d.getName();
            dd.type = d.getType().name();
            dd.ip = d.getIp();
            dd.gateway = d.getGateway();
            dd.x = d.xProperty().get();
            dd.y = d.yProperty().get();
            dd.poweredOn = d.isPoweredOn();
            dd.installedModule = d.installedModuleProperty().get();
            data.devices.add(dd);
        }
        for (Connection c : connections) {
            ConnectionData cd = new ConnectionData();
            cd.fromId = c.getFrom().getId();
            cd.toId = c.getTo().getId();
            cd.type = c.getType();
            cd.weight = c.getWeight();
            data.connections.add(cd);
        }

        try (Writer writer = new FileWriter(SAVE_FILE)) {
            gson.toJson(data, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static ProjectData load() {
        File file = new File(SAVE_FILE);
        if (!file.exists()) return null;
        try (Reader reader = new FileReader(file)) {
            return gson.fromJson(reader, ProjectData.class);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
