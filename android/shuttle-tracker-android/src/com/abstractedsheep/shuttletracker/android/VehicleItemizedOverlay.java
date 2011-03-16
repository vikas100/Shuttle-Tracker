/* 
 * Copyright 2011 Austin Wagner
 *     
 * This file is part of Mobile Shuttle Tracker.
 *
 *  Mobile Shuttle Tracker is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Mobile Shuttle Tracker is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Mobile Shuttle Tracker.  If not, see <http://www.gnu.org/licenses/>.
 *  
 */

package com.abstractedsheep.shuttletracker.android;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
//import android.util.Log;
import android.view.View;
import android.view.ViewGroup.LayoutParams;

import com.abstractedsheep.shuttletracker.json.RoutesJson;
import com.abstractedsheep.shuttletracker.json.VehicleJson;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;
import com.google.android.maps.OverlayItem;
import com.google.android.maps.Projection;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.readystatesoftware.mapviewballoons.BalloonItemizedOverlay;
import com.readystatesoftware.mapviewballoons.BalloonOverlayView;

public class VehicleItemizedOverlay extends BalloonItemizedOverlay<DirectionalOverlayItem> {

	private static final int MAGENTA = Color.rgb(255, 0, 255);
	
	
	private Bitmap markerBitmap;
	private Bitmap markerBitmapFlipped;
	private HashMap<Integer, Bitmap> coloredMarkers = new HashMap<Integer, Bitmap>();
	private HashMap<Integer, Bitmap> coloredMarkersFlipped = new HashMap<Integer, Bitmap>();
	private HashMap<Integer, RoutesJson.Route> routes = new HashMap<Integer, RoutesJson.Route>();
	private BiMap<Integer, Integer> idToIndex = HashBiMap.create();
	private ArrayList<VehicleJson> vehicles = new ArrayList<VehicleJson>();
	private Drawable marker;
	private int visibleBalloon = -1;
	SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	SimpleDateFormat formatter2 = new SimpleDateFormat("MM/dd/yy h:mm:ss a");
	
	public VehicleItemizedOverlay(Drawable defaultMarker, MapView map) {
		super(boundCenter(defaultMarker), map);
		this.marker = boundCenter(defaultMarker);
		populate();
		
		Matrix flip = new Matrix();
		flip.reset();
		flip.setScale(-1.0f, 1.0f);

		markerBitmap = ((BitmapDrawable) marker).getBitmap();
		markerBitmapFlipped = Bitmap.createBitmap(markerBitmap, 0, 0, markerBitmap.getWidth(), markerBitmap.getHeight(), flip, true);
	}
	
	@Override
	protected boolean onTap(int index) {
		visibleBalloon = idToIndex.inverse().get(index);
		return super.onTap(index);
	}
	
	@Override
	public void hideBalloon() {
		visibleBalloon = -1;
		super.hideBalloon();
	}
	
	public void vehiclesUpdated() {
		BalloonOverlayView balloonView = getBalloonView();
		Integer vehicleId = idToIndex.get(visibleBalloon);
		if (balloonView != null && balloonView.isVisible() && vehicleId != null) {
			
			OverlayItem oi = createItem(vehicleId);
			balloonView.setData(oi);
			
			MapView.LayoutParams params = new MapView.LayoutParams(
					LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, oi.getPoint(),
					MapView.LayoutParams.BOTTOM_CENTER);
			params.mode = MapView.LayoutParams.MODE_MAP;
			
			balloonView.setVisibility(View.VISIBLE);
			balloonView.setLayoutParams(params);
		} else if (balloonView != null && balloonView.isVisible() && vehicleId == null) {
			hideBalloon();
		}
	}

	public synchronized void addVehicle(VehicleJson vehicle) {
		vehicles.add(vehicle);
		idToIndex.put(vehicle.getShuttle_id(), vehicles.size() - 1);
	    populate();
	}
	
	public synchronized void removeAllVehicles() {
		idToIndex.clear();
		vehicles.clear();
		populate();
	}
	
	public synchronized void putRoutes(List<RoutesJson.Route> routeList) {
		RoutesJson.Route route;
		for (int i = 0; i < routeList.size(); i++) {
			route = routeList.get(i);
			routes.put(route.getId(), route);
			
			coloredMarkers.put(route.getId(), recolorBitmap(markerBitmap, route.getColorInt()));
			coloredMarkersFlipped.put(route.getId(), recolorBitmap(markerBitmapFlipped, route.getColorInt()));
		}
	}

	@Override
	protected synchronized DirectionalOverlayItem createItem(int i) {
		VehicleJson v = vehicles.get(i);
		GeoPoint gp = new GeoPoint((int)(v.getLatitude() * 1e6), (int)(v.getLongitude() * 1e6));
		String updateTime = "";
		
		try {
			updateTime = "Last Updated at\n" + formatter2.format(formatter.parse(v.getUpdate_time()));
		} catch (ParseException e) {
			updateTime = v.getUpdate_time();
		}
		
		return new DirectionalOverlayItem(gp, v.getHeading(), v.getName(), updateTime);
	}

	@Override
	public int size() {
		return vehicles.size();
	}	
	
	@Override
	public synchronized void draw(Canvas canvas, MapView mapView, boolean shadow) {
		//long start = System.currentTimeMillis();
		Projection p = mapView.getProjection();
		Point pt;		
		Matrix rotate = new Matrix();
		Bitmap tempBitmap;
		long now;
		Date lastUpdate;
			
		for (VehicleJson v : vehicles) {
			try {				
				now = (new Date()).getTime();
				lastUpdate = formatter.parse(v.getUpdate_time());
		
				if ((now - lastUpdate.getTime()) > 60000)
					continue;
				
				GeoPoint gp = new GeoPoint((int)(v.getLatitude() * 1e6), (int)(v.getLongitude() * 1e6));
				pt = p.toPixels(gp, null);

				
				rotate.reset();
				
				
				
				if (v.getHeading() > 180) {
					tempBitmap = coloredMarkersFlipped.get(v.getRoute_id());
					rotate.postRotate(v.getHeading(), tempBitmap.getWidth() / 2, tempBitmap.getHeight() / 2);
					tempBitmap = Bitmap.createBitmap(tempBitmap, 0, 0, tempBitmap.getWidth(), tempBitmap.getHeight(), rotate, true);
				} else {
					tempBitmap = coloredMarkers.get(v.getRoute_id());
					rotate.postRotate(v.getHeading(), tempBitmap.getWidth() / 2, tempBitmap.getHeight() / 2);
					tempBitmap = Bitmap.createBitmap(tempBitmap, 0, 0, tempBitmap.getWidth(), tempBitmap.getHeight(), rotate, true);
				}
				
				canvas.drawBitmap(tempBitmap, pt.x - (tempBitmap.getWidth() / 2), pt.y - (tempBitmap.getHeight() / 2), null);
				
				
			} catch (ParseException e) {
				e.printStackTrace();
			}
		}	
		
		//Log.d("Tracker", "Shuttle drawing complete in " + String.valueOf(System.currentTimeMillis() - start) + "ms");
	}
	
	private Bitmap recolorBitmap(Bitmap bitmap, int color) {
		Bitmap b = bitmap.copy(Config.ARGB_8888, true);
		for (int i = 0; i < b.getWidth(); i++) {
			for (int j = 0; j < b.getHeight(); j++) {
				if (b.getPixel(i, j) == MAGENTA)
					b.setPixel(i, j, color);
			}
		}
		return b;
	}
}
