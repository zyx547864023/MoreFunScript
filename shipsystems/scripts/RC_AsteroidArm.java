package real_combat.shipsystems.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.combat.*;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

import java.util.*;
import java.util.List;

public class RC_AsteroidArm extends BaseShipSystemScript {
    private final static String ID = "RC_AsteroidArm";
    private final static String IS_ON = "IS_ON";
    private final static String IS_SET = "IS_SET";
    private final static String WHO_CATCH = "WHO_CATCH";
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
            ship.getMutableStats().getFluxDissipation().modifyPercent(ID,100);
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
            ship.getMutableStats().getFluxDissipation().unmodifyPercent(ID);
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
        private List<CombatEntityAPI> hulkList = new ArrayList<>();
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
                for(CombatEntityAPI h:hulkList) {
                    if (!ship.getFluxTracker().isOverloaded()) {
                        Vector2f hulkLocation = h.getLocation();
                        Vector2f shipLocation = ship.getLocation();
                        float shipToHulkAngle = VectorUtils.getAngle(shipLocation, hulkLocation);
                        //h.setMass(h.getMass()*100);
                        h.getVelocity().set(MathUtils.getPoint(new Vector2f(0, 0), maxSpeed * 10000, shipToHulkAngle));
                    }
                    h.getCustomData().remove(WHO_CATCH);
                }
                hulkList.clear();
                return;
            }
            //搜寻船周围的残骸和陨石
            for(ShipAPI s:engine.getShips()){
                if(s.isHulk()) {
                    giveSpeed(s);
                }
            }
            for(CombatEntityAPI a:engine.getAsteroids())
            {
                giveSpeed(a);
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

        public void giveSpeed(CombatEntityAPI s){
            Vector2f shipLocation = ship.getLocation();
            Vector2f hulkLocation = s.getLocation();
            float shipToHulkAngle = VectorUtils.getAngle(shipLocation,hulkLocation);
            float hulkToshipAngle = VectorUtils.getAngle(hulkLocation,shipLocation);
            //获取完整距离
            float distance = MathUtils.getDistance(ship,s);
            float nowSpeed = maxSpeed*distance/ship.getCollisionRadius()+minSpeed*100;
            //间距在一个半径内0.5半径外
            if(distance<ship.getCollisionRadius()*100
                    &&distance>=ship.getCollisionRadius()*1.5
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
                        ShipAPI whoCatch = (ShipAPI) customData.get(s + WHO_CATCH);
                        try {
                            if (whoCatch == null) {
                                s.setCustomData(WHO_CATCH, ship);
                            }
                            if (ship.equals(customData.get(WHO_CATCH))) {
                                //向内移动
                                Vector2f newSpeed = MathUtils.getPoint(new Vector2f(0, 0), nowSpeed, hulkToshipAngle);
                                s.getVelocity().set(newSpeed);
                            }
                        } catch (Exception e) {
                            Global.getLogger(this.getClass()).info(e);
                        }
                    }
                }
            }
            //只有在内环有相切速度
            else if(distance<ship.getCollisionRadius()*1.5){
                //将所有在内环里面的东西加入List
                if(hulkList.indexOf(s)==-1)
                {
                    hulkList.add(s);
                    Map<String, Object> customData = s.getCustomData();
                    if(customData.get(ID+IS_SET)==null)
                    {
                        s.setCustomData(ID+IS_SET,true);
                        s.setHitpoints(s.getHitpoints()*3);
                        //s.setMass(s.getMass()*3);
                    }
                }
                //越外围给越多相切速度
                //获取相切速度方向
                float newAngle = MathUtils.clampAngle(hulkToshipAngle-90);
                s.getVelocity().set(ship.getVelocity());
                float addAngle = 0.1f;
                addAngle = ship.getCollisionRadius()*2/distance;
                if(addAngle>1)
                {
                    addAngle = 1f;
                }
                //越远转的越慢
                s.getLocation().set(MathUtils.getPointOnCircumference(shipLocation,distance+s.getCollisionRadius()+ship.getCollisionRadius(),
                        MathUtils.clampAngle(shipToHulkAngle+addAngle)
                ));
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

        }

        @Override
        public boolean isExpired() {
            return false;
        }
    }
}