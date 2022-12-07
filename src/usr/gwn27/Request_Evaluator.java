package usr.gwn27;

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Request_Evaluator {
    private final Server_Connection_Handler server_conn_handler;
    private final Read_Write_Lock file_lock;
    private final int server_port;
    private final String group_ip;
    private final SelectionKey channel_key;
    private final Json_Handler json_handler;

    public Request_Evaluator(Server_Connection_Handler server_conn_handler, Read_Write_Lock file_lock, int server_port, String group_ip, SelectionKey channel_key) {
        this.server_conn_handler = server_conn_handler;
        this.file_lock = file_lock;
        this.server_port = server_port;
        this.group_ip = group_ip;
        this.channel_key = channel_key;
        this.json_handler = new Json_Handler();
    }

    public boolean evaluate_command(String command_requested) {
        String[] command_args = command_requested.split(" ");
        try{
            switch(command_args[0]){
                case "register": register_user(command_args); break;
                case "play": start_user_game(command_args); break;
                case "login": login_user(command_args); break;
                case "stats": get_user_stats(command_args); break;
                case "guess": evaluate_user_guess(command_args); break;
                case "share": forward_shared_game(command_args); break;
                case "logout": logout_user(command_args); break;
                case "disconnect": return disconnect_client();
                case "play_disconnect": play_disconnect(command_args[1]);
            }
        }catch (IOException | NoSuchAlgorithmException e){
            return false;
        }
        return true;
    }

    private void logout_user(String[] command_args) throws IOException {
        if(json_handler.is_user_logged(command_args[1])){
            json_handler.set_user_logged(command_args[1], false);
            channel_key.attach(null);
            server_conn_handler.send_response(ByteBuffer.wrap("logout_success".getBytes()));
        }else{
            server_conn_handler.send_response(ByteBuffer.wrap("logout_failure".getBytes()));
        }
    }

    public void register_user(String[] command_args) throws IOException, NoSuchAlgorithmException {
        File registered = new File("user_data/"+command_args[1]+".json");
        if(registered.createNewFile()){
            MessageDigest encrypter = MessageDigest.getInstance("MD5");
            encrypter.update(command_args[2].getBytes());
            json_handler.write_to_json(command_args[1], new User_Data(command_args[1], new String(encrypter.digest())));
            server_conn_handler.send_response(ByteBuffer.wrap("registration_success".getBytes()));
        }else{
            server_conn_handler.send_response(ByteBuffer.wrap("registration_failure".getBytes()));
        }
    }

    public void start_user_game(String[] command_args) throws IOException{
        if(!command_args[1].equals(Colors.RED.get_color_code()+"No_User"+Colors.RESET.get_color_code())
            && json_handler.is_user_logged(command_args[1])){
            try{

                file_lock.lockRead();
                String read;
                BufferedReader reader = new BufferedReader(new FileReader("already_played.wordconf"));
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
                writer.flush();
                writer.close();
                file_lock.unlockWrite();

                server_conn_handler.send_response(ByteBuffer.wrap("play_started".getBytes()));
                User_Data playing_user = json_handler.read_from_json(command_args[1]);
                playing_user.games_played++;
                playing_user.was_playing = true;
                playing_user.current_word_to_guess = Word_Selector.get_current_word();
                playing_user.current_word_number = Word_Selector.get_word_number()+"";
                json_handler.write_to_json(command_args[1], playing_user);

            }catch (InterruptedException e){
                server_conn_handler.send_response(ByteBuffer.wrap("checks_error".getBytes()));
            }
        }else{
            server_conn_handler.send_response(ByteBuffer.wrap("not_logged".getBytes()));
        }
    }

    public void login_user(String[] command_args) throws IOException, NoSuchAlgorithmException {
        if(command_args[3].equals(Colors.RED.get_color_code()+"No_User"+Colors.RESET.get_color_code())){
            if(new File("user_data/"+command_args[1]+".json").exists()){
                User_Data logging = json_handler.read_from_json(command_args[1]);
                if(!json_handler.is_user_logged(command_args[1])){
                    MessageDigest encrypter = MessageDigest.getInstance("MD5");
                    encrypter.update(command_args[2].getBytes());
                    if(logging.password.equals(new String(encrypter.digest()))){
                        json_handler.set_user_logged(command_args[1], true);
                        ((StringBuilder) channel_key.attachment()).setLength(0);
                        ((StringBuilder) channel_key.attachment()).append(command_args[1]);
                        server_conn_handler.send_response(ByteBuffer.wrap("login_success".getBytes()));
                        return;
                    }
                }else{
                    server_conn_handler.send_response(ByteBuffer.wrap("already_occupied".getBytes()));
                }
            }
            server_conn_handler.send_response(ByteBuffer.wrap("no_match".getBytes()));
        }else{
            server_conn_handler.send_response(ByteBuffer.wrap("already_logged".getBytes()));
        }
    }

    public void get_user_stats(String[] command_args) throws IOException {
        if(!command_args[1].equals(Colors.RED.get_color_code()+"No_User"+Colors.RESET.get_color_code())
            && json_handler.is_user_logged(command_args[1])){
            User_Data user = json_handler.read_from_json(command_args[1]);
            String user_stats = "Partite giocate: "+user.games_played+"\nPercentuale vittorie: "+user.percent_won
                    +"%\nSerie vittorie attuale: "+user.latest_streak+"\nSerie vittorie migliore: "+user.longest_streak
                    +"\nDistribuzione tentativi: "+user.get_distribution();
            server_conn_handler.send_response(ByteBuffer.wrap(("success,"+user_stats).getBytes()));
        }else{
            server_conn_handler.send_response(ByteBuffer.wrap("not_logged".getBytes()));
        }
    }

    public void evaluate_user_guess(String[] command_args) throws IOException {
        String read;
        if(json_handler.is_user_logged(command_args[3])){
            BufferedReader reader = new BufferedReader(new FileReader("words.txt"));
            while((read = reader.readLine()) != null){
                if(read.equals(command_args[1])){
                    String word_to_guess = json_handler.get_playing_word(command_args[3]), response;
                    if(command_args[1].equals(word_to_guess)){
                        response = "guessed " + build_next_hint(command_args[1], word_to_guess);
                        json_handler.update_user_win(command_args[3], command_args[2]);
                    }else if(Integer.parseInt(command_args[2]) == 11){
                        response = "defeat "+ build_next_hint(command_args[1], word_to_guess);
                        json_handler.update_user_defeat(command_args[3]);
                    }else{
                        response = "valid " + build_next_hint(command_args[1], word_to_guess);
                    }
                    server_conn_handler.send_response(ByteBuffer.wrap(response.getBytes()));
                    reader.close();
                    return;
                }
            }
            reader.close();
        }
        server_conn_handler.send_response(ByteBuffer.wrap("invalid".getBytes()));
    }

    private String build_next_hint(String guess, String word_to_guess){
        StringBuilder next_hint = new StringBuilder();
        next_hint.append("-");
        for(int i = 0; i < 10; i++){
            if(guess.charAt(i) == word_to_guess.charAt(i)){
                next_hint.append(Colors.GREEN_BACK.get_color_code()).append("\u205F").append(guess.charAt(i))
                        .append("\u205F").append(Colors.RESET.get_color_code()).append("-");
            }else if(word_to_guess.contains(guess.charAt(i)+"")){
                next_hint.append(Colors.YELLOW_BACK.get_color_code()).append("\u205F").append(guess.charAt(i))
                        .append("\u205F").append(Colors.RESET.get_color_code()).append("-");
            }else{
                next_hint.append(Colors.WHITE_BACK.get_color_code()).append("\u205F").append(guess.charAt(i))
                        .append("\u205F").append(Colors.RESET.get_color_code()).append("-");
            }
        }
        return next_hint.toString();
    }

    public void forward_shared_game(String[] command_args) throws IOException {
        try{
            DatagramSocket socket = new DatagramSocket();
            InetAddress group = InetAddress.getByName(group_ip);
            System.out.println("SHARING "+json_handler.get_playing_number(command_args[3]));
            String to_share = "Wordle "+json_handler.get_playing_number(command_args[3])+" "+command_args[1]+"/12\n"+command_args[2];
            DatagramPacket packet = new DatagramPacket(to_share.getBytes(),0, to_share.getBytes().length, group, server_port);
            socket.send(packet);
            socket.close();
            server_conn_handler.send_response(ByteBuffer.wrap("shared_success".getBytes()));
        } catch (SocketException | UnknownHostException e) {
            server_conn_handler.send_response(ByteBuffer.wrap("shared_failure".getBytes()));
        }
    }

    public boolean disconnect_client() throws IOException {
        server_conn_handler.send_response(ByteBuffer.wrap("close_connection".getBytes()));
        return false;
    }

    public void play_disconnect(String user_name) throws IOException {
        json_handler.update_user_defeat(user_name);
        server_conn_handler.send_response(ByteBuffer.wrap("disconnecting".getBytes()));
    }
}
