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

import java.util.ArrayList;

import com.abstractedsheep.shuttletracker.json.EtaJson;
import com.abstractedsheep.shuttletracker.json.RoutesJson;
import com.abstractedsheep.shuttletracker.json.VehicleJson;

import android.app.Activity;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuItem;
import android.view.View;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.ExpandableListContextMenuInfo;

public class EtaActivity extends Activity implements IShuttleServiceCallback {
	
	private final static int MENU_REMOVE_FAV = 1;
	private final static int MENU_ADD_FAV = 2;
	private ArrayList<EtaJson> etas;
	private RoutesJson routes;
	private ShuttleDataService dataService;
	private ExpandableListView etaListView;
	private EtaListAdapter etaAdapter;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.eta);
		
		etaListView = (ExpandableListView) findViewById(R.id.eta_list);
		etaAdapter = new EtaListAdapter(this, getLayoutInflater());
		etaListView.setAdapter(etaAdapter);
		etaListView.setOnCreateContextMenuListener(new View.OnCreateContextMenuListener() {
			public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
					ExpandableListContextMenuInfo info = (ExpandableListContextMenuInfo) menuInfo;
					int type = ExpandableListView.getPackedPositionType(info.packedPosition);
					int group = ExpandableListView.getPackedPositionGroup(info.packedPosition);
					
					//Only create a context menu for child items
					if (type == 1) {
						if (group == 0 && etaAdapter.favoritesVisible())
							menu.add(0, MENU_REMOVE_FAV, 0, getString(R.string.remove_favorite));
						else
							menu.add(0, MENU_ADD_FAV, 0, getString(R.string.add_favorite));
						
					}		
			}			
		});
		
		dataService = ShuttleDataService.getInstance();
		routesUpdated(dataService.getRoutes());
	}
   
	public void dataUpdated(ArrayList<VehicleJson> vehicles, ArrayList<EtaJson> etas) {
		if (etas != null) {
			this.etas = etas;
			runOnUiThread(updateList);	
		}
	}
	
	private Runnable updateList = new Runnable() {
		public void run() {
			etaAdapter.putEtas(etas);
		}
	};
	
	private Runnable setRoutes = new Runnable() {
		public void run() {
			etaAdapter.setRoutes(routes);
			
			if (etaAdapter.favoritesVisible())
				etaListView.expandGroup(0);
		}
	};

	

	public void routesUpdated(RoutesJson routes) {
		if (routes != null) {
			this.routes = routes;
			runOnUiThread(setRoutes);
		}
	}

	public void dataServiceError(int errorCode) {
	}
	
	public boolean onContextItemSelected(MenuItem menuItem) {
		ExpandableListContextMenuInfo info = (ExpandableListContextMenuInfo) menuItem.getMenuInfo();
		int groupPos = -1;
		int childPos = -1;
		int type = ExpandableListView.getPackedPositionType(info.packedPosition);
		if (type == ExpandableListView.PACKED_POSITION_TYPE_CHILD) {
			groupPos = ExpandableListView.getPackedPositionGroup(info.packedPosition);
			childPos = ExpandableListView.getPackedPositionChild(info.packedPosition);
		}

		switch (menuItem.getItemId()) {
		case MENU_REMOVE_FAV:
			etaAdapter.removeFavorite(childPos);
			return true;
		case MENU_ADD_FAV:
			etaAdapter.addFavorite(groupPos, childPos);
			return true;
		default:
			return super.onContextItemSelected(menuItem);
		}
	}

}