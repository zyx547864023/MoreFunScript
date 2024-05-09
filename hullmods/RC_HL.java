package real_combat.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;
import com.fs.starfarer.api.combat.listeners.DamageTakenModifier;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.combat.RiftLanceEffect;
import com.fs.starfarer.api.loading.WeaponGroupSpec;
import com.fs.starfarer.api.loading.WeaponSpecAPI;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 寒流
 */
public class RC_HL extends BaseHullMod {
    public static String ID = "RC_HL";
    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {

    }

    @Override
    public void applyEffectsBeforeShipCreation(HullSize hullSize,
                                               MutableShipStatsAPI stats, String id) {
    }

    @Override
    public void advanceInCampaign(FleetMemberAPI member, float amount) {

    }

    @Override
    public void advanceInCombat(final ShipAPI ship, float amount) {
        if (ship.getCustomData().get(ID)==null) {
            ship.addListener(new DamageTakenMod());
            ship.setCustomData(ID,true);
        }
        List<HLDamage> hlDamageList = new ArrayList<>();
        if (ship.getCustomData().get(ID+"hlDamageList")!=null) {
            hlDamageList = (List<HLDamage>) ship.getCustomData().get(ID+"hlDamageList");
        }
        List<HLDamage> removeList = new ArrayList<>();
        for (HLDamage d:hlDamageList) {
            if (d.damage>0) {
                ship.getFluxTracker().decreaseFlux(ship.getMutableStats().getFluxDissipation().modified*amount);
                CombatEngineAPI engine = Global.getCombatEngine();
                Vector2f partvec = Vector2f.sub(d.point, ship.getLocation(), (Vector2f) null);
                //渲染
                float size = MathUtils.getRandomNumberInRange(5F*ship.getHullSize().ordinal(), 10F*ship.getHullSize().ordinal());
                engine.addNegativeNebulaParticle(d.point, partvec, size, 1.5f, 0.05f, 0f, 1f+ MathUtils.getRandomNumberInRange(0.5f, 1f), RiftLanceEffect.getColorForDarkening(ship.getVentCoreColor()));
                engine.addSwirlyNebulaParticle(d.point, partvec, size, 2f, 0.05f, 0f, 1f+MathUtils.getRandomNumberInRange(0.5f, 1f), ship.getVentFringeColor(), false);
                d.damage-=ship.getMutableStats().getFluxDissipation().modified*amount;
            }
            else {
                removeList.add(d);
            }
        }
        hlDamageList.removeAll(removeList);
        ship.setCustomData(ID+"hlDamageList",hlDamageList);
    }
    @Override
    public boolean isApplicableToShip(ShipAPI ship) {
        return true;
    }
    @Override
    public String getUnapplicableReason(ShipAPI ship) {
        return null;
    }

    private static class DamageTakenMod implements DamageTakenModifier {
        @Override
        public String modifyDamageTaken(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
            if (!shieldHit) {
                //记录被打位置
                //记录被打伤害
                List<HLDamage> hlDamageList = new ArrayList<>();
                if (target.getCustomData().get(ID+"hlDamageList")!=null) {
                    hlDamageList = (List<HLDamage>) target.getCustomData().get(ID+"hlDamageList");
                }
                hlDamageList.add(new HLDamage(point, damage.getDamage()));
                target.setCustomData(ID+"hlDamageList",hlDamageList);
            }
            return null;
        }
    }

    public static class HLDamage {
        Vector2f point;
        float damage;
        public HLDamage(Vector2f point,float damage) {
            this.damage = damage;
            this.point = point;
        }
    }
}
