package real_combat.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponSize;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponType;
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;
import com.fs.starfarer.api.combat.listeners.WeaponRangeModifier;
import org.lwjgl.util.vector.Vector2f;
import real_combat.combat.MonsterBallOnHitEffect;

import java.util.*;

/**
 * �����ܵ������ ckem_hullmod
 */
public class CKEM_Hullmod extends BaseHullMod {
    public String ID = "CKEM_Plugin";
    public CombatEngineAPI engine;
    public ShipAPI ship;
    @Override
    public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {

    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        engine = Global.getCombatEngine();
        if(engine!=null) {
            CKEM_Plugin cemPlugin = new CKEM_Plugin();
            engine.addLayeredRenderingPlugin(cemPlugin);
            this.ship = ship;
            this.ID = ship.getId();
        }
    }

    class CKEM_Plugin implements CombatLayeredRenderingPlugin
    {
        @Override
        public void init(CombatEntityAPI entity) {
            engine.getCustomData().put(ID, new LocalData());
        }

        @Override
        public void cleanup() {

        }

        @Override
        public boolean isExpired() {
            return false;
        }

        @Override
        public void advance(float amount) {
            if(Global.getCombatEngine().isPaused()) return;
            if(!ship.getShield().isOn()) return;
            if(!engine.getCustomData().containsKey(ID)) return;
            LocalData localData = (LocalData)engine.getCustomData().get(ID);
            //�洢������һ֡��λ��
            Map<MissileAPI, MineData> mineData = localData.mineData;

            //��ȡ���з�����
            List<DamagingProjectileAPI> damagingProjectiles = engine.getProjectiles();
            List<MissileAPI> keMissileList = new ArrayList<>();
            for(DamagingProjectileAPI damagingProjectile : damagingProjectiles){
                if (!(damagingProjectile instanceof MissileAPI)) continue;
                MissileAPI missile = (MissileAPI)damagingProjectile;
                if(missile.getOwner()!=1){
                    mineData.remove(missile);
                    continue;
                }
                if (!engine.isEntityInPlay(missile) ||
                        missile.getProjectileSpecId() == null ||
                        missile.didDamage() ||
                        missile.isFading())
                {
                    mineData.remove(missile);
                    continue;
                }
                //�����Ƕ��ܵ���
                if(DamageType.KINETIC.equals(missile.getDamageType()))
                {
                    Vector2f now = missile.getLocation();
                    Vector2f shipLocation = ship.getLocation();
                    //������λ���Ƿ��봬Խ��Խ��
                    if(mineData.get(missile)!=null) {
                        Vector2f from = mineData.get(missile).lastLocation;
                        float fromToShip = getVector(from, shipLocation).length() - ship.getCollisionRadius();
                        float nowToShip = getVector(now, shipLocation).length() - ship.getCollisionRadius();
                        //������λ���Ƿ��봬Խ��Խ��
                        if (nowToShip<=fromToShip)
                        {
                            keMissileList.add(missile);
                            mineData.put(missile,new MineData(now));
                        }
                        else
                        {
                            mineData.remove(missile);
                        }
                        //
                    }
                    //���û�д�������
                    else {
                        mineData.put(missile,new MineData(now));
                    }
                }
            }
            //ѭ��������������
            Comparator<MissileAPI> missileAPIComparator = new Comparator<MissileAPI>(){

                @Override
                public int compare(MissileAPI m1, MissileAPI m2) {
                    Vector2f m1Location = m1.getLocation();
                    Vector2f m2Location = m2.getLocation();
                    Vector2f shipLocation = ship.getLocation();
                    float m1ToShip = getVector(m1Location, shipLocation).length() - ship.getCollisionRadius();
                    float m2ToShip = getVector(m2Location, shipLocation).length() - ship.getCollisionRadius();
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
            }
            if(lately==null)return;
            //��ʼ����PD����
            for(WeaponAPI weapon:ship.getAllWeapons())
            {
                //�����PD����
                if(weapon.getSpec().getAIHints().contains(WeaponAPI.AIHints.PD))
                {
                    Vector2f missileLocation = lately.getLocation();
                    Vector2f weaponLocation = weapon.getLocation();
                    //��������������
                    float weaponToMissile = getVector(missileLocation, weaponLocation).length();
                    if(weaponToMissile<=weapon.getRange())
                    {
                        //��ȡ��ǰ�Ƕ�
                        float now = weapon.getCurrAngle();
                        //���㵼���������ĽǶ�
                        //���Ǻ���
                        //float weaponToMissileX = weaponLocation.x - missileLocation.x;
                        //float weaponToMissileY = weaponLocation.y - missileLocation.y;
                        //Double targetAngle = getAngle(weaponToMissileX,weaponToMissile,weaponToMissileY);
                        //float targetAngle = getAngle(weaponLocation,missileLocation);
                        //Double angle=(targetAngle*180/Math.PI)%360;
                        Double targetAngle = calcAngle(weaponLocation.x,weaponLocation.y,missileLocation.x,missileLocation.y);
                        targetAngle = targetAngle+90;
                        if(targetAngle>=360){targetAngle=targetAngle-360;}
                        if(Math.abs(now-targetAngle)<=weapon.getTurnRate()*amount)
                        {
                            weapon.setCurrAngle(targetAngle.floatValue());
                        }
                        else {
                            //��ʱ��
                            float angleL = now+weapon.getTurnRate()*amount;
                            if(angleL>=360){angleL=angleL-360;}
                            //˳ʱ��
                            float angleR = now-weapon.getTurnRate()*amount;
                            if(angleR>=360){angleR=angleR-360;}
                            weapon.setCurrAngle(angleCompare(angleL,angleR,targetAngle.floatValue()));
                        }
                    }
                }
            }
        }

        @Override
        public EnumSet<CombatEngineLayers> getActiveLayers() {
            return null;
        }

        @Override
        public float getRenderRadius() {
            return 0;
        }

        @Override
        public void render(CombatEngineLayers layer, ViewportAPI viewport) {

        }
    }


