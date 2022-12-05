package usr.gwn27;

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;

public class Request_Evaluator {
    private final Server_Connection_Handler server_conn_handler;
    private final Read_Write_Lock file_lock;
    private final Json_Handler json_handler;

    public Request_Evaluator(Server_Connection_Handler server_conn_handler, Read_Write_Lock file_lock) {
        this.server_conn_handler = server_conn_handler;
        this.file_lock = file_lock;
        this.json_handler = new Json_Handler();
    }

    public boolean evaluate_command(String command_requested) {
        String[] command_args = command_requested.split(" ");
        try{
            switch(command_args[0]){
                case "register": register_user(command_args);
                    break;
                case "play": start_user_game(command_args);
                    break;
                case "login": login_user(command_args);
                    break;
                case "stats": get_user_stats(command_args);
                    break;
                case "guess": evaluate_user_guess(command_args);
                    break;
                case "share": forward_shared_game(command_args);
                    break;
                case "disconnect": return false;
                default:


            }
        }catch (IOException e){
            return false;
        }
        return true;
    }

    public void register_user(String[] command_args) throws IOException {
        File registered = new File("user_data/"+command_args[1]+".json");
        if(registered.createNewFile()){
            json_handler.write_to_json(command_args[1], new User_Data(command_args[1], command_args[2]));
            server_conn_handler.send_response(ByteBuffer.wrap("registration_success".getBytes()));
        }else{
            server_conn_handler.send_response(ByteBuffer.wrap("registration_failure".getBytes()));
        }
    }

    public void start_user_game(String[] command_args) throws IOException{
        if(!command_args[1].equals(Colors.RED+"No_User"+Colors.RESET)){
            try{
                //TODO: clean start_user_game
                String read;
                BufferedReader reader = new BufferedReader(new FileReader("already_played.wordconf"));
                file_lock.lockRead();
                while((read = reader.readLine()) != null){
                    if(read.equals(command_args[1])){
                        server_conn_handler.send_response(ByteBuffer.wrap("already_played".getBytes()));
                        file_lock.unlockRead();
                        return;
                    }
                }
                file_lock.unlockRead();
                file_lock.lockWrite();
                FileWriter writer = new FileWriter("already_played.wordconf", true);
                writer.write(command_args[1]+"\n");
                file_lock.unlockWrite();
                server_conn_handler.send_response(ByteBuffer.wrap("play_started".getBytes()));

                User_Data playing_user = json_handler.read_from_json(command_args[1]);
                playing_user.games_played++;
                playing_user.current_word_to_guess = Word_Selector.get_current_word();
                json_handler.write_to_json(command_args[1], playing_user);

            }catch (InterruptedException e){
                server_conn_handler.send_response(ByteBuffer.wrap("checks_error".getBytes()));
            }
        }else{
            server_conn_handler.send_response(ByteBuffer.wrap("not_logged".getBytes()));
        }
    }

    public void login_user(String[] command_args) throws IOException {
        if(command_args[3].equals(Colors.RED+"No_User"+Colors.RESET)){
            if(new File("user_data/"+command_args[1]+".json").exists()){
                User_Data logging = json_handler.read_from_json(command_args[1]);
                if(logging.password.equals(command_args[2])){
                    server_conn_handler.send_response(ByteBuffer.wrap("login_success".getBytes()));
                }
            }
            server_conn_handler.send_response(ByteBuffer.wrap("no_match".getBytes()));
        }else{
            server_conn_handler.send_response(ByteBuffer.wrap("already_logged".getBytes()));
        }
    }

    public void get_user_stats(String[] command_args) throws IOException {
        if(!command_args[1].equals(Colors.RED+"No_User"+Colors.RESET)){
            User_Data user = json_handler.read_from_json(command_args[1]);
            String user_stats = "Partite giocate: "+user.games_played+"\nPercentuale vittorie: "+user.percent_won
                    +"%\nSerie vittorie attuale: "+user.latest_streak+"\nSerie vittorie migliore: "+user.longest_streak
                    +"\nTentativi per vittoria: "+user.average_guesses;
            server_conn_handler.send_response(ByteBuffer.wrap(("success,"+user_stats).getBytes()));
        }else{
            server_conn_handler.send_response(ByteBuffer.wrap("not_logged".getBytes()));
        }
    }

    public void evaluate_user_guess(String[] command_args) throws IOException {
        String read;
        BufferedReader reader = new BufferedReader(new FileReader("words.txt"));
        while((read = reader.readLine()) != null){
            if(read.equals(command_args[1])){
                String word_to_guess = json_handler.get_playing_word(command_args[3]), response;
                if(command_args[1].equals(word_to_guess)){
                    response = "guessed " + build_next_hint(command_args[1], word_to_guess);
                    json_handler.update_user_win(command_args[3], command_args[2]);
                }else if(Integer.parseInt(command_args[2]) == 11){
                    response = "defeat "+ build_next_hint(command_args[1], word_to_guess);
                    json_handler.update_user_defeat(command_args[3], command_args[2]);
                }else{
                    response = "valid " + build_next_hint(command_args[1], word_to_guess);
                }
                server_conn_handler.send_response(ByteBuffer.wrap(response.getBytes()));
                reader.close();
                return;
            }
        }
        reader.close();
        server_conn_handler.send_response(ByteBuffer.wrap("invalid".getBytes()));
    }

    private String build_next_hint(String guess, String word_to_guess){
        StringBuilder next_hint = new StringBuilder();
        next_hint.append("-");
        for(int i = 0; i < 10; i++){
            if(guess.charAt(i) == word_to_guess.charAt(i)){
                next_hint.append(Colors.GREEN_BACK).append(guess.charAt(i)).append(Colors.RESET).append("-");
            }else if(word_to_guess.contains(guess.charAt(i)+"")){
                next_hint.append(Colors.YELLOW_BACK).append(guess.charAt(i)).append(Colors.RESET).append("-");
            }else{
                next_hint.append(Colors.WHITE_BACK).append(guess.charAt(i)).append(Colors.RESET).append("-");
            }
        }
        return next_hint.toString();
    }

    public void forward_shared_game(String[] command_args) throws IOException {
        try{
            DatagramSocket socket = new DatagramSocket();
            InetAddress group = InetAddress.getByName(Wordle_Server.group_ip.toString());
            String to_share = "Wordle "+Word_Selector.get_word_number()+" "+command_args[1]+"/12\n"+command_args[2];
            DatagramPacket packet = new DatagramPacket(to_share.getBytes(),0, to_share.length(), group, Wordle_Server.server_port.get());
            socket.send(packet);
            socket.close();
            server_conn_handler.send_response(ByteBuffer.wrap("shared_success".getBytes()));
        } catch (SocketException | UnknownHostException e) {
            server_conn_handler.send_response(ByteBuffer.wrap("shared_failure".getBytes()));
        }
    }
}
