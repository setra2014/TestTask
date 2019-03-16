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
            unlockInterface(false);
        }

        //цикличное воспроизведение треков
        @Override
        protected Void doInBackground(Void...arg0){
            try {
                //воспроизведение первого трека без увеличесния громкости в начале
                TaskBegin taskBegin = new TaskBegin();
                taskBegin.execute();

                //цикл воспроизведения треков с кроссфейдом
                while (true) {
                    TaskSecondAudio taskSecond = new TaskSecondAudio();
                    taskSecond.execute();

                    TaskFirstAudio taskFirst = new TaskFirstAudio();
                    taskFirst.execute();
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
            unlockInterface(true);
        }
    }

    //поток для воспроизведения 1 трека без увеличения громкости в начале
    public class TaskBegin extends AsyncTask <Void, Void, Void> {

        final int CrossFade = (seekBar.getProgress()+2) * 1000;             //длительность кроссфейда

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            playerArr[0].start();
            playerArr[0].setVolume(1,1);
            int sleepDuration = playerArr[0].getDuration() - CrossFade;
            try { TimeUnit.MILLISECONDS.sleep(sleepDuration); } catch (InterruptedException e) {}

        }

        @Override
        protected Void doInBackground(Void... arg0) {
            reduceVolume(0, CrossFade);
            return null;
        }
    }

    //поток для воспроизведения 1 трека с кроссфейдом
    public class TaskFirstAudio extends AsyncTask <Void, Void, Void> {

        final int CrossFade = (seekBar.getProgress()+2) * 1000;         //длительность кроссфейда

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            playerArr[0].setVolume(0, 0);
            playerArr[0].start();
            increaseVolume(0, CrossFade);                           //плавное увеличение громкости
            int sleepDuration = playerArr[0].getDuration() - 2*CrossFade;
            try {
                TimeUnit.MILLISECONDS.sleep(sleepDuration);
            } catch (InterruptedException e) {}
        }

        @Override
        protected Void doInBackground(Void... arg0) {
            reduceVolume(0, CrossFade);                             //плавное уменьшение громкости
            return null;
        }
    }

    //поток для воспроизведения 2 трека
    public class TaskSecondAudio extends AsyncTask <Void, Void, Void>{
        final int CrossFade = (seekBar.getProgress()+2)*1000;             //длительность кроссфейда

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            playerArr[1].setVolume(0,0);
            playerArr[1].start();
            increaseVolume(1, CrossFade);                           //плавное увеличение громкости
            long sleepDuration = playerArr[1].getDuration() - 2*CrossFade;
            try{ TimeUnit.MILLISECONDS.sleep(sleepDuration);} catch (InterruptedException e){}
        }

        @Override
        protected Void doInBackground(Void... arg0) {
            reduceVolume(1, CrossFade);                             //плавное уменьшение громкости
            return null;
        }
    }

    //метод разблокировки/блокировки элементов интерфейса
    public void unlockInterface(boolean bool){
        seekBar.setEnabled(bool);
        play.setEnabled(bool);
        getFirstAudio.setEnabled(bool);
        getSecondAudio.setEnabled(bool);
    }

    //метод плавного увеличения громкости
    public void increaseVolume(int index, int CrossFade){
        for (float i = 0.1f ; i <= 1; i+=0.1f) {
            playerArr[index].setVolume(i, i);
            try {
                TimeUnit.MILLISECONDS.sleep(CrossFade / 10);
            } catch (InterruptedException e) { }
        }
    }

    //метод плавного уменьшения громкости
    public void reduceVolume(int index, int CrossFade){
        for (float i = 0.9f; i >= 0; i-=0.1f) {
            playerArr[index].setVolume(i, i);
            try {
                TimeUnit.MILLISECONDS.sleep(CrossFade / 10);
            } catch (InterruptedException e) { }
        }
    }
}
