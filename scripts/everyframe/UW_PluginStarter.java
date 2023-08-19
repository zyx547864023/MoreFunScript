package real_combat.scripts.everyframe;

import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import data.scripts.UnderworldModPlugin;

public class UW_PluginStarter extends BaseEveryFrameCombatPlugin {

    @Override
    public void init(CombatEngineAPI engine) {
        if (UnderworldModPlugin.hasMagicLib) {
            engine.addPlugin(new UW_Trails());
        }
    }
}