    private final static class LocalData{
        private final Map<MissileAPI, MineData> mineData = new HashMap<>();

    }

    private final static class MineData{
        private Vector2f lastLocation;
        MineData(Vector2f lastLocation){
            this.lastLocation = new Vector2f(lastLocation.x,lastLocation.y);
        }

    }

    private Vector2f getVector(Vector2f from, Vector2f to){
        return new Vector2f(to.x - from.x, to.y - from.y);
    }

    public float getAngle(Vector2f str,Vector2f target) {
        Double angle = new Double(0);
        double cos = new Double(0);
        if((str.y>target.y&&str.x<=target.x)||(str.y<=target.y&&str.x<=target.x))
        {
            cos =(str.y-target.y)/(Math.sqrt(Math.pow(target.y-str.y,2)+Math.pow(target.x-str.x,2)));
            angle = Math.acos((str.y-target.y)/(Math.sqrt(Math.pow(target.y-str.y,2)+Math.pow(target.x-str.x,2))));
        }
        else if((str.y>target.y&&str.x>target.x)||(str.y<=target.y&&str.x>target.x))
        {
            cos =(str.y-target.y)/(Math.sqrt(Math.pow(target.y-str.y,2)+Math.pow(target.x-str.x,2)));
            angle = -Math.acos((str.y-target.y)/(Math.sqrt(Math.pow(target.y-str.y,2)+Math.pow(target.x-str.x,2))));
        }
        return angle.floatValue();
    }

