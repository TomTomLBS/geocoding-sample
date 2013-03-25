package com.tomtom.lbs.samples.geocoding_sample;

import java.io.IOException;
import java.net.URLEncoder;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A simple class which demonstrates the use of the TomTom Geocoding API.
 * See http://developer.tomtom.com/ for more details.
 */
public class SingleCallSampleApp
{
	public static void main(String[] args)
	{
		String address = "";
		
		// get the address, if no arguments were passed, use a default
		if (args.length == 0)
			address = "11 Lafayette St, Lebanon, NH";
		else
			address = args[0];
		
		// call the API
		try
		{
			// append the address to the "query" parameter
			HttpGet get = new HttpGet(GEOCODE_URL + API_KEY + "&query=" + URLEncoder.encode(address, "UTF-8"));
			
			// execute the call
			HttpResponse response = httpClient.execute(get);
			
			// parse the response as JSON
			JSONObject jsonResponse = new JSONObject(EntityUtils.toString(response.getEntity()));
			
			// display the results
			displayResults(jsonResponse);
		}
		catch (IOException ex)
		{
			LOGGER.error("An error occurred calling the Geocoding API.", ex);
		}
		catch (JSONException ex)
		{
			LOGGER.error("An error occurred parsing the response from the Geocoding API.", ex);
		}
	}
	
	private static void displayResults(JSONObject response)
	{
		JSONObject geoResponse = response.optJSONObject("geoResponse");
		
		// if no response node was found, an error response or invalid response was received
		if (geoResponse == null)
		{
			LOGGER.warn("An invalid response was received: " + response.toString());
		}
		else
		{
			// get the result array
			JSONArray geoResult = geoResponse.optJSONArray("geoResult");
			
			// if the result was not an array, only one response was returned
			if (geoResult == null)
			{
				displayResult(geoResponse.optJSONObject("geoResult"));
			}
			else
			{
				// display each returned result
				for (int geoResultIndex = 0; geoResultIndex < geoResult.length(); geoResultIndex++) 
				{
					displayResult(geoResult.optJSONObject(geoResultIndex));
				}
			}
		}
	}
	
	private static void displayResult(JSONObject result)
	{
		LOGGER.info(result.optString("formattedAddress") + " - " + result.optDouble("latitude") + ", " + result.optDouble("longitude"));
	}
	
	private static Logger LOGGER = LoggerFactory.getLogger(SingleCallSampleApp.class);
	private static HttpClient httpClient = new DefaultHttpClient();
	
	// the URL for the batch geocoder call
	private static final String GEOCODE_URL = "https://api.tomtom.com/lbs/geocoding/geocode?format=json&key=";
	
	// the API used, must be a Geocoding API key, see http://developer.tomtom.com/ for more information.
	// TODO: Change the API key entered here
	private static final String API_KEY = "YOUR API KEY HERE";
}
