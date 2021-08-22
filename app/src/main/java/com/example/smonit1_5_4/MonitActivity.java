package com.example.smonit1_5_4;

import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.CursorAdapter;
import android.widget.ScrollView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.androidplot.Plot;
import com.androidplot.util.Redrawer;
import com.androidplot.xy.AdvancedLineAndPointRenderer;
import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.FastLineAndPointRenderer;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.XYPlot;
import com.androidplot.xy.XYSeries;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static android.os.Environment.DIRECTORY_DOWNLOADS;


public class MonitActivity extends Activity {

    private static final String TAG = "SMonit";
    private int mMaxChars = 50000;
    private UUID mDeviceUUID;
    private BluetoothSocket mmSocket;
    private ReadInput mReadThread = null;

    private TextView mTxtReceive;
    private ScrollView scrollView;

    private boolean mIsBluetoothConnected = false;

    private BluetoothDevice mDevice;

    private ProgressDialog progressDialog;

    //private ListView listResults;
    private SQLiteDatabase db;
    private Cursor resultsCursor;

    int theLastOne;

    private XYPlot mySimpleXYPlot0;
    private XYPlot mySimpleXYPlot1;
    private XYPlot mySimpleXYPlot2;
    private Redrawer my_redrawer;
    private Redrawer redrawer_ultra;



//------------------WSTAWKA---------------------------------------
    private XYPlot plot;

    /**
     * Uses a separate thread to modulate redraw frequency.
     */
    private Redrawer redrawer;
//------------------KONIEC---------------------------------------




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_monit);
        mTxtReceive = findViewById(R.id.txtReceive);
        scrollView = findViewById(R.id.viewScroll);
        mySimpleXYPlot0 = (XYPlot) findViewById(R.id.plot0);
        mySimpleXYPlot1 = (XYPlot) findViewById(R.id.plot1);
        mySimpleXYPlot2 = (XYPlot) findViewById(R.id.plot2);
        //listResults = findViewById(R.id.listview_results);


//------------------WSTAWKA-------------------------------------------------------------------
        plot = (XYPlot) findViewById(R.id.plot);
//------------------KONIEC-------------------------------------------------------------------


//------------------WSTAWKA------------------------------------------------------------------------
        //-----------UTWORZENIE INSTANCJI SERII DANYCH---
//        ECGModel ecgSeries = new ECGModel(1000, 200);
//
//        //-------WYKORZYSTANIE FORMATTERA - DODANIE (NA RAZIE TYLKO) SERII WYKRESU DO WIDOKU ITP.----------
//        // add a new series' to the xyplot:
//        MyFadeFormatter formatter = new MyFadeFormatter(1000);
//        formatter.setLegendIconEnabled(false);
//        plot.addSeries(ecgSeries, formatter);
//
//        //----------USTAWIENIE DODATKOWYCH ZNACZNIKÓW - LINII POMOCNICZYCH NA TLE WYKRESU (POLA GRAFICZNEGO)
//        // reduce the number of range labels
//        plot.setLinesPerRangeLabel(3);
//        plot.setRangeBoundaries( 0, 50, BoundaryMode.FIXED);
//
//        //---------WĄTEK Z PODANIEM - PRZEKAZANIEM DANYCH DANYCH DO RENDEROWANIA
//        // start generating ecg data in the background:
//        ecgSeries.start(new WeakReference<>(plot.getRenderer(AdvancedLineAndPointRenderer.class)));
//
//        //USTAWIENIE ODŚWIEŻANIA WYKRESU CO PEWIEN CZAS
//        // set a redraw rate of 30hz and start immediately:
//        redrawer = new Redrawer(plot, 30, true);
//------------------KONIEC--------------------------------------------------------------------------

        Bundle b = getIntent().getExtras();
        assert b != null;
        mDevice = b.getParcelable(MainActivity.DEVICE_EXTRA);
        mDeviceUUID = UUID.fromString(b.getString(MainActivity.DEVICE_UUID));

        mTxtReceive.setMovementMethod(new ScrollingMovementMethod());

        if (mmSocket == null || !mIsBluetoothConnected) {
            new ConnectBT().execute();
        }
        new createDatabaseTask().execute();
    }

