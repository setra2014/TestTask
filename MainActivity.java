package com.example.aleksey.testtask;

import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
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
                TaskClickPlay task = new TaskClickPlay();                       //основной поток воспроизведения треков
                int CrossFade = (seekBar.getProgress()+2)*1000;                 //длительность склейки

                try {
                    int FirstAudioDuration = playerArr[0].getDuration();
                    int SecondAudioDuration = playerArr[1].getDuration();

                    //если треки слишком коротки, то выводится соответвующее сообщение
                    if (FirstAudioDuration > 2 * CrossFade && SecondAudioDuration > 2 * CrossFade) {
                        unlockInterface(false);
                        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                    }
                    else{
                        Toast toast = Toast.makeText(getApplicationContext(), "Аудиодорожки коротки!", Toast.LENGTH_SHORT);
                        toast.show();
                    }
                }
                catch (NullPointerException e) {
                    Toast toast = Toast.makeText(getApplicationContext(), "Аудиодорожка не выбрана", Toast.LENGTH_SHORT);
                    toast.show();
                    unlockInterface(true);
                }

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

    //основной поток, вызывающийся при нажатии на кнопку Play
    public class TaskClickPlay extends AsyncTask <Void, Void, Void>{
        @Override
        protected Void doInBackground(Void... arg0){
            TaskFirstAudio taskFirst;                                           //поток воспроизведения первого трека
            TaskSecondAudio taskSecond;                                         //поток воспроизведения второго трека
            int flag = 1;                                                       //флаг, нужный для определения того, в первый ли раз проигрывается цикл
            int sleepDuration = 1000 * (seekBar.getProgress() + 2);             //длительность ожидания для начала новой итерации цикла
            try {
                while (true) {
                    taskFirst = new TaskFirstAudio();
                    taskSecond = new TaskSecondAudio();

                    taskFirst.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, flag);

                    taskSecond.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);
                    try { TimeUnit.MILLISECONDS.sleep(sleepDuration); } catch (InterruptedException e) { }
                    flag = 0;
                    sleepDuration = 3000 * (seekBar.getProgress() + 2);
                }
            }catch (NullPointerException e){}
            return null;
        }
    }

    //поток воспроизведения первого трека
    public class TaskFirstAudio extends AsyncTask <Integer, Void, Void>{
        final int CrossFade = (seekBar.getProgress()+2) * 1000;                 //длительность склейки

        @Override
        protected Void doInBackground(Integer... flag){
            int sleepDuration = playerArr[0].getDuration() - (2-flag[0])*CrossFade;    //длительность ожидания завершения потока

            playerArr[0].start();
            if(flag[0] != 1) increaseVolume(0, CrossFade);                //если это не первое воспроизведение этого потока,
            else playerArr[0].setVolume(1, 1);              // то звук плавно увеличивается

            try{ TimeUnit.MILLISECONDS.sleep(sleepDuration);} catch (InterruptedException e) {}
            return null;
        }
        @Override
        protected void onPostExecute(Void result){
            new Thread(){
                @Override
                public void run(){
                    reduceVolume(0, CrossFade);                            //при завершении потока звук плавно затухает
                }
            }.start();
        }
    }

    //поток воспроизведения второго трека
    public class TaskSecondAudio extends AsyncTask <Void, Void, Void>{
        final int CrossFade = (seekBar.getProgress()+2) * 1000;                 //длительность склейки
        int sleepDuration = playerArr[1].getDuration() - 2*CrossFade;           //длительность ожидания завершения потока

        @Override
        protected Void doInBackground(Void... arg0){
            playerArr[1].start();
            increaseVolume(1, CrossFade);                                 //плавное увеличение звука
            try{ TimeUnit.MILLISECONDS.sleep(sleepDuration);} catch (InterruptedException e){}
            return null;
        }

        @Override
        protected void onPostExecute(Void result){
            new Thread(){
                @Override
                public void run(){
                    reduceVolume(1, CrossFade);                           //плавное затухание звука
                }
            }.start();
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
                TimeUnit.MILLISECONDS.sleep((CrossFade) / 10);
            } catch (InterruptedException e) { }
        }
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
}
