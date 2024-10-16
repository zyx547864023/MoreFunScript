package real_combat.hullmods;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponSize;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponType;
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;
import com.fs.starfarer.api.combat.listeners.WeaponRangeModifier;
import org.lwjgl.util.vector.Vector2f;

import java.util.HashMap;
import java.util.Map;

public class RealCombatHullmod extends BaseHullMod {

    public static final Map<WeaponSize, Float> BALLISTIC_RANGE_UP = new HashMap<>(3);
    public static final Map<WeaponSize, Float> BALLISTIC_DAMAGE_DOWN = new HashMap<>(3);
    public static final int ENGINE_RANGE_UP = 100;
    public static final int MISSILE_RANGE_UP = 200;

    private static final int INDEX_ENGINE_RANGE_UP = 0;
    private static final int INDEX_BALLISTIC_RANGE_UP_SMALL = 1;
    private static final int INDEX_BALLISTIC_RANGE_UP_MEDIUM = 2;
    private static final int INDEX_BALLISTIC_RANGE_UP_LARGE = 3;
    private static final int INDEX_BALLISTIC_DAMAGE_DOWN_SMALL = 4;
    private static final int INDEX_BALLISTIC_DAMAGE_DOWN_MEDIUM = 5;
    private static final int INDEX_BALLISTIC_DAMAGE_DOWN_LARGE = 6;
    private static final int INDEX_MISSILE_RANGE_UP = 7;

    static {
        BALLISTIC_RANGE_UP.put(WeaponSize.LARGE, 150f);
        BALLISTIC_RANGE_UP.put(WeaponSize.MEDIUM, 100f);
        BALLISTIC_RANGE_UP.put(WeaponSize.SMALL, 0f);

        BALLISTIC_DAMAGE_DOWN.put(WeaponSize.LARGE, -50f);
        BALLISTIC_DAMAGE_DOWN.put(WeaponSize.MEDIUM, -50f);
        BALLISTIC_DAMAGE_DOWN.put(WeaponSize.SMALL, 0f);
    }

