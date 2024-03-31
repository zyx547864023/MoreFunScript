package real_combat.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.combat.listeners.WeaponRangeModifier;
import com.fs.starfarer.api.impl.combat.RiftLanceEffect;
import com.fs.starfarer.api.loading.WeaponSpecAPI;
import com.fs.starfarer.loading.specs.BaseWeaponSpec;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;
import real_combat.RCModPlugin;
import real_combat.combat.RealCombatEveryFrameCombatPlugin;
import real_combat.entity.RC_AnchoredEntity;
import real_combat.util.MyMath;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 反光束高射炮
 *
 * 高射炮发射锚点 到达位置之后爆开持续产生烟雾
 * 光束无法穿过烟雾
 * 烟雾内光束武器损坏
 *
 */

public class RC_ReflectiveBeamPlugin implements EveryFrameWeaponEffectPlugin, OnFireEffectPlugin , OnHitEffectPlugin {
    public static String ID = "RC_ReflectiveBeamPlugin";
    /**
     * 释放烟雾处理光束
     *
     *
     *
     * @param amount
     * @param engine
     * @param weapon
     */
    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        //engine.getNebula().setHasNebula();
        if (!engine.isPaused()) {
            for (ShipAPI s:engine.getShips()) {
                if (s.isAlive()) {
                    if (s.getListeners(WeaponRangeMod.class).size() == 0) {
                        s.addListener(new WeaponRangeMod());
                    }
                }
            }

            List<RC_ReflectiveBeam> allReflectiveBeamList = new ArrayList<>();
            if (engine.getCustomData().get(ID) != null) {
                allReflectiveBeamList = (List<RC_ReflectiveBeam>) engine.getCustomData().get(ID);
            }
            //没有命中到达最远距离收回
            List<RC_ReflectiveBeam> reflectiveBeamList = new ArrayList<>();
            if (engine.getCustomData().get(weapon+ID) != null) {
                reflectiveBeamList = (List<RC_ReflectiveBeam>) engine.getCustomData().get(weapon+ID);
            }
            List<RC_ReflectiveBeam> removeList = new ArrayList<>();
            for (RC_ReflectiveBeam b : reflectiveBeamList) {
                if (b.stage == Stage.ON_HIT) {
                    Vector2f location = b.location;
                    if (b.anchoredEntity!=null) {
                        b.location = b.anchoredEntity.getLocation();
                    }
                    //判断剩余时间
                    //旋转
                    b.facing+=amount*360;
                    //喷气
                    Vector2f partstartloc = MathUtils.getPointOnCircumference(b.location, 20f, b.facing+MathUtils.getRandomNumberInRange(-5f,5f));
                    Vector2f partvec = Vector2f.sub(b.location, partstartloc, (Vector2f) null);
                    partvec.scale(15F);
                    float size = MathUtils.getRandomNumberInRange(40F, 60F);
                    //engine.addNegativeNebulaParticle(partstartloc, partvec, size, 1.5f, 0.5f, 0f, 1f+MathUtils.getRandomNumberInRange(0.5f, 1f), Color.PINK);
                    engine.addSwirlyNebulaParticle(partstartloc, partvec, size, 3f+MathUtils.getRandomNumberInRange(0.5f, 1f), 0.25f, 0f, 1f+MathUtils.getRandomNumberInRange(0.5f, 1f), Color.PINK, false);

                    //喷气
                    partstartloc = MathUtils.getPointOnCircumference(b.location, 20f, b.facing+90+MathUtils.getRandomNumberInRange(-5f,5f));
                    partvec = Vector2f.sub(b.location, partstartloc, (Vector2f) null);
                    partvec.scale(15F);
                    size = MathUtils.getRandomNumberInRange(40F, 60F);
                    //engine.addNegativeNebulaParticle(partstartloc, partvec, size, 1.5f, 0.5f, 0f, 1f+MathUtils.getRandomNumberInRange(0.5f, 1f), Color.PINK);
                    engine.addSwirlyNebulaParticle(partstartloc, partvec, size, 3f+MathUtils.getRandomNumberInRange(0.5f, 1f), 0.25f, 0f, 1f+MathUtils.getRandomNumberInRange(0.5f, 1f), Color.PINK, false);

                    //喷气
                    partstartloc = MathUtils.getPointOnCircumference(b.location, 20f, b.facing+180+MathUtils.getRandomNumberInRange(-5f,5f));
                    partvec = Vector2f.sub(b.location, partstartloc, (Vector2f) null);
                    partvec.scale(15F);
                    size = MathUtils.getRandomNumberInRange(40F, 60F);
                    //engine.addNegativeNebulaParticle(partstartloc, partvec, size, 1.5f, 0.5f, 0f, 1f+MathUtils.getRandomNumberInRange(0.5f, 1f), Color.PINK);
                    engine.addSwirlyNebulaParticle(partstartloc, partvec, size, 3f+MathUtils.getRandomNumberInRange(0.5f, 1f), 0.25f, 0f, 1f+MathUtils.getRandomNumberInRange(0.5f, 1f), Color.PINK, false);

                    //喷气
                    partstartloc = MathUtils.getPointOnCircumference(b.location, 20f, b.facing+270+MathUtils.getRandomNumberInRange(-5f,5f));
                    partvec = Vector2f.sub(b.location, partstartloc, (Vector2f) null);
                    partvec.scale(15F);
                    size = MathUtils.getRandomNumberInRange(40F, 60F);
                    //engine.addNegativeNebulaParticle(partstartloc, partvec, size, 1.5f, 0.5f, 0f, 1f+MathUtils.getRandomNumberInRange(0.5f, 1f), Color.PINK);
                    engine.addSwirlyNebulaParticle(partstartloc, partvec, size, 3f+MathUtils.getRandomNumberInRange(0.5f, 1f), 0.25f, 0f, 1f+MathUtils.getRandomNumberInRange(0.5f, 1f), Color.PINK, false);

                    allReflectiveBeamList.add(b);

                    b.timer -= amount;
                    if (b.timer<0) {
                        b.timer = 0;
                        removeList.add(b);
                    }
                }
                else if (b.stage == Stage.ON_FIRE) {
                    if (b.projectile.isFading()&&!b.projectile.didDamage()) {
                        b.stage = Stage.ON_HIT;
                        b.location = b.projectile.getLocation();
                    }
                }
            }
            allReflectiveBeamList.removeAll(removeList);
            engine.getCustomData().put(ID, allReflectiveBeamList);
            reflectiveBeamList.removeAll(removeList);
            engine.getCustomData().put(weapon+ID, reflectiveBeamList);
        }
    }

    /**
     * 存放子弹
     *
     *
     */
    @Override
    public void onFire(DamagingProjectileAPI projectile, WeaponAPI weapon, CombatEngineAPI engine) {
        RC_ReflectiveBeam reflectiveBeam = new RC_ReflectiveBeam(weapon,projectile);
        List<RC_ReflectiveBeam> reflectiveBeamList = new ArrayList<>();
        if (engine.getCustomData().get(weapon+ID)!=null) {
            reflectiveBeamList = (List<RC_ReflectiveBeam>) engine.getCustomData().get(weapon+ID);
        }
        reflectiveBeamList.add(reflectiveBeam);
        engine.getCustomData().put(weapon+ID,reflectiveBeamList);
        engine.getCustomData().put(projectile+ID,reflectiveBeam);
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
        //设置锚点
        if (engine.getCustomData().get(projectile+ID)!=null) {
            RC_ReflectiveBeam reflectiveBeam = (RC_ReflectiveBeam) engine.getCustomData().get(projectile+ID);
            reflectiveBeam.stage = Stage.ON_HIT;
            reflectiveBeam.facing = projectile.getFacing();
            if (shieldHit) {
                reflectiveBeam.location = point;
            }
            else {
                reflectiveBeam.anchoredEntity = new RC_AnchoredEntity(target, point, projectile.getFacing());
            }
            engine.getCustomData().put(projectile+ID,reflectiveBeam);

            List<RC_ReflectiveBeam> allReflectiveBeamList = new ArrayList<>();
            if (engine.getCustomData().get(ID) != null) {
                allReflectiveBeamList = (List<RC_ReflectiveBeam>) engine.getCustomData().get(ID);
            }
            allReflectiveBeamList.add(reflectiveBeam);
            engine.getCustomData().put(ID, allReflectiveBeamList);

            List<RC_ReflectiveBeam> reflectiveBeamList = new ArrayList<>();
            if (engine.getCustomData().get(reflectiveBeam.weapon+ID)!=null) {
                reflectiveBeamList = (List<RC_ReflectiveBeam>) engine.getCustomData().get(reflectiveBeam.weapon+ID);
            }
            for (RC_ReflectiveBeam b:reflectiveBeamList) {
                if (b.projectile == reflectiveBeam.projectile) {
                    b.stage = Stage.ON_HIT;
                    b.facing = projectile.getFacing();
                    if (shieldHit) {
                        b.location = point;
                    }
                    else {
                        b.anchoredEntity = reflectiveBeam.anchoredEntity;
                    }
                }
            }
        }
    }


    /**
     * 烟雾
     * 位置
     * 范围
     * 持续时间
     * 角度
     * 旋转释放烟雾
     */
    public static class RC_ReflectiveBeam{
        //武器
        public WeaponAPI weapon;
        //子弹
        public DamagingProjectileAPI projectile;
        //位置
        public Vector2f location;
        //范围
        public float radius=500f;
        //角度
        public float facing;
        public Stage stage = Stage.ON_FIRE;
        public float timer=10f;
        //命中后锚点
        public RC_AnchoredEntity anchoredEntity;
        //1、命中后偏移
        //2、首位停止，但是惯性力向前
        public RC_ReflectiveBeam(WeaponAPI weapon,DamagingProjectileAPI projectile){
            this.weapon = weapon;
            this.projectile = projectile;
        }
    }

    public static enum Stage {
        ON_FIRE,
        ON_HIT,
        ON_NULL,
    }

    private static class WeaponRangeMod implements WeaponRangeModifier {

        @Override
        public float getWeaponRangePercentMod(ShipAPI ship, WeaponAPI weapon) {
            return 0f;
        }

        @Override
        public float getWeaponRangeMultMod(ShipAPI ship, WeaponAPI weapon) {
            if (weapon.isBeam()) {
                CombatEngineAPI engine = Global.getCombatEngine();
                //没有命中到达最远距离收回
                List<RC_ReflectiveBeam> reflectiveBeamList = new ArrayList<>();
                if (engine.getCustomData().get(ID) != null) {
                    reflectiveBeamList = (List<RC_ReflectiveBeam>) engine.getCustomData().get(ID);
                }
                for (RC_ReflectiveBeam b : reflectiveBeamList) {
                    for (BeamAPI beam:weapon.getBeams()) {
                        float weaponRange = 0;
                        if (engine.getCustomData().get(ID+weapon+"WEAPON_RANGE")!=null) {
                            weaponRange = (float) engine.getCustomData().get(ID+weapon+"WEAPON_RANGE");
                        }
                        else {
                            weaponRange = weapon.getSpec().getMaxRange();
                            engine.getCustomData().put(ID+weapon+"WEAPON_RANGE",weaponRange);
                        }
                        if (b.location==null) {
                            return weaponRange;
                        }
                        if (MathUtils.isWithinRange(beam.getFrom(), b.location, b.radius)) {
                            return 0f;
                        }
                        else if (MathUtils.isWithinRange(beam.getTo(),b.location,b.radius)&&!MathUtils.isWithinRange(beam.getFrom(),b.location,b.radius)) {
                            return (MathUtils.getDistance(beam.getFrom(),b.location)-b.radius)/weaponRange;
                        }
                        //穿过
                        //
                        else if (weaponRange>MathUtils.getDistance(beam.getFrom(),b.location)+b.radius) {
                            float beamToBAngle = VectorUtils.getAngle(beam.getFrom(), b.location);
                            float beamToBeamAngle = VectorUtils.getAngle(beam.getFrom(), beam.getTo());
                            Vector2f bRoundPoint = MathUtils.getPoint(b.location, b.radius, beamToBAngle + 90);
                            float beamToOtherRoundPoint = VectorUtils.getAngle(beam.getFrom(), bRoundPoint);
                            if (Math.abs(MathUtils.getShortestRotation(beamToBAngle, beamToOtherRoundPoint))
                                    > Math.abs(MathUtils.getShortestRotation(beamToBAngle, beamToBeamAngle))
                            )
                            {
                                Vector2f hitPoint = MathUtils.getNearestPointOnLine(b.location,beam.getFrom(),beam.getTo());
                                return MathUtils.getDistance(beam.getFrom(),hitPoint)/weaponRange;
                            }
                        }
                    }
                }
            }
            return 1f;
        }

        @Override
        public float getWeaponRangeFlatMod(ShipAPI ship, WeaponAPI weapon) {
            return 0f;
        }
    }
}
