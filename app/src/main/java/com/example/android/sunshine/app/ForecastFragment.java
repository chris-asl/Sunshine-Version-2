package com.example.android.sunshine.app;

import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import timber.log.Timber;

public class ForecastFragment extends Fragment {
    private final String LOG_TAG = ForecastFragment.class.getSimpleName();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Reports that the fragment wants to participate in menu inflation.
        setHasOptionsMenu(true);
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
            FetchWeatherTask weatherTask = new FetchWeatherTask();
            weatherTask.execute("18546");
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Create some dummy data for the ListView.  Here's a sample weekly forecast
        String[] data = {
                "Mon 6/23 - Sunny - 31/17",
                "Tue 6/24 - Foggy - 21/8",
                "Wed 6/25 - Cloudy - 22/17",
                "Thurs 6/26 - Rainy - 18/11",
                "Fri 6/27 - Foggy - 21/10",
                "Sat 6/28 - TRAPPED IN WEATHERSTATION - 23/18",
                "Sun 6/29 - Sunny - 20/7"
        };
        List weekForecast = new ArrayList<>(Arrays.asList(data));
        // Let's create and ArrayAdapter and bind it to the ListView
        ArrayAdapter<String> mForecastAdapter = new ArrayAdapter<>(
                getActivity(),
                R.layout.list_item_forecast,
                R.id.list_item_forecast_textview,
                weekForecast);

        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        // Get a reference to the View that is going to be bound with the Adapter
        ListView listViewForecast = (ListView) rootView.findViewById(R.id.listview_forecast);
        listViewForecast.setAdapter(mForecastAdapter);

        return rootView;
    }


    public class FetchWeatherTask extends AsyncTask<String, Void, Void> {
        private final String LOG_TAG = FetchWeatherTask.class.getSimpleName();

        @Override
        protected Void doInBackground(String... params) {
            // Check input
            String zipcode = null;
            if (params.length == 0) {
                // Set default zip code
                zipcode = "18546";
            }
            else
                zipcode = params[0];
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
                        .appendQueryParameter(QUERY_PARAM, zipcode)
                        .appendQueryParameter(FORMAT_PARAM, format)
                        .appendQueryParameter(UNITS_PARAM, units)
                        .appendQueryParameter(DAYS_PARAM, Integer.toString(numDays))
                        .appendQueryParameter(APPID, BuildConfig.OPEN_WEATHER_MAP_API_KEY)
                        .build();
                URL url = new URL(builtUri.toString());
                Timber.v("Built URI %s", builtUri.toString());

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

                Timber.v("Forecast JSON String: %s", forecastJsonStr);
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
            return null;
        }
    }

}
