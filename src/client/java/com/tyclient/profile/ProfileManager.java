package com.tyclient.profile;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.tyclient.module.Module;
import com.tyclient.module.ModuleManager;
import com.tyclient.module.setting.FloatSetting;
import com.tyclient.module.setting.Setting;
import net.minecraft.client.Minecraft;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ProfileManager {
    private static ProfileManager instance;
    private final List<Profile> profiles = new ArrayList<>();
    private String activeName;
    private boolean loadedFromDisk;

    public record Snapshot(boolean enabled, int keyCode, Map<String, Float> settings) {}
    public static final class Profile {
        public final String name;
        public final Map<Module, Snapshot> snapshots;

        public Profile(String name, Map<Module, Snapshot> snapshots) {
            this.name = name;
            this.snapshots = snapshots;
        }
    }

    private ProfileManager() {}

    public static ProfileManager getInstance() {
        if (instance == null) {
            instance = new ProfileManager();
            instance.loadFromDisk();
        }
        return instance;
    }

    public List<Profile> getProfiles() {
        return profiles;
    }

    public String getActiveName() {
        return activeName;
    }

    public void createProfile(String name) {
        profiles.removeIf(profile -> profile.name.equalsIgnoreCase(name));
        profiles.add(new Profile(name, capture()));
        activeName = name;
        saveToDisk();
    }

    public void loadProfile(String name) {
        Profile profile = profiles.stream()
                .filter(p -> p.name.equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
        if (profile == null) {
            return;
        }
        apply(profile.snapshots);
        activeName = name;
        saveActive();
    }

    private Map<Module, Snapshot> capture() {
        Map<Module, Snapshot> map = new IdentityHashMap<>();
        for (Module module : ModuleManager.getInstance().getModules()) {
            Map<String, Float> values = new HashMap<>();
            for (Setting setting : module.getSettings()) {
                if (setting instanceof FloatSetting floatSetting) {
                    values.put(setting.getName(), floatSetting.getValue());
                }
            }
            map.put(module, new Snapshot(module.isEnabled(), module.getKeyCode(), values));
        }
        return map;
    }

    private void apply(Map<Module, Snapshot> snapshots) {
        for (Module module : ModuleManager.getInstance().getModules()) {
            Snapshot snapshot = snapshots.get(module);
            if (snapshot == null) {
                continue;
            }
            if (module.isEnabled() != snapshot.enabled) {
                module.toggle();
            }
            module.setKeyCode(snapshot.keyCode);
            for (Setting setting : module.getSettings()) {
                if (setting instanceof FloatSetting floatSetting) {
                    Float value = snapshot.settings.get(setting.getName());
                    if (value != null) {
                        floatSetting.setValue(value);
                    }
                }
            }
        }
    }

    private static String fileNameFor(String name) {
        String sanitized = name.replaceAll("[^A-Za-z0-9._-]", "_");
        if (sanitized.length() > 60) {
            sanitized = sanitized.substring(0, 60);
        }
        if (sanitized.isEmpty()) {
            sanitized = "profile";
        }
        return sanitized + ".json";
    }

    private Path profilesFolder() {
        File folder = new File(Minecraft.getInstance().gameDirectory, "tyclient/profiles");
        folder.mkdirs();
        return folder.toPath();
    }

    private Path activeFile() {
        File folder = new File(Minecraft.getInstance().gameDirectory, "tyclient");
        folder.mkdirs();
        return new File(folder, "active.json").toPath();
    }

    private void saveToDisk() {
        try {
            for (Profile profile : profiles) {
                Files.writeString(profilesFolder().resolve(fileNameFor(profile.name)), toJson(profile));
            }
            Set<String> current = profiles.stream()
                    .map(profile -> fileNameFor(profile.name))
                    .collect(Collectors.toSet());
            try (Stream<Path> files = Files.list(profilesFolder())) {
                files.filter(path -> path.getFileName().toString().endsWith(".json"))
                        .filter(path -> !current.contains(path.getFileName().toString()))
                        .forEach(path -> {
                            try {
                                Files.deleteIfExists(path);
                            } catch (Exception ignored) {
                            }
                        });
            }
            saveActive();
        } catch (Exception ignored) {
        }
    }

    private void saveActive() {
        try {
            JsonObject active = new JsonObject();
            if (activeName != null) {
                active.addProperty("active", activeName);
            }
            Files.writeString(activeFile(), active.toString());
        } catch (Exception ignored) {
        }
    }

    private static String toJson(Profile profile) {
        JsonObject profileObject = new JsonObject();
        profileObject.addProperty("name", profile.name);
        JsonObject modulesJson = new JsonObject();
        for (Map.Entry<Module, Snapshot> entry : profile.snapshots.entrySet()) {
            JsonObject moduleJson = new JsonObject();
            moduleJson.addProperty("enabled", entry.getValue().enabled);
            moduleJson.addProperty("key", entry.getValue().keyCode);
            JsonObject settingsJson = new JsonObject();
            for (Map.Entry<String, Float> setting : entry.getValue().settings.entrySet()) {
                settingsJson.addProperty(setting.getKey(), setting.getValue());
            }
            moduleJson.add("settings", settingsJson);
            modulesJson.add(entry.getKey().getName(), moduleJson);
        }
        profileObject.add("modules", modulesJson);
        return profileObject.toString();
    }

    private static Profile fromJson(JsonObject profileObject) {
        String name = profileObject.has("name") && !profileObject.get("name").isJsonNull()
                ? profileObject.get("name").getAsString()
                : "Unnamed";
        Map<Module, Snapshot> snapshots = new IdentityHashMap<>();
        JsonObject modulesJson = profileObject.has("modules") && !profileObject.get("modules").isJsonNull()
                ? profileObject.getAsJsonObject("modules")
                : null;
        for (Module module : ModuleManager.getInstance().getModules()) {
            if (modulesJson == null || !modulesJson.has(module.getName())) {
                continue;
            }
            JsonObject moduleJson = modulesJson.getAsJsonObject(module.getName());
            boolean enabled = moduleJson.has("enabled") && moduleJson.get("enabled").getAsBoolean();
            int keyCode = moduleJson.has("key") ? moduleJson.get("key").getAsInt() : -1;
            Map<String, Float> settings = new HashMap<>();
            if (moduleJson.has("settings") && !moduleJson.get("settings").isJsonNull()) {
                for (Map.Entry<String, JsonElement> setting : moduleJson.getAsJsonObject("settings").entrySet()) {
                    settings.put(setting.getKey(), setting.getValue().getAsFloat());
                }
            }
            snapshots.put(module, new Snapshot(enabled, keyCode, settings));
        }
        return new Profile(name, snapshots);
    }

    private void loadFromDisk() {
        if (loadedFromDisk) {
            return;
        }
        loadedFromDisk = true;
        try {
            File file = activeFile().toFile();
            if (file.exists()) {
                JsonObject active = JsonParser.parseString(Files.readString(file.toPath())).getAsJsonObject();
                activeName = active.has("active") && !active.get("active").isJsonNull()
                        ? active.get("active").getAsString()
                        : null;
            }
            Path folder = profilesFolder();
            if (!Files.isDirectory(folder)) {
                return;
            }
            try (Stream<Path> files = Files.list(folder)) {
                files.filter(path -> path.getFileName().toString().endsWith(".json"))
                        .forEach(path -> {
                            try {
                                profiles.add(fromJson(JsonParser.parseString(Files.readString(path)).getAsJsonObject()));
                            } catch (Exception ignored) {
                            }
                        });
            }
        } catch (Exception ignored) {
        }
    }
}