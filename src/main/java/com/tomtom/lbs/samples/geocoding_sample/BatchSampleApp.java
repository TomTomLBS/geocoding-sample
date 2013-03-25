package com.tomtom.lbs.samples.geocoding_sample;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A simple class which demonstrates the use of the TomTom Batch Geocoding API.
 * See http://developer.tomtom.com/ for more details.
 */
public class BatchSampleApp
{
	public static void main(String[] args)
	{		
		// if an input file path was specified, use the specified file path
		String inputFilePath = args.length > 0 ? args[0] : "sample-addresses.txt";
		// if an output file path was specified, use the specified file path
		String outputFilePath = args.length > 1 ? args[1] : "address-output.txt";
		
		// make sure the input file exists
		if (!new File(inputFilePath).exists())
		{
			LOGGER.error("The specified input file does not exist: " + inputFilePath);
			return;
		}
		
		// this will keep track of the record count, only 100 addresses is allowed per batch
		int recordCount = 0;
		int totalRecordsProcessed = 0;
		int batchesProcessed = 0;
		
		// read the address file into a list
		List<String> addresses = readAddressFile(inputFilePath);
		
		// determine the total number of batches that will need to be sent
		int totalBatches = addresses.size() / BATCH_SIZE;
		
		// this will contain all of the processed results
		List<JSONObject> results = new ArrayList<JSONObject>();
		
		// build a JSON object to post to the server
		JSONObject json = buildJSONContainer();
		JSONArray location = json.optJSONObject("locations").optJSONArray("location");
		
		// loop through each address in the list
		for (String address : addresses)
		{
			// the sample file is pipe delimited, split on pipe
			String[] fields = address.split("\\|");
			
			location.put(buildJSONRecord(fields));
			
			recordCount++;
			
			// once the 100 record count has been reached, or if we reach the end, send the batch
			if (recordCount == BATCH_SIZE || recordCount + totalRecordsProcessed == addresses.size())
			{
				// inform the user of progress
				LOGGER.info("Processing batch " + (batchesProcessed + 1) + " / " + totalBatches);
				
				// send the batch to the geocoder
				results.add(sendBatch(json));
				
				// create a new container to start the next batch
				json = buildJSONContainer();
				location = json.optJSONObject("locations").optJSONArray("location");

				totalRecordsProcessed += recordCount;
				recordCount = 0;
				batchesProcessed++;
			}
			
		}
		
		LOGGER.info("Total records processed: " + totalRecordsProcessed);
		LOGGER.info("Results written to " + outputFilePath);
		
		// now that all batches have been submitted, display the results
		writeResults(results, outputFilePath);
	}
	
	private static void writeResults(List<JSONObject> results, String outputFilePath)
	{
		List<String> lines = new ArrayList<String>();
		
		for (JSONObject result : results)
		{
			// get the response node
			JSONObject geoResponse = result.optJSONObject("geoResponse");
			
			// if no response node was found, an error response or invalid response was received
			if (geoResponse == null)
			{
				LOGGER.warn("An invalid response was received: " + result.toString());
			}
			else
			{
				// get the result array
				JSONArray geoResult = geoResponse.optJSONArray("geoResult");
				
				// if the result was not an array, only one response was returned
				if (geoResult == null)
				{
					lines.add(formatResult(geoResponse.optJSONObject("geoResult")));
				}
				else
				{
					// display each returned result
					for (int geoResultIndex = 0; geoResultIndex < geoResult.length(); geoResultIndex++) 
					{
						lines.add(formatResult(geoResult.optJSONObject(geoResultIndex)));
					}
				}
			}
		}
		
		try
		{
			// open the output file
			OutputStream output = new FileOutputStream(outputFilePath);
			
			// writhe the output file
			IOUtils.writeLines(lines, IOUtils.LINE_SEPARATOR, output);
		}
		catch (IOException ex)
		{
			LOGGER.error("An error occurred writing the output file.", ex);
		}
	}
	
	private static String formatResult(JSONObject geoResult)
	{
		return geoResult.optString("formattedAddress") + "|" + geoResult.optDouble("latitude") + ", " + geoResult.optDouble("longitude");
	}
	
	private static JSONObject buildJSONRecord(String[] fields)
	{
		JSONObject json = new JSONObject();
		
		try
		{
			json.put("ST", fields[0]); // street number
			json.put("T", fields[1]); // street name
			json.put("L", fields[2]); // city
			json.put("AA", fields[3]); // state
			json.put("PC", fields[4]); // postal code
			json.put("CN", fields[5]); // country
		}
		catch (JSONException ex)
		{
			LOGGER.error("An error occurred building an address record.", ex);
		}
		
		return json;
	}
	
	private static JSONObject sendBatch(JSONObject json)
	{
		// the geocode batch expects an HTTP POST, use the HTTPComponents HttpPost object
		HttpPost post = new HttpPost(BATCH_GEOCODE_URL + API_KEY);
		
		// create the body of the post
		post.setEntity(new StringEntity(json.toString(), ContentType.create("application/json", "UTF-8")));
		
		try
		{
			// send the POST request
			HttpResponse response = httpClient.execute(post);
			
			// get the response string
			String jsonResponse = EntityUtils.toString(response.getEntity());
			
			// log out the response
			LOGGER.debug(jsonResponse);
			
			// return the parsed JSON	
			return new JSONObject(jsonResponse);
		}
		catch (IOException ex)
		{
			LOGGER.error("An error occurred sending a batch to the geocoder.", ex);
		}
		catch (JSONException ex)
		{
			LOGGER.error("An error occurred parsing the JSON response from the server.  See DEBUG output for full response text.", ex);
		}
		
		return new JSONObject();
	}
	
	private static JSONObject buildJSONContainer()
	{
		JSONObject json = new JSONObject();
		JSONObject locations = new JSONObject();
		JSONArray location = new JSONArray();
		
		try
		{		
			// add the location and locations nodes
			json.put("locations", locations);
			locations.put("location", location);
		}
		catch (JSONException ex)
		{
			LOGGER.error("An error occurred building the root JSON container for a geocode batch.", ex);
		}
		
		return json;
	}
	
	private static List<String> readAddressFile(String filePath)
	{
		InputStream input = null;
		List<String> result = null;
		
		try
		{
			// open the sample file
			input = new FileInputStream(filePath);
			
			// read the whole file
			result = IOUtils.readLines(input);
		}
		catch (IOException ex)
		{
			LOGGER.error("An error occurred reading the input address file.", ex);
		}
		finally
		{
			IOUtils.closeQuietly(input);
		}
		
		return result;
	}
	
	private static Logger LOGGER = LoggerFactory.getLogger(BatchSampleApp.class);
	private static HttpClient httpClient = new DefaultHttpClient();

	// the max batch size that the batch geocoder can handle
	private static final int BATCH_SIZE = 100;
	
	// the URL for the batch geocoder call
	private static final String BATCH_GEOCODE_URL = "https://api.tomtom.com/lbs/geocoding/geocode_batch?format=json&key=";
	
	// the API used, must be a Geocoding API key, see http://developer.tomtom.com/ for more information.
	private static final String API_KEY = "YOUR API KEY HERE";
}
