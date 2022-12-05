package usr.gwn27;

public class User_Data {
    public String username;
    public String password;
    public int games_played = 0;
    public int games_won = 0;
    public float percent_won = 0;
    public int latest_streak = 0;
    public int longest_streak = 0;
    public float average_guesses = 0;
    public String current_word_to_guess = "";

    public User_Data(String username, String password){
        this.username = username;
        this.password = password;
    }
}
