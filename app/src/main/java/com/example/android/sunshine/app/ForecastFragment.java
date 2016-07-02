package com.example.android.sunshine.app;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
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

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

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
        ArrayAdapter<String> mForecastAdapter = new ArrayAdapter<String>(getActivity(),
                R.layout.list_item_forecast, R.id.list_item_forecast_textview, weekForecast);

        // Get a reference to the View that is going to be bound with the Adapter
        ListView listViewForecast = (ListView) rootView.findViewById(R.id.listview_forecast);
        listViewForecast.setAdapter(mForecastAdapter);

        return rootView;
    }

    public class FetchWeatherTask extends AsyncTask<String, Void, String> {

        private final String LOG_TAG = FetchWeatherTask.class.getSimpleName();

        @Override
        protected String doInBackground(String... params) {
            // Fetching data from OpenWeatherMap
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

            // Will contain raw JSON response as string
            String forecastJsonStr = null;

            try {
                String baseURL = "http://api.openweathermap.org/data/2.5/forecast?q=18546,gr&" +
                        "units=metric&cnt=7";
                String apiKey = "&appid=" + BuildConfig.OPEN_WEATHER_MAP_API_KEY;
                URL url = new URL(baseURL.concat(apiKey));

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
            return forecastJsonStr;
        }
    }
}
