package real_combat.shipsystems.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.combat.*;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.util.Misc;
import data.shipsystems.scripts.AmmoFeedStats;
import data.shipsystems.scripts.HighEnergyFocusStats;
import data.shipsystems.scripts.MicroBurnStats;
import org.lwjgl.util.vector.Vector2f;
import real_combat.constant.RC_ComboConstant;
import real_combat.util.RC_Util;

import java.awt.*;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RC_ComboSystem extends BaseShipSystemScript {
    private String ID = "RC_ComboSystem";
    boolean init = false;

    public final Map<MissileAPI, MissileAPI> destroyMap = new HashMap<>();
    private ShipAPI ship;
    @Override
    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        CombatEngineAPI engine = Global.getCombatEngine();
        if(engine.isPaused()) {return;}
        ship = (ShipAPI) stats.getEntity();
        if (ship == null) {
            return;
        }
        try {
            String skill = (String) ship.getCustomData().get("Combo");
            if(skill==null)
            {
                return;
            }
            if (RC_ComboConstant.SKILL.get(skill) instanceof MicroBurnStats ){
                MicroBurnStats system = (MicroBurnStats) RC_ComboConstant.SKILL.get(skill);
                system.apply(stats, id, state, effectLevel);
            }
            else if (RC_ComboConstant.SKILL.get(skill) instanceof EntropyAmplifierStats ){
                EntropyAmplifierStats system = (EntropyAmplifierStats) RC_ComboConstant.SKILL.get(skill);
                system.apply(stats, id, state, effectLevel);
            }
            else if (RC_ComboConstant.SKILL.get(skill) instanceof HighEnergyFocusStats ){
                HighEnergyFocusStats system = (HighEnergyFocusStats) RC_ComboConstant.SKILL.get(skill);
                system.apply(stats, id, state, effectLevel);
            }
            else if (RC_ComboConstant.SKILL.get(skill) instanceof AmmoFeedStats ){
                AmmoFeedStats system = (AmmoFeedStats) RC_ComboConstant.SKILL.get(skill);
                system.apply(stats, id, state, effectLevel);
            }
            else if (RC_ComboConstant.SKILL.get(skill) instanceof AcausalDisruptorStats ){
                AcausalDisruptorStats system = (AcausalDisruptorStats) RC_ComboConstant.SKILL.get(skill);
                system.apply(stats, id, state, effectLevel);
            }
            else if (RC_ComboConstant.SKILL.get(skill) instanceof DamperFieldOmegaStats ){
                DamperFieldOmegaStats system = (DamperFieldOmegaStats) RC_ComboConstant.SKILL.get(skill);
                system.apply(stats, id, state, effectLevel);
            }
            else if (RC_ComboConstant.SKILL.get(skill) instanceof TemporalShellStats ){
                TemporalShellStats system = (TemporalShellStats) RC_ComboConstant.SKILL.get(skill);
                system.apply(stats, id, state, effectLevel);
            }

            if (!init){
                init = true;
                engine.addFloatingText(ship.getLocation(), RC_ComboConstant.SKILL_CHINESE.get(skill).toString(), 50f, Color.YELLOW, ship, 5f, 10f);
            }
        }
        catch (Exception e)
        {
            Global.getLogger(this.getClass()).info(e);
        }
    }

    @Override
    public void unapply(MutableShipStatsAPI stats, String id) {
        CombatEngineAPI engine = Global.getCombatEngine();
        if(engine.isPaused()) {return;}
        if (ship == null) {
            return;
        }
        try {
            String skill = (String) ship.getCustomData().get("Combo");
            if(skill==null)
            {
                return;
            }
            if (RC_ComboConstant.SKILL.get(skill) instanceof MicroBurnStats ){
                MicroBurnStats system = (MicroBurnStats) RC_ComboConstant.SKILL.get(skill);
                system.unapply(stats, id);
            }
            else if (RC_ComboConstant.SKILL.get(skill) instanceof EntropyAmplifierStats ){
                EntropyAmplifierStats system = (EntropyAmplifierStats) RC_ComboConstant.SKILL.get(skill);
                system.unapply(stats, id);
                final String targetDataKey = ship.getId() + "_entropy_target_data";
                engine.getCustomData().remove(targetDataKey);
            }
            else if (RC_ComboConstant.SKILL.get(skill) instanceof HighEnergyFocusStats ){
                HighEnergyFocusStats system = (HighEnergyFocusStats) RC_ComboConstant.SKILL.get(skill);
                system.unapply(stats, id);
            }
            else if (RC_ComboConstant.SKILL.get(skill) instanceof AmmoFeedStats ){
                AmmoFeedStats system = (AmmoFeedStats) RC_ComboConstant.SKILL.get(skill);
                system.unapply(stats, id);
            }
            else if (RC_ComboConstant.SKILL.get(skill) instanceof AcausalDisruptorStats ){
                AcausalDisruptorStats system = (AcausalDisruptorStats) RC_ComboConstant.SKILL.get(skill);
                system.unapply(stats, id);
            }
            else if (RC_ComboConstant.SKILL.get(skill) instanceof DamperFieldOmegaStats ){
                DamperFieldOmegaStats system = (DamperFieldOmegaStats) RC_ComboConstant.SKILL.get(skill);
                system.unapply(stats, id);
            }
            else if (RC_ComboConstant.SKILL.get(skill) instanceof TemporalShellStats ){
                TemporalShellStats system = (TemporalShellStats) RC_ComboConstant.SKILL.get(skill);
                system.unapply(stats, id);
            }

            init = false;
            ship.removeCustomData("Combo");
            ship.setShipSystemDisabled(true);
        }
        catch (Exception e)
        {
            Global.getLogger(this.getClass()).info(e);
        }
    }

    public StatusData getStatusData(int index, State state, float effectLevel) {
        CombatEngineAPI engine = Global.getCombatEngine();
        if(engine.isPaused()) {return null;}
        if (ship == null) {
            return null;
        }
        if (!ship.getId().equals(engine.getPlayerShip().getId()))
        {
            return null;
        }
        try {
            String skill = (String) ship.getCustomData().get("Combo");
            if(skill==null)
            {
                return null;
            }
            BaseShipSystemScript system = (BaseShipSystemScript) RC_ComboConstant.SKILL.get(skill);
            return system.getStatusData(index,state,effectLevel);
        }
        catch (Exception e)
        {
            Global.getLogger(this.getClass()).info(e);
        }
        return null;
    }

    @Override
    public String getInfoText(ShipSystemAPI system, ShipAPI ship) {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine.isPaused()) {return null;}
        if (ship == null) {
            return null;
        }
        if (!ship.getId().equals(engine.getPlayerShip().getId()))
        {
            return null;
        }
        try {
            String skill = (String) ship.getCustomData().get("Combo");
            if (skill==null)
            {
                return null;
            }
            BaseShipSystemScript useSystem = (BaseShipSystemScript) RC_ComboConstant.SKILL.get(skill);
            return useSystem.getInfoText(system,ship);
        }
        catch (Exception e)
        {
            Global.getLogger(this.getClass()).info(e);
        }
        return null;
    }


    @Override
    public boolean isUsable(ShipSystemAPI system, ShipAPI ship) {
        CombatEngineAPI engine = Global.getCombatEngine();
        if(engine.isPaused()) {return false;}
        if (ship == null) {
            return false;
        }
        if (!ship.getId().equals(engine.getPlayerShip().getId()))
        {
            return false;
        }
        try {
            String skill = (String) ship.getCustomData().get("Combo");
            if(skill==null)
            {
                return false;
            }
            BaseShipSystemScript useSystem = (BaseShipSystemScript) RC_ComboConstant.SKILL.get(skill);
            return useSystem.isUsable(system,ship);
        }
        catch (Exception e)
        {
            Global.getLogger(this.getClass()).info(e);
        }
        return false;
    }
}