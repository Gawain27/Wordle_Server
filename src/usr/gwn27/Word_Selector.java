package usr.gwn27;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Random;
import java.util.stream.Stream;

public class Word_Selector implements Runnable{
    private final long word_time;
    private final Read_Write_Lock file_lock;
    private static int word_number;
    private static String current_word;

    public Word_Selector(long word_time, Read_Write_Lock file_lock) {
        this.word_time = word_time;
        this.file_lock = file_lock;
        word_number = 0;
    }

    @Override
    public void run() {
        try {
            do{
                Stream<String> lines = Files.lines(Paths.get("words.txt"));
                int word_index = new Random().nextInt(30823);
                if(lines.skip(word_index).findFirst().isPresent()){
                    current_word = lines.skip(word_index).findFirst().get();
                    lines.close();
                    word_number++;
                    file_lock.lockWrite();
                    new FileWriter("already_played.wordconf", false).close();
                    file_lock.unlockRead();
                    //noinspection BusyWait
                    Thread.sleep(word_time);
                }else{
                    lines.close();
                }
            }while(!Wordle_Server.stop_server);
        } catch (IOException e) {
            System.out.println("Impossibile accedere al file delle parole");
            System.exit(0);
        } catch (InterruptedException e) {
            //TODO: can force quit?
            System.out.println("Errore di accesso al file .wordconf");
            System.exit(0);
        }
    }

    public static String get_current_word(){
        return current_word;
    }

    public static int get_word_number(){
        return word_number;
    }
}
