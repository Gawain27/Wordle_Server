package usr.gwn27;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;


public class Word_Selector implements Runnable{
    private final long word_time;
    private final Read_Write_Lock file_lock;
    private static int word_number;
    private static String current_word;
    private final AtomicBoolean stop_server;

    public Word_Selector(long word_time, Read_Write_Lock file_lock, AtomicBoolean stop_server) {
        this.word_time = word_time;
        this.file_lock = file_lock;
        this.stop_server = stop_server;
        word_number = 0;
    }

    @Override
    public void run() {
        try {
            do{
                BufferedReader words_reader = new BufferedReader(new FileReader("words.txt"));
                int word_index = new Random().nextInt(30823);
                for(int i = 0; i < word_index; i++){
                    words_reader.readLine();
                }
                current_word = words_reader.readLine();
                word_number++;
                file_lock.lockWrite();
                new FileWriter("already_played.wordconf", false).close();
                file_lock.unlockWrite();
                //noinspection BusyWait
                Thread.sleep(word_time*1000);

            }while(!stop_server.get());
        } catch (IOException e) {
            System.out.println("Impossibile accedere al file delle parole");
        } catch (InterruptedException e) {
            System.out.println("Chiusura modulo nuova parola...");
        }
    }

    public static String get_current_word(){
        return current_word;
    }

    public static int get_word_number(){
        return word_number;
    }
}