//------------------WSTAWKA-------------------------------------------------------------------
    /*
    SPECJALNA KLASA DZIEDZICZĄCA PO ADVANCEDLINANDPOINTRENDERER.FORMATTER KTÓRA RYSUJE LINIĘ ZNIKAJĄCĄ PO PEWNYM CZASIE.
    ZAPROJEKTOWANA DO UŻYWANIA Z BUFOREM CYKLICZNYM.

    * */
    /**
     * Special {@link AdvancedLineAndPointRenderer.Formatter} that draws a line
     * that fades over time.  Designed to be used in conjunction with a circular buffer model.
     */
    public static class MyFadeFormatter extends AdvancedLineAndPointRenderer.Formatter {

        //ZADEKLAROWANA DŁUGOŚĆ "OGONA" SYGNAŁU - CZĘŚCI WIDOCZNEJ
        private int trailSize;

        //KONSTRUKTOR MYFADEFORMATTERA Z JEDNYM PARAMETREM - DŁUGOŚCIĄ OGONA
        MyFadeFormatter(int trailSize) {
            this.trailSize = trailSize;
        }

        //NADPISANA METODA GETLINEPAINT Z TRZEMA PARAMETRAMI - OBECNY INDEKS, OSTATNI I ROZMIAR SERII?
        @Override
        public Paint getLinePaint(int thisIndex, int latestIndex, int seriesSize) {
            //JAKIŚ OFFSET OD OSTATNIEGO INDEKSU - O CO CHODZI?
            // offset from the latest index:
            int offset;
            //WARUNEK - JEŚLI OBECNY INDEKS JEST WIĘKSZY OD OSTATNIEGO INDEKSU, TO WTEDY
            // USTAL OFFSET JAKO WARTOŚC OSTATNIEGO INDEKSU + ROZMIAR SERII - OBECNY INDEKS
            //W OBU PRZYPADKACH WARTOŚĆ OFFSETU JEST TAKA SAMA!
            if (thisIndex > latestIndex) {
                offset = latestIndex + (seriesSize - thisIndex);
            } else {
                offset =  latestIndex - thisIndex;
            }

            //255f - CHODZI PO PROSTU O ZWYKŁE 255 TYLKO W FORMACIE FLOAT
            float scale = 255f / trailSize;
            //NADANIE WARTOŚCI PARAMETROWI ALFA W ZALEŻNOŚCI OD SKALI (JAK DŁUGI MA BYĆ WIDOCZNY OGON SYGNAŁU)
            //ORAZ OFSETU - ZALEŻNEGO OD OBECNEGO POŁOŻENIA I OSTATNIEGO INDEKSU
            //MOŻNA WYWNIOSKOWAĆ ŻE 255 TO WARTOŚĆ MAX
            //JEŚLI TU ZMIENIMY WE WZORZE NA SAM OFSET (ZAMIAST ILOCZYNU), TO WTEDY SAM CIEŃ SIĘ PRZESUWA
            int alpha = (int) (255 - (offset * scale));
//            getLinePaint().setAlpha(alpha > 0 ? alpha : 0);

            //USTAWIENIE WARTOŚCI ALFA KOLORU - OKREŚLAJĄCEGO PRZEZROCZYSTOŚĆ
            if (alpha > 0) {
                getLinePaint().setAlpha(alpha);
            } else {
                getLinePaint().setAlpha(0);
            }
            return getLinePaint();
        }
    }


//------------------KONIEC-------------------------------------------------------------------


