package dev.nhachicha.parisparking.test;
/*
Copyright 2012 Nabil HACHICHA

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

import com.jayway.android.robotium.solo.Solo;

import dev.nhachicha.parisparking.MainActivity_;
import dev.nhachicha.parisparking.util.Constant;
import dev.nhachicha.parisparking.util.Misc;
import dev.nhachicha.parisparking.util.RESTHelper;

import org.json.JSONArray;
import org.json.JSONObject;

import android.preference.PreferenceManager;
import android.test.ActivityInstrumentationTestCase2;

import java.util.Locale;

import junit.framework.Assert;
/**
 * @author Nabil HACHICHA
 * http://nhachicha.wordpress.com
 */

public class BackEndTest extends ActivityInstrumentationTestCase2<MainActivity_>{
    private Solo solo;

    public BackEndTest() {
        super(MainActivity_.class);
    }
    
    public void testUnitSystemConversion () {
        PreferenceManager.getDefaultSharedPreferences(getActivity()).edit().putString("pref_units_system", "0").commit();
        assertEquals("191 "+getActivity().getText(dev.nhachicha.parisparking.R.string.unit_meter), Misc.getHumanReadableDistance(getActivity(), 190.765773902654d));
        assertEquals("2 "+getActivity().getText(dev.nhachicha.parisparking.R.string.unit_kilo), Misc.getHumanReadableDistance(getActivity(), 2004.30739081975477d));
        
        PreferenceManager.getDefaultSharedPreferences(getActivity()).edit().putString("pref_units_system", "1").commit();
        
        assertEquals("209 "+getActivity().getText(dev.nhachicha.parisparking.R.string.unit_yard),Misc.getHumanReadableDistance(getActivity(), 190.765773902654d));
        assertEquals("1 "+getActivity().getText(dev.nhachicha.parisparking.R.string.unit_mile) ,Misc.getHumanReadableDistance(getActivity(), 2004.30739081975477d));
    }
    
    public void testPayloadFormat () {
        String request = String.format( Locale.ENGLISH/*To get a . instead of , for float*/,
                                        Constant.JSON_SEARCH_PAYLOAD_TEMPLATE,
                                        2.294254/*Long*/,
                                        48.858278/*Lat*/,
                                        5/*result nbr*/
                                      );
        assertEquals("{\"geoNear\": \"places\",\"near\":[2.294254,48.858278], \"spherical\": true, \"distanceMultiplier\": 6378000, \"num\": 5}", request);
    }
    
    public void testPingMongoLab () throws Exception {
        String urlPing = Constant.MANGODB_URL+"/"+Constant.MANGODB_DB_NAME+"/collections/"+Constant.MANGODB_COLLECTION_NAME+"?apiKey="+Constant.MANGODB_API_KEY+"&l=1";

        String json = RESTHelper.getJson(urlPing);
        JSONArray jArr = new JSONArray(json);
        JSONObject obj = (JSONObject) jArr.get(0);
        JSONArray location = (JSONArray)obj.get("loc");
        assertNotNull(location);
        assertEquals(48.858534, (Double)location.get(1), 1e-8);//Latitude
        assertEquals(2.34361, (Double)location.get(0), 1e-8);//Longitude
    }
    
    public void testSearchPlaces () throws Exception {//Search POI around Eiffel Tour 
        String payload = String.format(Locale.ENGLISH, Constant.JSON_SEARCH_PAYLOAD_TEMPLATE, 2.294254, 48.858278, Constant.DEFAULT_SEARCH_NUMBER_OF_RESULT);
        String json = RESTHelper.postJson(Constant.JSON_SEARCH_URL, payload);
        JSONObject jObj = new JSONObject(json);
        JSONArray jArr = (JSONArray)jObj.get("results");
        assertEquals(Constant.DEFAULT_SEARCH_NUMBER_OF_RESULT, jArr.length());
        
        double distance = (Double) ((JSONObject)jArr.get(0)).get("dis");
        //first parking place is at exactly ~ 190 meter 190.765773902654
        assertEquals(190.765773902654, distance, 1e-8);
        
    }
    
    public void testStartActivity () {
        solo = new Solo(getInstrumentation(), getActivity());
        // Note: Local should be English, otherwise clickOnButton will not match, also GPS should be enabled to avoid activation dialog.
        
        //Make sure we use the Metric system
        PreferenceManager.getDefaultSharedPreferences(getActivity()).edit().putString("pref_units_system", "0").commit();
        
        // Set developer mode to true 
        Constant.DEVELOPER_MODE = true;
        
        solo.assertCurrentActivity("Expected MainActivity", "MainActivity_"); 
        solo.clickInList(2);//Enter address
        solo.waitForText(getActivity().getText(dev.nhachicha.parisparking.R.string.alert_dialog_text_entry).toString());
        solo.enterText(0, "33 Avenue de Suffren");
        solo.clickOnButton(getActivity().getText(android.R.string.ok).toString());
        solo.waitForText(getActivity().getText(dev.nhachicha.parisparking.R.string.dlg_search_choose_address).toString());
        solo.clickInList(1);//select first address
        Assert.assertTrue(solo.searchText("42"));
        solo.clickLongOnText("42");
        solo.clickOnText("View");
        //solo.assertCurrentActivity("Expected Map", "MapsActivity"); 
        solo.finishOpenedActivities();
    }
    
}
