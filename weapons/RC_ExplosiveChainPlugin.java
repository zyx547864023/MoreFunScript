package real_combat.weapons;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;
import real_combat.entity.RC_AnchoredEntity;
import real_combat.shipsystems.scripts.RC_FlyingThunderGod;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RC_ExplosiveChainPlugin implements EveryFrameWeaponEffectPlugin, OnFireEffectPlugin , OnHitEffectPlugin {
    public static String ID = "RC_ExplosiveChainPlugin";
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

    }

    /**
     * 添加锁链
     * 发射设置开始
     * 移动锁链并添加锁链扣
     */
    @Override
    public void onFire(DamagingProjectileAPI projectile, WeaponAPI weapon, CombatEngineAPI engine) {
        RC_ChainPlugin.RC_Chain chain = new RC_ChainPlugin.RC_Chain(weapon,projectile);
        List<RC_ChainPlugin.RC_Chain> chainList = new ArrayList<>();
        if (engine.getCustomData().get(ID)!=null) {
            chainList = (List<RC_ChainPlugin.RC_Chain>) engine.getCustomData().get(ID);
        }
        chainList.add(chain);
        engine.getCustomData().put(ID,chainList);
        engine.getCustomData().put(projectile+ID,chain);
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
        if (engine.getCustomData().get(projectile+ID)!=null) {
            RC_ChainPlugin.RC_Chain chain = (RC_ChainPlugin.RC_Chain) engine.getCustomData().get(projectile+ID);
            chain.positiveAndNegative = MathUtils.getRandomNumberInRange(-1,1);
            if (chain.positiveAndNegative==0) {
                chain.positiveAndNegative=1;
            }
            chain.isShieldHit = shieldHit;
            chain.stage = RC_ChainPlugin.Stage.ON_HIT;
            chain.anchoredEntity = new RC_AnchoredEntity(target,point,projectile.getFacing());
            engine.getCustomData().put(projectile+ID,chain);

            List<RC_ChainPlugin.RC_Chain> chainList = new ArrayList<>();
            if (engine.getCustomData().get(ID)!=null) {
                chainList = (List<RC_ChainPlugin.RC_Chain>) engine.getCustomData().get(ID);
            }
            for (RC_ChainPlugin.RC_Chain c:chainList) {
                if (c.projectile == chain.projectile) {
                    c.isShieldHit = chain.isShieldHit;
                    if (shieldHit) {
                        c.stage = RC_ChainPlugin.Stage.ON_PULL;
                    }
                    else {
                        c.stage = RC_ChainPlugin.Stage.ON_HIT;
                    }
                    c.positiveAndNegative = chain.positiveAndNegative;
                    c.anchoredEntity = chain.anchoredEntity;
                }
            }
        }
    }
}
