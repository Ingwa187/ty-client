package com.tyclient.module;

import com.tyclient.module.blatant.Scaffold;
import com.tyclient.module.blatant.Speed;
import com.tyclient.module.combat.AimAssist;
import com.tyclient.module.combat.AutoClicker;
import com.tyclient.module.combat.RightClicker;
import com.tyclient.module.combat.TriggerBot;
import com.tyclient.module.movement.LegitScaffold;
import com.tyclient.module.render.ArrayListModule;
import com.tyclient.module.render.EspModule;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ModuleManager {
    private static ModuleManager instance;
    private final List<Module> modules = new ArrayList<>();

    private ModuleManager() {
        register(new AimAssist());
        register(new AutoClicker());
        register(new TriggerBot());
        register(new RightClicker());
        register(new Speed());
        register(new LegitScaffold());
        register(new ArrayListModule());
        register(new EspModule());
    }

    public Scaffold getScaffold() {
        return null;
    }

    public AutoClicker getAutoClicker() {
        return (AutoClicker) modules.stream()
                .filter(m -> m instanceof AutoClicker)
                .findFirst()
                .orElse(null);
    }

    public static ModuleManager getInstance() {
        if (instance == null) {
            instance = new ModuleManager();
        }
        return instance;
    }

    private void register(Module module) {
        modules.add(module);
    }

    public List<Module> getModules() {
        return modules;
    }

    public List<Module> getModulesByCategory(Category category) {
        return modules.stream()
                .filter(m -> m.getCategory() == category)
                .collect(Collectors.toList());
    }

    public AimAssist getAimAssist() {
        return (AimAssist) modules.stream()
                .filter(m -> m instanceof AimAssist)
                .findFirst()
                .orElse(null);
    }

    public TriggerBot getTriggerBot() {
        return (TriggerBot) modules.stream()
                .filter(m -> m instanceof TriggerBot)
                .findFirst()
                .orElse(null);
    }

    public RightClicker getRightClicker() {
        return (RightClicker) modules.stream()
                .filter(m -> m instanceof RightClicker)
                .findFirst()
                .orElse(null);
    }

    public LegitScaffold getLegitScaffold() {
        return (LegitScaffold) modules.stream()
                .filter(m -> m instanceof LegitScaffold)
                .findFirst()
                .orElse(null);
    }

    public Speed getSpeed() {
        return (Speed) modules.stream()
                .filter(m -> m instanceof Speed)
                .findFirst()
                .orElse(null);
    }

    public ArrayListModule getArrayList() {
        return (ArrayListModule) modules.stream()
                .filter(m -> m instanceof ArrayListModule)
                .findFirst()
                .orElse(null);
    }

    public EspModule getEsp() {
        return (EspModule) modules.stream()
                .filter(m -> m instanceof EspModule)
                .findFirst()
                .orElse(null);
    }
}
