package usr.gwn27;

enum Colors {
    RESET("\u001B[0m"),
    RED("\u001B[31m"),
    GREEN_BACK("\u001B[42m"),
    WHITE_BACK("\u001B[47m"),
    YELLOW_BACK("\u001B[43m");

    private final String color_code;


    Colors(String color_code) {
        this.color_code = color_code;
    }
}
