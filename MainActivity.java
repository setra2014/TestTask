package com.example.aleksey.testtask;

import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import java.util.concurrent.TimeUnit;


public class MainActivity extends AppCompatActivity implements SeekBar.OnSeekBarChangeListener {
    public Button getFirstAudio;
    public Button getSecondAudio;
    public ImageButton play;
    public MediaPlayer playerArr[] = new MediaPlayer[2];
    public TextView textForSeekBar;
    public SeekBar seekBar;
    final Handler handlerForToast = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Блок инициализации объектов интерфейса
        seekBar = (SeekBar)findViewById(R.id.seekBar);
        play = (ImageButton)findViewById(R.id.play);
        getFirstAudio = (Button)findViewById(R.id.getFirstAudio);
        getSecondAudio = (Button)findViewById(R.id.getSecondAudio);
        textForSeekBar = (TextView)findViewById(R.id.textForSeekBar);

        textForSeekBar.setText(String.valueOf(seekBar.getProgress()+2));

        seekBar.setOnSeekBarChangeListener(this);

        //Обработчик нажатия кнопки play
        play.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TaskPlayClick task = new TaskPlayClick();                   //создание отдельного потока для воспроизведения треков
                task.execute();
            }
        });

        //обработчики кнопок выбора треков
        getFirstAudio.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent getAudio = new Intent(Intent.ACTION_GET_CONTENT);
                getAudio.setType("audio/*");
                startActivityForResult(getAudio, 1);
            }
        });

        getSecondAudio.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent getAudio = new Intent(Intent.ACTION_GET_CONTENT);
                getAudio.setType("audio/*");
                startActivityForResult(getAudio,2 );
            }
        });

    }

    //переопределение метода возврата данных в нашу активность
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode==RESULT_OK){
            Uri path = data.getData();
            if(requestCode == 1) {                                  //если нажата кнопка firstAudio
                playerArr[0] = MediaPlayer.create(this, path);
            }
            else if (requestCode == 2){                             //если нажата кнопка secondAudio
                playerArr[1] = MediaPlayer.create(this, path);
            }
        }
    }

    //переопредление методов SeekBar'a
    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean state){
        textForSeekBar.setText(String.valueOf(seekBar.getProgress()+2));
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar){}

    @Override
    public void onStopTrackingTouch(SeekBar seekBar){}


    //отдельный поток для воспроизведения треков
    public class TaskPlayClick extends AsyncTask <Void, Void, Void>{
        //блокировка кнопок и SeekBar'a
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            play.setEnabled(false);
            getFirstAudio.setEnabled(false);
            getSecondAudio.setEnabled(false);
            seekBar.setEnabled(false);
        }

        //цикличное воспроизведение треков
        @Override
        protected Void doInBackground(Void...arg0){
            int i = 0;
            long sleepDuration;                                           //длительность ожидания воспроизведения следующего трека
            try {
                while (true) {
                    playerArr[i].start();
                    if (playerArr[i].getDuration() > (seekBar.getProgress() + 2) * 1000) {
                        sleepDuration = playerArr[i].getDuration() - seekBar.getProgress() * 1000;
                    }
                    else {                                               //если трек длится меньше заданного для кроссфейда времени,
                        sleepDuration = playerArr[i].getDuration();      //то следующий начнется сразу после окончания текущего
                    }

                    try {
                        TimeUnit.MILLISECONDS.sleep(sleepDuration);
                    } catch (InterruptedException e) {}

                    i = i == 1 ? 0 : 1;
                }
            }
            catch (NullPointerException e){
                //выводим сообщение об ошибке
                handlerForToast.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast toast = Toast.makeText(getApplicationContext(), "Вы не выбрали аудиодорожку!", Toast.LENGTH_SHORT);
                        toast.show();
                    }
                });
            }

            return null;
        }

        //разблокировка кнопок и SeekBar'a
        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            seekBar.setEnabled(true);
            play.setEnabled(true);
            getFirstAudio.setEnabled(true);
            getSecondAudio.setEnabled(true);
        }
    }
}
