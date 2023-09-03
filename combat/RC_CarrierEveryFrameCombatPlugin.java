package real_combat.combat;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.input.InputEventType;
import com.fs.starfarer.api.loading.FighterWingSpecAPI;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.ui.FontException;
import org.lazywizard.lazylib.ui.LazyFont;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RC_CarrierEveryFrameCombatPlugin implements EveryFrameCombatPlugin {
	private CombatEngineAPI engine = Global.getCombatEngine();
	private final static Color COLOR = new Color(255, 255, 255, 255);
	private final static String FONT_FILE = "graphics/fonts/orbitron20aabold.fnt";
	private final static float SYSTEM_AMMO_FONT_SIZE = 20;
	private final static float THICKNESS = 3;
	private static List<Integer> keyList = new ArrayList<>();
	private Set<RC_Command> commandList= new HashSet<>();
	private ShipAPI lastShip;
	private LazyFont font;
	static {
		keyList.add(Keyboard.KEY_F1);
		keyList.add(Keyboard.KEY_F2);
		keyList.add(Keyboard.KEY_F3);
		keyList.add(Keyboard.KEY_F4);
		keyList.add(Keyboard.KEY_F5);
		keyList.add(Keyboard.KEY_F6);
		keyList.add(Keyboard.KEY_F7);
		keyList.add(Keyboard.KEY_F8);
		keyList.add(Keyboard.KEY_F9);
		keyList.add(Keyboard.KEY_F10);
		keyList.add(Keyboard.KEY_F11);
		keyList.add(Keyboard.KEY_F12);
	}

	public void advance(float amount, List<InputEventAPI> events) {
		/*
		if (engine == null) return;
		if (!engine.isPaused()) {
			//获取玩家的船
			ShipAPI player = engine.getPlayerShip();
			if (lastShip == null) {
				lastShip = player;
			}
			else if(!lastShip.equals(player)){
				//玩家开船 记录玩家开的船 如果和上一艘不一致取消上一搜的所有信标 重置AI
				for(FighterWingAPI w :lastShip.getAllWings()){
					w.getLeader().resetDefaultAI();
				}
				commandList.clear();
				lastShip = player;
			}

			//监听
			//有没有甲板
			if (!lastShip.hasLaunchBays()) {
				return;
			}
			//有没有舰载机
			if (lastShip.getAllWings().size()==0) {
				return;
			}
			//当玩家按下中键
			for (InputEventAPI event : events) {
				if (event.isConsumed()) {
					continue;
				}
				if (event.getEventType() == InputEventType.KEY_UP && keyList.indexOf(event.getEventValue())>-1){
					int index = keyList.indexOf(event.getEventValue());
					if (index<lastShip.getAllWings().size()) {
						FighterWingAPI fighterWing = lastShip.getAllWings().get(index);
						ShipAPI leader = fighterWing.getLeader();
						ShipAPI target = lastShip.getShipTarget();
						Vector2f targetLocation = player.getMouseTarget();
						CombatAssignmentType combatAssignmentType = CombatAssignmentType.RALLY_TASK_FORCE;
						if (target!=null) {
							targetLocation = target.getLocation();
							if (target.getOwner() == lastShip.getOwner()) {
								combatAssignmentType = CombatAssignmentType.DEFEND;
							}
							else if(target.getOwner()!=100){
								combatAssignmentType = CombatAssignmentType.ASSAULT;
							}
						}
						RC_Command command = new RC_Command(leader,target,targetLocation,combatAssignmentType);
						commandList.add(command);
					}
				}
			}
			if (lastShip!=null&&commandList.size()>0) {
				CombatFleetManagerAPI manager = engine.getFleetManager(lastShip.getOwner());
				CombatTaskManagerAPI task = manager.getTaskManager(false);
				for (RC_Command c : commandList) {

					//task.giveAssignment(
					//				engine.getFleetManager(c.ship.getOwner()).getDeployedFleetMember(c.ship),
					//				task.createAssignment(c.command, manager.createWaypoint(c.location, false), false),
					//				false);


				}
				commandList.clear();
			}
		}
		*/

		//R锁定+F1进攻/护卫

		//红色进攻蓝色防守绿色前往
	}

	public void renderInUICoords(ViewportAPI viewport) {

	}

	public void init(CombatEngineAPI engine) {
		try {
			font = LazyFont.loadFont(FONT_FILE);
		} catch (FontException ex) {
			throw new RuntimeException("Failed to load font");
		}
	}

	private boolean hide(){
		if (engine == null || engine.getCombatUI() == null || engine.getPlayerShip() == null) return true;
		if(!engine.getPlayerShip().isAlive() || engine.getPlayerShip().isHulk()) return true;
		if (engine.getCombatUI().isShowingCommandUI() || engine.isUIShowingDialog()) return true;
		return !engine.isUIShowingHUD();
	}


	public void renderInWorldCoords(ViewportAPI viewport) {
		//if (engine.isPaused()) return;
		/*
		for (ShipAPI s:engine.getShips()) {
			if (s.getMouseTarget()!=null&&!s.equals(engine.getPlayerShip())) {
				drawArc(COLOR, viewport.getAlphaMult(), 361f, s.getMouseTarget(), SYSTEM_AMMO_FONT_SIZE * viewport.getViewMult(), 0f, 0f, 0f, 0f, THICKNESS);
			}
		}
		*/
	}

	public void processInputPreCoreControls(float amount, List<InputEventAPI> events) {

	}

	private void drawFont(String text, Color color, float aimAngle, float right, float up, float size){
		color = new Color(color.getRed(), color.getGreen(), color.getBlue());
		LazyFont.DrawableString toDraw = font.createText(text, color, size);
		ShipAPI playerShip = engine.getPlayerShip();
		Vector2f point = MathUtils.getPoint(playerShip.getLocation(),right,playerShip.getFacing()+aimAngle);
		point = MathUtils.getPoint(point,size/2,135F);
		point = MathUtils.getPoint(point,size/8,90F);
		toDraw.draw(
				point.getX(),
				point.getY()

		);
	}

	private void drawArc(Color color, float alpha, float angle, Vector2f loc, float radius, float aimAngle, float aimAngleTop, float x, float y, float thickness){
		GL11.glPushMatrix();
		//禁用纹理
		GL11.glDisable(GL11.GL_TEXTURE_2D);
		//抗锯齿
		GL11.glEnable(GL11.GL_LINE_SMOOTH);
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

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
		GL11.glPopMatrix();
	}

	public class RC_Command {
		public ShipAPI ship;
		public ShipAPI target;
		public Vector2f location;
		public CombatAssignmentType command;

		public RC_Command(ShipAPI ship, ShipAPI target, Vector2f location, CombatAssignmentType command) {
			this.ship = ship;
			this.target = target;
			this.location = location;
			this.command = command;
		}
	}
}