//------------------WSTAWKA-------------------------------------------------------------------
    //-----------------TU SIĘ ZACZYNA CIEKAWSZE!--------

    /*
     * Prosta symulacja pewnego rodzaju sygnału. W tym przypadku załóżmy że jest to EKG.
     * Ta klasa (re)prezentuje dane w postaci cyklicznego bufora. Dane są dodawane sekwencyjnie
     * od lewej do prawej. Kiedy zostanie osiągnięty koniec bufora, to wtedy indeks "i" jest ustawiany
     * z powrotem na 0 (zerowany) i symulacja jest kontynuowana.
     * */

    /**
     * Primitive simulation of some kind of signal.  For this example,
     * we'll pretend its an ecg.  This class represents the data as a circular buffer;
     * data is added sequentially from left to right.  When the end of the buffer is reached,
     * i is reset back to 0 and simulated sampling continues.
     */
    public static class ECGModel implements XYSeries {

        //zadeklarowane zmienne: numeryczny bufor na dane, wartość opóźnienia, poboczny wątek,
        // zmienna czy (wątek) nadal żyje i zmienna ostatni indeks
        private final Number[] data;
        private final long delayMs;
        private final Thread thread;
        private boolean keepRunning;
        private int latestIndex;

        //pole "słabej referencji"
        private WeakReference<AdvancedLineAndPointRenderer> rendererRef;

        //parametr size - rozmiar "Próbki danych" zawartej w tym modelu
        //paramter updateFreqHz - częstotliwość z jaką dane są dodawane do modelu
        /**
         * @param size Sample size contained within this model
         * @param updateFreqHz Frequency at which new samples are added to the model
         */
        //konstruktor klasy ECGModel z dwoma powyższymi parametrami: rozmiar, częstotliwość
        ECGModel(int size, int updateFreqHz, final String strumienInput) {
            //tworzymy nowy bufor numeryczny (tablicę) o nazwie data i rozmiarze takim, jaki podaliśmy w parametrze wejściowym
            data = new Number[size];
            //pętla - do każdego elementu tego bufora przypisujemy 0 (na start)
            for(int i = 0; i < data.length; i++) {
                data[i] = 0;
            }

            //przeliczenie częstotliwości odświeżania na czas (opóźnienie)
            // translate hz into delay (ms):
            delayMs = 1000 / updateFreqHz;

            final Number[] kontenerek = new Number[4096];

            //powołanie do życia wątku pobocznego do rysowania
            thread = new Thread(new Runnable() {
                @Override
                //metoda run wątku - tu dzieje się mięsko
                public void run() {
                    try {
                        //pętla - wykonująca się dopóki wątek żywy
                        while (keepRunning) {
                            //warunek - jeśli ostatni indeks przekracza długość (rozmiar) bufora, to ustaw ostatni indeks jako 0
                            if (latestIndex >= data.length) {
                                latestIndex = 0;
                            }

//------------------------------MOJA WSTAWKA------------------------------------------------------
                            String[] splitted_data_strumien = strumienInput.split("\\r?\\n");
                            Log.d(TAG, splitted_data_strumien[0]);
                            Log.d(TAG, splitted_data_strumien[1]);
                            Log.d(TAG, splitted_data_strumien[2]);
//------------------------------MOJA WSTAWKA------------------------------------------------------

                            //element bufora o indeksie latestIndex to jest obecny indeks!
                            // insert a random sample:
                            //wstaw losową liczbę - symulacja EKG


                            for (int j = 0; j < splitted_data_strumien.length; j++) {
                                kontenerek[j] = Integer.parseInt(splitted_data_strumien[j]);
                            }

                            data[latestIndex] = kontenerek[0];
//                            data[latestIndex] = Math.random() * 17;

                            if(latestIndex < data.length - 1) {
                                // null out the point immediately following i, to disable
                                // connecting i and i+1 with a line:
                                //jakieś zabezpieczenie, żeby aktualny i następny element się nie łączyły automatycznie linią
                                // (albo po prostu chodzi o zerowanie pozostałości z poprzedniej serii danych)
                                data[latestIndex +1] = null;
                            }

                            //warunek - jeśli słaba referencja będzie różna od nulla, to ustaw ostatni indeks jako aktualny (?)
                            if(rendererRef.get() != null) {
                                rendererRef.get().setLatestIndex(latestIndex);
                                //odczekanie czasu w wątku - opóźnienia związanego z częstotliwością
                                Thread.sleep(delayMs);
                            } else {
                                //w innym przypadku - ustaw flagę na false
                                keepRunning = false;
                            }
                            //niezależnie od warunków, zwiększ numer aktualnego (ostatniego) o jeden
                            latestIndex++;
                        }
                        //złapanie wyjątku i też ustawienie flagi na false
                    } catch (InterruptedException e) {
                        keepRunning = false;
                    }
                }
            });
        }

        //start - uruchomienie wątku
        void start(final WeakReference<AdvancedLineAndPointRenderer> rendererRef) {
            this.rendererRef = rendererRef;
            keepRunning = true;
            thread.start();
        }

        //funkcja zwracająca rozmiar bufora - jego długość
        @Override
        public int size() {
            return data.length;
        }

        //funkcja zwracająca indeks x - numer elementu
        @Override
        public Number getX(int index) {
            return index;
        }

        //funkcja zwracająca wartość y - wartość elementu o indeksie x
        @Override
        public Number getY(int index) {
            return data[index];
        }

        //zwraca tytuł - niby zbędna metoda, ale bez niej nie uruchomi się serii...
        @Override
        public String getTitle() {
            return "Signal";
        }
    }

