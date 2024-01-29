package real_combat.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.combat.entities.Ship;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;
import real_combat.entity.RC_AnchoredEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * 打中添加计时器
 * 计时器时限内被打爆召唤等质量/10的蜘蛛
 */
public class RC_OvumOnHitEffect implements OnHitEffectPlugin {
    private final static String ID="RC_OvumOnHitEffect";
    private final static String OVUM_LIST="OVUM_LIST";
    private final static String OVUM_SHIP="OVUM_SHIP";
    private List<RC_Ovum> ovumList = new ArrayList<>();
    private List<ShipAPI> ovumShip = new ArrayList<>();
    public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target,
                      Vector2f point, boolean shieldHit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {
        if (!shieldHit&&target instanceof  ShipAPI) {
            ShipAPI ship = (ShipAPI) target;
            if (!ship.isFighter()) {
                /*
                    float targetFace = target.getFacing();
                    float landingFace = projectile.getFacing();
                    float relativeAngle = MathUtils.getShortestRotation(targetFace,landingFace);
                    RC_AnchoredEntity a = new RC_AnchoredEntity(target,point,relativeAngle);
                */
                //相对位置
                if (engine.getCustomData().get(OVUM_LIST) != null) {
                    ovumList = (List<RC_Ovum>) engine.getCustomData().get(OVUM_LIST);
                }
                if (engine.getCustomData().get(OVUM_SHIP) != null) {
                    ovumShip = (List<ShipAPI>) engine.getCustomData().get(OVUM_SHIP);
                }
                if (ovumShip.indexOf(ship)==-1) {
                    ovumShip.add(ship);
                    ovumList.add(new RC_Ovum((ShipAPI) target, 120F));
                }
                else {
                    for (RC_Ovum o:ovumList) {
                        if (o.ship == ship) {
                            o.time = 120F;
                        }
                    }
                }
                engine.getCustomData().put(OVUM_SHIP, ovumShip);
                engine.getCustomData().put(OVUM_LIST, ovumList);
            }
        }
    }

    public class RC_Ovum{
        //船
        public ShipAPI ship;
        //时间
        public float time;
        public RC_AnchoredEntity anchoredEntity;
        public RC_Ovum(ShipAPI ship,float time)
        {
            this.ship = ship;
            this.time = time;
        }
    }
}
