package real_combat.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponSize;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponType;
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;
import com.fs.starfarer.api.combat.listeners.WeaponRangeModifier;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector2f;
import real_combat.combat.MonsterBallOnHitEffect;
import real_combat.util.RC_Util;

import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * �����ܵ������ ckem_hullmod
 */
public class CKEM_Hullmod extends BaseHullMod {
    private String ID = "CKEM_Plugin";
    @Override
    public void advanceInCombat(final ShipAPI ship, float amount) {
        CombatEngineAPI engine =  Global.getCombatEngine();

        //����
        Map<MissileAPI, Vector2f> missileLocationMap = new HashMap<>();
        if (engine.getCustomData().get(ship + ID) != null) {
            missileLocationMap = (Map<MissileAPI, Vector2f>) engine.getCustomData().get(ship + ID);
        }
        //�洢������һ֡��λ��
        //��ȡ���з�����
        List<DamagingProjectileAPI> damagingProjectiles = engine.getProjectiles();
        List<MissileAPI> keMissileList = new ArrayList<>();
        //��������Ҷ��ܵ��������û�������������
        if(ship.getShield().isOn()) {
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
                //�����Ƕ��ܵ���
                if (DamageType.KINETIC.equals(missile.getDamageType())) {
                    setMissile(missile,ship,missileLocationMap,keMissileList);
                }
            }
        }

        engine.getCustomData().put(ship+ID,missileLocationMap);
        //ѭ��������������
        Comparator<MissileAPI> missileAPIComparator = new Comparator<MissileAPI>(){
            @Override
            public int compare(MissileAPI m1, MissileAPI m2) {
                Vector2f m1Location = m1.getLocation();
                Vector2f m2Location = m2.getLocation();
                Vector2f shipLocation = ship.getLocation();
                float m1ToShip = MathUtils.getDistance(m1Location, shipLocation);
                float m2ToShip = MathUtils.getDistance(m2Location, shipLocation);
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
        //��ʼ����PD����
        for(WeaponAPI weapon:ship.getAllWeapons())
        {
            //�����PD����
            if(weapon.getSpec().getAIHints().contains(WeaponAPI.AIHints.PD))
            {
                Vector2f missileLocation = lately.getLocation();
                Vector2f weaponLocation = weapon.getLocation();
                //�����������̺������
                float weaponToMissileAngle = VectorUtils.getAngle(weaponLocation,missileLocation);
                float arcFacing =  MathUtils.clampAngle(weapon.getArcFacing()+ship.getFacing());
                if(Math.abs(MathUtils.getShortestRotation(arcFacing,weaponToMissileAngle))<weapon.getArc()/2)
                {
                    //��������������
                    float weaponToMissile = MathUtils.getDistance(weaponLocation, missileLocation);
                    if(weaponToMissile<=weapon.getRange())
                    {
                        //��ȡ��ǰ�Ƕ�
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
        //������λ���Ƿ��봬Խ��Խ��
        if (missileLocationMap.get(missile) != null) {
            Vector2f from = missileLocationMap.get(missile);
            float fromToShip = MathUtils.getDistance(from, shipLocation);
            float nowToShip = MathUtils.getDistance(now, shipLocation);
            //������λ���Ƿ��봬Խ��Խ��
            if (nowToShip <= fromToShip) {
                keMissileList.add(missile);
                missileLocationMap.put(missile, new Vector2f(now.getX(), now.getY()));
            } else {
                missileLocationMap.remove(missile);
            }
            //
        }
        //���û�д�������
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
            return "�������� ����λ��";
        }
        if (ship.getVariant().getHullMods().contains("advancedoptics")) {
            return "�������� �Ƚ���ѧ����";
        }
        if (ship.getVariant().getHullMods().contains("armoredweapons")) {
            return "�������� ����װ��";
        }
        return null;
    }
}
