package real_combat.shipsystems.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.AsteroidAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.combat.*;
import com.fs.starfarer.api.util.IntervalUtil;
import com.sun.javafx.image.BytePixelSetter;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.util.vector.Vector2f;
import real_combat.ai.RC_BaseShipAI;
import real_combat.entity.RC_AnchoredEntity;

import java.util.*;
import java.util.List;

/**
 * 每一个陨石有三个状态
 *
 * 脚抖动 获取最近的陨石移动到 船和模块夹角的位置前方 然后发射 移动速度和chargeup相关 发射速度1000
 *
 * 重写逻辑
 * 在外面 拉进圈
 * 到圈内 虹吸旋转
 * 吸进来甩出去吸进来
 * 相对位置固定
 * 一部分转圈
 */

public class RC_AsteroidArm extends BaseShipSystemScript {
    protected IntervalUtil tracker = new IntervalUtil(5f, 6f);
    private final static String ID = "RC_AsteroidArm";
    private final static String RC_AnchoredEntity = "RC_AnchoredEntity";
    private final static String IS_ON = "IS_ON";
    private final static String IS_SET = "IS_SET";
    public final static String WHO_CATCH = "WHO_CATCH";
    private final static String STATUS = "STATUS";
    private final static String RADIUS = "RADIUS";
    private static float maxSpeed = 10f;
    private static float minSpeed = 1f;
    private boolean init = false;
    private ShipAPI ship;
    CombatEngineAPI engine = Global.getCombatEngine();
    @Override
    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        if (engine == null) return;
        if (engine.isPaused()) {return;}
        ship = (ShipAPI) stats.getEntity();
        if (ship == null) {
            return;
        }
        try {
            ship.setCustomData(ID+IS_ON,true);
            //ship.getMutableStats().getFluxDissipation().modifyPercent(ID,100);
            ship.getMutableStats().getFighterRefitTimeMult().modifyPercent(ID,-100);
            if (!init) {
                init = true;
                Global.getCombatEngine().addLayeredRenderingPlugin(new RC_AsteroidsArmCombatPlugin(ship));
            }
        }
        catch (Exception e)
        {
            Global.getLogger(this.getClass()).info(e);
        }
    }

    @Override
    public void unapply(MutableShipStatsAPI stats, String id) {
        if (engine == null) return;
        if(engine.isPaused()) {return;}
        if (ship == null) {
            return;
        }
        try {
            ship.setCustomData(ID+IS_ON,false);
            //ship.getMutableStats().getFluxDissipation().unmodifyPercent(ID);
            ship.getMutableStats().getFighterRefitTimeMult().unmodifyPercent(ID);
        }
        catch (Exception e)
        {
            Global.getLogger(this.getClass()).info(e);
        }
    }

    public StatusData getStatusData(int index, State state, float effectLevel) {
        return null;
    }

    @Override
    public String getInfoText(ShipSystemAPI system, ShipAPI ship) {
        return null;
    }

    @Override
    public boolean isUsable(ShipSystemAPI system, ShipAPI ship) {
        return true;
    }

    public class RC_AsteroidsArmCombatPlugin extends BaseCombatLayeredRenderingPlugin {
        private final ShipAPI ship;
        private Set<CombatEntityAPI> hulkList = new HashSet<>();
        private CombatEngineAPI engine = Global.getCombatEngine();
        public RC_AsteroidsArmCombatPlugin(ShipAPI ship) {
            this.ship = ship;
        }

        @Override
        public void advance(float amount) {
            if (engine == null) return;
            if (engine.isPaused()) {return;}
            if (!ship.isAlive()) {return;}
            if (ship.getCustomData().get(ID+IS_ON)==null) {return;}
            if (!(boolean)ship.getCustomData().get(ID+IS_ON)) {
                int count = 0;
                for(CombatEntityAPI h:hulkList) {
                    if (!ship.getFluxTracker().isOverloaded()) {
                        Vector2f hulkLocation = h.getLocation();
                        Vector2f shipLocation = ship.getLocation();
                        float shipToHulkAngle = VectorUtils.getAngle(shipLocation, hulkLocation);
                        //h.setMass(h.getMass()*100);
                        h.getVelocity().set(MathUtils.getPoint(new Vector2f(0, 0), maxSpeed*100*MathUtils.getRandomNumberInRange(0.1f,2f), shipToHulkAngle));
                        float distance = MathUtils.getDistance(ship,h);
                        if (CollisionClass.NONE.equals(h.getCollisionClass())&&distance>0){
                            h.setCollisionClass(CollisionClass.ASTEROID);
                            count++;
                        }
                    }
                    h.getCustomData().remove(WHO_CATCH);
                }
                if (count==0) {
                    for (CombatEntityAPI h:hulkList) {
                        h.setCollisionClass(CollisionClass.ASTEROID);
                    }
                    hulkList.clear();
                }
                return;
            }
            tracker.advance(amount);
            if (tracker.intervalElapsed()) {
                //int size, float x, float y, float dx, float dy
                Vector2f spawnPonit = MathUtils.getRandomPointInCircle(ship.getLocation(), 0);
                Vector2f spawnVelocity = MathUtils.getRandomPointInCircle(new Vector2f(0, 0), ship.getCollisionRadius() / 4);
                CombatEntityAPI asteroid = engine.spawnAsteroid(MathUtils.getRandomNumberInRange(0, 3), spawnPonit.x, spawnPonit.y, spawnVelocity.x, spawnVelocity.y);
                asteroid.setCollisionClass(CollisionClass.NONE);
            }
            //搜寻船周围的残骸和陨石
            for (ShipAPI s : RC_BaseShipAI.getHulksOnMap(new HashSet<ShipAPI>())) {
                if (s.isHulk()) {
                    giveSpeed(s, amount);
                }
            }
            for (CombatEntityAPI a : engine.getAsteroids()) {
                giveSpeed(a, amount);
            }
        }

        public class TwoAngle{
            float leftAngle;
            float rightAngle;
            public TwoAngle(float leftAngle,float rightAngle){
                this.leftAngle = leftAngle;
                this.rightAngle = rightAngle;
            }
        }

        public TwoAngle getTwoAngle(CombatEntityAPI s){
            Vector2f hulkLocation = s.getLocation();
            Vector2f shipLocation = ship.getLocation();
            float shipToHulkAngle = VectorUtils.getAngle(shipLocation,hulkLocation);
            float hulkToshipAngle = VectorUtils.getAngle(hulkLocation,shipLocation);
            //获取完整距离
            float distance = MathUtils.getDistance(ship,s);
            //先拿到圆周上的点
            Vector2f midPoint = MathUtils.getPointOnCircumference(shipLocation,distance,shipToHulkAngle);
            Vector2f leftPoint = MathUtils.getPoint(midPoint,s.getCollisionRadius(),hulkToshipAngle-90);
            Vector2f rightPoint = MathUtils.getPoint(midPoint,s.getCollisionRadius(),hulkToshipAngle+90);
            //拿到两个角度
            float leftAngle = MathUtils.clampAngle(VectorUtils.getAngle(shipLocation,leftPoint));
            float rightAngle = MathUtils.clampAngle(VectorUtils.getAngle(shipLocation,rightPoint));

            return new TwoAngle(leftAngle,rightAngle);
        }

        public void giveSpeed(CombatEntityAPI s, float amount){
            Vector2f shipLocation = ship.getLocation();
            Vector2f hulkLocation = s.getLocation();
            float shipToHulkAngle = VectorUtils.getAngle(shipLocation,hulkLocation);
            float hulkToshipAngle = VectorUtils.getAngle(hulkLocation,shipLocation);
            //获取完整距离
            float distance = MathUtils.getDistance(ship,s);
            float nowSpeed = maxSpeed*distance/ship.getCollisionRadius()+minSpeed*10;
            //float nowSpeed = distance/ship.getCollisionRadius()+minSpeed*10;
            float radius = 1f;
            if (s.getCustomData().get(ID+RADIUS)!=null) {
                radius = (float) s.getCustomData().get(ID+RADIUS);
            }
            else {
                s.setCustomData(ID+RADIUS, MathUtils.getRandomNumberInRange(0.8f,1.5f));
            }
            //间距在一个半径内0.5半径外
            if(distance<ship.getCollisionRadius()*10
                    &&distance>=ship.getCollisionRadius()*0.25*radius
            )
            {
                hulkList.remove(s);
                TwoAngle newTwoAngle = getTwoAngle(s);//拿到两个角度
                //获取最小夹角
                float angle = Math.abs(MathUtils.getShortestRotation(newTwoAngle.leftAngle,newTwoAngle.rightAngle));
                //当前角度范围没有与完整角度范围重叠即可
                boolean notIn = true;
                //当360完全被包围时
                //没有被包围时剩余的角度
                /*
                for(CombatEntityAPI h:hulkList) {
                    TwoAngle oldTwoAngle = getTwoAngle(h);
                    //外面两个角和里面两个角是否有重叠
                    float aToC = Math.abs(MathUtils.getShortestRotation(newTwoAngle.leftAngle, oldTwoAngle.leftAngle));
                    float aToD = Math.abs(MathUtils.getShortestRotation(newTwoAngle.leftAngle, oldTwoAngle.rightAngle));
                    float bToC = Math.abs(MathUtils.getShortestRotation(newTwoAngle.rightAngle, oldTwoAngle.leftAngle));
                    float bToD = Math.abs(MathUtils.getShortestRotation(newTwoAngle.rightAngle, oldTwoAngle.rightAngle));
                    //A到C小于A到B
                    if (aToC <= angle
                            //A到D小于A到D
                            ||aToD <= angle
                            //B到C小与B到A
                            ||bToC <= angle
                            //B到D小于B到A
                            ||bToD <= angle)
                    {
                        notIn = false;
                    }
                }
                */
                if(notIn)
                {
                    Map<String, Object> customData = s.getCustomData();
                    if (customData!=null) {
                        ShipAPI whoCatch = (ShipAPI) customData.get(WHO_CATCH);
                        try {
                            if (whoCatch == null) {
                                s.setCustomData(WHO_CATCH, ship);
                            }
                            if (ship.equals(whoCatch)) {
                                //向内移动
                                Vector2f newSpeed = MathUtils.getPoint(new Vector2f(0, 0), nowSpeed, hulkToshipAngle);
                                s.getVelocity().set(newSpeed);
                                if (s.getCustomData().get(ID+STATUS)==null) {
                                    if (MathUtils.getRandomNumberInRange(0,2)==0) {
                                        s.setCustomData(ID+STATUS,"IN");
                                    }
                                    else {
                                        s.setCustomData(ID+STATUS,"OUT");
                                    }
                                }
                                if (distance<ship.getCollisionRadius()*2) {
                                    s.setCollisionClass(CollisionClass.ASTEROID);
                                    for (MissileAPI p : engine.getMissiles()) {
                                        //for (DamagingProjectileAPI p:engine.getProjectiles()) {
                                        if (ship.getOwner() == p.getOwner() && !p.isExpired() && !p.isFading()) {
                                            if (p.getCollisionRadius() > MathUtils.getDistance(p, s) || MathUtils.getDistance(p, s) == 0) {
                                                s.setCollisionClass(CollisionClass.NONE);
                                                break;
                                            }
                                        }
                                    }
                                }
                            }
                        } catch (Exception e) {
                            Global.getLogger(this.getClass()).info(e);
                        }
                    }
                }
            }
            //只有在内环有相切速度 //增加随机半径效果
            else if (distance<ship.getCollisionRadius()*0.25*radius) {
                Map<String, Object> customData = s.getCustomData();
                if (customData!=null) {
                    try {
                        if (!ship.equals(customData.get(WHO_CATCH))) {
                            return;
                        }
                    } catch (Exception e) {
                        Global.getLogger(this.getClass()).info(e);
                    }
                }
                //将所有在内环里面的东西加入List
                if(!hulkList.contains(s))
                {
                    hulkList.add(s);
                    customData = s.getCustomData();
                    if(customData.get(ID+IS_SET)==null)
                    {
                        s.setCustomData(ID+IS_SET,true);
                        s.setHitpoints(s.getHitpoints()*3);
                        //s.setMass(s.getMass()*3);
                    }
                }
                if (s.getCustomData().get(ID+STATUS)=="IN") {
                    s.setCollisionClass(CollisionClass.NONE);
                    //记录现在的位置
                    RC_AnchoredEntity anchoredEntity = new RC_AnchoredEntity(ship,new Vector2f(s.getLocation()),hulkToshipAngle);
                    if (s.getCustomData().get(ID+RC_AnchoredEntity)!=null){
                        anchoredEntity = (RC_AnchoredEntity)s.getCustomData().get(ID+RC_AnchoredEntity);
                    }
                    //s.getLocation().set(anchoredEntity.getLocation());
                    //s.getVelocity().set(MathUtils.getPoint(new Vector2f(0,0),nowSpeed,VectorUtils.getAngle(s.getLocation(),anchoredEntity.getLocation())));
                    if (distance>=ship.getCollisionRadius()*0.15) {
                        s.getVelocity().set(MathUtils.getPoint(new Vector2f(0, 0), distance, hulkToshipAngle));
                    }
                }
                else {
                    s.getVelocity().set(ship.getVelocity());
                    //越远转的越慢
                    s.getLocation().set(MathUtils.getPointOnCircumference(shipLocation,distance+s.getCollisionRadius()+ship.getCollisionRadius(),
                            MathUtils.clampAngle(shipToHulkAngle+0.5f)
                    ));
                    s.setCollisionClass(CollisionClass.ASTEROID);
                    for (DamagingProjectileAPI p:engine.getProjectiles()) {
                    //for (DamagingProjectileAPI p:engine.getProjectiles()) {
                        if (ship.getOwner() == p.getOwner()&&!p.isExpired()&&!p.isFading()) {
                            if (p.getCollisionRadius() > MathUtils.getDistance(p, s)||MathUtils.getDistance(p, s)==0) {
                                s.setCollisionClass(CollisionClass.NONE);
                                break;
                            }
                        }
                    }
                }
            }
        }

        public void getOut(CombatEntityAPI s){

        }

        public void init(CombatEngineAPI engine) {

        }

        @Override
        public float getRenderRadius() {
            return 0f;
        }

        @Override
        public EnumSet<CombatEngineLayers> getActiveLayers() {
            return EnumSet.of(CombatEngineLayers.ABOVE_SHIPS_LAYER);
        }

        @Override
        public void render(CombatEngineLayers layer, ViewportAPI viewport) {

        }

        @Override
        public boolean isExpired() {
            return false;
        }
    }
}