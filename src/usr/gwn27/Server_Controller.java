package usr.gwn27;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.atomic.AtomicBoolean;

public class Server_Controller implements Runnable{
    private final Thread word_selector_thread;
    private final AtomicBoolean stop_server;

    public Server_Controller(Thread word_selector_thread, AtomicBoolean stop_server) {
        this.word_selector_thread = word_selector_thread;
        this.stop_server = stop_server;
    }

    @Override
    public void run() {
        Runtime.getRuntime().addShutdownHook(new Thread(this::force_log_out));
        try(BufferedReader command_stream = new BufferedReader(new InputStreamReader(System.in))){
            String read;
            while(!stop_server.get()){
                System.out.print("Inserisci comando: ");
                read = command_stream.readLine().trim();
                System.out.println();
                switch (read) {
                    case "shutdown":
                        force_log_out();
                        stop_server.set(true);
                        break;
                    case "help":
                        System.out.println(help_list());
                        break;
                    case "word":
                        System.out.println("Parola corrente: " + Word_Selector.get_current_word());
                        break;
                    default:
                        System.out.println("Comando sconosciuto. Prova 'help' per avere una lista dei comandi.");
                        break;
                }
            }
            word_selector_thread.interrupt();
            Thread.sleep(1000);
        } catch (IOException e) {
            System.out.println("Errore di avvio shell dei comandi.");
            System.exit(0);
        } catch (InterruptedException ignored) {
        }
    }

    private void force_log_out(){
        Json_Handler handler = new Json_Handler();
        File[] json_list = new File("user_data/").listFiles();
        if(json_list == null){
            return;
        }
        try{
            for (File user_file : json_list) {
                if (user_file.getName().contains(".json")) {
                    handler.set_user_logged(user_file.getName().replace(".json", ""), false);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String help_list(){
        return "help - Mostra una lista dei comandi disponibili\n" +
                "shutdown - Termina le attività del server dopo aver soddisfatto le richieste rimanenti\n"+
                "word - Mostra sul terminale la parola da indovinare al momento";
    }
}
