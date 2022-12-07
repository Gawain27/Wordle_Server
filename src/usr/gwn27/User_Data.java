package usr.gwn27;
import java.util.LinkedHashMap;

public class User_Data {
    public String username;
    public String password;
    public int games_played = 0;
    public int games_won = 0;
    public float percent_won = 0;
    public int latest_streak = 0;
    public int longest_streak = 0;
    public String current_word_to_guess = "";
    public LinkedHashMap<Integer, Integer> guess_distribution = new LinkedHashMap<>();

    public User_Data(String username, String password){
        this.username = username;
        this.password = password;
        for(int i = 1; i <= 12; i++){
            guess_distribution.putIfAbsent(i, 0);
        }
    }

    public void update_distribution(String guesses) {
        int guess_no = Integer.parseInt(guesses);
        guess_distribution.put(guess_no+1, guess_distribution.get(guess_no+1)+1);
    }

    public String get_distribution(){
        return guess_distribution.toString().replaceAll(",", "");
    }
}