//------------------KONIEC-------------------------------------------------------------------






    @Override
    protected void onDestroy() {
        super.onDestroy();
        resultsCursor.close();
        db.close();
    }



    private class ReadInput extends Thread {

        private boolean bStop = false;
        private Thread t;

        public ReadInput() {
            t = new Thread(this, "Input Thread");
            t.start();
        }


        public boolean isRunning() {
            return t.isAlive();
        }

        @Override
        public void run() {
            InputStream mmInStream;
            byte[] buffer;

            try {
                mmInStream = mmSocket.getInputStream();

                while (true) {

//                    buffer = new byte[1024];
                    buffer = new byte[4096];

                    //Number[] kontener = new Number[4096];

//                    Number[] kontener0 = new Number[4096];
                    Number[] kontener0 = new Number[16384];
                    Number[] kontener1 = new Number[4096];
                    Number[] kontener2 = new Number[4096];


                    if (mmInStream.available() > 0) {
                        mmInStream.read(buffer);

                        int i;

                        for (i = 0; i < buffer.length && buffer[i] != 0; i++) {

                        }

                        final String strInput = new String(buffer, 0, i);

                        int k = 0;
                        try {
                            String[] splitted_data = strInput.split("\\r?\\n");
//                            String[] splitted_data = strInput.split(",");
                            String[][] splitted_channels = new String[splitted_data.length][3];
                            for (int z = 0; z < splitted_data.length; z++) {
                                splitted_channels[z] = splitted_data[z].split(",",3);
                                //Log.d(TAG,splitted_channels[z][0]);
                            }


                            for (int j = 0; j < splitted_data.length; j++) {

//                                if (Integer.parseInt(splitted_data[j]) < 660 && Integer.parseInt(splitted_data[j]) > 10) {
//                                    kontener[k] = Integer.parseInt(splitted_data[j]);
//                                    k++;
//                                }

                                if (Integer.parseInt(splitted_channels[j][0]) < 1000 && Integer.parseInt(splitted_channels[j][0]) > 10) {
                                    kontener0[k] = Integer.parseInt(splitted_channels[j][0]);
                                    Log.d(TAG, kontener0[k].toString());
                                    k++;
                                }

//                                if (Integer.parseInt(splitted_channels[j][1]) < 1000 && Integer.parseInt(splitted_channels[j][1]) > 10) {
//                                    kontener1[k] = Integer.parseInt(splitted_channels[j][1]);
//                                    //Log.d(TAG, kontener1[k].toString());
//                                    k++;
//                                }
//
//                                if (Integer.parseInt(splitted_channels[j][2]) < 1000 && Integer.parseInt(splitted_channels[j][2]) > 10) {
//                                    kontener2[k] = Integer.parseInt(splitted_channels[j][2]);
//                                    Log.d(TAG, kontener2[k].toString());
//                                    k++;
//                                }

                            }

                            mySimpleXYPlot0.clear();
                            mySimpleXYPlot1.clear();
                            mySimpleXYPlot2.clear();

                        } catch (NumberFormatException e) {
                            System.err.println("This is not a number!");
                            Log.d(TAG, "wyjątek związany z NFE");
                        }



                        XYSeries series0 = new SimpleXYSeries(
                                Arrays.asList(kontener0),
                                SimpleXYSeries.ArrayFormat.Y_VALS_ONLY,
                                "Dane - Seria0");

                        XYSeries series1 = new SimpleXYSeries(
                                Arrays.asList(kontener0),
                                SimpleXYSeries.ArrayFormat.Y_VALS_ONLY,
                                "Dane - Seria1");

                        XYSeries series2 = new SimpleXYSeries(
                                Arrays.asList(kontener0),
                                SimpleXYSeries.ArrayFormat.Y_VALS_ONLY,
                                "Dane - Seria2");



                        FastLineAndPointRenderer.Formatter series0Format = new FastLineAndPointRenderer.Formatter(
                                Color.rgb(0, 255, 0),
                                null,
                                null);

                        FastLineAndPointRenderer.Formatter series1Format = new FastLineAndPointRenderer.Formatter(
                                Color.rgb(0, 255, 0),
                                null,
                                null);

                        FastLineAndPointRenderer.Formatter series2Format = new FastLineAndPointRenderer.Formatter(
                                Color.rgb(0, 255, 0),
                                null,
                                null);

//                        series0Format.getLinePaint().setAntiAlias(false);
//                        series1Format.getLinePaint().setAntiAlias(false);
//                        series2Format.getLinePaint().setAntiAlias(false);


                        series0Format.setLegendIconEnabled(false);
                        series1Format.setLegendIconEnabled(false);
                        series2Format.setLegendIconEnabled(false);


                        mySimpleXYPlot0.setRangeBoundaries( 0, 700, BoundaryMode.FIXED);
                        mySimpleXYPlot0.addSeries(series0, series0Format);
                        mySimpleXYPlot0.getBackgroundPaint().setAlpha(0);

                        mySimpleXYPlot1.setRangeBoundaries( 0, 700, BoundaryMode.FIXED);
                        mySimpleXYPlot1.addSeries(series1, series1Format);
                        mySimpleXYPlot1.getBackgroundPaint().setAlpha(0);

                        mySimpleXYPlot2.setRangeBoundaries( 0, 700, BoundaryMode.FIXED);
                        mySimpleXYPlot2.addSeries(series2, series2Format);
                        mySimpleXYPlot2.getBackgroundPaint().setAlpha(0);


                        mySimpleXYPlot0.setLinesPerRangeLabel(3);
                        mySimpleXYPlot1.setLinesPerRangeLabel(3);
                        mySimpleXYPlot2.setLinesPerRangeLabel(3);



                        //-----------------------------------------WSTAWKA----------------------------------
                        ECGModel ecgSeries = new ECGModel(1000, 200, strInput);
//                        ECGModel ecgSeries = new ECGModel(1000, 200);

                        MyFadeFormatter formatter = new MyFadeFormatter(1000);
                        formatter.setLegendIconEnabled(false);
                        plot.addSeries(ecgSeries, formatter);

                        plot.setLinesPerRangeLabel(3);
                        plot.setRangeBoundaries( 0, 50, BoundaryMode.FIXED);

                        ecgSeries.start(new WeakReference<>(plot.getRenderer(AdvancedLineAndPointRenderer.class)));

                        redrawer = new Redrawer(plot, 30, true);
                        //-----------------------------------------KONIEC WSTAWKI----------------------------------



                        List<Plot> lista_wykresow = new ArrayList<>(3);

                        //ArrayList<XYPlot> lista_wykresow2 = new ArrayList<>(3);
                        lista_wykresow.add(mySimpleXYPlot0);
                        lista_wykresow.add(mySimpleXYPlot1);
                        lista_wykresow.add(mySimpleXYPlot2);



//po zakomentowaniu tego fragmentu symulowane ekg rysuje się płynnie... łącznie z tym, też się przycina rysowanie symulowanego ekg
                        redrawer_ultra = new Redrawer(lista_wykresow, 20, true);
                        mySimpleXYPlot0.redraw();
                        mySimpleXYPlot1.redraw();
                        mySimpleXYPlot2.redraw();
//-------------------------------koniec komentarza-----------------------------------------------


//                        my_redrawer = new Redrawer(mySimpleXYPlot0, 10, true);
//                        mySimpleXYPlot0.redraw();
//
//                        my_redrawer = new Redrawer(mySimpleXYPlot1, 10, true);
//                        mySimpleXYPlot1.redraw();
//
//                        my_redrawer = new Redrawer(mySimpleXYPlot2, 10, true);
//                        mySimpleXYPlot2.redraw();

                        mTxtReceive.post(new Runnable() {
                            @Override
                            public void run() {
                                mTxtReceive.append(strInput);
                                int txtLength = mTxtReceive.getEditableText().length();

                                if(txtLength > mMaxChars){
                                    mTxtReceive.getEditableText().delete(0, txtLength - mMaxChars);
                                }


                                scrollView.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        scrollView.fullScroll(View.FOCUS_DOWN);
                                    }
                                });

                            }
                        });

                    }
                    //Thread.sleep(500);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

        public void stop2() {
            bStop = true;
        }

    }

    private class DisConnectBT extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            if (mReadThread != null) {
                mReadThread.stop2();
                while (mReadThread.isRunning()) {
                }
                mReadThread = null;
            }

            try {
                mmSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }


        @Override
        protected void onPostExecute(Void result) {
            //super.onPostExecute(result);
            mIsBluetoothConnected = false;
            //finish();
        }
    }


    @Override
    protected void onPause() {
        if (mmSocket != null && mIsBluetoothConnected) {
            new DisConnectBT().execute();
        }
        Log.d(TAG, "Paused");
        super.onPause();
    }


    @Override
    protected void onResume() {
        Log.d(TAG, "Resumed");
        super.onResume();
    }


    @Override
    protected void onStop() {
        Log.d(TAG, "Stopped");
        super.onStop();
        redrawer_ultra.finish();
        redrawer.finish();
    }


