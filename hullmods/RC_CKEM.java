package real_combat.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.List;
import java.util.*;

/**
 * 反动能导弹插件 ckem_hullmod
 */
public class RC_CKEM extends BaseHullMod {
    private String ID = "RC_CKEM";
    @Override
    public void advanceInCombat(final ShipAPI ship, float amount) {
        CombatEngineAPI engine =  Global.getCombatEngine();

        //改造
        Map<MissileAPI, Vector2f> missileLocationMap = new HashMap<>();
        if (engine.getCustomData().get(ship + ID) != null) {
            missileLocationMap = (Map<MissileAPI, Vector2f>) engine.getCustomData().get(ship + ID);
        }
        //存储导弹上一帧的位置
        //获取所有飞行物
        List<DamagingProjectileAPI> damagingProjectiles = engine.getProjectiles();
        List<MissileAPI> keMissileList = new ArrayList<>();
        //如果开盾找动能导弹，如果没开盾找最近导弹
        if(ship.getShield()!=null) {
            if (ship.getShield().isOn()) {
                for (DamagingProjectileAPI damagingProjectile : damagingProjectiles) {
                    if (!(damagingProjectile instanceof MissileAPI)) continue;
                    MissileAPI missile = (MissileAPI) damagingProjectile;
                    if (missile.getOwner() == ship.getOwner()) {
                        missileLocationMap.remove(missile);
                        continue;
                    }
                    if (!engine.isEntityInPlay(missile) ||
                            missile.getProjectileSpecId() == null ||
                            missile.didDamage() ||
                            missile.isFading()) {
                        missileLocationMap.remove(missile);
                        continue;
                    }
                    //导弹是动能导弹
                    if (DamageType.KINETIC.equals(missile.getDamageType())) {
                        setMissile(missile, ship, missileLocationMap, keMissileList);
                    }
                }
            }
        }

        engine.getCustomData().put(ship+ID,missileLocationMap);
        //循环导弹重新排序
        Comparator<MissileAPI> missileAPIComparator = new Comparator<MissileAPI>(){
            @Override
            public int compare(MissileAPI m1, MissileAPI m2) {
                Vector2f m1Location = m1.getLocation();
                Vector2f m2Location = m2.getLocation();
                Vector2f shipLocation = ship.getLocation();
                float m1ToShip = MathUtils.getDistance(m1, ship);
                float m2ToShip = MathUtils.getDistance(m2, ship);
                if(m1ToShip>m2ToShip)
                {
                    return 1;
                }
                return 0;
            }
        };
        Collections.sort(keMissileList,missileAPIComparator);
        MissileAPI lately = null;
        if(keMissileList.size()!=0) {
            lately = keMissileList.get(0);
            ViewportAPI viewport = engine.getViewport();
            float alphaMult = viewport.getAlphaMult();
            GL11.glMatrixMode(GL11.GL_PROJECTION);
            GL11.glPushMatrix();
            GL11.glLoadIdentity();
            GL11.glOrtho(viewport.getLLX(), viewport.getLLX() + viewport.getVisibleWidth(), viewport.getLLY(),
                    viewport.getLLY() + viewport.getVisibleHeight(), -1,
                    1);
            GL11.glMatrixMode(GL11.GL_MODELVIEW);
            GL11.glPushMatrix();
            GL11.glLoadIdentity();
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glEnable(GL11.GL_LINE_SMOOTH);
            GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL11.glTranslatef(0.01f, 0.01f, 0);

            drawArc(new Color(255,155,255,75), alphaMult, 360f, lately.getLocation(), lately.getCollisionRadius(), 0f, 0f, 0f, 0f, 2f / viewport.getViewMult());

            GL11.glDisable(GL11.GL_BLEND);
            GL11.glMatrixMode(GL11.GL_MODELVIEW);
            GL11.glPopMatrix();
            GL11.glMatrixMode(GL11.GL_PROJECTION);
            GL11.glPopMatrix();
            GL11.glPopAttrib();
        }
        if(lately==null)return;
        if(engine.isPaused()) return;
        ShipAPI target = ship.getShipTarget();
        ship.setShipTarget(null);
        //开始调用PD武器
        for(WeaponAPI weapon:ship.getAllWeapons())
        {
            //如果是PD武器
            if(weapon.getSpec().getAIHints().contains(WeaponAPI.AIHints.PD))
            {
                Vector2f missileLocation = lately.getLocation();
                Vector2f weaponLocation = weapon.getLocation();
                //如果导弹在射程和射角内
                float weaponToMissileAngle = VectorUtils.getAngle(weaponLocation,missileLocation);
                float arcFacing =  MathUtils.clampAngle(weapon.getArcFacing()+ship.getFacing());
                if(Math.abs(MathUtils.getShortestRotation(arcFacing,weaponToMissileAngle))<weapon.getArc()/2)
                {
                    //如果导弹在射程内
                    float weaponToMissile = MathUtils.getDistance(weaponLocation, missileLocation);
                    if(weaponToMissile<=weapon.getRange())
                    {
                        //获取当前角度
                        float now = weapon.getCurrAngle();
                        float weaponToMissileRotation = MathUtils.getShortestRotation(now,weaponToMissileAngle);
                        if(Math.abs(weaponToMissileRotation)<=weapon.getTurnRate()*amount)
                        {
                            weapon.setCurrAngle(weaponToMissileAngle);
                        }
                        else {
                            if(weaponToMissileRotation<0)
                            {
                                weapon.setCurrAngle(now-weapon.getTurnRate()*amount);
                            }
                            else {
                                weapon.setCurrAngle(now+weapon.getTurnRate()*amount);
                            }
                        }
                    }
                }
            }
        }
        //ship.setShipTarget(target);
    }
    private void drawArc(Color color, float alpha, float angle, Vector2f loc, float radius, float aimAngle, float aimAngleTop, float x, float y, float thickness){
        GL11.glLineWidth(thickness);
        GL11.glColor4ub((byte) color.getRed(), (byte) color.getGreen(), (byte) color.getBlue(), (byte)Math.max(0, Math.min(Math.round(alpha * 255f), 255)) );
        GL11.glBegin(GL11.GL_LINE_STRIP);
        for(int i = 0; i < Math.round(angle); i++){
            GL11.glVertex2f(
                    loc.x + (radius * (float)Math.cos(Math.toRadians(aimAngleTop + i)) + x * (float)Math.cos(Math.toRadians(aimAngle - 90f)) - y * (float)Math.sin(Math.toRadians(aimAngle - 90f))),
                    loc.y + (radius * (float)Math.sin(Math.toRadians(aimAngleTop + i)) + x * (float)Math.sin(Math.toRadians(aimAngle - 90f)) + y * (float)Math.cos(Math.toRadians(aimAngle - 90f)))
            );
        }
        GL11.glEnd();
    }

