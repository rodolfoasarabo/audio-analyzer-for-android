package github.bewantbe.audio_analyzer_for_android;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.text.DecimalFormat;

import antonkozyriatskyi.circularprogressindicator.CircularProgressIndicator;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class GaugeActivity extends AppCompatActivity {

    @BindView(R.id.circularProgress)
    CircularProgressIndicator circularProgress;
    @BindView(R.id.view)
    View view;
    @BindView(R.id.txtValue)
    TextView txtValue;
    @BindView(R.id.txtValueDb)
    TextView txtValueDb;
    @BindView(R.id.btnCorda1)
    ToggleButton btnCorda1;
    @BindView(R.id.btnCorda2)
    ToggleButton btnCorda2;
    @BindView(R.id.btnCorda3)
    ToggleButton btnCorda3;
    @BindView(R.id.btnCorda4)
    ToggleButton btnCorda4;
    @BindView(R.id.btnCorda5)
    ToggleButton btnCorda5;
    @BindView(R.id.btnCorda6)
    ToggleButton btnCorda6;
    private int i = 0;

    double maxAmpDB;
    double maxAmpFreq;
    private double[] E =  new double[]{165,330,660,1320};
    private double[] A =  new double[]{110,220,440,880};
    private double[] D =  new double[]{146,292,584,1168};
    private double[] G =  new double[]{196,392,748,1586};
    private double[] B =  new double[]{247,494,988,1976};
    private double[] nota = E;

    SamplingLoop samplingThread = null;
    private AnalyzerParameters analyzerParameters = null;

    private static int MAX_PROGRESS = 1000;
    private boolean bSamplingPreparation = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gauge);
        ButterKnife.bind(this);

        circularProgress.setMaxProgress(MAX_PROGRESS);
        circularProgress.setCurrentProgress(0);
        circularProgress.setProgress(0, MAX_PROGRESS);
        circularProgress.setProgressStrokeWidthDp(10);

        Resources res = getResources();
        analyzerParameters = new AnalyzerParameters(res);
    }

    @Override
    protected void onResume() {
        super.onResume();
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);

        analyzerParameters.wndFuncName = sharedPref.getString("windowFunction", "Hanning");

        bSamplingPreparation = true;

        restartSampling(analyzerParameters);

    }

    @Override
    protected void onPause() {
        super.onPause();
        if (samplingThread != null){
            samplingThread.finish();
        }
    }

    private void restartSampling(final AnalyzerParameters analyzerParameters){
        if (samplingThread != null){
            samplingThread.finish();
            try{
                samplingThread.join();
            } catch (InterruptedException e){
                e.printStackTrace();
            }
            samplingThread = null;
        }

        if (!bSamplingPreparation)
            return;

        samplingThread = new SamplingLoop(this, analyzerParameters);
        samplingThread.start();
    }

    void updateRec() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                if (maxAmpDB > -55) {
                    selecionarFrequencia(nota);
                }
                txtValue.setText(String.format("%1$,.2fHz", maxAmpFreq));
                txtValueDb.setText(String.format("%1$,.2fdB", maxAmpDB));
            }
        });
    }

    private double closest(double[] numbers, double frequency) {
        double distance = Math.abs(numbers[0] - frequency);
        int idx = 0;
        for(int c = 1; c < numbers.length; c++){
            double cdistance = Math.abs(numbers[c] - frequency);
            if(cdistance < distance){
                idx = c;
                distance = cdistance;
            }
        }
        return numbers[idx];
    }


    @OnClick({R.id.btnCorda1, R.id.btnCorda2, R.id.btnCorda3, R.id.btnCorda4, R.id.btnCorda5, R.id.btnCorda6})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.btnCorda1:
                nota = E;
                btnCorda2.setChecked(false);
                btnCorda3.setChecked(false);
                btnCorda4.setChecked(false);
                btnCorda5.setChecked(false);
                btnCorda6.setChecked(false);
                break;
            case R.id.btnCorda2:
                nota = B;
                btnCorda1.setChecked(false);
                btnCorda3.setChecked(false);
                btnCorda4.setChecked(false);
                btnCorda5.setChecked(false);
                btnCorda6.setChecked(false);
                break;
            case R.id.btnCorda3:
                nota = G;
                btnCorda1.setChecked(false);
                btnCorda2.setChecked(false);
                btnCorda4.setChecked(false);
                btnCorda5.setChecked(false);
                btnCorda6.setChecked(false);
                break;
            case R.id.btnCorda4:
                nota = D;
                btnCorda1.setChecked(false);
                btnCorda2.setChecked(false);
                btnCorda3.setChecked(false);
                btnCorda5.setChecked(false);
                btnCorda6.setChecked(false);
                break;
            case R.id.btnCorda5:
                nota = A;
                btnCorda1.setChecked(false);
                btnCorda2.setChecked(false);
                btnCorda3.setChecked(false);
                btnCorda4.setChecked(false);
                btnCorda6.setChecked(false);
                break;
            case R.id.btnCorda6:
                nota = E;
                btnCorda1.setChecked(false);
                btnCorda2.setChecked(false);
                btnCorda3.setChecked(false);
                btnCorda4.setChecked(false);
                btnCorda5.setChecked(false);
                break;
        }
    }

    private void selecionarFrequencia(double[] nota){
        if (nota != null) {
            double proximo = closest(nota, maxAmpFreq);
            Log.e("test", "Frequencia atual: " + (int) maxAmpFreq + "\nProximo: " + proximo);

            if (proximo - maxAmpFreq > 3 || proximo - maxAmpFreq < -3) {
                circularProgress.setProgress((int) (proximo - maxAmpFreq), MAX_PROGRESS);
                circularProgress.setDotColor(ContextCompat.getColor(this, android.R.color.holo_red_dark));
            }
            else {
                circularProgress.setProgress(0, MAX_PROGRESS);
                circularProgress.setDotColor(ContextCompat.getColor(this, android.R.color.holo_green_dark));
            }

            Log.e("Progress", String.valueOf(circularProgress.getProgress()));
        }
    }
}
