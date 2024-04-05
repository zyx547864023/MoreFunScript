package real_combat.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;
import real_combat.combat.RC_CamouflageNetRenderingPlugin;
import real_combat.entity.RC_AnchoredEntity;

import java.util.ArrayList;
import java.util.List;


public class RC_CamouflageNetOnHitEffect implements OnHitEffectPlugin {
    private final static String ID="RC_CamouflageNetOnHitEffect";
    private final static String CAMOUFLAGENET_LIST="CAMOUFLAGENET_LIST";
    private List<CamouflageNet> camouflageNetList = new ArrayList<>();
    public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target,
                      Vector2f point, boolean shieldHit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {
        if (target instanceof MissileAPI) {
            Global.getLogger(this.getClass()).info(((MissileAPI)target).getWeapon().getDisplayName());
        }
        //相对位置
        float targetFace = target.getFacing();
        float landingFace = projectile.getFacing();
        float relativeAngle = MathUtils.getShortestRotation(targetFace,landingFace);
        RC_AnchoredEntity a = new RC_AnchoredEntity(target,point,relativeAngle);
        //存放剩余时间
        //存放命中的物体
        //存放是否命中盾
        //存放发射的武器
        if(engine.getCustomData().get(CAMOUFLAGENET_LIST)!=null)
        {
            camouflageNetList = (List<CamouflageNet>) engine.getCustomData().get(CAMOUFLAGENET_LIST);
        }
        camouflageNetList.add(new CamouflageNet(point,6,0,0.1f,targetFace,a,10,projectile));
        engine.getCustomData().put(CAMOUFLAGENET_LIST,camouflageNetList);
        float radius = 0;
        for (int index=1;index<6;index++) {
            radius+=index * RC_CamouflageNetRenderingPlugin.DISTANCE;
        }
        for (MissileAPI m:AIUtils.getNearbyEnemyMissiles(projectile,radius)) {
            engine.applyDamage(m,m.getLocation(),0,DamageType.ENERGY,projectile.getEmpAmount(),false,true,projectile.getSource(),true);
        }
        for (ShipAPI s:AIUtils.getNearbyEnemies(projectile,radius)) {
            engine.applyDamage(s,s.getLocation(),0,DamageType.ENERGY,projectile.getEmpAmount(),false,true,projectile.getSource(),true);
        }
    }

    public class CamouflageNet{
        //位置
        public Vector2f location;
        //阶段
        public int allStage;
        //阶段
        public int nowStage;
        //剩余时间
        public float time;
        //起始角度
        public float startAngle;
        //锚点
        public RC_AnchoredEntity anchoredEntity;
        //多少根线
        public int line;
        //本体
        public DamagingProjectileAPI projectile;
        public CamouflageNet(Vector2f location,int allStage,int nowStage,float time,float startAngle,RC_AnchoredEntity anchoredEntity,int line,DamagingProjectileAPI projectile)
        {
            this.location = location;
            this.allStage = allStage;
            this.nowStage = nowStage;
            this.time = time;
            this.startAngle = startAngle;
            this.anchoredEntity = anchoredEntity;
            this.line = line;
            this.projectile = projectile;
        }
    }
}
