package com.networksim.model;

public enum DeviceType {
    // Routers
    ROUTER_1941,
    ROUTER_2901,
    ROUTER_2911,
    ROUTER_1841,
    ROUTER_WIRELESS,
    
    // Switches & Hubs
    SWITCH_2960,
    SWITCH_PT,
    L3_SWITCH,
    HUB,
    
    // Security & Wireless
    FIREWALL_ASA,
    ACCESS_POINT,
    
    // Surveillance (Expanded)
    CAMERA_DAHUA,
    CAMERA_HIKVISION,
    CAMERA_TPLINK,
    CAMERA_UBIQUITI,
    CAMERA_AXIS,
    CAMERA_BOSCH,
    NVR_DAHUA,
    NVR_HIKVISION,
    NVR_UBIQUITI,
    NVR_GENERIC,
    
    // IoT Home
    IOT_SPEAKER,
    IOT_SMART_PLUG,
    IOT_MOTION_SENSOR,
    IOT_SIREN,
    IOT_FAN,
    IOT_AC,
    IOT_GARAGE_DOOR,
    IOT_LIGHT,
    IOT_THERMOSTAT,
    IOT_LOCK,
    
    // IoT Industrial
    IOT_PLC,
    IOT_TEMP_SENSOR,
    IOT_PRESSURE_SENSOR,
    IOT_VALVE,
    IOT_CONVEYOR,
    
    // End Devices
    PC,
    LAPTOP,
    SERVER,
    PRINTER,
    IP_PHONE,
    SMARTPHONE,
    
    // Misc
    CLOUD
}