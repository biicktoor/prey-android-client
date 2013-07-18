package com.prey.events.manager;

import org.json.JSONObject;

import android.content.Context;

import com.prey.PreyConfig;
import com.prey.PreyLogger;
import com.prey.events.Event;
import com.prey.events.retrieves.EventRetrieveDataBattery;
import com.prey.events.retrieves.EventRetrieveDataPrivateIp;
import com.prey.events.retrieves.EventRetrieveDataUptime;
import com.prey.events.retrieves.EventRetrieveDataWifi;
import com.prey.managers.PreyConnectivityManager;
import com.prey.managers.PreyWifiManager;
import com.prey.net.PreyWebServices;

public class EventManager {

	private EventMap<String, JSONObject> mapData = null;
	private Context ctx = null;
	private Event event = null;

	public final static String WIFI = "wifi";
	public final static String UPTIME = "uptime";
	public final static String PRIVATE_IP = "privateip";
	public final static String BATTERY = "battery";

	public EventManager(Context ctx) {
		this.ctx = ctx;
	}

	public void execute(Event event) {
		boolean isDeviceRegistered = isThisDeviceAlreadyRegisteredWithPrey(ctx);
		boolean isConnectionExists = false;
		boolean isOnline = false;
		
		String ssid=PreyWifiManager.getInstance(ctx).getSSID();
		if (ssid!=null&&!ssid.equals(PreyConfig.getPreyConfig(ctx).getPreviousSsid())){
			PreyConfig.getPreyConfig(ctx).setPreviousSsid(ssid);
			try {
				isConnectionExists = isConnectionExists(ctx);
				isOnline = isOnline(ctx);
			} catch (Exception e) {

			}
			// if This Device Already Registered With Prey
			if (isDeviceRegistered) {
				// if connection exists
				if (isConnectionExists) {
					// if there is connection, verify if online
					if (isOnline) {
						
						this.mapData = new EventMap<String, JSONObject>();
						this.event = event;
						this.mapData.put(EventManager.UPTIME, null);
						this.mapData.put(EventManager.WIFI, null);
						this.mapData.put(EventManager.PRIVATE_IP, null);
						this.mapData.put(EventManager.BATTERY, null);
						new EventRetrieveDataUptime().execute(ctx, this);
						new EventRetrieveDataWifi().execute(ctx, this);
						new EventRetrieveDataPrivateIp().execute(ctx, this);
						new EventRetrieveDataBattery().execute(ctx, this);
					}

				}
			}
		} 
		
		
	}

	public void receivesData(String key, JSONObject data) {
		mapData.put(key, data);
		if (mapData.isCompleteData()) {
			sendEvents();
		}
	}

	private void sendEvents() {
		if (mapData != null) {
			JSONObject jsonObjectStatus = mapData.toJSONObject();
			PreyLogger.i("jsonObjectStatus: " + jsonObjectStatus.toString());
			if (event != null) {
				if (isOnline(ctx)) {
					PreyLogger.i("event name[" + this.event.getName() + "], info[" + this.event.getInfo() + "]");
					PreyWebServices.getInstance().sendPreyHttpEvent(ctx, event, jsonObjectStatus);
				}
			}
		}
	}

	private boolean isOnline(Context ctx) {
		boolean isOnline = false;
		try {
			int i = 0;
			// wait at most 10 seconds
			while (!isOnline) {
				isOnline = PreyWifiManager.getInstance(ctx).isOnline();
				if (i < 10 && !isOnline) {
					PreyLogger.i("Phone doesn't have internet connection now. Waiting 1 secs for it");
					Thread.sleep(1000);
				}
				i++;
			}
		} catch (Exception e) {
			PreyLogger.e("Error, because:" + e.getMessage(), e);
		}
		return isOnline;
	}

	private boolean isConnectionExists(Context ctx) {
		boolean isConnectionExists = false;
		// There is wifi connexion?
		if (PreyConnectivityManager.getInstance(ctx).isWifiConnected()) {
			isConnectionExists = true;
		}
		// if there is no connexion wifi, verify mobile connection?
		if (!isConnectionExists && PreyConnectivityManager.getInstance(ctx).isMobileConnected()) {
			isConnectionExists = true;
		}
		return isConnectionExists;
	}

	private boolean isThisDeviceAlreadyRegisteredWithPrey(Context ctx) {
		return PreyConfig.getPreyConfig(ctx).isThisDeviceAlreadyRegisteredWithPrey();
	}

}