    @Override
    public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
        stats.getEnergyWeaponRangeBonus().modifyPercent(id, ENGINE_RANGE_UP);
        stats.getMissileWeaponRangeBonus().modifyPercent(id, MISSILE_RANGE_UP);
    }
    /*
    @Override
    public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
        stats.getDynamic().getMod(Stats.SMALL_PD_MOD).modifyFlat(id, OP_INCREASE_SMALL);
        stats.getDynamic().getMod(Stats.MEDIUM_PD_MOD).modifyFlat(id, OP_INCREASE_MEDIUM);
        stats.getDynamic().getMod(Stats.LARGE_PD_MOD).modifyFlat(id, OP_INCREASE_LARGE);
    }
	*/
    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        ship.addListener(new DamageDealtMod());
        ship.addListener(new WeaponRangeMod());
    }


    private static class DamageDealtMod implements DamageDealtModifier {

        @Override
        public String modifyDamageDealt(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
            WeaponAPI weapon = null;
            if (param instanceof DamagingProjectileAPI) {
                weapon = ((DamagingProjectileAPI) param).getWeapon();
            } else if (param instanceof BeamAPI) {
                weapon = ((BeamAPI) param).getWeapon();
            } else if (param instanceof MissileAPI) {
                weapon = ((MissileAPI) param).getWeapon();
            }
            String id = "real_combat_dam_mod";
            if (weapon == null) {
                if (damage.getStats() != null) {
                    damage.getStats().getHitStrengthBonus().unmodify(id);
                }
                return null;
            }
            if (weapon.getType()!=WeaponType.ENERGY&&weapon.getType()!=WeaponType.BALLISTIC&&weapon.getType()!=WeaponType.BUILT_IN) {
                if (damage.getStats() != null) {
                    damage.getStats().getHitStrengthBonus().unmodify(id);
                }
                return null;
            }
            if (weapon.getType()==WeaponType.ENERGY)
            {
                //Vector2f weaponlocation = weapon.getLocation();
                //double distance = Math.sqrt((weaponlocation.x - point.x)*(weaponlocation.x - point.x)+(weaponlocation.y - point.y)*(weaponlocation.y - point.y));
                //如果距离大于射程威力减小，距离小于射程威力增加，最大为2
                if (param instanceof BeamAPI) {
                    double percent = ((BeamAPI) param).getLength()/weapon.getRange();
                    damage.getModifier().modifyPercent(id, (float) ((1-percent)*100));
                }
                if (param instanceof DamagingProjectileAPI) {
                    Vector2f weaponlocation = ((DamagingProjectileAPI) param).getSpawnLocation();
                    double distance = Math.sqrt((weaponlocation.x - point.x)*(weaponlocation.x - point.x)+(weaponlocation.y - point.y)*(weaponlocation.y - point.y));
                    double percent = distance/weapon.getRange();
                    damage.getModifier().modifyPercent(id, (float) ((1-percent)*100));
                }
            }
            if (weapon.getType()==WeaponType.BALLISTIC)//||weapon.getType()==WeaponType.BUILT_IN
            {
                damage.getModifier().modifyPercent(id, BALLISTIC_DAMAGE_DOWN.get(weapon.getSize()));
            }
            return id;
        }
    }

    private static class WeaponRangeMod implements WeaponRangeModifier {

        @Override
        public float getWeaponRangePercentMod(ShipAPI ship, WeaponAPI weapon) {
            if (!(weapon.getType() == WeaponType.BALLISTIC)) {
                return 0f;
            }
            return (BALLISTIC_RANGE_UP.get(weapon.getSize())) / 100f;
        }

        @Override
        public float getWeaponRangeMultMod(ShipAPI ship, WeaponAPI weapon) {
            return 1f;
        }

        @Override
        public float getWeaponRangeFlatMod(ShipAPI ship, WeaponAPI weapon) {
            return 0f;
        }
    }

    //根据武器规格，增加能量武器{%s}的射程，根据命中距离衰减伤害，增加实弹武器{%s/%s/%s}的射程，同时减少实弹武器{%s/%s/%s}的伤害，增加导弹{%s}的射程。
    @Override
    public String getDescriptionParam(int index, HullSize hullSize) {
        if (index == INDEX_ENGINE_RANGE_UP) {
            return "" + ENGINE_RANGE_UP+"%";
        }
        if (index == INDEX_BALLISTIC_RANGE_UP_SMALL) {
            return "" + BALLISTIC_RANGE_UP.get(WeaponSize.SMALL).intValue()+"%";
        }
        if (index == INDEX_BALLISTIC_RANGE_UP_MEDIUM) {
            return "" + BALLISTIC_RANGE_UP.get(WeaponSize.MEDIUM).intValue()+"%";
        }
        if (index == INDEX_BALLISTIC_RANGE_UP_LARGE) {
            return "" + BALLISTIC_RANGE_UP.get(WeaponSize.LARGE).intValue()+"%";
        }
        if (index == INDEX_BALLISTIC_DAMAGE_DOWN_SMALL) {
            return "" + BALLISTIC_DAMAGE_DOWN.get(WeaponSize.SMALL).intValue()+"%";
        }
        if (index == INDEX_BALLISTIC_DAMAGE_DOWN_MEDIUM) {
            return "" + BALLISTIC_DAMAGE_DOWN.get(WeaponSize.MEDIUM).intValue()+"%";
        }
        if (index == INDEX_BALLISTIC_DAMAGE_DOWN_LARGE) {
            return "" + BALLISTIC_DAMAGE_DOWN.get(WeaponSize.LARGE).intValue()+"%";
        }
        if (index == INDEX_MISSILE_RANGE_UP) {
            return "" + MISSILE_RANGE_UP+"%";
        }
        return null;
    }
}