    private void setMissile(MissileAPI missile,ShipAPI ship,Map<MissileAPI,Vector2f> missileLocationMap,List keMissileList)
    {
        Vector2f now = missile.getLocation();
        Vector2f shipLocation = ship.getLocation();
        //导弹的位置是否与船越来越近
        if (missileLocationMap.get(missile) != null) {
            Vector2f from = missileLocationMap.get(missile);
            float fromToShip = MathUtils.getDistance(from, shipLocation);
            float nowToShip = MathUtils.getDistance(now, shipLocation);
            //导弹的位置是否与船越来越近
            if (nowToShip <= fromToShip) {
                keMissileList.add(missile);
                missileLocationMap.put(missile, new Vector2f(now.getX(), now.getY()));
            } else {
                missileLocationMap.remove(missile);
            }
            //
        }
        //如果没有存入数据
        else {
            missileLocationMap.put(missile, new Vector2f(now.getX(), now.getY()));
        }
    }
    @Override
    public boolean isApplicableToShip(ShipAPI ship) {
        return (ship.getHullSize() == HullSize.CAPITAL_SHIP || ship.getHullSize() == HullSize.CRUISER) &&
                !ship.getVariant().getHullMods().contains("damaged_mounts") &&
                !ship.getVariant().getHullMods().contains("advancedoptics") &&
                !ship.getVariant().getHullMods().contains("armoredweapons");
    }
    public String getUnapplicableReason(ShipAPI ship) {
        if (ship.getVariant().getHullMods().contains("damaged_mounts")) {
            return "不兼容于 武器位损坏";
        }
        if (ship.getVariant().getHullMods().contains("advancedoptics")) {
            return "不兼容于 先进光学器件";
        }
        if (ship.getVariant().getHullMods().contains("armoredweapons")) {
            return "不兼容于 炮塔装甲";
        }
        return null;
    }

    @Override
    public void applyEffectsBeforeShipCreation(HullSize hullSize,
                                               MutableShipStatsAPI stats, String id) {
        /*
        if(stats.getFleetMember()!=null)
        {
            float deploymentPointsCost = stats.getFleetMember().getHullSpec().getSuppliesPerMonth();
            if(stats.getFleetMember().getVariant()!=null)
            {
                if (stats.getFleetMember().getVariant()!=null) {
                    try {
                        List<String> slotIds = stats.getFleetMember().getVariant().getModuleSlots();
                        for (String s : slotIds) {
                            //stats.getFleetMember().getVariant().setModuleVariant(s,Global.getSettings().getVariant("onslaught_Standard"));
                            ShipVariantAPI variant = stats.getFleetMember().getVariant().getModuleVariant(s);

                            stats.getFleetMember().getVariant().setSource(VariantSource.REFIT);
                            variant.setSource(VariantSource.REFIT);
                            ShipVariantAPI newVariant = Global.getSettings().getVariant("conquest_Standard");
                            newVariant.setSource(VariantSource.REFIT);
                            stats.getFleetMember().getVariant().setModuleVariant(s, newVariant);

                            deploymentPointsCost += variant.getHullSpec().getSuppliesPerMonth();
                        }
                    }
                    catch (Exception e)
                    {

                    }
                }
            }
            stats.getDynamic().getMod(Stats.DEPLOYMENT_POINTS_MOD).modifyMult(ID, deploymentPointsCost/stats.getFleetMember().getHullSpec().getSuppliesPerMonth());
        }
         */
    }
}