    /**
     *
     * @param a ��1
     * @param b ��2
     * @param c Ŀ��Ƕ�
     * @return
     */
    public float angleCompare(float a, float b, float c)
    {

        //����a��c����С�н�
        float min1=999;
        if(a<=0&&c>0)
        {
            //�ж����Ǳ�ת��һ��
            if(360+a-c>-a+c)
                min1=-a+c;
            else
                min1=360+a-c;
        }
        else if(c<=0&&a>0)
        {
            if(360+c-a>a-c)
                min1=a-c;
            else
                min1=360+c-a;
        }
        else if(c>0&&a>0)
        {
            if(a>c)
                min1=a-c;
            else
                min1=c-a;
        }
        else if(c<=0&&a<=0)
        {
            if(a>c)
                min1=a-c;
            else
                min1=c-a;
        }
        //����b��c����С�н�
        float min2=999;
        if(b<=0&&c>0)
        {
            //�ж����Ǳ�ת��һ��
            if(360+b-c>-b+c)
                min2=-b+c;
            else
                min2=360+b-c;
        }
        else if(c<=0&&b>0)
        {
            if(360+c-b>b-c)
                min2=-b+c;
            else
                min2=360+b-c;
        }
        else if(c>0&&b>0)
        {
            if(b>c)
                min2=b-c;
            else
                min2=c-b;
        }
        else if(c<=0&&b<=0)
        {
            if(b>c)
                min2=b-c;
            else
                min2=c-b;
        }
        if(min1>min2)
        {
            //console.log("execb");
            return b;
        }
        else
        {
            //console.log("execa");
            return a;
        }

    }
    public static double calcAngle(float centerLat, float centerLon, float anotherLat, float anotherLon) {
        //��ֵ
        double subLat = anotherLat - centerLat;
        double subLon = anotherLon - centerLon;
        double angle = 0;

        if (subLat == 0) {
            //γ�Ȳ�ֵΪ0 ��ʾ������ͬһ�߶� ��ʱ���Ȼ��x���Ҳ� ���� x�����
            if (subLon > 0) {
                //x���Ҳ�
                angle = 0;
            } else if (subLon < 0) {
                //x�����
                angle = 180;
            }
        } else if (subLon == 0) {
            //���Ȳ�ֵΪ0 ˵������y���Ϸ�����y���·�
            if (subLat > 0) {
                //y���Ϸ�
                angle = 270;
            } else if (subLat < 0) {
                //y���·�
                angle = 90;
            }
        } else {
            //����tan��ֵ����Ƕ� subLon����Ϊ0  γ�Ȳ�ֵ ���� ���Ȳ�ֵ = tan��ֵ
            double v = subLat / subLon;
            angle = Math.atan(v) * 180 / Math.PI;
            System.out.println("δ����ĽǶ�ֵ:" + (angle));
            //�ڶ�����Ƕȵķ���
            //angle = Math.atan2(subLat,subLon) * 180 / PI  ;
            //�ж������ڵڼ�����
            //1���Ƕ�С��0 ˵��another���ڶ������ޣ�ֱ������ϵ�����½ǻ������Ͻǣ�
            if (angle < 0) {
                if (subLon >= 0) {
                    //��ʱ�ĵ������ĵ�����½� ȡ����ֵ
                    angle = Math.abs(angle);
                } else if (subLon < 0) {
                    //��ʱ�ĵ������ĵ�����Ͻ� ȡ����ֵ �ټ�180
                    angle = Math.abs(angle) + 180;
                }
            }
            //2���Ƕȴ���0 ˵��another����һ�����ޣ�ֱ������ϵ�����Ͻǻ������½ǣ�
            else if (angle > 0) {
                if (subLat > 0) {
                    //��ʱ�ĵ������ĵ�����Ͻ�  360-angle
                    angle = 360 - angle;
                } else if (subLat < 0) {
                    // ��ʱ�ĵ������ĵ�����½�
                    angle += 90;
                }
            }
        }
        return angle;
    }
    /*
    public static void main(String[] args) {
        String angle = AngleService.calcAngle(30.0, 111.0, 22.0, 111.0);
        System.out.println("�����ĽǶ�ֵ��" + angle);
    }
    */
}
