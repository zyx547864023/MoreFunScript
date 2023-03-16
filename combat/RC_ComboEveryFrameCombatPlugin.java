package real_combat.combat;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;
import com.fs.starfarer.api.combat.listeners.WeaponRangeModifier;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.input.InputEventType;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.lwjgl.input.Keyboard;
import org.lwjgl.util.vector.Vector2f;
import real_combat.constant.RC_ComboConstant;

import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RC_ComboEveryFrameCombatPlugin implements EveryFrameCombatPlugin {
	private Color START_COLOR = new Color(0, 255, 0, 255);
	private Color OVER_COLOR = new Color(255, 0, 0, 255);
	private Color FINSH_COLOR = new Color(184, 134, 11, 255);
	private Color TEXT_COLOR = new Color(255, 255, 255, 255);
	private List<String> oldKeys = new ArrayList<>();
	private List<String> keys = new ArrayList<>();
	private float timer = 3f;
	private boolean isListening = false;
	private static int listeningKey = -1;
	private static boolean initialized = false;
	private static final String SETTINGS_FILE = "KEY.ini";

	public static void reloadSettings() throws IOException, JSONException {
		JSONObject settings = Global.getSettings().loadJSON(SETTINGS_FILE);
		JSONArray options = settings.getJSONArray("comboOptions");
		if (options.length() == 0) {
			return;
		}
		for (int i = 0; i < options.length(); i++) {
			JSONObject option = options.getJSONObject(i);
			listeningKey = option.optInt("listeningKey", -1);
		}
		initialized = true;
	}

	public void advance(float amount, List<InputEventAPI> events) {
		CombatEngineAPI engine = Global.getCombatEngine();
		//游戏没有暂停的时候
		if (!engine.isPaused()) {
			//获取玩家的船
			ShipAPI player = engine.getPlayerShip();
			if (!player.isAlive())
			{
				return;
			}
			if(player.getSystem()==null)
			{
				return;
			}
			if (!"combo".equals(player.getSystem().getId()))
			{
				return;
			}
			//如果系统在使用，清空出招表
			if (player.getSystem().isActive())
			{
				keys.clear();
				oldKeys.clear();
				isListening=false;
				return;
			}
			//当玩家按下中键
			for (InputEventAPI event : events) {
				if (event.isConsumed()) {
					continue;
				}
				if (!isListening) {
					if ((event.getEventType() == InputEventType.MOUSE_UP && event.getEventValue() == 2)||
							(event.getEventType() == InputEventType.KEY_UP && event.getEventValue() == listeningKey)
					) {
						isListening = true;
						timer = 3f;
						//渲染进度条和文字
						engine.getCombatUI().addMessage(0, START_COLOR,
								"请输入您的指令!");
					}
				}
				else
				{
					if (event.getEventType() == InputEventType.MOUSE_UP && event.getEventValue() == 2||
							(event.getEventType() == InputEventType.KEY_UP && event.getEventValue() == listeningKey)
					) {
						keys.clear();
						oldKeys.clear();
						engine.getCombatUI().addMessage(0, OVER_COLOR,
								"指令取消！");
						isListening = false;
						player.removeCustomData("Combo");
						player.setShipSystemDisabled(true);
					}
				}
				//是否在计时中
				if (isListening) {
					//记录WSAD
					if (event.getEventType() == InputEventType.KEY_UP) {
						//是否超过四个按键
							if (event.getEventValue() == Keyboard.KEY_W) {
								if (keys.size()<3) {
									keys.add("W");
								}
							}
							if (event.getEventValue() == Keyboard.KEY_S) {
								if (keys.size()<3) {
									keys.add("S");
								}
							}
							if (event.getEventValue() == Keyboard.KEY_A) {
								if (keys.size()<3) {
									keys.add("A");
								}
							}
							if (event.getEventValue() == Keyboard.KEY_D) {
								if  (keys.size()<3) {
									keys.add("D");
								}
							}
						}
					}
			}
			if (!keys.equals(oldKeys)&&keys.size()!=0){
				String comboString = "";
				for (String key : keys)
				{
					comboString+=key;
				}
				//
				if (keys.size()==3)
				{
					if(RC_ComboConstant.SKILL_CHINESE.get(comboString)!=null)
					{
						engine.getCombatUI().addMessage(0, FINSH_COLOR,
								RC_ComboConstant.SKILL_CHINESE.get(comboString)+"准备就绪！");
						player.setCustomData("Combo",comboString);
						timer = 999f;
						player.setShipSystemDisabled(false);
					}
					else {
						keys.clear();
						oldKeys.clear();
						engine.getCombatUI().addMessage(0, OVER_COLOR,
								"指令错误！");
						isListening = false;
					}
				}
				else {
					engine.getCombatUI().addMessage(0, START_COLOR,
							"已输入指令："+comboString);
				}
				oldKeys.clear();
				oldKeys.addAll(keys);
			}
			//开始计时
			if(isListening)
			{
				timer-=amount;
				//是否计时已到
				if(timer<=0)
				{
					keys.clear();
					oldKeys.clear();
					engine.getCombatUI().addMessage(0, OVER_COLOR,
							"输入超时！");
					isListening = false;
					player.removeCustomData("Combo");
				}
			}

		}
	}

	public void renderInUICoords(ViewportAPI viewport) {
	}

	public void init(CombatEngineAPI engine) {
	}

	public void renderInWorldCoords(ViewportAPI viewport) {
		
	}

	public void processInputPreCoreControls(float amount, List<InputEventAPI> events) {
		
	}
}
