package sarah.rita.tugas1pbd;

import android.os.CountDownTimer;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import android.app.Activity;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.TextView;
import android.os.AsyncTask;
import android.content.Context;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.widget.Toast;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class MapsActivity extends FragmentActivity implements SensorEventListener {

    private GoogleMap mMap; // Might be null if Google Play services APK is not available.

    // define the display assembly compass picture
    private ImageView image;

    // record the compass picture angle turned
    private float currentDegree = 0f;

    // device sensor manager
    private SensorManager mSensorManager;

    static final String ACTION_SCAN = "com.google.zxing.client.android.SCAN";

    TextView tvHeading;

    double latit, longit;
    long valid12;
    long now = System.currentTimeMillis();
    long estimatedtime;

    String postMessage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        String result = "";
        new GetTask().execute(getApplicationContext());

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        //
        image = (ImageView) findViewById(R.id.imageViewCompass);

        // TextView that will tell the user what degree is he heading
        tvHeading = (TextView) findViewById(R.id.tvHeading);

        // initialize your android device sensor capabilities
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        setUpMapIfNeeded();
        estimatedtime=10000;
        estimatedtime = (valid12*1000) - now ;
        timer();

    }

    @Override
    protected void onResume() {
        super.onResume();
        setUpMapIfNeeded();
        // for the system's orientation sensor registered listeners
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION),
                SensorManager.SENSOR_DELAY_GAME);
    }

    @Override
    protected void onPause() {
        super.onPause();

        // to stop the listener and save battery
        mSensorManager.unregisterListener(this);
    }


    @Override
    public void onSensorChanged(SensorEvent event) {
        // get the angle around the z-axis rotated
        float degree = Math.round(event.values[0]);
//        tvHeading.setText("Heading: " + Float.toString(degree) + " degrees");

        // create a rotation animation (reverse turn degree degrees)
        RotateAnimation ra = new RotateAnimation(currentDegree, -degree, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);

        // how long the animation will take place
        ra.setDuration(210);

        // set the animation after the end of the reservation status
        ra.setFillAfter(true);
        // Start the animation
        image.startAnimation(ra);
        currentDegree = -degree;

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // not in use
    }

    /**
     * Sets up the map if it is possible to do so (i.e., the Google Play services APK is correctly
     * installed) and the map has not already been instantiated.. This will ensure that we only ever
     * call {@link #setUpMap()} once when {@link #mMap} is not null.
     * <p/>
     * If it isn't installed {@link SupportMapFragment} (and
     * {@link com.google.android.gms.maps.MapView MapView}) will show a prompt for the user to
     * install/update the Google Play services APK on their device.
     * <p/>
     * A user can return to this FragmentActivity after following the prompt and correctly
     * installing/updating/enabling the Google Play services. Since the FragmentActivity may not
     * have been completely destroyed during this process (it is likely that it would only be
     * stopped or paused), {@link #onCreate(Bundle)} may not be called again so we should call this
     * method in {@link #onResume()} to guarantee that it will be called.
     */
    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null) {
            // Try to obtain the map from the SupportMapFragment.
            mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                    .getMap();
            // Check if we were successful in obtaining the map.
            if (mMap != null) {
                setUpMap();
            }
        }
    }

    /**
     * This is where we can add markers or lines, add listeners or move the camera. In this case, we
     * just add a marker near Africa.
     * <p/>
     * This should only be called once and when we are sure that {@link #mMap} is not null.
     */
    private void setUpMap() {
        Marker a = null;
        a= mMap.addMarker(new MarkerOptions().position(new LatLng(latit, longit)).title("Jerry"));
        // Move the camera instantly to location with a zoom of 15.
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(latit, longit), 10));
        // Zoom in, animating the camera.
        mMap.animateCamera(CameraUpdateFactory.zoomTo(10), 2000, null);
    }


    //product qr code mode
    public void scanQR(View v) {
        try {
            //start the scanning activity from the com.google.zxing.client.android.SCAN intent
            Intent intent = new Intent(ACTION_SCAN);
            intent.putExtra("SCAN_MODE", "QR_CODE_MODE");
            startActivityForResult(intent, 0);
        } catch (ActivityNotFoundException anfe) {
            //on catch, show the download dialog
            showDialog(MapsActivity.this, "No Scanner Found", "Download a scanner code activity?", "Yes", "No").show();
        }
    }

    //alert dialog for downloadDialog
    private static AlertDialog showDialog(final Activity act, CharSequence title, CharSequence message, CharSequence buttonYes, CharSequence buttonNo) {
        AlertDialog.Builder downloadDialog = new AlertDialog.Builder(act);
        downloadDialog.setTitle(title);
        downloadDialog.setMessage(message);
        downloadDialog.setPositiveButton(buttonYes, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialogInterface, int i) {
                Uri uri = Uri.parse("market://search?q=pname:" + "com.google.zxing.client.android");
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                try {
                    act.startActivity(intent);
                } catch (ActivityNotFoundException anfe) {
                }
            }
        });
        downloadDialog.setNegativeButton(buttonNo, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialogInterface, int i) {
            }
        });
        return downloadDialog.show();
    }

    //on ActivityResult method
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == 0) {
            if (resultCode == RESULT_OK) {
                //get the extras that are returned from the intent
                String contents = intent.getStringExtra("SCAN_RESULT");
                String format = intent.getStringExtra("SCAN_RESULT_FORMAT");
                Toast toast = Toast.makeText(this, "Content:" + contents + " Format:" + format, Toast.LENGTH_LONG);
                toast.show();
                postMessage = contents;
                new PostTask().execute();

            }
        }
    }

    public void timer (){
        new CountDownTimer(estimatedtime, 1000) {
            public void onTick(long millisUntilFinished) {
                tvHeading.setText(Long.toString(estimatedtime/1000));
                estimatedtime = millisUntilFinished;
            }

            public void onFinish() {
              new GetTask().execute(getApplicationContext());
                estimatedtime = (valid12*1000) - now ;
                setUpMap();
                timer();
            }
        }.start();

    }

    public class GetTask extends AsyncTask<Context, String, String> {

        @Override
        protected String doInBackground(Context... params) {
            // TODO Auto-generated method stub

            HttpClient client = new DefaultHttpClient();
            HttpGet request = new HttpGet("http://167.205.32.46/pbd/api/track?nim=13512009");
            HttpResponse response;
            String result = "";

            try {
                response = client.execute(request);

                // Get the response
                BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));

                String line = "";
                while ((line = rd.readLine()) != null) {
                    result += line;
                }
                result = result.substring(result.indexOf("{"), result.indexOf("}") + 1);

                try {
                    JSONObject res = new JSONObject(result);
                    latit = res.getDouble("lat");
                    longit = res.getDouble("long");
                    valid12 = res.getLong("valid_until");

                    Log.d("Result", result);

                } catch (JSONException e) {
                    e.printStackTrace();
                }

            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            return result;
        }

    }

    public class PostTask extends AsyncTask<Void, Void, JSONObject> {
        private final String TAG = "HttpClient";
        private JSONObject result = null;

        @Override
        protected JSONObject doInBackground(Void... params) {
        String sendMessage;

            try {
                DefaultHttpClient httpclient = new DefaultHttpClient();
                HttpPost httpPostRequest = new HttpPost("http://167.205.32.46/pbd/api/catch");

                JSONObject sendObject = new JSONObject();
                sendObject.put("nim","13512009");
                sendObject.put("token",postMessage);
                sendMessage = sendObject.toString();

                StringEntity se;
                se = new StringEntity(sendMessage);

                // Set HTTP parameters
                httpPostRequest.setEntity(se);
                httpPostRequest.setHeader("Accept", "application/json");
                httpPostRequest.setHeader("Content-type", "application/json");

                long t = System.currentTimeMillis();
                HttpResponse response = (HttpResponse) httpclient.execute(httpPostRequest);
                Log.i(TAG, "HTTPResponse received in [" + (System.currentTimeMillis() - t) + "ms]");

                HttpEntity entity = response.getEntity();
                if (entity != null) {
                    // Read the content stream
                    InputStream instream = entity.getContent();

                    // convert content stream to a String
                    String resultString = convertStreamToString(instream);
                    instream.close();
                    resultString = resultString.substring(1, resultString.length() - 1); // remove wrapping "[" and "]"

                    JSONObject jsonObjRecv = new JSONObject(resultString);

                    // Raw DEBUG output of our received JSON object:
                    Log.i(TAG, "<JSONObject>\n" + jsonObjRecv.toString() + "\n</JSONObject>");

                    return jsonObjRecv;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        private String convertStreamToString(InputStream is) {

            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            StringBuilder sb = new StringBuilder();

            String line = null;
            try {
                while ((line = reader.readLine()) != null) {
                    sb.append(line + "\n");
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return sb.toString();
        }
    }
}