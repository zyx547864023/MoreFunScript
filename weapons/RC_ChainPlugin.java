package real_combat.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.entities.AnchoredEntity;
import org.lwjgl.util.vector.Vector2f;
import real_combat.entity.RC_AnchoredEntity;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class RC_ChainPlugin implements EveryFrameWeaponEffectPlugin, OnFireEffectPlugin , OnHitEffectPlugin {
    public static String ID = "RC_ChainPlugin";
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
        RC_Chain chain = new RC_Chain(weapon,projectile);
        List<RC_Chain> chainList = new ArrayList<>();
        if (engine.getCustomData().get(ID)!=null) {
            chainList = (List<RC_Chain>) engine.getCustomData().get(ID);
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
            RC_Chain chain = (RC_Chain) engine.getCustomData().get(projectile+ID);
            chain.positiveAndNegative = MathUtils.getRandomNumberInRange(-1,1);
            if (chain.positiveAndNegative==0) {
                chain.positiveAndNegative=1;
            }
            chain.isShieldHit = shieldHit;
            chain.stage = Stage.ON_HIT;
            chain.anchoredEntity = new RC_AnchoredEntity(target,point,projectile.getFacing());
            engine.getCustomData().put(projectile+ID,chain);

            List<RC_Chain> chainList = new ArrayList<>();
            if (engine.getCustomData().get(ID)!=null) {
                chainList = (List<RC_Chain>) engine.getCustomData().get(ID);
            }
            for (RC_Chain c:chainList) {
                if (c.projectile == chain.projectile) {
                    c.isShieldHit = chain.isShieldHit;
                    if (shieldHit) {
                        c.stage = Stage.ON_PULL;
                    }
                    else {
                        c.stage = Stage.ON_HIT;
                    }
                    c.positiveAndNegative = chain.positiveAndNegative;
                    c.anchoredEntity = chain.anchoredEntity;
                }
            }
        }
    }


    /**
     * 变化是整体的
     *
     * 力的产生与结束
     *
     * 源头从距离锁链开始产生推力
     *
     * 一个锁链一度先试用 最多增加360换向
     */
    public static class RC_ChainOne{
        public Vector2f v = new Vector2f(0,0);
        //位置
        public Vector2f location;
        //速度
        //角度
        public float facing;
        //当前偏移量
        public float deflection = 0;
        public SpriteAPI sprite;
        //牵拉力
        //前连接点
        //后推力
        //后链接点
        //摆动角度
        //摆动极限 摆动到达极限调转方向 依照锁链数量决定极限
        //两次过后不摆 第三次直

        //所有锁链循环收到 前拉力

        //第一个锁链收到拉力
        //最后一个锁链受到推力

        //基于上一个锁链方向对下一个锁链产生力方向

        //命中之后才计算推力

        //

        //1、命中后偏移
        //2、首位停止，但是惯性力向前
        public RC_ChainOne(Vector2f location,float facing,SpriteAPI sprite){
            this.location = location;
            this.facing = facing;
            this.sprite = sprite;
        }
    }

    public class RC_Chain{
        //船
        //武器
        public WeaponAPI weapon;
        //子弹
        public DamagingProjectileAPI projectile;
        //锁链List
        public List<RC_ChainOne> chainOneList = new ArrayList<>();
        //锚点
        public RC_AnchoredEntity anchoredEntity;
        //状态 发射 命中 拉取 收回
        public Stage stage = Stage.ON_FIRE;
        //状态切换剩余时间
        public float hitTime = 0.5f;
        //折叠方向 正反
        public int positiveAndNegative;
        //
        float deflection = 0f;
        public boolean isShieldHit = false;
        public RC_Chain(WeaponAPI weapon,DamagingProjectileAPI projectile){
            this.weapon = weapon;
            this.projectile = projectile;
        }
    }

    public static enum Stage {
        ON_FIRE,
        ON_HIT,
        ON_HIT_WAIT,
        ON_PULL_WAIT,
        ON_PULL,
    }
}
