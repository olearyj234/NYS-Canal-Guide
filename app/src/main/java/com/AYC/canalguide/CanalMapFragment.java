package com.AYC.canalguide;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.xmlpull.v1.XmlPullParserException;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.AYC.canalguide.canalparser.CanalGuideXmlParser;
import com.AYC.canalguide.canalparser.MapMarker;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.GoogleMap.OnInfoWindowClickListener;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

public class CanalMapFragment extends MapFragment {

	public static final LatLng saratogaSprings = new LatLng(43.0616419,-73.7719178);
	public static final LatLng startLocation = saratogaSprings;
	public static final float startZoom = 8.0f;	// 8.0f is perfect
	
	private HashMap<String, String> xmlStrings, navInfoXmlStrings;
	private Activity activity;
	private Context context;
	
	private GoogleMap mMap;
	private PoiAdapter poiAdapter;
	
	private CameraPosition lastCameraPosition;
	
	public CanalMapFragment(){
		super();
		poiAdapter = new PoiAdapter();
	}
	
	public CanalMapFragment(HashMap<String, String> xmlStrings, Context context){
		super();
		this.xmlStrings = xmlStrings;
		this.context = context;
	}
	
    @Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
	    Bundle savedInstanceState) {
    	log("onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)");
		View view = super.onCreateView(inflater, container, savedInstanceState);
		activity = getActivity();
		
		this.activity = getActivity();
		this.xmlStrings = ((MainActivity) activity).getXmlStrings();
		this.context = getActivity().getApplicationContext();

		initMap();
		/*
		if(savedInstanceState != null)
			restoreSavedInstanceState(savedInstanceState);
		log("Instance ocv zoom = " + zoom);
		*/
		return view;
	}    
    
    // TODO
    /*
    @Override
	public void onSaveInstanceState(Bundle outState) {
    	//super.onSaveInstanceState(outState);
    	
		log("Saving InstanceState");
		outState.putDouble("CurLat", mMap.getCameraPosition().target.latitude);
		outState.putDouble("CurLon", mMap.getCameraPosition().target.longitude);
		outState.putFloat("CurZoom", mMap.getCameraPosition().zoom);
		log("InstanceState outstate.toString() = " + outState.toString());
	}
    
    public double lat = -1.0, lon;
    public float zoom = -1.0f;
    
    public void restoreSavedInstanceState(Bundle savedInstanceState){
    	log("Restoring InstanceState");
    	lat = (double) savedInstanceState.getDouble("CurLat");
    	lon = (double) savedInstanceState.getDouble("CurLon");
    	zoom = (float) savedInstanceState.getFloat("CurZoom");
    	CU = CameraUpdateFactory.newLatLngZoom(
        		new LatLng(lat, lon), zoom);
		log("InstanceState savedInstancestate.toString() = " + savedInstanceState.toString());
		log("InstanceState CU.to = " + CU.toString());
		log("Instance zoom = " + zoom);
    }
    */
    
    /**
     * When the app resumes, it will set the camera position
     */
    @Override
    public void onResume() {
        super.onResume();

        mMap = getMap();
        
        // If the app was never paused(freshly opened) move camera to default position
        if(lastCameraPosition == null)
	        // Move camera to area that includes POIs (hudson river)
	        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
	        		startLocation, startZoom));
        else
        	// Move camera to the position when the app was paused
        	mMap.moveCamera(CameraUpdateFactory.newCameraPosition(lastCameraPosition));
        
        // TODO
        /*
        log("Instance null??");
        if( CU != null){
        	log("Instance - MOVING MAP");
        	mMap.moveCamera(CU);
        }
        
        log("Instance null??   zoom = " + zoom);
        if( zoom != -1.0f){
        	log("Instance - MOVING MAP");
        	mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
            		new LatLng(lat, lon), zoom));
        }
		*/
    }
    
    /**
     * When the application is paused, save the last camera position so when
     * this is resumed, the map will be shown with the same camera position
     */
    @Override
    public void onPause() {
        super.onPause();
        
        lastCameraPosition = mMap.getCameraPosition();
    }
    
    protected void setCameraPositionToDefault(){
    	lastCameraPosition = null;
    }
    
    private void initMap(){
    	
    	mMap = getMap();
    	
    	// Initialize map options
        mMap.setMyLocationEnabled(true);
        
        // Set map type to what the user selected in the options
    	OptionsFragment optFrag = (OptionsFragment) ((MainActivity) activity).getOptionsFragment();
    	log("init setting MAP TYPE = " + optFrag.getMapType());
        mMap.setMapType(optFrag.getMapType());
        
        // Setting a custom info window adapter for the google map
        mMap.setInfoWindowAdapter(new CanalMapInfoWindowAdapter((MainActivity) activity));
        
        // Setting click listener so if a user clicks on a markers pop-up window, it will do something
        mMap.setOnInfoWindowClickListener(new OnInfoWindowClickListener(){
			@Override
			public void onInfoWindowClick(Marker marker) {
				MapMarker mapMarker = poiAdapter.getMapMarker(marker);
				log("MapMarker InfoWindow clicked: " + mapMarker.getName());
				
	            log("Creating the Marker Info Activity");
	            Intent intent = new Intent(context, MarkerInfoActivity.class);
	            intent.putExtra("MapMarker", mapMarker.cloneWithoutMarker());
	            startActivity(intent);
			}
		});
        
        if(poiAdapter.getCount() != 0)
        	addExistingMarkersToMap();
        else
        	parseXmlStringsAndAddMarkersToMap(xmlStrings);

        if(markersNotFilteredOut("navinfo") && navInfoXmlStrings == null && 
        		!poiAdapter.containsNavInfoMarkers()){
        		
    		// If the data isnt too old, use saved data
    		if(isSavedNavinfoDataValid()){
    	
    			navInfoXmlStrings = loadNavInfoXmlStrings();
    			parseXmlStringsAndAddMarkersToMap(navInfoXmlStrings);
    		
    		}
    		else{	// If the data is too old, re-download it to get latest update
    			
    			if( !((MainActivity) getActivity()).dowloadThreadPoolServiceRunning() )
    				((MainActivity) getActivity()).startDownloadThreadPoolService();
    			
    			else
    				Toast.makeText(getActivity(), "Downloading data for buoys", Toast.LENGTH_SHORT).show();
    		
    		}
    	}
    	// else if(navInfoXmlStrings != null)
    		// NavInfoMarkers were already in the poiAdapter and added from 
    		// the above method: addExistingMarkersToMap();

		log("PoiAdapter.size() = " + poiAdapter.getCount());
    }
    
    /**
     * 
     * @param xmlStrings
     */
    protected void parseXmlStringsAndAddMarkersToMap(HashMap<String, String> xmlStrings){
    	List<MapMarker> markerDataList = new ArrayList<MapMarker>();
		// For each xml document, get the data
    	Iterator<Map.Entry<String, String>> iterator = xmlStrings.entrySet().iterator();
	    while (iterator.hasNext()) {
	        Map.Entry<String, String> pairs = (Map.Entry<String, String>) iterator.next();
	        String currentURL = pairs.getKey();
	        String currentXmlString = pairs.getValue();
	        //iterator.remove(); // Avoids a ConcurrentModificationException
	        
			String currentXmlDocName = currentURL
					.replace("http://www.canals.ny.gov/xml/", "").replace(".xml", "");
			
				if(markersNotFilteredOut(currentXmlDocName)){
				
				try {
					markerDataList = new CanalGuideXmlParser(currentURL).parse(new StringReader(
							currentXmlString));
					log("Completed parsing for " + currentURL);
				} catch (XmlPullParserException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
				
				log("for url: " + currentURL + "size() = " + markerDataList.size());
				
				// Create each MapMarker that came from the xmlString from one URL and
				// add the marker to the map
				Marker marker;
				MarkerOptions markerOptions;
				
				for(MapMarker mapMarker : markerDataList){
					markerOptions = mapMarker.getMarkerOptions();
					if(markerOptions != null && mapMarker != null){
						marker = mMap.addMarker(markerOptions);	// Will get an error at this line on emulator
						
						mapMarker.setMarker(marker);
						poiAdapter.addItem(mapMarker);
					}
				}
			}
		}
    }
    
    private void addExistingMarkersToMap(){
    	log("Adding existing markers to the map. poiAdapter size = " + poiAdapter.getCount());
    	Marker marker;
		MarkerOptions markerOptions;
		
    	for(MapMarker mapMarker : poiAdapter){
    		if(markersNotFilteredOut(mapMarker)){
    			markerOptions = mapMarker.getMarkerOptions();
				
	    		if(markerOptions != null && mapMarker != null){
					marker = mMap.addMarker(markerOptions);	// Will get an error at this line on emulator
					
		    		mapMarker.setMarker(marker);
	    		}
    		}
    	}
    }
    
    private boolean markersNotFilteredOut(MapMarker mapMarker){
    	return markersNotFilteredOut(MapMarker.urlDocName(mapMarker));
    }
    
    private boolean markersNotFilteredOut(String urlDocName){
    	OptionsFragment optFrag = (OptionsFragment) ((MainActivity) activity).getOptionsFragment();
    	boolean[] switchValues = optFrag.getFilterData();
    	
    	if(switchValues == null){
    		if(urlDocName.contains("navinfo"))
    			return false;
    		else
    			return true;
    	}
    	else{
    		if(urlDocName.equals("locks"))
    			return switchValues[0];
    		else if(urlDocName.equals("marinas"))
    			return switchValues[1];
    		else if(urlDocName.equals("canalwatertrail"))
    			return switchValues[2];
    		else if(urlDocName.equals("liftbridges") || urlDocName.equals("guardgates"))
    			return switchValues[3];
    		else if(urlDocName.equals("boatsforhire"))
    			return switchValues[4];
    		if(urlDocName.contains("navinfo"))
    			return switchValues[5];
    		
    	}
    	return true;
    }
    
	/**
	 * This method will use the date that the data was last downloaded to determine 
	 * whether the data is too old.
	 * 
	 * @return true if the data isn't too old
	 */
	private boolean isSavedNavinfoDataValid(){
		Calendar calendar = Calendar.getInstance();
        Date lastValidDataDate = new Date(calendar.getTimeInMillis() - 
        		(getUpdateFrequency() * SplashActivity.DAY_IN_MILLISECONDS));
        Date lastDownloadDataDate = new Date(loadDataLastSavedDate());
        
        log("Last valid data date = " + lastValidDataDate);
        log("Last download data date = " + lastDownloadDataDate);
        
        if(lastDownloadDataDate.after(lastValidDataDate)){
        	log("Saved data is valid");
        	return true;
        } else {
        	log("Saved data is not valid");
        	return false;
        }
	}
	
	/**
	 * This will get the date that the data was last downloaded from the canal site
	 * 
	 * @return Date in milliseconds
	 */
	private long loadDataLastSavedDate(){
		log("Loading the date that the data was saved last");
	    SharedPreferences sharedPref = getActivity()
	    		.getSharedPreferences(SplashActivity.PREFS_NAME, SplashActivity.PREFS_MODE);
		
		return sharedPref.getLong(
				ThreadPoolDownloadService.NAV_INFO_DATA_LAST_SAVED_DATE_TAG, -1);
	}
	
	/**
     * This method will get the update frequency that was saved in the OptionsFragment
     * 
     * @return Update frequency in days
     */
    private int getUpdateFrequency(){
	    SharedPreferences sharedPref = getActivity()
	    		.getSharedPreferences(OptionsFragment.PREFS_NAME, SplashActivity.PREFS_MODE);
		return sharedPref.getInt(OptionsFragment.UPDATE_FREQ_KEY, 7);
    }
    
    private HashMap<String, String> loadNavInfoXmlStrings(){
		log("Loading navInfoXmlStrings");
	    SharedPreferences xmlStringsPref = getActivity()
	    		.getSharedPreferences(SplashActivity.PREFS_NAME, SplashActivity.PREFS_MODE);
		HashMap<String, String> xmlStrings = new HashMap<String, String>();
		
		for(String url : SplashActivity.navInfoURLs)
			xmlStrings.put(url, xmlStringsPref.getString(url, ""));
		
		return xmlStrings;
	}
    
    private void log(String msg){
    	if(SplashActivity.LOG_ENABLED)
    		Log.i("CanalMapFragment", msg);
    }
    
}