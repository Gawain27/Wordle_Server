package usr.gwn27;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class Server_Controller implements Runnable{
    @Override
    public void run() {
        try(BufferedReader command_stream = new BufferedReader(new InputStreamReader(System.in))){
            String read;
            while(true){
                read = command_stream.readLine().trim();
                if(read.equals("shutdown")){
                    Wordle_Server.stop_server = true;
                    break;
                }else if(read.equals("help")){
                    System.out.println(help_list());
                }else{
                    System.out.println("Unknown command - try 'help' for a list of commands");
                }
            }
            System.out.println("Shutting down...");
            Thread.sleep(1000);
        } catch (IOException e) {
            System.out.println("Error while trying to start server's command line - Shutting down...");
            System.exit(0);
        } catch (InterruptedException ignored) {

        }
    }

    private String help_list(){
        return "help - shows a list of available commands\n" +
                "shutdown - terminates all server activities after completing ongoing requests";
    }
}
