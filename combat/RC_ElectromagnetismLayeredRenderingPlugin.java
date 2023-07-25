package real_combat.combat;


import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.combat.systems.EmpArcEntity;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lazywizard.lazylib.combat.entities.SimpleEntity;
import org.lwjgl.util.vector.Vector2f;
import real_combat.entity.RC_AnchoredEntity;
import real_combat.weapons.RC_ElectromagnetismOnHitEffect;

import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * 命中之后持续生成电弧 给一个力拉向目标
 * 命中盾电弧到对面
 * 命中船体电弧到最近的武器
 * 电弧断开再爆炸给一次EMP伤害
 */

public class RC_ElectromagnetismLayeredRenderingPlugin extends BaseCombatLayeredRenderingPlugin {
    private final static String ID = "RC_ElectromagnetismLayeredRenderingPlugin";
    private final static String ELECTROMAGNETISM_LIST="ELECTROMAGNETISM_LIST";
    private SpriteAPI sprite  = Global.getSettings().getSprite("graphics/missiles/shell_gauss_cannon.png");
    protected IntervalUtil tracker = new IntervalUtil(0.4f, 0.5f);
    List<Emp> empEntityList = new ArrayList<>();
    public RC_ElectromagnetismLayeredRenderingPlugin() {}
    @Override
    public void advance(float amount) {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (!engine.isPaused()) {
            if (engine.getCustomData().get(ELECTROMAGNETISM_LIST)!=null) {
                List<RC_ElectromagnetismOnHitEffect.ElectromagnetismProj> electromagnetismProjList = (List<RC_ElectromagnetismOnHitEffect.ElectromagnetismProj>) engine.getCustomData().get(ELECTROMAGNETISM_LIST);
                List<RC_ElectromagnetismOnHitEffect.ElectromagnetismProj> removeList = new ArrayList<>();
                for (RC_ElectromagnetismOnHitEffect.ElectromagnetismProj ep : electromagnetismProjList) {
                    ep.timeForExplosion -= amount;
                    float distance = MathUtils.getDistance(ep.weapon.getLocation(),ep.anchoredEntity.getLocation());
                    if (distance<ep.weapon.getRange()*0.8F) {
                        if (ep.timeForExplosion <= 0||(ep.shieldHit&&ep.target.getShield().isOff())||ep.target.isExpired()) {
                            engine.spawnExplosion(ep.anchoredEntity.getLocation(), ep.target.getVelocity(), Color.BLUE, 10F, 2F);
                            removeList.add(ep);
                            break;
                        }
                        float emp = ep.projectile.getEmpAmount();
                        float dam = ep.projectile.getDamageAmount();
                        float epToTarget = VectorUtils.getAngle(ep.anchoredEntity.getLocation(),ep.target.getLocation());
                        //如果命中的是船
                        if (ep.target instanceof ShipAPI) {
                            tracker.advance(amount);
                            if (tracker.intervalElapsed()) {
                                if (ep.shieldHit && ep.target.getShield().getActiveArc() == 360) {
                                    Vector2f point = MathUtils.getPoint(ep.target.getLocation(), ((ShipAPI) ep.target).getShieldRadiusEvenIfNoShield(), epToTarget);
                                    EmpArcEntityAPI empArcEntity = engine.spawnEmpArc(ep.projectile.getSource(), ep.anchoredEntity.getLocation(), new SimpleEntity(ep.anchoredEntity.getLocation()), new SimpleEntity(point),
                                            DamageType.ENERGY,
                                            dam,
                                            emp, // emp
                                            100000f, // max range
                                            "tachyon_lance_emp_impact",
                                            20f, // thickness
                                            new Color(25, 100, 155, 255),
                                            new Color(255, 255, 255, 255)
                                    );
                                } else if (ep.shieldHit) {
                                    if (1-((ShipAPI) ep.target).getMutableStats().getDynamic().getStat(Stats.SHIELD_PIERCED_MULT).getBaseValue()<MathUtils.getRandomNumberInRange(0F,1F)) {
                                        EmpArcEntityAPI empArcEntity = engine.spawnEmpArcPierceShields(ep.projectile.getSource(), ep.anchoredEntity.getLocation(), ep.target, ep.target,
                                                DamageType.ENERGY,
                                                dam,
                                                emp * ((ShipAPI) ep.target).getShield().getFluxPerPointOfDamage(),//.getMutableStats().getDynamic().getStat(Stats.SHIELD_PIERCED_MULT).getBaseValue(), // emp
                                                100000f, // max range
                                                "tachyon_lance_emp_impact",
                                                20f, // thickness
                                                new Color(25, 100, 155, 255),
                                                new Color(255, 255, 255, 255)
                                        );
                                    }
                                } else {
                                    EmpArcEntityAPI empArcEntity = engine.spawnEmpArcPierceShields(ep.projectile.getSource(), ep.anchoredEntity.getLocation(), ep.target, ep.target,
                                            DamageType.ENERGY,
                                            dam,
                                            emp, // emp
                                            100000f, // max range
                                            "tachyon_lance_emp_impact",
                                            20f, // thickness
                                            new Color(25, 100, 155, 255),
                                            new Color(255, 255, 255, 255)
                                    );
                                }
                            }
                        }
                        else {
                            tracker.advance(amount);
                            if (tracker.intervalElapsed()) {
                                EmpArcEntityAPI empArcEntity = engine.spawnEmpArc(ep.projectile.getSource(), ep.anchoredEntity.getLocation(), ep.target, ep.target,
                                        DamageType.ENERGY,
                                        dam,
                                        emp, // emp
                                        100000f, // max range
                                        "tachyon_lance_emp_impact",
                                        20f, // thickness
                                        new Color(25, 100, 155, 255),
                                        new Color(255, 255, 255, 255)
                                );
                            }
                        }

                        if (tracker.intervalElapsed()) {
                            EmpArcEntityAPI empArcEntity = engine.spawnEmpArcVisual(ep.weapon.getLocation(),ep.projectile.getSource(), ep.anchoredEntity.getLocation(), ep.anchoredEntity,
                                    20f, // thickness
                                    new Color(25, 100, 155, 255),
                                    new Color(255, 255, 255, 255)
                            );
                        }

                        if (distance>ep.weapon.getRange()*0.5&&ep.distance!=0) {
                            float shipToEp = VectorUtils.getAngle(ep.weapon.getShip().getLocation(), ep.anchoredEntity.getLocation());
                            //给目标一个力
                            CombatUtils.applyForce(ep.target, shipToEp+180, ep.weapon.getShip().getMass() * amount * 0.2f);
                            //给自己一个力
                            CombatUtils.applyForce(ep.weapon.getShip(), shipToEp, ep.target.getMass() * amount * 0.2f);
                        }
                    }
                }
                electromagnetismProjList.removeAll(removeList);
            }
        }
    }

