package com.example.android.sunshine.app;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;

import timber.log.Timber;

public class ForecastFragment extends Fragment {
    // Fields --------------------------------------------------------------------------------------
    private final String LOG_TAG = ForecastFragment.class.getSimpleName();
    private ArrayAdapter<String> mForecastAdapter;


    // Fragment methods ----------------------------------------------------------------------------
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Reports that the fragment wants to participate in menu inflation.
        setHasOptionsMenu(true);
    }

    @Override
    public void onStart() {
        updateWeather();
        super.onStart();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.forecastfragment, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here.
        int id = item.getItemId();
        if (id == R.id.action_refresh) {
            updateWeather();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        List<String> weekForecast = new ArrayList<>();
        // Let's create and ArrayAdapter and bind it to the ListView
        mForecastAdapter = new ArrayAdapter<>(
                getActivity(),
                R.layout.list_item_forecast,
                R.id.list_item_forecast_textview,
                weekForecast);

        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        // Get a reference to the View that is going to be bound with the Adapter
        ListView listViewForecast = (ListView) rootView.findViewById(R.id.listview_forecast);
        listViewForecast.setAdapter(mForecastAdapter);
        listViewForecast.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(getActivity(), DetailActivity.class)
                        .putExtra(Intent.EXTRA_TEXT, mForecastAdapter.getItem(position));
                startActivity(intent);
            }
        });

        return rootView;
    }

    // Helpers -------------------------------------------------------------------------------------
    private void updateWeather() {
        FetchWeatherTask weatherTask = new FetchWeatherTask();

        // Getting *Default*SharedPreferences, and then the value of the pref_location_key.
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        getActivity().getSharedPreferences(
                getString(R.string.pref_location_key), Context.MODE_PRIVATE);
        String defaultLocation = getResources().getString(R.string.pref_location_default);
        String location = prefs.getString(getString(R.string.pref_location_key), defaultLocation);

        weatherTask.execute(location);
    }

    // AsyncTask for fetching the JSON weather data and formatting them to String[] ----------------
    public class FetchWeatherTask extends AsyncTask<String, Void, String[]> {
        // Fields ----------------------------------------------------------------------------------
        private final String LOG_TAG = FetchWeatherTask.class.getSimpleName();

        // AsyncTask Methods -----------------------------------------------------------------------
        @Override
        protected String[] doInBackground(String... params) {
            // Check input
            String location = params[0];
            Timber.v("NewFetchWeatherTask with location: %s", location);
            // Set up query parameters
            String format = "json";
            String units = "metric";
            int numDays = 7;

            // Fetching data from OpenWeatherMap
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

            // Will contain raw JSON response as string
            String forecastJsonStr = null;

            try {
                final String FORECAST_BASE_URL = "http://api.openweathermap.org/data/2.5/forecast?";
                final String QUERY_PARAM = "q";
                final String FORMAT_PARAM = "mode";
                final String UNITS_PARAM = "units";
                final String DAYS_PARAM = "cnt";
                final String APPID = "APPID";

                Uri builtUri = Uri.parse(FORECAST_BASE_URL).buildUpon()
                        .appendQueryParameter(QUERY_PARAM, location)
                        .appendQueryParameter(FORMAT_PARAM, format)
                        .appendQueryParameter(UNITS_PARAM, units)
                        .appendQueryParameter(DAYS_PARAM, Integer.toString(numDays))
                        .appendQueryParameter(APPID, BuildConfig.OPEN_WEATHER_MAP_API_KEY)
                        .build();
                URL url = new URL(builtUri.toString());

                // Create a connection to OpenWeatherMap and connect
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                // Read response into an inpuStream
                InputStream inputStream = urlConnection.getInputStream();
                StringBuffer buffer = new StringBuffer();
                reader = new BufferedReader(new InputStreamReader(inputStream));

                // Pass the response into the buffer appending '\n' for easier debugging
                String line;
                while((line = reader.readLine()) != null) {
                    buffer.append(line + '\n');
                }

                if (buffer.length() == 0)
                    // Stream was empty. No point in parsing.
                    return null;

                forecastJsonStr = buffer.toString();
            } catch (IOException e) {
                Timber.e("Forecast fragment error: %s", e.toString());
                return null;
            } finally {
                if (urlConnection != null)
                    urlConnection.disconnect();
                if (reader != null) {
                    try {
                        reader.close();
                    }
                    catch (IOException e) {
                        Timber.e("Forecast fragment: Failed to close reader: %s", e.toString());
                    }
                }
            }
            try {
                return getWeatherDataFromJson(forecastJsonStr, numDays);
            } catch (JSONException e) {
                Timber.e("Failure parsing forecastJSONString: %s", e.getMessage());
            }
            return null;
        }

        @Override
        protected void onPostExecute(String[] weatherStrs) {
            if (weatherStrs != null) {
                mForecastAdapter.clear();
                mForecastAdapter.addAll(weatherStrs);
            }
        }
    }

    // Helpers -------------------------------------------------------------------------------------
    /**
     * Prepare the weather high/lows for presentation.
     */
    private String formatHighLows(double high, double low) {
        // Check for user-preferred temperature units.
        String temperatureUnits = PreferenceManager.getDefaultSharedPreferences(getActivity())
                .getString(getString(R.string.pref_temperature_units_key),
                        getString(R.string.pref_temperature_units_default));

        long roundedHigh, roundedLow;
        // We know that OpenWeatherAPI sends to values to metric units.
        if (!temperatureUnits.equals(
                getResources().getStringArray(R.array.pref_temperature_units_values)[0])) {
                roundedHigh = Math.round(celciusToFahrenheit(high));
                roundedLow = Math.round(celciusToFahrenheit(low));
        }
        else {
            roundedHigh = Math.round(high);
            roundedLow = Math.round(low);
        }

        return roundedHigh + " / " + roundedLow;
    }

    private double celciusToFahrenheit(double celcius) {
        return 1.8 * celcius + 32;
    }

    /**
     * Take the String representing the complete forecast in JSON Format and
     * pull out the data we need to construct the Strings needed for the wireframes.
     *
     * Fortunately parsing is easy:  constructor takes the JSON string and converts it
     * into an Object hierarchy for us.
     */
    private String[] getWeatherDataFromJson(String forecastJsonStr, int numDays)
        throws JSONException {
        // These are the names of the JSON objects that need to be extracted.
        final String OWM_LIST = "list";
        final String OWM_WEATHER = "weather";
        final String OWM_MAIN = "main";
        final String OWM_MAX = "temp_max";
        final String OWM_MIN = "temp_min";
        final String OWM_MAIN_DESCRIPTION = "main";

        JSONObject forecastJson = new JSONObject(forecastJsonStr);
        JSONArray weatherArray = forecastJson.getJSONArray(OWM_LIST);

        // OWM returns daily forecasts based upon the local time of the city that is being
        // asked for, which means that we need to know the GMT offset to translate this data
        // properly.
        // Since this data is also sent in-order and the first day is always the
        // current day, we're going to take advantage of that to get a nice
        // normalized UTC date for all of our weather.

        String[] resultStrs = new String[numDays];
        for (int i = 0; i < weatherArray.length(); i++) {
            // For now, using the format "Day, description, hi/low"
            String day;
            String description;
            String highAndLow;

            JSONObject dayForecast = weatherArray.getJSONObject(i);

            // We're going to ignore the actual time data, and exploit the fact that the 1st day
            // that we receive is the current one.
            GregorianCalendar calendar = new GregorianCalendar();
            // Advance i days the current date.
            calendar.add(GregorianCalendar.DATE, i);
            // Get that date, and format it appropriately
            SimpleDateFormat shortenedDateFormat =
                    new SimpleDateFormat("EEE, MMM dd", Locale.getDefault());
            day = shortenedDateFormat.format(calendar.getTime());

            // Description is in a child array called "weather", which is 1 element long.
            description = dayForecast
                    .getJSONArray(OWM_WEATHER).getJSONObject(0)
                    .getString(OWM_MAIN_DESCRIPTION);

            // Temperatures are in a child object called "main".
            JSONObject temperaturesObject = dayForecast.getJSONObject(OWM_MAIN);
            highAndLow = formatHighLows(
                    temperaturesObject.getDouble(OWM_MAX), temperaturesObject.getDouble(OWM_MIN));

            resultStrs[i] = day + " - " + description + " - " + highAndLow;
        }

        return resultStrs;
    }
}