//    @Override
//    protected void onSaveInstanceState(Bundle outState) {
//        super.onSaveInstanceState(outState);
//    }


    private class ConnectBT extends AsyncTask<Void, Void, Void> {

        private boolean mConnectSuccessful = true;

        @Override
        protected void onPreExecute() {

            progressDialog = ProgressDialog.show(MonitActivity.this, "Please wait", "Connecting");
        }
        @Override
        protected Void doInBackground(Void... params) {
            try {
                if (mmSocket == null || !mIsBluetoothConnected) {
                    mmSocket = mDevice.createRfcommSocketToServiceRecord(mDeviceUUID);
                    BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
                    mmSocket.connect();
                }
            } catch (IOException e) {
                e.printStackTrace();
                mConnectSuccessful = false;
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            if (!mConnectSuccessful) {
                Toast.makeText(getApplicationContext(), "Could not connect to device. Is it a Serial device? Also check if the UUID is correct in the settings", Toast.LENGTH_LONG).show();
                //finish();
            } else {
                Toast.makeText(getApplicationContext(), "Connected to device", Toast.LENGTH_SHORT).show();
                mIsBluetoothConnected = true;
                mReadThread = new ReadInput();
            }
            progressDialog.dismiss();
        }
    }



    private class createDatabaseTask extends AsyncTask<Void, Void, CursorAdapter> {

        @Override
        protected CursorAdapter doInBackground(Void... voids) {
            CursorAdapter resultsAdapter = null;
            try {
                SQLiteOpenHelper ecgDatabaseHelper = new ECGDatabaseHelper(MonitActivity.this);
                SQLiteDatabase db = ecgDatabaseHelper.getWritableDatabase();
                resultsCursor = db.query("RESULTS",
                        new String[] { "_id", "DATA0", "DATA1", "DATA2"},
                        null,
                        null,
                        null, null, null);
                resultsAdapter = new SimpleCursorAdapter(MonitActivity.this,
                        android.R.layout.simple_list_item_1,
                        resultsCursor,
                        new String[]{"DATA0"},
                        new int[]{android.R.id.text1}, 0);

                Log.d(TAG, "createDatabaseTask się melduje");
            }
            catch (SQLiteException e) {
                Toast.makeText(MonitActivity.this, "Baza danych jest niedostępna", Toast.LENGTH_SHORT).show();
                Log.d("TAG", "Problem z bazą");
            }
            return resultsAdapter;
        }

        @Override
        protected void onPostExecute(CursorAdapter resultsAdapter) {
            //listResults.setAdapter(resultsAdapter);
        }
    }


    public void onSaveClicked(View view) {
        new UpdateDatabaseTask().execute();
        saveToTxtfile();
    }


    void saveToTxtfile() {
        try {
            String root = Environment.getExternalStoragePublicDirectory(DIRECTORY_DOWNLOADS).toString();
            File myFile = new File(root, "ecgResults.txt");
            myFile.createNewFile();
            FileOutputStream fOut = new FileOutputStream(myFile);
            OutputStreamWriter myOutWriter = new OutputStreamWriter(fOut);

            myOutWriter.append(mTxtReceive.getText());

            myOutWriter.close();
            fOut.close();
            Toast.makeText(getApplicationContext(), "Done writing to file 'ecgResults.txt' in the path Phone/Download",
                    Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(getApplicationContext(),e.getMessage(), Toast.LENGTH_SHORT)
                    .show();
            Log.d(TAG, "Brak uprawnien - brak dostepu");
        }
    }


    private class UpdateDatabaseTask extends AsyncTask<Void, Void, CursorAdapter>{
        ContentValues resultValues;

        int ostatniMohikanin;

        private int insertResults(SQLiteDatabase db) {
            String results_db = mTxtReceive.getText().toString();
            String[] splitted_results_db = results_db.split("\\r?\\n");
//            String[] splitted_results_db = results_db.split(",");
            String[][] splitted_channels_db = new String[splitted_results_db.length][3];

            Log.d(TAG, "Split po raz pierwszy się melduje");

            for (int z = 0; z < splitted_channels_db.length; z++) {
                splitted_channels_db[z] = splitted_results_db[z].split(",",3);
                //Log.d(TAG,splitted_channels[z][0]);
            }

//            for (int i = 0; i < splitted_results_db.length; i++) {
//                resultValues.put("DATA0", splitted_results_db[i]);
//                db.insert("RESULTS", null, resultValues);
//            }

            for (int i = 0; i < splitted_channels_db.length; i++) {
//                resultValues.put("DATA0", splitted_channels_db[i][0]);
//                db.insert("RESULTS", null, resultValues);
//                resultValues.put("DATA1", splitted_channels_db[i][1]);
//                db.insert("RESULTS", null, resultValues);
//                resultValues.put("DATA2", splitted_channels_db[i][2]);
//                db.insert("RESULTS", null, resultValues);
                resultValues.put("DATA0", splitted_channels_db[i][0]);
                resultValues.put("DATA1", splitted_channels_db[i][1]);
                resultValues.put("DATA2", splitted_channels_db[i][2]);
                db.insert("RESULTS", null, resultValues);
            }

            Log.d(TAG, "Dodanie do bazy się melduje");
            ostatniMohikanin = splitted_results_db.length;
            return ostatniMohikanin;
        }


        private int insertResultsAgain(SQLiteDatabase db, Integer the_last_element) {
            String results_db = mTxtReceive.getText().toString();
            String[] splitted_results_db = results_db.split("\\r?\\n");
//            String[] splitted_results_db = results_db.split(",");
            String[][] splitted_channels_db = new String[splitted_results_db.length][3];

            for (int z = the_last_element; z < splitted_channels_db.length; z++) {
                splitted_channels_db[z] = splitted_results_db[z].split(",",3);
                //Log.d(TAG,splitted_channels[z][0]);
            }

//            for (int i = the_last_element; i < splitted_results_db.length; i++) {
//                resultValues.put("DATA1", splitted_results_db[i]);
//                db.insert("RESULTS", null, resultValues);
//            }

            for (int i = the_last_element; i < splitted_channels_db.length; i++) {
//                resultValues.put("DATA0", splitted_channels_db[i][0]);
//                db.insert("RESULTS", null, resultValues);
//                resultValues.put("DATA1", splitted_channels_db[i][1]);
//                db.insert("RESULTS", null, resultValues);
//                resultValues.put("DATA2", splitted_channels_db[i][2]);
//                db.insert("RESULTS", null, resultValues);

                resultValues.put("DATA0", splitted_channels_db[i][0]);
                resultValues.put("DATA1", splitted_channels_db[i][1]);
                resultValues.put("DATA2", splitted_channels_db[i][2]);
                db.insert("RESULTS", null, resultValues);
            }


            ostatniMohikanin = splitted_results_db.length;
            return ostatniMohikanin;
        }

        protected void onPreExecute() {
        }

        protected CursorAdapter doInBackground(Void... voids) {

            resultValues = new ContentValues();
            CursorAdapter resultsAdapter = null;

            try {
                SQLiteOpenHelper ECGDatabaseHelper = new ECGDatabaseHelper(MonitActivity.this);
                SQLiteDatabase db = ECGDatabaseHelper.getWritableDatabase();

                if (!resultsCursor.moveToFirst() && !resultsCursor.moveToLast()) {
                    ostatniMohikanin = insertResults(db);
                    String t = Integer.toString(ostatniMohikanin);
                    Log.d(TAG, "Po raz pierwszy: Brak pierwszego punktu bazy, wrzuć dane");
                    //Log.d(TAG, t);
                } else {
                    String g = Integer.toString(theLastOne);
                    //Log.d(TAG, g);
                    ostatniMohikanin = insertResultsAgain(db, theLastOne);
                    Log.d(TAG, "Po raz kolejny: Mamy pierwszy punkt bazy, nic nie dodawaj ze starych danych");
                }

                resultsAdapter = new SimpleCursorAdapter(MonitActivity.this,
                        android.R.layout.simple_list_item_1,
                        resultsCursor,
                        new String[]{"DATA0"},
                        new int[]{android.R.id.text1}, 0);

            } catch (SQLiteException e) {
                Toast.makeText(MonitActivity.this, "Baza danych jest niedostępna", Toast.LENGTH_SHORT).show();
                Log.d("TAG", "Problem z bazą 2");
            }

            return resultsAdapter;
        }

        protected void onPostExecute(CursorAdapter resultsAdapter) {
            theLastOne = ostatniMohikanin;

            try {
                ECGDatabaseHelper ecg1DatabaseHelper = new ECGDatabaseHelper(MonitActivity.this);
                db = ecg1DatabaseHelper.getWritableDatabase();
                Cursor newCursor = db.query("RESULTS",
                        new String[] { "_id", "DATA0", "DATA1", "DATA2"}, null, null, null, null, null);
//                ListView listResults = findViewById(R.id.listview_results);
//                CursorAdapter adapter = (CursorAdapter) listResults.getAdapter();
//                adapter.changeCursor(newCursor);

                resultsCursor = newCursor;

            } catch (SQLiteException e) {
                Toast.makeText(MonitActivity.this, "Baza danych jest niedostępna", Toast.LENGTH_SHORT).show();
                Log.d("TAG", "Problem z bazą 3");
            }
        }
    }



    /**
     * Special {@link AdvancedLineAndPointRenderer.Formatter} that draws a line
     * that fades over time.  Designed to be used in conjunction with a circular buffer model.
     */
//    public static class MyFadeFormatter extends AdvancedLineAndPointRenderer.Formatter {
//
//        private int trailSize;
//
//        MyFadeFormatter(int trailSize) {
//            this.trailSize = trailSize;
//        }
//
//        @Override
//        public Paint getLinePaint(int thisIndex, int latestIndex, int seriesSize) {
//            // offset from the latest index:
//            int offset;
//            if(thisIndex > latestIndex) {
//                offset = latestIndex + (seriesSize - thisIndex);
//            } else {
//                offset =  latestIndex - thisIndex;
//            }
//
//            float scale = 255f / trailSize;
//            int alpha = (int) (255 - (offset * scale));
//            getLinePaint().setAlpha(alpha > 0 ? alpha : 0);
//            return getLinePaint();
//        }
//    }


}
