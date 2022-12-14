package usr.gwn27;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;

import java.io.*;

public class Json_Handler {
    JsonReader json_reader;
    FileWriter json_writer;

    public Json_Handler(){}

    public void write_to_json(String user_name, User_Data user) throws IOException {
        this.json_writer = new FileWriter("user_data/"+user_name+".json");
        new Gson().toJson(user, json_writer);
        json_writer.flush();
        json_writer.close();
    }

    public User_Data read_from_json(String user_name) throws IOException {
        this.json_reader = new JsonReader(new FileReader("user_data/"+user_name+".json"));
        User_Data user = new Gson().fromJson(json_reader, User_Data.class);
        json_reader.close();
        return user;
    }

    public void update_user_win(String user_name, String guesses) throws IOException {
        User_Data user = read_from_json(user_name);
        user.games_won++;
        user.update_distribution(guesses);
        user.percent_won = ((float)user.games_won/(float)user.games_played)*100;
        user.latest_streak++;
        if(user.latest_streak > user.longest_streak){
            user.longest_streak = user.latest_streak;
        }
        user.was_playing = false;
        user.current_word_to_guess = "";
        write_to_json(user_name, user);
    }

    public void update_user_defeat(String user_name) throws IOException {
        User_Data user = read_from_json(user_name);
        user.percent_won = ((float)user.games_won/(float)user.games_played)*100;
        user.latest_streak = 0;
        user.was_playing = false;
        user.current_word_to_guess = "";
        write_to_json(user_name, user);
    }

    public String get_playing_word(String user_name) throws IOException {
        User_Data user = read_from_json(user_name);
        return user.current_word_to_guess;
    }

    public String get_playing_number(String user_name) throws IOException {
        User_Data user = read_from_json(user_name);
        return user.current_word_number;
    }

    public boolean is_user_logged(String user_name) throws IOException {
        if(user_name.equals(Colors.RED.get_color_code()+"No_User"+Colors.RESET.get_color_code())){
            return false;
        }
        User_Data user = read_from_json(user_name);
        return user.is_logged;
    }

    public void set_user_logged(String user_name, boolean logged) throws IOException {
        User_Data user = read_from_json(user_name);
        user.is_logged = logged;
        write_to_json(user_name, user);
    }

    public void handle_play_disconnection(String user_name) throws IOException {
        User_Data user = read_from_json(user_name);
        if(user.was_playing){
            update_user_defeat(user_name);
        }
    }
}