    public void getOut(CombatEntityAPI s){

    }

    public void init(CombatEngineAPI engine) {

    }

    @Override
    public float getRenderRadius() {
        return 1000000f;
    }

    @Override
    public EnumSet<CombatEngineLayers> getActiveLayers() {
        return EnumSet.of(CombatEngineLayers.ABOVE_SHIPS_LAYER);
    }

    @Override
    public void render(CombatEngineLayers layer, ViewportAPI viewport) {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (!engine.isPaused()) {
            if (engine.getCustomData().get(ELECTROMAGNETISM_LIST)!=null) {
                List<RC_ElectromagnetismOnHitEffect.ElectromagnetismProj> electromagnetismProjList = (List<RC_ElectromagnetismOnHitEffect.ElectromagnetismProj>) engine.getCustomData().get(ELECTROMAGNETISM_LIST);
                for (RC_ElectromagnetismOnHitEffect.ElectromagnetismProj ep : electromagnetismProjList) {
                    float targetFace = ep.anchoredEntity.getAnchor().getFacing();
                    float relativeAngle = ep.anchoredEntity.getRelativeAngle();
                    sprite.setAngle(relativeAngle + targetFace - 90);
                    sprite.renderAtCenter(ep.anchoredEntity.getLocation().getX(), ep.anchoredEntity.getLocation().getY());
                }
            }
        }
    }

    @Override
    public boolean isExpired() {
        return false;
    }

    public class Emp{
        WeaponAPI weapon;
        EmpArcEntityAPI empArcEntity;
        public Emp(WeaponAPI weapon,EmpArcEntityAPI empArcEntity)
        {
            this.weapon = weapon;
            this.empArcEntity = empArcEntity;
        }
    }
}