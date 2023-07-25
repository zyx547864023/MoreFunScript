package real_combat.combat;


import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.graphics.SpriteAPI;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector2f;
import real_combat.ai.RC_Drone_borerAI;
import real_combat.util.MyMath;

import java.awt.*;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

public class RC_HyperionEngineLayeredRenderingPlugin extends BaseCombatLayeredRenderingPlugin {
    private final static String ID = "RC_HyperionEngineLayeredRenderingPlugin";
    private final static String HYPERION_ULTIMATE = "hyperion_ultimate";

    private Map<ShipEngineControllerAPI.ShipEngineAPI,Vector2f> engineAPIVector2fMap = new HashMap<>();
    public RC_HyperionEngineLayeredRenderingPlugin() {}

    @Override
    public void advance(float amount) {
        CombatEngineAPI engine = Global.getCombatEngine();
        for (ShipAPI s:engine.getShips()) {
            if (HYPERION_ULTIMATE.equals(s.getHullSpec().getHullId().replace("_default_D", ""))) {
                ShipEngineControllerAPI shipEngineControllerAPI = s.getEngineController();
                for (ShipEngineControllerAPI.ShipEngineAPI e:shipEngineControllerAPI.getShipEngines()) {
                    if (s.getSystem().isActive()) {
                        if (engineAPIVector2fMap.get(e)!=null&&MathUtils.getRandomNumberInRange(0F,1F)>0.5F){
                            Vector2f lastVelocity = engineAPIVector2fMap.get(e);
                            Vector2f partstartloc = MathUtils.getPointOnCircumference(e.getLocation(), 30 * MathUtils.getRandomNumberInRange(0.1F, 0.2F), e.getEngineSlot().getAngle()+s.getFacing()+MathUtils.getRandomNumberInRange(-90F, 90F));
                            Vector2f partvec = new Vector2f(-lastVelocity.x+s.getVelocity().x,-lastVelocity.y+s.getVelocity().y);
                            float size = MathUtils.getRandomNumberInRange(1.0F, 2.0F);
                            float damage = MathUtils.getRandomNumberInRange(0.6F, 1.0F);
                            float brightness = MathUtils.getRandomNumberInRange(0.1F, 1F);
                            engine.addSmoothParticle(partstartloc, partvec, size, brightness, damage, Color.green);
                            //engine.addNegativeNebulaParticle(partstartloc, partvec, size, 1.5F, 0.25F , 0F, 1F, Color.green);
                            //engine.addSwirlyNebulaParticle(partstartloc, partvec, size, 1.5f, 0.25F, 0f, 1F, Color.green, false);
                        }
                        engineAPIVector2fMap.put(e,s.getVelocity());
                    }
                }
            }
        }
    }

    public void getOut(CombatEntityAPI s){

    }

    public void init(CombatEngineAPI engine) {
        engineAPIVector2fMap = new HashMap<>();
    }

    @Override
    public float getRenderRadius() {
        return 1000000f;
    }

    @Override
    public EnumSet<CombatEngineLayers> getActiveLayers() {
        return EnumSet.of(CombatEngineLayers.UNDER_SHIPS_LAYER);
    }

    @Override
    public void render(CombatEngineLayers layer, ViewportAPI viewport) {

    }

    @Override
    public boolean isExpired() {
        return false;
    }
}