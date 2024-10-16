package real_combat.weapons;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import org.lwjgl.util.vector.Vector2f;
import real_combat.shipsystems.scripts.RC_FlyingThunderGod;

import java.util.HashSet;
import java.util.Set;

public class RC_KunaiPlugin implements EveryFrameWeaponEffectPlugin, OnFireEffectPlugin , OnHitEffectPlugin {
    public static String ID = "RC_KunaiPlugin";
    /**
     * 用于计算所有锁链扣的位置
     * 命中后双向折叠
     *
     *
     * @param amount
     * @param engine
     * @param weapon
     */
    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        if (engine == null) return;
        if (engine.isPaused()) {return;}
        if (engine.getCustomData().get(RC_FlyingThunderGod.ID+"SET")!=null) {
            Set<DamagingProjectileAPI> projectileSet = (Set<DamagingProjectileAPI>) engine.getCustomData().get(RC_FlyingThunderGod.ID+"SET");
            Set<DamagingProjectileAPI> removeSet = new HashSet<>();
            for (DamagingProjectileAPI p:projectileSet) {
                if (p instanceof MissileAPI)
                {
                    MissileAPI missile = (MissileAPI) p;
                    if (missile.isFizzling() || missile.isFading() || missile.didDamage() || missile.isExpired()) {
                        if (missile.getSource()!=null) {
                            missile.getSource().getCustomData().remove(RC_FlyingThunderGod.ID);
                            removeSet.add(p);
                        }
                    }
                }
            }
            projectileSet.removeAll(removeSet);
            engine.getCustomData().put(RC_FlyingThunderGod.ID+"SET", projectileSet);
        }
    }

    /**
     * 添加锁链
     * 发射设置开始
     * 移动锁链并添加锁链扣
     */
    @Override
    public void onFire(DamagingProjectileAPI projectile, WeaponAPI weapon, CombatEngineAPI engine) {
        ShipAPI ship = projectile.getSource();
        if (ship!=null) {
            ship.setCustomData(RC_FlyingThunderGod.ID, projectile);
        }
        Set<DamagingProjectileAPI> projectileSet = new HashSet<>();
        if (engine.getCustomData().get(RC_FlyingThunderGod.ID+"SET")!=null) {
            projectileSet = (Set<DamagingProjectileAPI>) engine.getCustomData().get(RC_FlyingThunderGod.ID+"SET");
        }
        projectileSet.add(projectile);
        engine.getCustomData().put(RC_FlyingThunderGod.ID+"SET", projectileSet);
    }

    /**
     * 命中后设置一个时间
     * @param projectile
     * @param target
     * @param point
     * @param shieldHit
     * @param damageResult
     * @param engine
     */
    @Override
    public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target, Vector2f point, boolean shieldHit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {

    }
}
